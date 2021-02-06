package lu.uni.serval.instrumentation.strategies;

import lu.uni.serval.data.TestMethod;

public interface Strategy {
    double computeProbability(TestMethod test, int lineNumber);
}
