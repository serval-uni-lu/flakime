package lu.uni.serval.instrumentation.strategies.bernoulli;

import lu.uni.serval.data.Project;
import lu.uni.serval.data.TestMethod;
import lu.uni.serval.instrumentation.strategies.Strategy;
import org.apache.maven.plugin.logging.Log;


/**
 * Strategy implementation that follows bernoulli distribution.
 * <p>
 * The probability of flaking after the execution of a statement is p. (probability of not flaking is 1-p)
 * Each statement has the same flakiness probability.
 * <p>
 * fixed p = 0.005
 * <p>
 * The overall test flakiness probability is given by P(test) = (1 - (1-p)^#numberOfStatements))
 * i.e. the probability success (thus to flake) after #numberOfStatements trials
 * <p>
 * The statement flakiness probability is given by P(stmt) = 1 - (1-p)^#executedStmts
 */
public class BernoulliStrategy implements Strategy {

    private final Log logger;
    private double flakinessProbability;

    public BernoulliStrategy(Log logger) {
        this.logger = logger;
    }


    @Override
    public void preProcess(Project p) {
        this.flakinessProbability = .005;
    }

    @Override
    public String getProbabilityFunction(TestMethod test, int lineNumber) {
        double testProbability = getTestFlakinessProbability(test);
        double statementProbability = getStatementFlakinessProbability(test, lineNumber);
        double normalizedProbability = statementProbability / testProbability;
        String result = String.format("%f", normalizedProbability);
        return result;
    }


    @Override
    public double getTestFlakinessProbability(TestMethod test) {
        int numberOfStatement = test.getStatementLineNumbers().size();
        double probabilityOfNotFlaking = 1 - this.flakinessProbability;
        return 1 - Math.pow(probabilityOfNotFlaking, numberOfStatement);
    }

    @Override
    public double getStatementFlakinessProbability(TestMethod test,
                                                   int lineNumber) {
        int numberOfStatementExecuted = 1 + test.getStatementLineNumbers()
                .stream()
                .filter(line -> line < lineNumber)
                .toArray().length;
        double probabilityOfNotFlaking = 1 - this.flakinessProbability;
        return 1 - Math.pow(probabilityOfNotFlaking, numberOfStatementExecuted);
    }

    @Override
    public void postProcess() {

    }
}
