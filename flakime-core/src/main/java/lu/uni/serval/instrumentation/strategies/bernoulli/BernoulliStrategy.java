package lu.uni.serval.instrumentation.strategies.bernoulli;

import lu.uni.serval.data.Project;
import lu.uni.serval.data.TestMethod;
import lu.uni.serval.instrumentation.strategies.Strategy;
import org.apache.maven.plugin.logging.Log;


/**
 * Strategy implementation that follows bernoulli distribution.
 */
public class BernoulliStrategy implements Strategy {

  private final Log logger;

  public BernoulliStrategy(Log logger) {
    this.logger = logger;
  }

  @Override
  public void preProcess(Project p) {

  }

  @Override
  public String getProbabilityFunction(TestMethod test, int lineNumber) {
    return String.format("(1 - (1 - Math.pow(Math.random(), (double)%d)))",
        test.getStatementLineNumbers().size());
  }

  @Override
  public double getTestFlakinessProbability(TestMethod test) {
    double testProbability = Math.random();
    return 0;
  }

  @Override
  public double getStatementFlakinessProbability(TestMethod tes,
                                                 int lineNumber) {
    return 0;
  }

  @Override
  public void postProcess() {

  }
}
