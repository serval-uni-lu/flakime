package lu.uni.serval.instrumentation.strategies;

import javafx.util.Pair;
import lu.uni.serval.data.Project;
import lu.uni.serval.data.TestMethod;

import java.util.Map;
import java.util.Properties;

public interface Strategy {
    void preProcess(Project p) throws Exception;
    String getProbabilityFunction(TestMethod test, int lineNumber);
    void postProcess();
}
