package lu.uni.serval.instrumentation.strategies;

import lu.uni.serval.model.TestMethod;

public class BernoulliStrategy implements Strategy{
    @Override
    public double computeProbability(TestMethod test) {
        return Math.random();
    }
}
