package lu.uni.serval.instrumentation.strategies.vocabulary;

import java.io.InputStream;
import org.apache.maven.plugin.logging.Log;
import weka.classifiers.trees.RandomForest;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializationHelper;


/**
 * Representation of an ml model. It holds the classifier object and the method allowing to (de)-serialize
 * the classifier as well as training.
 */
public class Model {

    private final RandomForest randomForest;
    private final Log logger;
    private boolean trainNeededFlag = true;


    /**
     * Constructs a model based on a pre-trained RandomForest
     *
     * @param modelPath The path to the pre-trained RandomForest
     */
    public Model(Log logger, String modelPath) throws Exception {
        RandomForest randomForest1;
        this.logger = logger;
        InputStream inputStream = null;


        randomForest1 = this.load(modelPath);
        this.logger.info("Successfully loader model");
        this.randomForest = randomForest1;
        this.trainNeededFlag = false;
    }

    /**
     * Constructs a model instance with specific number of trees and threads.
     *
     * @param logger   The logger attached to the running instance.
     * @param nTrees   The number of trees for the Random forest.
     * @param nThreads The number of threads used for the training.
     */
    public Model(Log logger, int nTrees, int nThreads) {
        this.logger = logger;
        RandomForest forest = new RandomForest();
        forest.setNumIterations(nTrees);
        forest.setSeed(0);
        forest.setDebug(true);
        forest.setNumExecutionSlots(nThreads);
        this.randomForest = forest;
    }

    /**
     * Non parameterized constructor that builds a RandomForest from scratch with default parameters
     * Number of trees : 100
     * random_state : 0
     * number of threads for training : 12
     *
     * @param logger The logger attached to the running instance.
     */
    public Model(Log logger) {
        this.logger = logger;
        int nbtrees = 100;
        int random_state = 0;
        int numberOfThreads = 12;

        RandomForest forest = new RandomForest();
        forest.setNumIterations(nbtrees);
        forest.setSeed(random_state);
        forest.setDebug(true);
        forest.setNumExecutionSlots(numberOfThreads);

        this.randomForest = forest;

    }

    /**
     * Method that will fit the classifier to the given Instances.
     *
     * @param trainingInstances Instances representing the training set.
     * @throws Exception Thrown by classifier build
     */
    public void trainModel(Instances trainingInstances) throws Exception {

        float startTime = System.nanoTime();
        this.randomForest.buildClassifier(trainingInstances);
        this.trainNeededFlag = false;
        float endTime = System.nanoTime();

        this.logger.info(String
                .format("%nRFC took %.1f seconds to train%n", (endTime - startTime) / 1000000000));

    }

    /**
     * Method to return the probability of flakiness of the given instance
     *
     * @param instance The instance to be labeled
     * @return A probability between 0.0 and 1.0
     * @throws Exception Throw if model needs to be trained
     */
    public double classify(Instance instance) throws Exception {
        if (this.trainNeededFlag) {
            throw new IllegalStateException("The model is not fitted");
        }

        return this.randomForest.classifyInstance(instance);
    }

    /**
     * Method to load a random forest from a serialized file
     *
     * @param path The path to the serialized RandomForest
     * @return Deserialized {@code weka.RandomForest} instance
     * @throws Exception Thrown if the random forest can not be deserialized.
     */
    public RandomForest load(String path) throws Exception {
        //FIXME stackoverflow error is thrown if the stack size is to low (must be manually set by -Xss10m)
        return (RandomForest) SerializationHelper.read(path);
    }

    /**
     * Serialize the Random forest instance and save to a path
     *
     * @param path The path to the randomforest file
     * @throws Exception Thrown if the file would not be written
     */
    public void save(String path) throws Exception {
        SerializationHelper.write(path, this.randomForest);
    }
}
