package anonymised.flakime.core.instrumentation.models.uniform;

import java.util.Collections;
import anonymised.flakime.core.data.Project;
import anonymised.flakime.core.data.TestMethod;
import anonymised.flakime.core.instrumentation.models.Model;
import anonymised.flakime.core.utils.Logger;

/**
 * Strategy implementation that follows normal distribution.
 * <p>
 * The probability of flaking after the execution of a basic block is p. (probability of not flaking is 1-p)
 * Each statement has the same flakiness probability.
 * <p>
 * The probability p is calculated by multiplying the proportion of executed lines of code wrt overall number of lines of code in the method body
 * by the pre-set flake rate
 */
public class UniformDistrubtionModel implements Model {
    private final Logger logger;

    public UniformDistrubtionModel(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void preProcess(Project p,double flakeRate) {
        //No preprocess needed.
    }

    @Override
    public double getTestFlakinessProbability(TestMethod test, int lineNumber,double flakeRate) {

        int numberOfLines = Collections.max(test.getStatementLineNumbers()) - Collections.min(test.getStatementLineNumbers())+1;

        int executedLine = 1 + lineNumber - Collections.min(test.getStatementLineNumbers());

        double proportion = (double)executedLine/numberOfLines;
        logger.debug(String.format("[%s][total: %d][executed: %d]",test.getName(),numberOfLines,executedLine));

        return proportion * flakeRate;
    }

    @Override
    public double getTestFlakinessProbability(TestMethod test,double flakeRate) {
        return flakeRate;
    }


    @Override
    public void postProcess() {
        //no post process needed
    }
}
