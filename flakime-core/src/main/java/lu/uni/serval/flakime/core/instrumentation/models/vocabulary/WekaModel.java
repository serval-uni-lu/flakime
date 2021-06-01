package lu.uni.serval.flakime.core.instrumentation.models.vocabulary;

import lu.uni.serval.flakime.core.utils.Logger;
import org.apache.commons.lang3.ArrayUtils;
import org.deeplearning4j.nn.modelimport.keras.preprocessing.text.KerasTokenizer;
import org.deeplearning4j.nn.modelimport.keras.preprocessing.text.TokenizerMode;
import weka.classifiers.trees.RandomForest;
import weka.core.*;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Representation of an ml model. It holds the classifier object and the method allowing to (de)-serialize
 * the classifier as well as training.
 */
public class WekaModel implements Model{
    private final RandomForest randomForest;
    private final Logger logger;
    private boolean trainNeededFlag = true;

    private KerasTokenizer tokenizer;
    private Instances trainingInstances;

    /**
     * Constructs a model based on a pre-trained RandomForest
     *
     * @param logger Reference to logger
     * @param randomForest pre-trained RandomForest
     */
    public WekaModel(Logger logger, RandomForest randomForest) {
        this.logger = logger;
        this.trainNeededFlag = false;
        this.randomForest = randomForest;
    }

    /**
     * Constructs a model instance with specific number of trees and threads.
     *
     * @param logger   The logger attached to the running instance.
     * @param nTrees   The number of trees for the Random forest.
     * @param nThreads The number of threads used for the training.
     */
    public WekaModel(Logger logger, int nTrees, int nThreads) {
        this.logger = logger;
        this.randomForest = new RandomForest();
        this.randomForest.setNumIterations(nTrees);
        this.randomForest.setSeed(0);
        this.randomForest.setDebug(true);
        this.randomForest.setNumExecutionSlots(nThreads);
    }

    @Override
    public void setData(TrainingData trainingData, Set<String> additionalTrainingText){
        final Integer[] yTrain = trainingData.getEntries().stream()
                .map(TrainingData.Entry::getLabel)
                .toArray(Integer[]::new);

        final String[] dataTrain = trainingData.getEntries().stream()
                .map(TrainingData.Entry::getBody)
                .toArray(String[]::new);

        final String[] additionalTrain = additionalTrainingText.toArray(new String[0]);

        this.tokenizer = this.createTokenizer(dataTrain, additionalTrain);
        this.trainingInstances = this.createInstances(tokenizer, yTrain.length, dataTrain, yTrain);
    }

    /**
     * Method that will fit the classifier to the given Instances.
     *
     * @throws Exception Thrown by classifier build
     */
    @Override
    public void train() throws Exception {
        this.logger.info(String.format("Training Random Forest Classifier on %d threads with %d trees...",
                this.randomForest.getNumExecutionSlots(),
                this.randomForest.getNumIterations()
        ));

        long startTime = System.nanoTime();
        this.randomForest.buildClassifier(trainingInstances);
        this.trainNeededFlag = false;
        long endTime = System.nanoTime();
        this.logger.info(String.format("Random Forest Classifier trained in %.1f seconds",
                (float)(endTime - startTime) / 1000000000
        ));
    }

    /**
     * Method to return the probability of flakiness of the given instance
     *
     * @param body The body of the test method for which to compute the probability
     * @return A probability between 0.0 and 1.0
     * @throws Exception Throw if model needs to be trained
     */
    @Override
    public double computeProbability(String body) throws Exception {
        if (this.trainNeededFlag) {
            throw new IllegalStateException("The model is not fitted");
        }

        final Instance instance = this.createSingleInstance(this.trainingInstances, body, 0, this.tokenizer);
        double[] dist = this.randomForest.distributionForInstance(instance);

        return dist[1];
    }

    /**
     * Serialize the Random forest instance and save to a path
     *
     * @param path The path to the randomforest file
     * @throws Exception Thrown if the file would not be written
     */
    @Override
    public void save(String path) throws Exception {
        SerializationHelper.write(path, this.randomForest);
    }

