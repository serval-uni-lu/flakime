package lu.uni.serval.flakime.core.flakime.maven;

import java.io.File;
import java.util.Properties;
import java.util.Set;
import javassist.CannotCompileException;
import javassist.NotFoundException;
import lu.uni.serval.flakime.core.data.Project;
import lu.uni.serval.flakime.core.data.TestClass;
import lu.uni.serval.flakime.core.data.TestMethod;
import lu.uni.serval.flakime.core.instrumentation.FlakimeInstrumenter;
import lu.uni.serval.flakime.core.instrumentation.strategies.Strategy;
import lu.uni.serval.flakime.core.instrumentation.strategies.StrategyFactory;
import lu.uni.serval.flakime.core.flakime.maven.utils.MavenLogger;
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

    @Parameter
    private Properties strategyParameters;

    @Parameter(defaultValue = "${project.build.directory}/flakime")
    private File outputDirectory;

    @Parameter(defaultValue = "FLAKIME_DISABLE", property = "flakime.disableFlag")
    private String disableFlagName;

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
            final MavenLogger mavenLogger = new MavenLogger(logger);
            final Project project = initializeProject(mavenProject, mavenLogger);
            strategyImpl = StrategyFactory.fromName(strategy, strategyParameters, mavenLogger);

            logger.info(String.format("Strategy %s loaded", strategyImpl.getClass().getName()));
            logger.info(String.format("FlakeRate: %f", flakeRate));
            logger.info(String.format("Found %d classes", project.getNumberClasses()));
            logger.debug(String.format("Running preProcess of %s", strategyImpl.getClass().getSimpleName()));

            strategyImpl.preProcess(project);

            for (TestClass testClass : project) {
                logger.debug(String.format("Process class %s", testClass.getName()));
                //TODO inject new method
                for (TestMethod testMethod : testClass) {
                    logger.debug(String.format("\tProcess method %s", testMethod.getName()));

                    try {
                        double probability = strategyImpl.getTestFlakinessProbability(testMethod);
                        logger.info(String.format("\tProbability of %s: %f",testMethod.getName(),probability));

                        if (probability > (1 - flakeRate)) {
                            FlakimeInstrumenter.instrument(testMethod, strategyImpl,outputDirectory,disableFlagName);
                        }
                    } catch (Exception e) {
                        logger.warn(String.format("Failed to instrument method %s: %s",
                                testMethod.getName(),
                                e.getMessage()
                        ));
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
     * @param mavenLogger Reference to logger
     * @return The instantiated project
     * @throws NotFoundException Thrown if the directories contains only jars or do not exist.
     * @throws DependencyResolutionRequiredException Thrown if an artifact is used but not resolved
     */
    public Project initializeProject(MavenProject mavenProject, MavenLogger mavenLogger)
            throws NotFoundException, DependencyResolutionRequiredException {
        return new Project(
                mavenLogger,
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
