import java.io.File;

import lu.uni.serval.flakime.core.flakime.maven.FlakimeMojo;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlakimeMojoTest extends AbstractMojoTestCase {

    private final Logger log = LoggerFactory.getLogger( FlakimeMojoTest.class );

    private final  File testPom = new File( getBasedir(),
            "src/test/resources/simple-java-2/pom.xml" );


    /**
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        // required for mojo lookups to work
        super.setUp();
    }
    public void testPOM() throws Exception {

        log.debug( "\n  Using POM '...{}'.", testPom.toString().substring( getBasedir().length() ) );

        Assertions.assertNotNull( testPom );

        Assertions.assertTrue( testPom.exists() );

        FlakimeMojo mojo = new FlakimeMojo();
        mojo = (FlakimeMojo) configureMojo(mojo,extractPluginConfiguration("flakime-maven-plugin",testPom));
        mojo.execute();
    }


    /**
     * @throws Exception
     */
    public void testMojoGoal() throws Exception
    {


        FlakimeMojo mojo = (FlakimeMojo) lookupMojo( "flakime-injector", testPom );

        Assertions.assertNotNull( mojo );
    }

}
