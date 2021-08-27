import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javassist.CtMethod;
import javassist.NotFoundException;
import anonymised.flakime.core.data.Project;
import anonymised.flakime.core.utils.NameFilter;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProjectLoadingTest {

    @Test
    void nofilter_projectLoadTest() throws NotFoundException, IOException, URISyntaxException, MavenInvocationException {
        Project p = Utils.createProject_noFilter();
        assertEquals(4, p.getTestClasses().size());
        assertEquals(9,p.getTestClasses().stream().reduce(0, (sub, elem) -> sub + elem.getnTestMethods(), Integer::sum));
    }

    @Test
    void junitAnnotation_projectLoadTest() throws NotFoundException, IOException, URISyntaxException, MavenInvocationException {
        Project p = Utils.createProject_junitAnnotation();
        assertEquals(4, p.getTestClasses().size());
        assertEquals(9,p.getTestClasses().stream().reduce(0, (sub, elem) -> sub + elem.getnTestMethods(), Integer::sum));
    }

    @Test
    void isNotATestTest_annotationFilter() throws NotFoundException, IOException, URISyntaxException, MavenInvocationException {
        Project p = Utils.createProject_noFilter();
        CtMethod mainMethod = p.getClassPool().get("org.example.MathUtilsTest").getDeclaredMethod("main");
        Set<String> annotationSet = Stream.of("^@org\\.junit\\.jupiter\\.api\\.Test*.","@org\\.junit\\.Test").collect(
                Collectors.toSet());
        assertFalse(anonymised.flakime.core.utils.Utils.isTest(mainMethod,new NameFilter(Collections.emptySet()),new NameFilter(annotationSet)));
    }

    @Test
    void isATestTest_annotationFilter() throws NotFoundException, IOException, URISyntaxException, MavenInvocationException {
        Project p = Utils.createProject_junitAnnotation();
        CtMethod mainMethod = p.getClassPool().get("org.example.MathUtilsTest").getDeclaredMethod("testIf");
        Set<String> annotationSet = Stream.of("^@org\\.junit\\.jupiter\\.api\\.Test*.","@org\\.junit\\.Test").collect(
                Collectors.toSet());
        assertTrue(anonymised.flakime.core.utils.Utils.isTest(mainMethod,new NameFilter(Collections.emptySet()),new NameFilter(annotationSet)));
    }

    @Test
    void isATestTest_nameFilterAnnotationFilter() throws MavenInvocationException, NotFoundException, IOException, URISyntaxException {
        Project p = Utils.createProject_junitAnnotation();
        CtMethod mainMethod = p.getClassPool().get("org.example.MathUtilsTest").getDeclaredMethod("main");
        CtMethod testMethod = p.getClassPool().get("org.example.MathUtilsTest").getDeclaredMethod("testIf");
        Set<String> annotationSet = Stream.of("^@org\\.junit\\.jupiter\\.api\\.Test*.","@org\\.junit\\.Test").collect(
                Collectors.toSet());
        Set<String> methodNameSet = Stream.of("^.*main").collect(Collectors.toSet());
        assertTrue(anonymised.flakime.core.utils.Utils.isTest(mainMethod,new NameFilter(methodNameSet),new NameFilter(annotationSet)));
        assertTrue(anonymised.flakime.core.utils.Utils.isTest(testMethod,new NameFilter(methodNameSet),new NameFilter(annotationSet)));

    }



    }

