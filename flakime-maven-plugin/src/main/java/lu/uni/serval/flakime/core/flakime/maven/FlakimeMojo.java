package lu.uni.serval.flakime.core.flakime.maven;

import javassist.NotFoundException;
import lu.uni.serval.flakime.core.data.Project;
import lu.uni.serval.flakime.core.data.TestClass;
import lu.uni.serval.flakime.core.data.TestMethod;
import lu.uni.serval.flakime.core.flakime.maven.utils.MavenLogger;
import lu.uni.serval.flakime.core.instrumentation.FlakimeInstrumenter;
import lu.uni.serval.flakime.core.instrumentation.strategies.Strategy;
import lu.uni.serval.flakime.core.instrumentation.strategies.StrategyFactory;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@Mojo(name = "flakime-injector", defaultPhase = LifecyclePhase.TEST_COMPILE, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class FlakimeMojo extends AbstractMojo {

    @Parameter(property = "project", readonly = true)
    MavenProject mavenProject;

    @Parameter(defaultValue = "uniformDistribution", property = "flakime.strategy")
    String strategy;

    @Parameter(defaultValue = "false",property = "flakime.disableReport")
    boolean disableReport;

    @Parameter(defaultValue = "0.1", property = "flakime.flakeRate")
    double flakeRate;

    @Parameter(defaultValue = " ",property = "flakime.annotationFilters")
    Set<String> annotationFilters;

    @Parameter(defaultValue = " ",property = "flakime.methodFilters")
    Set<String> methodFilters;

    @Parameter(defaultValue = " ",property = "flakime.classFilters")
    Set<String> classFilters;

    @Parameter(defaultValue = "${project.build.testOutputDirectory}", property = "flakime.testClassDirectory")
    private String testClassDirectory;

    @Parameter(defaultValue = "${project.build.testSourceDirectory}", property = "flakime.testSourceDirectory")
    private String testSourceDirectory;

    @Parameter
    private Properties strategyParameters;

    @Parameter(defaultValue = "${project.build.directory}/flakime", property = "flakime.outputDirectory")
    private File outputDirectory;

    @Parameter(defaultValue = "FLAKIME_DISABLE", property = "flakime.disableFlag")
    private String disableFlagName;

    @Parameter(defaultValue = "false",property = "flakime.skip")
    private boolean skip;

    /**
     * Plugin mojo entry point. The method iterates over all test-classes contained
     * in the project. For each of the test classes the method iterates over all the
     * test method (annotated by @test). Finally the method calculates the flakiness
     * probability of the given test method following the given strategy. If the
     * test flakiness probability is greater than the flakerate, the test method is
     * instrumented. Otherwise the test method is skipped.
     *
     *
     * @throws MojoExecutionException Thrown if any of the steps throws an exception
     *                                during its execution.
     */

    @Override
    public void execute() throws MojoExecutionException {
        Strategy strategyImpl = null;
        Log logger = getLog();

        if(!skip)
            try {
                final MavenLogger mavenLogger = new MavenLogger(logger);

                initialiseStrategyProperties();

                final Project project = initializeProject(mavenProject, mavenLogger);

                if(!disableReport){
                    logger.info("Report output directory: " + outputDirectory.getAbsolutePath());
                }

                initializeFilters();

                strategyImpl = StrategyFactory.fromName(strategy, strategyParameters, mavenLogger);
                logger.info("Test source directory : "+testSourceDirectory);
                logger.info("Test bin directory : "+testClassDirectory);
                logger.info("Annotation Filters :["+String.join(",", annotationFilters)+"]");
                logger.info("Method Filters :["+String.join(",", methodFilters)+"]");
                logger.info("Class Filters :["+String.join(",", classFilters)+"]");
                logger.info(String.format("Strategy %s loaded", strategyImpl.getClass().getName()));
                logger.info(String.format("FlakeRate: %f", flakeRate));

                int ntests = project.getTestClasses().stream().reduce(0, (sub, elem) -> sub + elem.getnTestMethods(), Integer::sum);
                logger.info(String.format("Found %d classes with %d tests", project.getNumberClasses(),ntests));
                logger.debug(String.format("Running preProcess of %s", strategyImpl.getClass().getSimpleName()));

                strategyImpl.preProcess(project,flakeRate);

                for (TestClass testClass : project) {
                    logger.debug(String.format("Process class %s", testClass.getName()));
                    for (TestMethod testMethod : testClass) {
                        logger.debug(String.format("\tProcess method %s", testMethod.getName()));
                        instrument(testMethod, strategyImpl, outputDirectory, disableFlagName,
                                flakeRate,disableReport);
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

    private void initializeFilters() {
        annotationFilters = annotationFilters.stream().filter(s -> !s.trim().isEmpty()).collect(Collectors.toSet());
        methodFilters = methodFilters.stream().filter(s -> !s.trim().isEmpty()).collect(Collectors.toSet());
        classFilters = classFilters.stream().filter(s -> !s.trim().isEmpty()).collect(Collectors.toSet());

        if(annotationFilters.isEmpty() && methodFilters.isEmpty()){
            annotationFilters.add("^@org\\.junit\\.jupiter\\.api\\.Test*.");
            annotationFilters.add("@org\\.junit\\.Test");
        }
    }

    private void instrument(TestMethod testMethod,Strategy strategyImpl,File outputDirectory,String disableFlagName, double flakeRate,boolean disableReport){
        try{
            FlakimeInstrumenter.instrument(testMethod, strategyImpl, outputDirectory, disableFlagName,
                    flakeRate,disableReport);
        }catch (Exception e){
            getLog().warn(String.format("Failed to instrument method %s: %s", testMethod.getName(),
                    e.getMessage()));
        }
    }
    /**
     * This method parse the {@code Maven project} into a {@code Project}
     *
     * @param mavenProject The target maven project containing the tests.
     * @param mavenLogger  Reference to logger
     * @return The instantiated project
     * @throws NotFoundException                     Thrown if the directories
     *                                               contains only jars or do not
     *                                               exist.
     * @throws DependencyResolutionRequiredException Thrown if an artifact is used
     *                                               but not resolved
     */
    public Project initializeProject(MavenProject mavenProject, MavenLogger mavenLogger)
            throws NotFoundException, DependencyResolutionRequiredException {

        return new Project(mavenLogger, annotationFilters, methodFilters, classFilters, getDirectory(testClassDirectory),
                getDirectory(testSourceDirectory), mavenProject.getTestClasspathElements());
    }

    private void initialiseStrategyProperties(){
        this.strategyParameters = Optional.ofNullable(strategyParameters).orElse(new Properties());
        this.strategyParameters.putIfAbsent("modelPath",mavenProject.getBuild().getDirectory()+"/rfc_classifier");

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
