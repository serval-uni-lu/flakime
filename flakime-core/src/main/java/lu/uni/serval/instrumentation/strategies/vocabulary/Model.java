package lu.uni.serval.instrumentation.strategies.vocabulary;

import org.deeplearning4j.nn.modelimport.keras.preprocessing.text.KerasTokenizer;
import org.deeplearning4j.nn.modelimport.keras.preprocessing.text.TokenizerMode;
import org.javatuples.Pair;
import org.nd4j.linalg.api.ndarray.INDArray;
import weka.classifiers.Evaluation;
import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils;

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

        List<String> x_train = this.trainingData.getEntries().stream().map(entry -> entry.body).collect(Collectors.toList());
        List<Integer> y_train = this.trainingData.getEntries().stream().map(entry -> entry.label).collect(Collectors.toList());
        KerasTokenizer tokenizer = new KerasTokenizer();

        String[] dataTrain = x_train.toArray(new String[0]);
        String[] additionalTrain = additionalTrainingText.toArray(new String[0]);
        tokenizer.fitOnTexts(dataTrain); // should be fitted on ALL data
        INDArray matrix = tokenizer.textsToMatrix(dataTrain, TokenizerMode.COUNT); // should be counted on trainData only
        Map<Integer,String> stringIndexes = tokenizer.getIndexWord();
        ArrayList<Attribute> attributeList = new ArrayList<>();

        stringIndexes.forEach((index,string)->{
            attributeList.add(new Attribute(string));
        });

        long sizeOfFeatureVector = matrix.shape()[1];
        long numberOfInstances = matrix.shape()[0];
        Instances trainInstances = new Instances("trainData",attributeList, (int) numberOfInstances);
        Attribute labelAttribute = new Attribute("label");
        trainInstances.setClass(labelAttribute);
        for(int i=0;i<x_train.size();i++){

            System.out.println("Index: "+i);
            Instance ins = new DenseInstance((int) sizeOfFeatureVector);
            ins.setDataset(trainInstances);
            double[] matrixRow = matrix.getRow(i).toDoubleVector();
            ins.copy(matrixRow);
            ins.setValue(labelAttribute,y_train.get(i));
        }

        int nbtrees = 100;
        int random_state = 0;

        RandomForest forest=new RandomForest();
        forest.setNumIterations(nbtrees);
        forest.setSeed(random_state);
        System.out.println("RFC Training started");
        forest.buildClassifier(trainInstances);

        Evaluation eval = new Evaluation(trainInstances);
        eval.evaluateModel(forest, trainInstances);
        this.result = eval.toSummaryString();
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
