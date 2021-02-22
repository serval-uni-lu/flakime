package lu.uni.serval.instrumentation.strategies;

import javafx.util.Pair;
import lu.uni.serval.data.Project;
import lu.uni.serval.data.TestMethod;

/**
 * The strategy interface allows the user to implement different flakiness probability calculation process.
 * A strategy instance should allow the user to estimate the flakiness probability of a test method inside a targeted project
 * and the flakiness probability of each composing statements. A statement is identified by its enclosing {@code TestMethod.class} and
 * its corresponding {@code lineNumber}.
 */
public interface Strategy {

    /**
     * Method used to initialize the probability calculation strategy.
     *
     * @param p The target project
     * @throws Exception If something fails during preprocess
     */
    void preProcess(Project p) throws Exception;


    /**
     * Method that returns the flakiness probability parsed as a string of a statement identified by the test method and the line number.
     *
     * @param test The targeted test method
     * @param lineNumber The statement line Number
     * @return the parsed probability string.
     */
    String getProbabilityFunction(TestMethod test, int lineNumber);


    /**
     * Method that computes the overall test flakiness probability.
     *
     * @param test The test method on which the probability is calculated
     * @return The double value representing the probability of the test being flaky
     */
    double getTestFlakinessProbability(TestMethod test);

    /**
     * Method that computes the flakiness probability of a particular statement identified by the enclosing test method and the statement line number.
     *
     * @param test The enclosing test method
     * @param lineNumber The statement line number
     * @return The double value representing the probability of the statement being flaky.
     */
    double getStatementFlakinessProbability(TestMethod test,int lineNumber);
    void postProcess();
}
