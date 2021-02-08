package lu.uni.serval.instrumentation.strategies.bernoulli;

import lu.uni.serval.data.TestMethod;
import lu.uni.serval.instrumentation.strategies.Strategy;

public class BernoulliStrategy implements Strategy {
    @Override
    public void preProcess() {

    }

    @Override
    public String getProbabilityFunction(TestMethod test, int lineNumber) {
        return String.format("(1 - (1 - Math.pow(Math.random(), (double)%d)))", test.getStatementLineNumbers().size());
    }

    @Override
    public void postProcess() {

    }
}
