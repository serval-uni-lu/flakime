package lu.uni.serval.instrumentation.strategies;

import lu.uni.serval.data.TestMethod;

public class VocabularyStrategy implements Strategy{
    @Override
    public double computeProbability(TestMethod test, int lineNumber) {
        return Math.random();
    }
}
