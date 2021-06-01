package lu.uni.serval.flakime.core.instrumentation.models;

import lu.uni.serval.flakime.core.data.Project;
import lu.uni.serval.flakime.core.data.TestMethod;

/**
 * The strategy interface allows the user to implement different flakiness probability calculation process.
 * A strategy instance should allow the user to estimate the flakiness probability of a test method inside a targeted project
 * and the flakiness probability of each composing statements. A statement is identified by its enclosing {@code TestMethod.class} and
 * its corresponding {@code lineNumber}.
 */
public interface Model {
    /**
     * Method used to initialize the probability calculation strategy.
     *
     * @param p The target project
     * @param flakeRate the flakeRate
     * @throws Exception If something fails during preprocess
     */
    void preProcess(Project p,double flakeRate) throws Exception;

    /**
     * Method that computes the flakiness probability of a particular statement identified by the enclosing test method and the statement line number.
     *
     * @param test The enclosing test method
     * @param lineNumber The statement line number
     * @param flakeRate the flakeRate Value influencing the probability of test to actually flake.
     * @return The double value representing the probability of the statement being flaky.
     */
    double getTestFlakinessProbability(TestMethod test, int lineNumber,double flakeRate);

    /**
     * Method that computes the overall test flakiness probability.
     * @param flakeRate the flakeRate Value influencing the probability of test to actually flake.
     * @param test The test method on which the probability is calculated
     * @return The double value representing the probability of the test being flaky
     */
    double getTestFlakinessProbability(TestMethod test,double flakeRate);

    void postProcess();
}
