package lu.uni.serval.instrumentation.strategies;

import lu.uni.serval.model.TestMethod;

public interface Strategy {
    double computeProbability(TestMethod test);
}
