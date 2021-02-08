package lu.uni.serval.instrumentation.strategies.vocabulary;

public class Model {
    /**
     * TODO: Generate the model that will do the same was what guillaume did in python
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
