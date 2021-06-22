import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.apache.maven.shared.invoker.*;
import org.codehaus.plexus.util.ReaderFactory;

public class SimpleJavaStub
        extends MavenProjectStub
{
    /**
     * Default constructor
     */

    private static SimpleJavaStub simpleJavaStub_instance;

    public static SimpleJavaStub getInstance() throws MavenInvocationException {
        if (simpleJavaStub_instance == null)
            simpleJavaStub_instance = new SimpleJavaStub();

        return simpleJavaStub_instance;
    }

    private void initStub(){
        MavenXpp3Reader pomReader = new MavenXpp3Reader();
        Model model;
        try
        {
            model = pomReader.read( ReaderFactory.newXmlReader( new File( getBasedir(), "pom.xml" ) ) );
            setModel( model );
        }
        catch ( Exception e )
        {
            throw new RuntimeException( e );
        }

        setGroupId( model.getGroupId() );
        setArtifactId( model.getArtifactId() );
        setVersion( model.getVersion() );
        setName( model.getName() );
        setUrl( model.getUrl() );
        setPackaging( model.getPackaging() );

        Build build = new Build();
        build.setFinalName( model.getArtifactId() );
        build.setDirectory( getBasedir() + "/target" );
        build.setSourceDirectory( getBasedir() + "/src/main/java" );
        build.setOutputDirectory( getBasedir() + "/target/classes" );
        build.setTestSourceDirectory( getBasedir() + "/src/test/java" );
        build.setTestOutputDirectory( getBasedir() + "/target/test-classes" );
        setBuild( build );


        List compileSourceRoots = new ArrayList();
        compileSourceRoots.add( getBasedir() + "/src/main/java" );
        setCompileSourceRoots( compileSourceRoots );

        List testCompileSourceRoots = new ArrayList();
        testCompileSourceRoots.add( getBasedir() + "/src/test/java" );
        setTestCompileSourceRoots( testCompileSourceRoots );
    }

    private void install_project() throws MavenInvocationException {
        InvocationRequest request = new DefaultInvocationRequest();
        request.setPomFile(new File( getBasedir(), "pom.xml" ));
        request.setGoals(Arrays.asList("clean","test-compile"));
        Invoker invoker = new DefaultInvoker();

        InvocationResult result = invoker.execute(request);

        if (result.getExitCode() != 0){
            throw new IllegalStateException("Build failed");
        }
    }

    private SimpleJavaStub() throws MavenInvocationException {
        initStub();
        install_project();
    }

    /** {@inheritDoc} */
    public File getBasedir()
    {
        return new File( super.getBasedir() + "/src/test/resources/unit/simple-java/simple-java-2" );
    }


}