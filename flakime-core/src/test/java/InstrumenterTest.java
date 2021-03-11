import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javassist.CannotCompileException;
import javassist.NotFoundException;
import javassist.bytecode.BadBytecode;
import lu.uni.serval.flakime.core.data.Project;
import lu.uni.serval.flakime.core.data.TestClass;
import lu.uni.serval.flakime.core.data.TestMethod;
import org.apache.commons.io.FilenameUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class InstrumenterTest {

    private static Project PROJECT;
    static lu.uni.serval.flakime.core.utils.Logger logger = new lu.uni.serval.flakime.core.utils.Logger() {
        @Override
        public void info(String message) {
            System.out.printf("[%s][%s]%n",this.getClass().getName(),message);
        }

        @Override
        public void warn(String message) {
            System.out.printf("[%s][%s]%n",this.getClass().getName(),message);
        }

        @Override
        public void debug(String message) {
            System.out.printf("[%s][%s]%n",this.getClass().getName(),message);
        }

        @Override
        public void error(String message) {
            System.out.printf("[%s][%s]%n",this.getClass().getName(),message);
        }
    };

    @BeforeAll
    static void setup() throws IOException, URISyntaxException, NotFoundException {
        Set<String> testAnnotation = Stream.of("@org.junit.jupiter.api.Test", "ABC").collect(
                Collectors.toSet());

        File f = unzip(getResourceFile("test-classes.zip"));
        PROJECT =  new Project(
                logger,
                testAnnotation,
                f,
                getResourceFile("sources"),
                Collections.singletonList(getResourceFile("dep/5-7-0-M1/junit-jupiter-5.7.0-M1.jar").getAbsolutePath())
        );

    }


    @Test
    void projectLoadTest(){
        assertEquals(PROJECT.getClassNames().size(), 4);
    }

    @Test
    void stackHeightTest() throws CannotCompileException {
        Optional<TestClass>
                mathUtilsTest = PROJECT.getTestClasses().stream().filter(tc -> tc.getName().equals("org.example.MathUtilsTest")).findFirst();

        assertTrue(mathUtilsTest.isPresent());

        Optional<TestMethod> testMethod = mathUtilsTest.get().getTestMethods().stream().filter(tm -> tm.getName().equals("testInflateWithNegativeNumber22")).findFirst();

        assertTrue(testMethod.isPresent());

        assertEquals(4,testMethod.get().getStatementLineNumbers().size());

        testMethod.get().getCtMethod().getName();
        for(int ln : testMethod.get().getStatementLineNumbers()){
                testMethod.get().insertAt(ln+1,"System.out.println(\"\");");
        }


    }


    public static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
    }

    public static File unzip(File fileZip) throws IOException {
        final File destDir = new File(new File(System.getProperty("java.io.tmpdir")), "test-classes-flakime");
        destDir.mkdirs();

        byte[] buffer = new byte[1024];
        ZipInputStream zis = new ZipInputStream(new FileInputStream(fileZip));
        ZipEntry zipEntry = zis.getNextEntry();

        while (zipEntry != null) {
            File newFile = newFile(destDir, zipEntry);

            if(zipEntry.isDirectory()){
                newFile.mkdirs();
            }
            else{

                // fix for Windows-created archives
                File parent = newFile.getParentFile();
                if (!parent.isDirectory() && !parent.mkdirs()) {
                    throw new IOException("Failed to create directory " + parent);
                }

                FileOutputStream fos = new FileOutputStream(newFile);
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
            }

            zipEntry = zis.getNextEntry();
        }

        zis.closeEntry();
        zis.close();

        return new File(destDir, FilenameUtils.getBaseName(fileZip.getAbsolutePath()));
    }


    public static File getResourceFile(String name) throws IOException, URISyntaxException {
        URL resource = InstrumenterTest.class.getClassLoader().getResource(name);
        if (resource == null) {
            throw new IOException("Failed to locate resource template for project analytics "+name);
        }

        return Paths.get(resource.toURI()).toFile();
    }
}
