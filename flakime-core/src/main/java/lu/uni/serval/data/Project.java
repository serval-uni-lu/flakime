package lu.uni.serval.data;

import javassist.*;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;

import static org.apache.commons.io.FilenameUtils.removeExtension;

public class Project implements Iterable<TestClass> {
    private final Set<String> testAnnotations;
    private final File classDirectory;
    private final File sourceDirectory;
    private final ClassPool classPool;

    public Project(Set<String> testAnnotations, File classDirectory, File sourceDirectory, ClassLoader classLoader) throws NotFoundException {
        this.testAnnotations = testAnnotations;
        this.classDirectory = classDirectory;
        this.sourceDirectory = sourceDirectory;
        this.classPool = configureClassPool(getDefaultClassPool(), this.classDirectory.getAbsolutePath(), classLoader);
    }

    public int getNumberClasses(){
        return getClassNames(classDirectory).size();
    }

    @Override
    public Iterator<TestClass> iterator(){
        return getClassNames(classDirectory).stream()
                .map(name -> TestClassFactory.create(name, classPool, sourceDirectory, classDirectory, testAnnotations))
                .filter(Objects::nonNull)
                .collect(Collectors.toList())
                .iterator();
    }

    /**
     * Create new instance of a ClassPool.
     *
     * @return never {@code null}
     */
    private ClassPool getDefaultClassPool() {
        return new ClassPool(ClassPool.getDefault());
    }

    /**
     * Configure the passed instance of a ClassPool and append required class paths on it
     *
     * @param classPool the ClassPool instance to append
     * @param targetDirectory A Directory of classes
     * @return The modified ClassPool
     * @throws NotFoundException if passed {@code classPool} is {@code null} or if passed
     *          {@code inputDir} is a JAR or ZIP and not found
     */
    private ClassPool configureClassPool(final ClassPool classPool, final String targetDirectory, ClassLoader classLoader) throws NotFoundException {
        final LoaderClassPath loaderClassPath = new LoaderClassPath(classLoader);

        classPool.childFirstLookup = true;
        classPool.appendClassPath(targetDirectory);
        classPool.appendClassPath(loaderClassPath);
        classPool.appendSystemPath();

        return classPool;
    }

    private static Set<String> getClassNames(final File directory) {
        if (!directory.exists()) {
            return Collections.emptySet();
        }

        final String[] extensions = {"class"};

        return FileUtils.listFiles(directory, extensions, true).stream()
                .map(f -> extractClassNameFromFile(directory, f))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private static String extractClassNameFromFile(final File parentDirectory, final File classFile) {
        if (null == classFile) {
            return null;
        }

        final String qualifiedFileName;

        try {
            qualifiedFileName = parentDirectory != null
                    ? classFile.getCanonicalPath()
                    .substring(parentDirectory.getCanonicalPath().length() + 1)
                    : classFile.getCanonicalPath();
        } catch (IOException e) {
            return null;
        }

        return removeExtension(qualifiedFileName.replace(File.separator, "."));
    }
}
