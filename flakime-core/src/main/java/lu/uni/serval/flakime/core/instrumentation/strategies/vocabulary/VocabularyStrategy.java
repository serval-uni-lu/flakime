package lu.uni.serval.flakime.core.instrumentation.strategies.vocabulary;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javassist.bytecode.LineNumberAttribute;
import javassist.bytecode.analysis.ControlFlow;
import lu.uni.serval.flakime.core.data.Project;
import lu.uni.serval.flakime.core.data.TestClass;
import lu.uni.serval.flakime.core.data.TestMethod;
import lu.uni.serval.flakime.core.instrumentation.strategies.Strategy;
import lu.uni.serval.flakime.core.utils.Logger;
import org.apache.commons.lang3.ArrayUtils;
import org.deeplearning4j.nn.modelimport.keras.preprocessing.text.KerasTokenizer;
import org.deeplearning4j.nn.modelimport.keras.preprocessing.text.TokenizerMode;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;


public class VocabularyStrategy implements Strategy {
    private final Logger logger;
    private int nTrees;
    private int nThreads;
    private Model model;
    private KerasTokenizer tokenizer;
    private Instances trainingInstances;
    private String pathToModel;
    private boolean trainModel;
    private Map<Integer, Double> probabilitiesPerStatement;

    public VocabularyStrategy(Logger logger) {
        this.logger = logger;
    }

    /**
     * Vocabulary strategy preProcess entry point that will trigger either the building of the Random forest model or loading from file system.
     *
     * @param p The project to run the Strategy on
     * @throws Exception Thrown if buildModel failed
     */
    @Override
    public void preProcess(final Project p) throws Exception {
        final InputStream dataSource = VocabularyStrategy.class.getClassLoader().getResourceAsStream("data/vocabulary.json");
        final TrainingData trainingData = new TrainingData(dataSource);
        final Set<String> additionalTrainingText = new HashSet<>();

        for (TestClass testClass : p) {
            for (TestMethod testMethod : testClass) {
                final File f = testMethod.getSourceCodeFile();
                additionalTrainingText.addAll(this.getTestMethodBodyText(f, testMethod).values());
            }
        }

        final List<Integer> yTrain = trainingData.getEntries().stream()
                .map(entry -> entry.label)
                .collect(Collectors.toList());

        final String[] dataTrain = trainingData.getEntries().stream()
                .map(entry -> entry.body)
                .toArray(String[]::new);

        final String[] additionalTrain = additionalTrainingText.toArray(new String[0]);

        this.tokenizer = this.createTokenizer(dataTrain, additionalTrain);
        this.trainingInstances = this.createInstances(tokenizer, yTrain.size(), dataTrain, yTrain);

        if (trainModel) {
            this.model = new Model(this.logger, this.nTrees, this.nThreads);

            this.logger.info(String.format("Training Random forest Classifier on %d threads with %d trees",
                    this.nThreads,
                    this.nTrees
            ));

            this.model.trainModel(this.trainingInstances);
            String modelPath = "rfc_classifier";
            this.model.save(modelPath);
            logger.info(String.format("Model saved under [%s]", modelPath));

        } else {
            try{
                this.model = new Model(this.logger, this.pathToModel);
            }catch (StackOverflowError e){
                logger.error(String.format("Error occurred when training vocabulary model: [%s] %s",
                        e.getClass().getSimpleName(),
                        e.getMessage()
                ));
            }
        }
    }

    /**
     * Method to return the particular block of test body (identified by its starting line number) probability to be flaky based on the random Forest.
     *
     * @param test       The test method which the codeblcok is.
     * @param lineNumber The line number at which a particular statement is executed
     * @return Return the probability of the codeBlock to be flaky
     */
    @Override
    public String getProbabilityFunction(TestMethod test, int lineNumber) {
        return String.valueOf(this.getStatementFlakinessProbability(test, lineNumber));
    }


