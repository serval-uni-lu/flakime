package lu.uni.serval.flakime.core.instrumentation.strategies.vocabulary;

import edu.stanford.nlp.classify.Dataset;
import edu.stanford.nlp.classify.LogisticClassifier;
import edu.stanford.nlp.classify.LogisticClassifierFactory;
import edu.stanford.nlp.ling.BasicDatum;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import lu.uni.serval.flakime.core.utils.Logger;

import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

public class StandfordModel implements Model{
    private final Logger logger;
    private final Dataset<Integer, String> dataset = new Dataset<>();
    private final StanfordCoreNLP pipeline;

    private LogisticClassifier<Integer, String> classifier = null;

    public StandfordModel(Logger logger) {
        this.logger = logger;

        final Properties props = new Properties();
        props.setProperty("annotators", "tokenize");

        this.pipeline = new StanfordCoreNLP(props);
    }

    @Override
    public void setData(TrainingData trainingData, Set<String> additionalTrainingText) {
        this.dataset.clear();

        for(TrainingData.Entry entry: trainingData.getEntries()){
            final List<String> tokens = getTokens(entry.getBody());
            dataset.add(new BasicDatum<>(tokens, entry.getLabel()));
        }
    }

    @Override
    public void train() {
        final LogisticClassifierFactory<Integer,String> factory = new LogisticClassifierFactory<>();

        this.logger.info("Training Standford Classifier...");

        long startTime = System.nanoTime();
        classifier = factory.trainClassifier(this.dataset);
        long endTime = System.nanoTime();

        this.logger.info(String.format("Standford Classifier trained in %.1f seconds [training accuracy = %.3f]",
                (float)(endTime - startTime) / 1000000000,
                classifier.evaluateAccuracy(this.dataset)
        ));
    }

    @Override
    public double computeProbability(String body) {
        return classifier.probabilityOf(getTokens(body), 1);
    }

    @Override
    public void save(String path) {
        throw new UnsupportedOperationException("Standford Model save is not available");
    }

    private List<String> getTokens(String text){
        return pipeline.processToCoreDocument(text).tokens().stream()
                .map(CoreLabel::toString)
                .collect(Collectors.toList());
    }
}
