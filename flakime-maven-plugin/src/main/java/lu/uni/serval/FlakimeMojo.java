package lu.uni.serval;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.Collection;

@Mojo(name = "flakime-injector", defaultPhase = LifecyclePhase.PROCESS_TEST_CLASSES)
public class FlakimeMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;

    @Parameter(defaultValue = "bernoulli")
    String strategy;

    @Parameter(defaultValue = "0.05")
    float flakeRate;


    public void execute() throws MojoExecutionException, MojoFailureException {
        String root = project.getBuild().getDirectory();
        File rootDir = new File(new File(root),"test-classes");
        Collection<File> files = FileUtils.listFiles(rootDir, new String[]{"class"}, true);
        getLog().info("Flake rate: "+flakeRate);
        getLog().info("Strategy: "+strategy);
        getLog().info("Root: "+root);
        files.forEach(file -> getLog().info(file.getAbsolutePath()));
    }
}
