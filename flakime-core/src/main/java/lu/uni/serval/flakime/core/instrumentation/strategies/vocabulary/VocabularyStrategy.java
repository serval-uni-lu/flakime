package lu.uni.serval.flakime.core.instrumentation.strategies.vocabulary;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
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

public class VocabularyStrategy implements Strategy {
    private final Logger logger;
    private int nTrees;
    private int nThreads;
    private Model model;
    private String pathToModel;
    private boolean trainModel;
    private Map<Integer, Double> probabilitiesPerStatement;
    private final Model.Implementation modelImplementation = Model.Implementation.WEKA;

    public VocabularyStrategy(Logger logger) {
        this.logger = logger;
    }

    /**
     * Vocabulary strategy preProcess entry point that will trigger either the building of the Random forest model or loading from file system.
     *
     * @param project The project to run the Strategy on
     * @throws Exception Thrown if buildModel failed
     */
    @Override
    public void preProcess(final Project project) throws Exception {
            final InputStream dataSource = VocabularyStrategy.class.getClassLoader().getResourceAsStream("data/vocabulary.json");
            final TrainingData trainingData = new TrainingData(dataSource);
            final Set<String> additionalTrainingText = new HashSet<>();

            for (TestClass testClass : project) {
                for (TestMethod testMethod : testClass) {
                    final File f = testMethod.getSourceCodeFile();
                    additionalTrainingText.addAll(this.getTestMethodBodyText(f, testMethod).values());
                }
            }

        if (trainModel) {
            this.model = ModelFactory.create(this.modelImplementation, this.logger, this.nTrees, this.nThreads);
            this.model.setData(trainingData, additionalTrainingText);
            this.model.train();
            this.model.save(this.pathToModel);
        } else {
            this.model = ModelFactory.load(this.modelImplementation, this.logger, this.pathToModel);
            this.model.setData(trainingData, additionalTrainingText);
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
    public double getTestFlakinessProbability(TestMethod test, int lineNumber,double flakeRate) {
        double probability = Optional.ofNullable(this.probabilitiesPerStatement.get(lineNumber)).orElse(0.0)*flakeRate;
        logger.info(String.format("[Test: %s][line: %d][proba: %.3f]",test.getName(),lineNumber,probability));
        return probability;
    }

    /**
     * Computes the overall test flakiness probability.
     *
     * @param test The targeted test to extract the flakiness probability from
     * @return The probability of being flaky
     */
    @Override
    public double getTestFlakinessProbability(TestMethod test,double flakeRate) {

        double testFlakinessProbability;

        try {
            final Map<Integer, String> methodBodyText = this.getTestMethodBodyText(test.getSourceCodeFile(), test);

            if(methodBodyText.isEmpty()){
                return 0.0;
            }

            final String completeBody = methodBodyText.values().stream()
                    .reduce((a, b) -> a + " " + b)
                    .get();

            testFlakinessProbability = this.model.computeProbability(completeBody); //0.15
            computeStatementProbability(test,testFlakinessProbability);
        }
        catch (NullPointerException npe){
            npe.printStackTrace();
            testFlakinessProbability = 0.0;
        }
        catch (Exception e) {
            this.logger.error(String.format("Failed to compute test probability, default to 0.0 for test '%s': %s",
                    test.getName(),
                    e.getMessage()
            ));

            testFlakinessProbability = 0.0;
        }

        return testFlakinessProbability*flakeRate;
    }

    /**
     * Method that compute the probability of each code blocks of the test method
     *
     * @param test The test method to extract the code blocks and compute flakiness probability
     * @throws IOException,Exception if the TestMethod source file could not be read or if an error occurs during prediction
     */
    private void computeStatementProbability(TestMethod test,double testProbability) throws Exception {
        double totalProbabilities = 0.0;

        this.probabilitiesPerStatement = new HashMap<>();
        double statementProbability;

        final Map<Integer, String> methodBodyText = this.getTestMethodBodyText(test.getSourceCodeFile(), test);

        for (Integer statementNum : test.getStatementLineNumbers()) {
            final String bodyToLine = getTextBodyToLine(methodBodyText, statementNum);
            statementProbability = this.model.computeProbability(bodyToLine); //proba de flakiness until linenumber

            this.probabilitiesPerStatement.put(statementNum, statementProbability);

            totalProbabilities += statementProbability;
        }

        double aggregateProbability = 0.0;

        for (Integer statementNum : test.getStatementLineNumbers()) {
            double unNormalizedP = this.probabilitiesPerStatement.get(statementNum); //proba de flakiness until linenumber
            double statementPnormalized = unNormalizedP / totalProbabilities; //Proportion of the block proba wrt overall sum of proba
            aggregateProbability += statementPnormalized;
            this.probabilitiesPerStatement.put(statementNum, aggregateProbability*testProbability);
        }
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
