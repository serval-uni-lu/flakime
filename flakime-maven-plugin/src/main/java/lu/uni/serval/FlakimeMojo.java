package lu.uni.serval;

import java.io.File;
import java.util.Properties;
import java.util.Set;
import javassist.CannotCompileException;
import javassist.NotFoundException;
import lu.uni.serval.data.Project;
import lu.uni.serval.data.TestClass;
import lu.uni.serval.data.TestMethod;
import lu.uni.serval.instrumentation.FlakimeInstrumenter;
import lu.uni.serval.instrumentation.strategies.Strategy;
import lu.uni.serval.instrumentation.strategies.StrategyFactory;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;



@Mojo(name = "flakime-injector",
        defaultPhase = LifecyclePhase.PROCESS_TEST_CLASSES,
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class FlakimeMojo extends AbstractMojo {

    @Parameter(property = "project", readonly = true)
    MavenProject mavenProject;

    @Parameter(defaultValue = "bernoulli", property = "flakime.strategy")
    String strategy;

    @Parameter(defaultValue = "0.05", property = "flakime.flakeRate")
    float flakeRate;

    @Parameter(property = "flakime.testAnnotations", required = true)
    Set<String> testAnnotations;

    @Parameter(defaultValue = "/target/test-classes", property = "flakime.testClassDirectory")
    private String testClassDirectory;

    @Parameter(defaultValue = "/src/test/java", property = "flakime.testSourceDirectory")
    private String testSourceDirectory;

    @Parameter(required = false)
    private Properties strategyParameters;

    /**
     * Plugin mojo entry point. The method iterates over all test-classes contained in the project.
     * For each of the test classes the method iterates over all the test method (annotated by @test).
     * Finally the method calculates the flakiness probability of the given test method following the
     * given strategy.
     * If the test flakiness probability is greater than the flakerate, the test method is intrumented.
     * Otherwise the test method is skipped.
     *
     *
     * @throws MojoExecutionException Thrown if any of the steps throws an exception during its execution.
     */

    @Override
    public void execute() throws MojoExecutionException {
        Strategy strategyImpl = null;
        Log logger = getLog();
        try {
            final Project project = initializeProject(mavenProject);
            strategyImpl = StrategyFactory
                    .fromName(strategy, strategyParameters, logger);

            logger.info(String.format("Strategy %s loaded",
                    strategyImpl.getClass().getName()));
            logger.info(String.format("FlakeRate: %f", flakeRate));
            logger.info(String.format("Found %d classes",
                    project.getNumberClasses()));

            logger.debug("Running preProcess of " +
                    strategyImpl.getClass().getSimpleName());

            strategyImpl.preProcess(project);

            for (TestClass testClass : project) {

                logger.debug(
                        String.format("Process class %s", testClass.getName()));

                for (TestMethod testMethod : testClass) {
                    logger.debug(String.format("\tProcess method %s",
                            testMethod.getName()));

                    try {
                        double probability = strategyImpl
                                .getTestFlakinessProbability(testMethod);
//                        getLog().info(String.format("Probability of %s: %f",testMethod.getName(),probability));
                        if (probability > (1 - flakeRate)) {
                            FlakimeInstrumenter
                                    .instrument(testMethod, strategyImpl);
                        }
                    } catch (CannotCompileException e) {
                        logger.warn(String.format(
                                "Failed to instrument method %s: %s",
                                testMethod.getName(), e.getMessage()), e);
                    }
                }

                testClass.write();
            }
        } catch (final Exception e) {
            logger.error(e.getMessage(), e);
            throw new MojoExecutionException(e.getMessage(), e);
        } finally {
            if (strategyImpl != null) {
                strategyImpl.postProcess();
            }
        }
    }

    /**
     * This method parse the {@code Maven project} into a {@code Project}
     *
     * @param mavenProject The target maven project containing the tests.
     * @return The instantiated project
     * @throws NotFoundException Thrown if the directories contains only jars or do not exist.
     * @throws DependencyResolutionRequiredException Thrown if an artifact is used but not resolved
     */
    public Project initializeProject(MavenProject mavenProject)
            throws NotFoundException, DependencyResolutionRequiredException {
        return new Project(
                testAnnotations,
                getDirectory(testClassDirectory),
                getDirectory(testSourceDirectory),
                mavenProject.getTestClasspathElements()
        );
    }

    private File getDirectory(String path) {
        final File directory = new File(path);

        if (directory.isAbsolute()) {
            return directory;
        } else {
            return new File(mavenProject.getBasedir(), path);
        }
    }
}