    /**
     * Computes the overall test flakiness probability.
     *
     * @param test The targeted test to extract the flakiness probability from
     * @return The probability of being flaky
     */
    @Override
    public double getTestFlakinessProbability(TestMethod test) {
        double testFlakinessProbability;

        try {
            final Map<Integer, String> methodBodyText = this.getTestMethodBodyText(test.getSourceCodeFile(), test);
            final String completeBody = methodBodyText.values().stream()
                    .reduce((a, b) -> a + " " + b)
                    .orElseThrow(() -> new IllegalStateException(String.format("Method body of %s is empty", test.getName())));

            final Instance bodyInstance = this.createSingleInstance(completeBody, 0, this.tokenizer);
            bodyInstance.setDataset(this.trainingInstances);
            testFlakinessProbability = this.model.classify(bodyInstance);
            computeStatementProbability(test);
        } catch (Exception e) {
            this.logger.error(String.format("Failed to compute test probability, default to 0.0 for test '%s': %s",
                    test.getName(),
                    e.getMessage()
            ));

            testFlakinessProbability = 0.0;
        }

        return testFlakinessProbability;
    }

    /**
     * Method that compute the probability of each code blocks of the test method
     *
     * @param test The test method to extract the code blocks and compute flakiness probability
     * @throws IOException,Exception if the TestMethod source file could not be read or if an error occurs during prediction
     */
    private void computeStatementProbability(TestMethod test) throws Exception {
        double totalProbabilities = 0.0;

        this.probabilitiesPerStatement = new HashMap<>();
        double statementProbability;

        final Map<Integer, String> methodBodyText = this.getTestMethodBodyText(test.getSourceCodeFile(), test);

        for (Integer statementNum : test.getStatementLineNumbers()) {
            final String stmtString = getTextBodyToLine(methodBodyText, statementNum);
            final Instance instance = this.createSingleInstance(stmtString, 0, this.tokenizer);
            instance.setDataset(this.trainingInstances);

            statementProbability = this.model.classify(instance);

            this.probabilitiesPerStatement.put(statementNum, statementProbability);

            totalProbabilities += statementProbability;
        }

        double aggregateProbability = 0.0;

        for (Integer statementNum : test.getStatementLineNumbers()) {
            double unNormalizedP = this.probabilitiesPerStatement.get(statementNum);
            double statementPnormalized = unNormalizedP / totalProbabilities;
            aggregateProbability += statementPnormalized;
            this.probabilitiesPerStatement.put(statementNum, aggregateProbability);
        }
    }

    /**
     * Returns the aggregated flakiness probability corresponding to the codeblock from start to {@code lineNumber}
     *
     * @param test       Not used
     * @param lineNumber The ending line of the code block to get the probability
     * @return The aggregated flakiness probability
     */
    @Override
    public double getStatementFlakinessProbability(TestMethod test, int lineNumber) {
        return Optional.ofNullable(this.probabilitiesPerStatement.get(lineNumber)).orElse(0.0);
    }

    @Override
    public void postProcess() {

    }

    /**
     * Method to get the string corresponding to the codeblock from the beginning of the method until a specific line number.
     *
     * @param methodBodyText The whole method body with the corresponding line number. Each entry corresponds to a a line number and its corresponding text.
     * @param lineNumber     The lineNumber until which the string should be recorded.
     * @return The resulting codeblock
     */
    public String getTextBodyToLine(Map<Integer, String> methodBodyText, int lineNumber) {
        final StringBuilder sb = new StringBuilder();

        for (Integer currentLine : methodBodyText.keySet()) {
            if(currentLine > lineNumber){
                break;
            }

            sb.append(methodBodyText.get(currentLine)).append(" ");
        }

        return sb.toString();
    }

