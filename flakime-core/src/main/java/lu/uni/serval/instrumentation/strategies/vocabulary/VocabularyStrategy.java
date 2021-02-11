package lu.uni.serval.instrumentation.strategies.vocabulary;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import lu.uni.serval.data.Project;
import lu.uni.serval.data.TestClass;
import lu.uni.serval.data.TestMethod;
import lu.uni.serval.instrumentation.strategies.Strategy;

import org.apache.commons.lang3.ArrayUtils;
import org.deeplearning4j.nn.modelimport.keras.preprocessing.text.KerasTokenizer;
import org.deeplearning4j.nn.modelimport.keras.preprocessing.text.TokenizerMode;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;


public class VocabularyStrategy implements Strategy {
    private Model model;
    private KerasTokenizer tokenizer;
    private Instances trainingInstances;
    private final String pathToModel;
    private final boolean trainModel;

    public VocabularyStrategy(String pathToModel, boolean trainModel) {
        this.pathToModel = pathToModel;
        this.trainModel = trainModel;
    }

    /**
     * Vocabulary strategy preProcess entry point that will trigger either the building of the Random forest model or loading from file system.
     *
     * @param p The project to run the Strategy on
     * @throws Exception Thrown if buildModel failed
     */
    @Override
    public void preProcess(final Project p) throws Exception {

        if(trainModel)
            preProcessBuildModel(p);
        else
            preProcessLoadModel(p, pathToModel);

        //TODO Selection between preProcessBuildModel() and preProcessLoadModel()
    }

    /**
     * Pre-process method to build a model on the given project.
     * This method extract the vocabulary from each test methods, create a {@code KerasTokenizer} on the training data and computed additional data to extract the feature vectors
     * and finally train the model on the training instance.
     *
     * @param p The project to run the Strategy on
     * @throws Exception If the training data or method source file could not be read
     */
    public void preProcessBuildModel(final Project p) throws Exception {
//        System.out.println("Entered preProcess()");
        final InputStream dataSource = VocabularyStrategy.class.getClassLoader().getResourceAsStream("data/vocabulary.json");
        final TrainingData trainingData = new TrainingData(dataSource);
        Set<String> additionalTrainingText = new HashSet<>();
        for (TestClass testClass: p) {
            for (TestMethod testMethod: testClass) {
                testMethod.getName();
                File f = testMethod.getSourceCodeFile();
                additionalTrainingText.addAll(this.getTestMethodBodyText(f, testMethod).values());
            }
        }


        List<Integer> y_train = trainingData.getEntries().stream().map(entry -> entry.label).collect(Collectors.toList());


        String[] dataTrain = trainingData.getEntries().stream().map(entry -> entry.body).toArray(String[]::new);
        String[] additionalTrain = additionalTrainingText.toArray(new String[0]);

        //Initialize tokeniser on trainData + additional text body
        this.tokenizer = this.createTokenizer(dataTrain,additionalTrain);
        this.trainingInstances = this.createInstances(tokenizer,y_train.size(),dataTrain,y_train);


        this.model = new Model();
        this.model.trainModel(this.trainingInstances);
//        this.model = new Model(originalTrainingData,additionalMethodsBody);
    }

    public void preProcessLoadModel(Project p,String modelPath) throws Exception {
        //TODO Create/Load tokenizer

        //TODO feature vector

        //TODO Create the corresponding instances

        //TODO Load model from file
        this.model = new Model(modelPath);

    }

    /**
     * Method to return the a particular block of test body probability to be flaky based on the previously trained model.
     *
     * @param test The test method which the codeblcok is.
     * @param lineNumber The line number at which a particular statement is executed
     * @return Return the probability of the codeBlock to be flaky
     */
    @Override
    public String getProbabilityFunction(TestMethod test, int lineNumber) {
        double probability =0.0;
        try {
            Map<Integer,String> methodBodyText = this.getTestMethodBodyText(test.getSourceCodeFile(),test);
            Instance instance = this.createSingleInstance(getTextBodyToLine(methodBodyText,lineNumber),0,this.tokenizer);
            instance.setDataset(this.trainingInstances);
            probability = this.model.classify(instance);

        } catch (Exception e) {

            e.printStackTrace();
        }

        return String.valueOf(probability);
    }

    @Override
    public void postProcess() {

    }


