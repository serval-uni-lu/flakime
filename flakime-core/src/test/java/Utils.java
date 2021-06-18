import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javassist.NotFoundException;
import lu.uni.serval.flakime.core.data.Project;
import lu.uni.serval.flakime.core.utils.Logger;
import org.apache.commons.io.FilenameUtils;
import org.apache.maven.shared.invoker.MavenInvocationException;

public class Utils {
    static Logger logger = new lu.uni.serval.flakime.core.utils.Logger() {
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

    public static Project createProject_noFilter()
            throws IOException, URISyntaxException, NotFoundException, MavenInvocationException {
        SimpleJavaStub simpleJavaStub = SimpleJavaStub.getInstance();

        return new Project(
                logger,
                Stream.of("^@org\\.junit\\.jupiter\\.api\\.Test*.","@org\\.junit\\.Test").collect(
                        Collectors.toSet()),
                Collections.emptySet(),
                Collections.emptySet(),
                new File(simpleJavaStub.getBuild().getTestOutputDirectory()),
                new File(simpleJavaStub.getBuild().getTestSourceDirectory()),
                simpleJavaStub.getDependencies().stream().map(d -> d.getSystemPath()).collect(
                        Collectors.toList()));
    }

    public static Project createProject_junitAnnotation()
            throws IOException, URISyntaxException, NotFoundException, MavenInvocationException {
//        File f = unzip(getResourceFile("test-classes.zip"));
        SimpleJavaStub simpleJavaStub = SimpleJavaStub.getInstance();

        return new Project(
                logger,
                Stream.of("^@org\\.junit\\.jupiter\\.api\\.Test*.","@org\\.junit\\.Test").collect(
                        Collectors.toSet()),
                Collections.emptySet(),
                Collections.emptySet(),
                new File(simpleJavaStub.getBuild().getTestOutputDirectory()),
                new File(simpleJavaStub.getBuild().getTestSourceDirectory()),
                simpleJavaStub.getDependencies().stream().map(d -> d.getSystemPath()).collect(
                        Collectors.toList()));
    }


    private static File getResourceFile(String name) throws IOException, URISyntaxException {
        URL resource = Utils.class.getClassLoader().getResource(name);
        if (resource == null) {
            throw new IOException("Failed to locate resource template for project analytics "+name);
        }

        return Paths.get(resource.toURI()).toFile();
    }

    private static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
    }

    private static File unzip(File fileZip) throws IOException {
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
}
