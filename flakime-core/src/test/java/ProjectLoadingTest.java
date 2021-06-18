import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.net.URISyntaxException;
import javassist.NotFoundException;
import lu.uni.serval.flakime.core.data.Project;
import lu.uni.serval.flakime.core.data.TestClass;
import lu.uni.serval.flakime.core.data.TestMethod;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ProjectLoadingTest {

    @Test
    void nofilter_projectLoadTest() throws NotFoundException, IOException, URISyntaxException, MavenInvocationException {
        Project p = Utils.createProject_noFilter();
        assertEquals(4, p.getTestClasses().size());
        assertEquals(12,p.getTestClasses().stream().reduce(0, (sub, elem) -> sub + elem.getnTestMethods(), Integer::sum));
    }

    @Test
    void junitAnnotation_projectLoadTest() throws NotFoundException, IOException, URISyntaxException, MavenInvocationException {
        Project p = Utils.createProject_junitAnnotation();
        assertEquals(4, p.getTestClasses().size());
        assertEquals(12,p.getTestClasses().stream().reduce(0, (sub, elem) -> sub + elem.getnTestMethods(), Integer::sum));
    }

//    @Test
//    void isTest_Test() throws NotFoundException, IOException, URISyntaxException {
//        Project p = Utils.createProject_junitAnnotation();
//        TestClass testClass = p.getTestClasses().stream().filter(tc -> tc.getName().contains("MathUtilsTest")).findAny().get();
//        TestMethod testMethod = testClass.getTestMethods().stream().filter(tm -> tm.getName().contains("main")).findAny().get();
//        assertFalse(testClass.isTest(testMethod.getCtMethod()));
//    }

    }

