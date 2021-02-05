package lu.uni.serval;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@Mojo(name = "flakime-injector", defaultPhase = LifecyclePhase.PROCESS_TEST_CLASSES,requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class FlakimeMojo extends AbstractMojo {

    @Parameter(property = "project", required = true, readonly = true)
    MavenProject project;

    @Parameter(defaultValue = "bernoulli")
    String strategy;

    @Parameter(defaultValue = "0.05")
    float flakeRate;

    @Parameter(required = true,property = "testAnnotations")
    List<String> testAnnotations;

    @Parameter(defaultValue = "target/test-classes", property = "javassist.testBuildDir")
    private final String testBuildDir = "target/test-classes";

    protected Iterator<String> iterateClassnames(final String directory) {
        final File dir = new File(directory);
        if (!dir.exists()) {
            return Collections.emptyIterator();
        }
        final String[] extensions = {".class"};
        final IOFileFilter fileFilter = new SuffixFileFilter(extensions);
        final IOFileFilter dirFilter = TrueFileFilter.INSTANCE;
        return ClassnameExtractor
                .iterateClassnames(dir, FileUtils.iterateFiles(dir, fileFilter, dirFilter));
    }

    @Override
    public void execute() throws MojoExecutionException {
        try {
            String testInputDirectory = computeDir(testBuildDir);

            getLog().info("TestInputDirectory: "+testInputDirectory);
            FlakimeInstrumenter flakimeInstrumenter = new FlakimeInstrumenter(this.testAnnotations,flakeRate);
            Iterator<String> classNameIterator = iterateClassnames(testInputDirectory);
            getLog().info("ClassNameIterator: ");
            while(classNameIterator.hasNext()){
                String targetClass = classNameIterator.next();
                getLog().info("TargetClass: "+targetClass);
                flakimeInstrumenter.instrument(testInputDirectory,testInputDirectory,targetClass);
            }
        } catch (final Exception e) {
            getLog().error(e.getMessage(), e);
            throw new MojoExecutionException(e.getMessage(), e);
        }

    }

    private String computeDir(String dir) {
        File dirFile = new File(dir);
        if (dirFile.isAbsolute()) {
            return dirFile.getAbsolutePath();
        } else {
            return new File(project.getBasedir(), dir).getAbsolutePath();
        }
    }

}
