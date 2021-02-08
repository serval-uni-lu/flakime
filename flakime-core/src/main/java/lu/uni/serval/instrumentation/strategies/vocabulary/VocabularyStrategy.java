package lu.uni.serval.instrumentation.strategies.vocabulary;

import lu.uni.serval.data.TestMethod;
import lu.uni.serval.instrumentation.strategies.Strategy;

import java.io.InputStream;

public class VocabularyStrategy implements Strategy {
    @Override
    public void preProcess() throws Exception {
        final InputStream dataSource = VocabularyStrategy.class.getClassLoader().getResourceAsStream("data/vocabulary.json");
        final TrainingData data = new TrainingData(dataSource);
    }

    @Override
    public String getProbabilityFunction(TestMethod test, int lineNumber) {
        return "Math.random()";
    }

    @Override
    public void postProcess() {

    }
}