    /**
     * Method to get the string corresponding to the codeblock from the beginning of the method until a specific line number.
     * @param methodBodyText The whole method body with the corresponding line number. Each entry corresponds to a a line number and its corresponding text.
     * @param lineNumber The lineNumber until which the string should be recorded.
     * @return The resulting codeblock
     */
    public String getTextBodyToLine(Map<Integer,String> methodBodyText, int lineNumber){
        StringBuilder sb = new StringBuilder();
        for(Integer ln:methodBodyText.keySet()){
            if(ln <= lineNumber){
                sb.append(methodBodyText.get(ln)).append(" ");
            }
        }
        return sb.toString();
    }

    /**
     * Retrieve the text set corresponding to a method in the source file
     *
     * @param f the source file
     * @param method the corresponding {@code TestMethod} instance
     * @return The mapping between the lineNumber and the corresponding sourceText
     * @throws IOException thrown if the test file could not be read
     */
    public Map<Integer, String> getTestMethodBodyText(File f,TestMethod method) throws IOException {
        List<String> sb = new ArrayList<>();
        Map<Integer, String> resultBody = new HashMap<>();
        BufferedReader br = new BufferedReader(new FileReader(f));
        String line;
        while ((line = br.readLine()) != null) {
            sb.add(line);
        }
        int size = method.getControlFlow().basicBlocks().length;

        for(int i = 0 ; i <size ; i++){
            int lineNumber = method.getCtMethod().getMethodInfo().getLineNumber(i);
            String res = sb.get(lineNumber);
            resultBody.put(lineNumber,res);
        }

        return resultBody;
    }

    /**
     * Method to create a not empty weka Instances object
     *
     * @param tokenizer The fitted kerasTokenizer from which the feature vector are extracted
     * @param numTrainInstances The size of the training set
     * @param featureTrain The body of each test method
     * @param labelTrain The label of each training sample
     * @return The Training instances
     */
    private Instances createInstances(KerasTokenizer tokenizer,int numTrainInstances,String[] featureTrain,List<Integer> labelTrain){
        Instances trainInstances = createEmptyInstances(tokenizer,numTrainInstances);

        for (int i=0; i< numTrainInstances;i++){
            Instance instance = createSingleInstance(featureTrain[i], labelTrain.get(i),tokenizer );
            instance.setDataset(trainInstances);
            trainInstances.add(instance);
        }

        return trainInstances;
    }

    /**
     * Method to create a weka Instance.
     * @param methodBody The test method body
     * @param label The test label value
     * @param tokenizer The kerasTokenizer from which the feature vector is extracted
     * @return The weka instance
     */
    private Instance createSingleInstance(String methodBody, double label, KerasTokenizer tokenizer){
        String[] str = new String[1];
        str[0] = methodBody;
        double[] featureVector = tokenizer.textsToMatrix(str, TokenizerMode.COUNT).toDoubleVector();
        featureVector = Arrays.copyOf(featureVector,featureVector.length+1);
        featureVector[featureVector.length-1] = label;
        SparseInstance sparseInstance = new SparseInstance(1.0, featureVector);
        return sparseInstance;
    }


    /**
     * Method to initialize the weka Instances. This method creates the attributes corresponding to each token extracted from the tokenizer and attach the attributes to the empty weka Instances
     * @param tokenizer Tokenizer fitted on training data
     * @param numTrainInstances Number on training samples
     * @return Empty initialized instances object
     */
    private Instances createEmptyInstances(KerasTokenizer tokenizer, int numTrainInstances){
        Map<Integer,String> stringIndexes = tokenizer.getIndexWord();

        Attribute labelAttribute = new Attribute("label_flakime___");

        ArrayList<Attribute> featuresList = (ArrayList<Attribute>) stringIndexes.values().stream().map(Attribute::new).collect(Collectors.toList());
        featuresList.add(labelAttribute);
        Instances trainInstances = new Instances("trainData", featuresList , numTrainInstances);
        trainInstances.setClass(labelAttribute);

        return trainInstances;
    }


    /**
     * Method to create and fit a KerasTokenizer
     * @param trainingVocabulary The training testmethod bodies
     * @param additonalVocabulary The additional testmehtod bodies
     * @return The fitted kerasTokenizer
     */
    private KerasTokenizer createTokenizer(String[] trainingVocabulary,String[] additonalVocabulary){
        String[] concat = ArrayUtils.addAll(trainingVocabulary,additonalVocabulary);
        KerasTokenizer tokenizer = new KerasTokenizer();
        tokenizer.fitOnTexts(concat);
        return tokenizer;
    }
}
