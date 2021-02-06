package lu.uni.serval;

import javassist.CannotCompileException;
import javassist.NotFoundException;
import lu.uni.serval.data.Project;
import lu.uni.serval.data.TestClass;
import lu.uni.serval.data.TestMethod;
import lu.uni.serval.instrumentation.FlakimeInstrumenter;
import lu.uni.serval.instrumentation.strategies.Strategy;
import lu.uni.serval.instrumentation.strategies.StrategyFactory;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.Set;

@Mojo(name = "flakime-injector", defaultPhase = LifecyclePhase.PROCESS_TEST_CLASSES,requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class FlakimeMojo extends AbstractMojo {

    @Parameter(property = "project", required = true, readonly = true)
    MavenProject mavenProject;

    @Parameter(defaultValue = "bernoulli")
    String strategy = "bernoulli";

    @Parameter(defaultValue = "0.05")
    float flakeRate;

    @Parameter(required = true,property = "testAnnotations")
    Set<String> testAnnotations;

    @Parameter(defaultValue = "target/test-classes", property = "javassist.testBuildDir")
    private final String testBuildDir = "target/test-classes";

    @Override
    public void execute() throws MojoExecutionException {
        try {
            final Strategy strategyImpl = StrategyFactory.fromName(strategy);
            final Project project = initializeProject(mavenProject);

            getLog().info(String.format("Found %d classes", project.getNumberClasses()));

            for(TestClass testClass: project){

                getLog().debug(String.format("Process class %s", testClass.getName()));

                for (TestMethod testMethod: testClass){
                    getLog().debug(String.format("\tProcess method %s", testMethod.getName()));

                    try {
                        FlakimeInstrumenter.instrument(testMethod, flakeRate, strategyImpl);
                    } catch (CannotCompileException e) {
                        getLog().warn(String.format(
                                "Failed to instrument method %s: %s",
                                testMethod.getName(), e.getMessage()), e);
                    }
                }

                testClass.write();
            }
        } catch (final Exception e) {
            getLog().error(e.getMessage(), e);
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private Project initializeProject(MavenProject mavenProject) throws NotFoundException {
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        return new Project(testAnnotations, getDirectory(testBuildDir), mavenProject.getBasedir(), classLoader);
    }

    private File getDirectory(String path) {
        final File directory = new File(path);

        if (directory.isAbsolute()) {
            return directory;
        } else {
            return new File(mavenProject.getBasedir(), path);
        }
    }

    /**
     * Evaluates and returns the output directory/
     *
     * If the passed {@code outputDir} is {@code null} or empty, the passed {@code inputDir} otherwise
     * the {@code outputDir} will returned.
     *
     * @param outputDir The class output directory
     * @param inputDir The class input directory
     * @return The computed output directory
     */
    private static String evaluateOutputDirectory(final String outputDir, final String inputDir) {
        return outputDir != null && !outputDir.trim().isEmpty() ? outputDir : inputDir.trim();
    }

}
