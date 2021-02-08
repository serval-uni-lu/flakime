package lu.uni.serval.instrumentation.strategies;

import lu.uni.serval.data.Project;
import lu.uni.serval.data.TestMethod;

public interface Strategy {
    void preProcess(Project p) throws Exception;
    String getProbabilityFunction(TestMethod test, int lineNumber);
    void postProcess();
}
