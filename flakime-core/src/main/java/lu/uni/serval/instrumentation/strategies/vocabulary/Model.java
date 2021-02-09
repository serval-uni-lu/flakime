package lu.uni.serval.instrumentation.strategies.vocabulary;

import org.deeplearning4j.nn.modelimport.keras.preprocessing.text.KerasTokenizer;
import org.deeplearning4j.nn.modelimport.keras.preprocessing.text.TokenizerMode;
import org.javatuples.Pair;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import weka.classifiers.Evaluation;
import weka.classifiers.trees.RandomForest;
import weka.core.*;
import weka.core.converters.ArffSaver;
import weka.core.converters.ConverterUtils;

import java.io.File;
import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Collectors;

public class Model {

    private TrainingData trainingData;
    private Set<String> additionalTrainingText;
    private String result;


    public String getResult() {
        return result;

    }

    public Model(TrainingData trainingData, Set<String> additionalTrainingText) throws Exception {
        this.trainingData = trainingData;
        this.additionalTrainingText = additionalTrainingText;

        List<Integer> y_train = this.trainingData.getEntries().stream().map(entry -> entry.label).collect(Collectors.toList());
        KerasTokenizer tokenizer = new KerasTokenizer();

        String[] dataTrain = this.trainingData.getEntries().stream().map(entry -> entry.body).toArray(String[]::new);
        String[] additionalTrain = additionalTrainingText.toArray(new String[0]);

        System.out.printf("[%d] Fit on texts%n",System.currentTimeMillis());
        tokenizer.fitOnTexts(dataTrain); // should be fitted on ALL data

        System.out.printf("[%d] Text to Matrix%n",System.currentTimeMillis());

//        INDArray matrix = tokenizer.textsToMatrix(dataTrain, TokenizerMode.COUNT); // should be counted on trainData only
        Map<Integer,String> stringIndexes = tokenizer.getIndexWord();
//        ArrayList<Attribute> attributeListFeatures = new ArrayList<>();
//        ArrayList<Attribute> attributeListAll = new ArrayList<>();


        Attribute labelAttribute = new Attribute("label_clazz");
//        stringIndexes.forEach((index,string)->{
//            Attribute a = new Attribute(string);
//            attributeListFeatures.add(a);
//            attributeListAll.add(a);
//        });
//        attributeListAll.add(labelAttribute);
        ArrayList<Attribute> featuresList = (ArrayList<Attribute>) stringIndexes.values().stream().map(Attribute::new).collect(Collectors.toList());
        featuresList.add(labelAttribute);
        Instances trainInstances = new Instances("trainData", featuresList , y_train.size());

        trainInstances.setClass(labelAttribute);
        System.out.printf("[%d] Creating instances%n",System.currentTimeMillis());
        Instance newInstance;
        double[] featureVector;
        for(int i=0;i<y_train.size();i++){
            String[] str = new String[1];
            str[0] = dataTrain[i];
            featureVector = tokenizer.textsToMatrix(str,TokenizerMode.COUNT).toDoubleVector();
            featureVector = Arrays.copyOf(featureVector,featureVector.length+1);
            featureVector[featureVector.length-1] = y_train.get(i);
            newInstance = new SparseInstance(1.0,featureVector);
            newInstance.setDataset(trainInstances);
            trainInstances.add(newInstance);
//            System.out.println("["+i+"]");
        }

//        System.out.printf("[%d] Saving dataset%n",System.currentTimeMillis());
//        ArffSaver saver = new ArffSaver();
//        saver.setInstances(trainInstances);
//        saver.setFile(new File("test.arff"));
//        saver.writeBatch();

        int nbtrees = 5;
        int random_state = 0;

        RandomForest forest=new RandomForest();
        forest.setNumIterations(nbtrees);
        forest.setSeed(random_state);
        System.out.printf("[%d] Building classifier%n",System.currentTimeMillis());
        forest.setDebug(true);
        forest.buildClassifier(trainInstances);

        System.out.printf("[%d] Evaluate classifier%n",System.currentTimeMillis());
        ArrayList<Integer> res = new ArrayList<>();
        int score = 0;
        for(Instance ins : trainInstances){
            double classValue = ins.classValue();
            double prediction = forest.classifyInstance(ins) >= 0.5 ? 1.0 : 0.0;
            System.out.println(classValue +" : "+prediction);
            score = score + (prediction == classValue ? 1 : 0);
        }

        System.out.println("Accuracy: "+score+" "+trainInstances.size()+" "+(float)(score/trainInstances.size()));

        Evaluation eval = new Evaluation(trainInstances);
        eval.evaluateModel(forest, trainInstances);
        this.result = eval.toSummaryString();
        System.out.println(this.result);
    }

    public static <K, V> K getKey(Map<K, V> map, V value) {
        return map.entrySet()
                .stream()
                .filter(entry -> value.equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .findFirst().get();
    }

    /**
     * TODO: Generate the model that will do the same what guillaume did in python
     * Note: The data class is already started and should load the json that Guillaume used for his model
     * This class should do the same but in Weka (dependency already present in the pom)
     *
     * Guillaume code:
     *
     *     from keras.preprocessing.text import Tokenizer
     *     from sklearn.ensemble import RandomForestClassifier
     *     from sklearn.metrics import precision_score
     *     from sklearn.metrics import matthews_corrcoef, recall_score, roc_auc_score
     *     from sklearn.model_selection import train_test_split
     *
     *     # Load Data
     *     datasetPath = sys.argv[1]
     *     data = pd.read_json(datasetPath)
     *
     *     # Build training and test set, carefully stratifying the sets to keep a correct distribution of each class
     *     data_train, data_test = train_test_split(data, test_size=0.3, stratify=data['Label'])
     *
     *     # Building Tokenizer and Vocabulary
     *     tokenizer = Tokenizer(lower=True)
     *     tokenizer.fit_on_texts(data['Body'].values)
     *     print("\nVocabulary size: ", len(tokenizer.word_index) + 1)
     *
     *     # Building X_train, y_train, X_test, y_test
     *     X_train = tokenizer.texts_to_matrix(data_train['Body'].values, mode='count')
     *     y_train = data_train['Label'].values
     *
     *     X_test = tokenizer.texts_to_matrix(data_test['Body'].values, mode='count')
     *     y_test = data_test['Label'].values
     *
     *     # Random Forest Model
     *     classifier = RandomForestClassifier(n_estimators = 10, random_state = 0, verbose=2)
     *
     *     # Fit the regressor with X_train and y_train
     *     classifier.fit(X_train, y_train)
     *
     *     # Prediction
     *     prediction_train = classifier.predict(X_train)
     *     prediction_test = classifier.predict(X_test)
     */
}
