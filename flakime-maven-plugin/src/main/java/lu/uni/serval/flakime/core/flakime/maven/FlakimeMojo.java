package lu.uni.serval.flakime.core.flakime.maven;

import javassist.NotFoundException;
import lu.uni.serval.flakime.core.data.Project;
import lu.uni.serval.flakime.core.data.TestClass;
import lu.uni.serval.flakime.core.data.TestMethod;
import lu.uni.serval.flakime.core.flakime.maven.utils.MavenLogger;
import lu.uni.serval.flakime.core.instrumentation.FlakimeInstrumenter;
import lu.uni.serval.flakime.core.instrumentation.models.Model;
import lu.uni.serval.flakime.core.instrumentation.models.ModelFactory;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Mojo(name = "flakime-injector", defaultPhase = LifecyclePhase.TEST_COMPILE, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class FlakimeMojo extends AbstractMojo {

    @Parameter(property = "project", readonly = true)
    MavenProject mavenProject;

    @Parameter(defaultValue = "uniformDistribution", property = "flakime.model")
    String model;

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
    private Properties modelParameters;

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
     * probability of the given test method following the given model. If the
     * test flakiness probability is greater than the flakerate, the test method is
     * instrumented. Otherwise the test method is skipped.
     *
     *
     * @throws MojoExecutionException Thrown if any of the steps throws an exception
     *                                during its execution.
     */

    @Override
    public void execute() throws MojoExecutionException {
        Model modelImpl = null;
        Log logger = getLog();

        if(!skip)
            try {
                final MavenLogger mavenLogger = new MavenLogger(logger);

                initialiseModelProperties();

                final Project project = initializeProject(mavenProject, mavenLogger);

                if(!disableReport){
                    logger.info("Report output directory: " + outputDirectory.getAbsolutePath());
                }

                initializeFilters();

                modelImpl = ModelFactory.fromName(model, modelParameters, mavenLogger);
                logger.info("Test source directory : "+testSourceDirectory);
                logger.info("Test bin directory : "+testClassDirectory);
                logger.info("Annotation Filters :["+String.join(",", annotationFilters)+"]");
                logger.info("Method Filters :["+String.join(",", methodFilters)+"]");
                logger.info("Class Filters :["+String.join(",", classFilters)+"]");
                logger.info(String.format("Model %s loaded", modelImpl.getClass().getName()));
                logger.info(String.format("FlakeRate: %f", flakeRate));

                int ntests = project.getTestClasses().stream().reduce(0, (sub, elem) -> sub + elem.getnTestMethods(), Integer::sum);
                logger.info(String.format("Found %d classes with %d tests", project.getNumberClasses(),ntests));
                logger.debug(String.format("Running preProcess of %s", modelImpl.getClass().getSimpleName()));

                modelImpl.preProcess(project,flakeRate);

                final Map<String, Double> testProbabilities = new HashMap<>();

                for (TestClass testClass : project) {
                    logger.debug(String.format("Process class %s", testClass.getName()));
                    for (TestMethod testMethod : testClass) {
                        logger.debug(String.format("\tProcess method %s", testMethod.getName()));
                        instrument(testMethod, modelImpl, outputDirectory, disableFlagName, flakeRate,disableReport);
                        testProbabilities.put(
                                testMethod.getLongName(),
                                modelImpl.getTestFlakinessProbability(testMethod, 1.)
                        );
                    }
                    testClass.write();
                }

                saveTestProbabilities(testProbabilities);

            } catch (final Exception e) {
                logger.error(e.getMessage(), e);
                throw new MojoExecutionException(e.getMessage(), e);
            } finally {
                if (modelImpl != null) {
                    modelImpl.postProcess();
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

    private void instrument(TestMethod testMethod, Model modelImpl, File outputDirectory, String disableFlagName, double flakeRate, boolean disableReport){
        try{
            FlakimeInstrumenter.instrument(testMethod, modelImpl, outputDirectory, disableFlagName, flakeRate,disableReport);
        }catch (Exception e){
            getLog().warn(String.format("Failed to instrument method %s: %s", testMethod.getName(), e.getMessage()));
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

    private void initialiseModelProperties(){
        this.modelParameters = Optional.ofNullable(modelParameters).orElse(new Properties());
        this.modelParameters.putIfAbsent("modelPath",mavenProject.getBuild().getDirectory()+"/rfc_classifier");

    }

    private File getDirectory(String path) {
        final File directory = new File(path);

        if (directory.isAbsolute()) {
            return directory;
        } else {
            return new File(mavenProject.getBasedir(), path);
        }
    }

    private void saveTestProbabilities(Map<String, Double> testProbabilities){
        File file = new File(outputDirectory, "test_probabilities.csv");
        if(!file.getParentFile().mkdirs()){
            getLog().error("Failed to create output summary file: " + file.getAbsolutePath());
        }

        try(BufferedWriter writer =new BufferedWriter(new FileWriter(file))){
            writer.write("name;probability");
            writer.newLine();

            for(Map.Entry<String, Double> entry: testProbabilities.entrySet()){
                writer.write(String.format("%s;%.4f", entry.getKey(), entry.getValue()));
                writer.newLine();
            }

            writer.flush();
        } catch (IOException e) {
            getLog().error(String.format(
                    "Failed to write summary file '%s': %s",
                    file.getAbsolutePath(),
                    e.getMessage()
            ));
        }
    }
}