    /**
     * Retrieve the text set corresponding to a method in the source file. The granularity is a {@code ControlFlow.Block} uniquely identified by its starting line number in the source code.
     *
     * @param f      the source file
     * @param method the corresponding {@code TestMethod} instance
     * @return The mapping between the lineNumber and the corresponding sourceText
     * @throws IOException thrown if the test file could not be read
     */
    public Map<Integer, String> getTestMethodBodyText(File f, TestMethod method) throws IOException {
        final Map<Integer, String> resultBody = new HashMap<>();
        final BufferedReader br = new BufferedReader(new FileReader(f));
        final List<String> sb = br.lines().collect(Collectors.toList());
        final LineNumberAttribute ainfo = (LineNumberAttribute) method.getCtMethod()
                .getMethodInfo()
                .getCodeAttribute()
                .getAttribute(LineNumberAttribute.tag);

        for (ControlFlow.Block b : method.getBlocks()) {
            int length = b.length();//The ByteCode size of the Basic block
            int pos = b.position(); //The position of the first byteCode instruction of the basic block
            int startLineNumber = ainfo.toLineNumber(pos); //The corresponding line number in the source code
            int endLineNumber = ainfo.toLineNumber(pos + length); //The First line of the next BasicBlock

            final StringBuilder stringBuilder = new StringBuilder();
            for (int ln = startLineNumber; ln <= endLineNumber; ln++) {
                stringBuilder.append(sb.get(ln - 1));
            }
            resultBody.put(startLineNumber, stringBuilder.toString());
        }

        return resultBody;
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
    private Instances createInstances(KerasTokenizer tokenizer, int numTrainInstances, String[] featureTrain, List<Integer> labelTrain) {
        final Instances trainInstances = createEmptyInstances(tokenizer, numTrainInstances);

        for (int i = 0; i < numTrainInstances; i++) {
            Instance instance = createSingleInstance(featureTrain[i], labelTrain.get(i), tokenizer);
            instance.setDataset(trainInstances);
            trainInstances.add(instance);
        }

        return trainInstances;
    }

    /**
     * Method to create a weka Instance.
     *
     * @param methodBody The test method body
     * @param label      The test label value
     * @param tokenizer  The kerasTokenizer from which the feature vector is extracted
     * @return The weka instance
     */
    private Instance createSingleInstance(String methodBody, double label, KerasTokenizer tokenizer) {
        String[] str = new String[1];
        str[0] = methodBody;
        double[] featureVector = tokenizer.textsToMatrix(str, TokenizerMode.COUNT).toDoubleVector();
        featureVector = Arrays.copyOf(featureVector, featureVector.length + 1);
        featureVector[featureVector.length - 1] = label;

        return new SparseInstance(1.0, featureVector);
    }

    /**
     * Method to initialize the weka Instances. This method creates the attributes corresponding to each token extracted
     * from the tokenizer and attach the attributes to the empty weka Instances
     *
     * @param tokenizer         Tokenizer fitted on training data
     * @param numTrainInstances Number on training samples
     * @return Empty initialized instances object
     */
    private Instances createEmptyInstances(KerasTokenizer tokenizer, int numTrainInstances) {
        Map<Integer, String> stringIndexes = tokenizer.getIndexWord();

        Attribute labelAttribute = new Attribute("label_flakime___");

        ArrayList<Attribute> featuresList = (ArrayList<Attribute>) stringIndexes.values().stream()
                .map(Attribute::new)
                .collect(Collectors.toList());

        featuresList.add(labelAttribute);

        Instances trainInstances = new Instances("trainData", featuresList, numTrainInstances);
        trainInstances.setClass(labelAttribute);

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
        KerasTokenizer tokenizer = new KerasTokenizer();
        tokenizer.fitOnTexts(concat);
        return tokenizer;
    }

    public void setnTrees(int nTrees) {
        this.nTrees = nTrees;
    }

    public void setnThreads(int nThreads) {
        this.nThreads = nThreads;
    }

    public void setPathToModel(String pathToModel) {
        this.pathToModel = pathToModel;
    }

    public void setTrainModel(boolean trainModel) {
        this.trainModel = trainModel;
    }
}