    /**
     * Method to load a random forest from a serialized file
     *
     * @param logger    The logger attached to the running instance.
     * @param path      The path to the serialized RandomForest
     * @return Deserialized {@code weka.RandomForest} instance
     * @throws Exception Thrown if the random forest can not be deserialized.
     */
    public static Model load(Logger logger, String path) throws Exception {
        final RandomForest randomForest;
        try{
            randomForest = (RandomForest) SerializationHelper.read(path);
            return new WekaModel(logger, randomForest);
        }catch (StackOverflowError stackOverflowError){
            logger.error("Stackoverflow due to insufficient stack size, please increment with -Xss10m");
            throw new IOException(stackOverflowError);
        }

    }

    /**
     * Method to create a not empty weka Instances object
     *
     * @param tokenizer         The fitted kerasTokenizer from which the feature vector are extracted
     * @param numTrainInstances The size of the training set
     * @param featureTrain      The body of each test method
     * @param labelTrain        The label of each training sample
     * @return The Training instances
     */
    private Instances createInstances(KerasTokenizer tokenizer, int numTrainInstances, String[] featureTrain, Integer[] labelTrain) {
        final Instances dataset = createEmptyInstances(tokenizer, numTrainInstances, labelTrain);

        for (int i = 0; i < numTrainInstances; i++) {
            Instance instance = createSingleInstance(dataset, featureTrain[i], labelTrain[i], tokenizer);
            dataset.add(instance);
        }

        return dataset;
    }

    /**
     * Method to create a weka Instance.
     *
     * @param dataset    Dataset
     * @param methodBody The test method body
     * @param label      The test label value
     * @param tokenizer  The kerasTokenizer from which the feature vector is extracted
     * @return The weka instance
     */
    private Instance createSingleInstance(Instances dataset, String methodBody, double label, KerasTokenizer tokenizer) {
        double[] featureVector = tokenizer.textsToMatrix(new String[]{methodBody}, TokenizerMode.COUNT).toDoubleVector();

        Instance instance = new SparseInstance(1.0, ArrayUtils.insert(0, featureVector, label));
        instance.setDataset(dataset);

        return instance;
    }

    /**
     * Method to initialize the weka Instances. This method creates the attributes corresponding to each token extracted
     * from the tokenizer and attach the attributes to the empty weka Instances
     *
     * @param tokenizer         Tokenizer fitted on training data
     * @param numTrainInstances Number on training samples
     * @return Empty initialized instances object
     */
    private Instances createEmptyInstances(KerasTokenizer tokenizer, int numTrainInstances, Integer[] labels) {
        Map<Integer, String> stringIndexes = tokenizer.getIndexWord();

        final List<String> uniqueLabels = Arrays.stream(labels)
                .distinct()
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.toList());

        final Attribute labelAttribute = new Attribute("flakime_label", uniqueLabels);

        final ArrayList<Attribute> featuresList = (ArrayList<Attribute>) stringIndexes.values().stream()
                .map(Attribute::new)
                .collect(Collectors.toList());

        featuresList.add(0, labelAttribute);

        final Instances trainInstances = new Instances("trainData", featuresList, numTrainInstances);
        trainInstances.setClass(labelAttribute);
        trainInstances.setClassIndex(0);

        return trainInstances;
    }

    /**
     * Method to create and fit a {@code KerasTokenizer}
     *
     * @param trainingVocabulary  The training testmethod bodies
     * @param additonalVocabulary The additional testmehtod bodies
     * @return The fitted kerasTokenizer
     */
    private KerasTokenizer createTokenizer(String[] trainingVocabulary, String[] additonalVocabulary) {
        String[] concat = ArrayUtils.addAll(trainingVocabulary, additonalVocabulary);
        KerasTokenizer kerasTokenizer = new KerasTokenizer();
        kerasTokenizer.fitOnTexts(concat);
        return kerasTokenizer;
    }
}
