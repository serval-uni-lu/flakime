package lu.uni.serval.instrumentation.strategies.vocabulary;

import lu.uni.serval.data.Project;
import lu.uni.serval.data.TestClass;
import lu.uni.serval.data.TestMethod;
import lu.uni.serval.instrumentation.strategies.Strategy;
import org.apache.commons.lang3.ArrayUtils;
import org.deeplearning4j.nn.modelimport.keras.preprocessing.text.KerasTokenizer;
import org.deeplearning4j.nn.modelimport.keras.preprocessing.text.TokenizerMode;
import org.javatuples.Pair;
import weka.core.*;


import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class VocabularyStrategy implements Strategy {
    private Model model;
    private KerasTokenizer tokenizer;
    private Instances trainingInstances;


    @Override
    public void preProcess(Project p) throws Exception {
        preProcessBuildModel(p);
        //TODO Selection between preProcessBuildModel() and preProcessLoadModel()
    }

    public void preProcessBuildModel(Project p) throws Exception {
//        System.out.println("Entered preProcess()");
        final InputStream dataSource = VocabularyStrategy.class.getClassLoader().getResourceAsStream("data/vocabulary.json");
        final TrainingData trainingData = new TrainingData(dataSource);
        Set<String> additionalTrainingText = new HashSet<>();
        for(TestClass testClass: p){
            for(TestMethod testMethod: testClass){
                testMethod.getName();
                File f = testMethod.getSourceCodeFile();
                additionalTrainingText.addAll(this.getTestMethodBodyText(f,testMethod).values());
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

    public void preProcessLoadModel(Project p) throws Exception {
        //TODO Create/Load tokenizer

        //TODO feature vector

        //TODO Create the corresponding instances

        //TODO Load model from file
        this.model = new Model("path/to/model");

    }

    @Override
    public String getProbabilityFunction(TestMethod test, int lineNumber) {
        double probability =0.0;
        try {
            Map<Integer,String> methodBodyText = this.getTestMethodBodyText(test.getSourceCodeFile(),test);
            Instance instance = this.createSingleInstance(getTextBodyToLine(methodBodyText,lineNumber),0,this.tokenizer);
            instance.setDataset(this.trainingInstances);
            probability = this.model.classify(instance);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return String.valueOf(probability);
    }

    @Override
    public void postProcess() {

    }

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
     * @throws IOException
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

    private Instances createInstances(KerasTokenizer tokenizer,int numTrainInstances,String[] featureTrain,List<Integer> labelTrain){
        Instances trainInstances = createEmptyInstances(tokenizer,numTrainInstances);

        for (int i=0; i< numTrainInstances;i++){
            Instance instance = createSingleInstance(featureTrain[i], labelTrain.get(i),tokenizer );
            instance.setDataset(trainInstances);
            trainInstances.add(instance);
        }

        return trainInstances;
    }

    private Instance createSingleInstance(String methodBody, double label, KerasTokenizer tokenizer){
        String[] str = new String[1];
        str[0] = methodBody;
        double[] featureVector = tokenizer.textsToMatrix(str, TokenizerMode.COUNT).toDoubleVector();
        featureVector = Arrays.copyOf(featureVector,featureVector.length+1);
        featureVector[featureVector.length-1] = label;
        SparseInstance sparseInstance = new SparseInstance(1.0, featureVector);
        return sparseInstance;
    }

    private Instances createEmptyInstances(KerasTokenizer tokenizer, int numTrainInstances){
        Map<Integer,String> stringIndexes = tokenizer.getIndexWord();

        Attribute labelAttribute = new Attribute("label_flakime___");

        ArrayList<Attribute> featuresList = (ArrayList<Attribute>) stringIndexes.values().stream().map(Attribute::new).collect(Collectors.toList());
        featuresList.add(labelAttribute);
        Instances trainInstances = new Instances("trainData", featuresList , numTrainInstances);
        trainInstances.setClass(labelAttribute);

        return trainInstances;
    }

    private KerasTokenizer createTokenizer(String[] trainingVocabulary,String[] additonalVocabulary){
        String[] concat = ArrayUtils.addAll(trainingVocabulary,additonalVocabulary);
        KerasTokenizer tokenizer = new KerasTokenizer();
        tokenizer.fitOnTexts(concat);
        return tokenizer;
    }
}
