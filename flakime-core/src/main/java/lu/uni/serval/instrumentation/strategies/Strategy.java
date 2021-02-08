package lu.uni.serval.instrumentation.strategies;

import lu.uni.serval.data.TestMethod;

public interface Strategy {
    void preProcess() throws Exception;
    String getProbabilityFunction(TestMethod test, int lineNumber);
    void postProcess();
}
