import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import org.mockito.*;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javassist.CannotCompileException;
import javassist.NotFoundException;
import lu.uni.serval.flakime.core.data.Project;
import lu.uni.serval.flakime.core.data.TestClass;
import lu.uni.serval.flakime.core.data.TestMethod;
import lu.uni.serval.flakime.core.utils.Logger;
import org.apache.commons.io.FilenameUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ProjectLoadingTest {

    SimpleJavaStub stub = new SimpleJavaStub();

    @Test
    void testLoadingStub(){
        stub.getBasedir();
    }

    @Test
    void nofilter_projectLoadTest() throws NotFoundException, IOException, URISyntaxException {
        Project p = Utils.createProject_noFilter();
        assertEquals(4, p.getTestClasses().size());
        assertEquals(12,p.getTestClasses().stream().reduce(0, (sub, elem) -> sub + elem.getnTestMethods(), Integer::sum));
    }

    @Test
    void junitAnnotation_projectLoadTest() throws NotFoundException, IOException, URISyntaxException {
        Project p = Utils.createProject_junitAnnotation();
        assertEquals(4, p.getTestClasses().size());
        assertEquals(10,p.getTestClasses().stream().reduce(0, (sub, elem) -> sub + elem.getnTestMethods(), Integer::sum));
    }

    @Test
    void isTest_Test(){
    TestClass tc = Mockito.mock(TestClass.class);
    TestMethod tm = Mockito.mock(TestMethod.class);

    Mockito.when(tm.getCtMethod().getMethodInfo().isConstructor()).thenReturn(true);
    Mockito.when(tm.getCtMethod().getMethodInfo().getName()).thenReturn("testMe");
    assertFalse(tc.isTest(tm.getCtMethod()));
    }

    }

