package lu.uni.serval.flakime.core.data;

import javassist.*;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import lu.uni.serval.flakime.core.utils.*;
import lu.uni.serval.flakime.core.utils.NameFilter;
import org.apache.commons.io.FileUtils;

import static org.apache.commons.io.FilenameUtils.removeExtension;

public class Project implements Iterable<TestClass> {
    private final Logger logger;
    private final NameFilter annotationFilters;
    private final NameFilter methodFilters;
    private final NameFilter classFilters;
    private final File classDirectory;
    private final File sourceDirectory;
    private final ClassPool classPool;
    private final List<TestClass> testClasses;

    public Project(Logger logger, Set<String> annotationFilters, Set<String> methodFilters, Set<String> classFilters, File classDirectory, File sourceDirectory, List<String> dependencies) throws NotFoundException {

        if (logger == null){
            this.logger = new Logger() {
                @Override
                public void info(String message) {

                }

                @Override
                public void warn(String message) {

                }

                @Override
                public void debug(String message) {

                }

                @Override
                public void error(String message) {

                }
            };
        } else {
            this.logger = logger;
        }

        this.annotationFilters = new NameFilter(annotationFilters);
        this.methodFilters = new NameFilter(methodFilters);
        this.classFilters = new NameFilter(classFilters);
        this.classDirectory = classDirectory;
        this.sourceDirectory = sourceDirectory;
        this.classPool = configureClassPool(getDefaultClassPool(), this.classDirectory, dependencies);
        this.testClasses = initTestClasses();
    }

    public int getNumberClasses(){
        return this.testClasses.size();
    }

    @Override
    public Iterator<TestClass> iterator(){
        return this.testClasses.iterator();
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
     * @param classDirectory A Directory of classes
     * @return The modified ClassPool
     * @throws NotFoundException if passed {@code classPool} is {@code null} or if passed
     *          {@code inputDir} is a JAR or ZIP and not found
     */
    public static ClassPool configureClassPool(final ClassPool classPool, final File classDirectory, List<String> dependencies) throws NotFoundException {
        classPool.childFirstLookup = true;

        classPool.appendClassPath(classDirectory.getAbsolutePath());

        for(String dependency: dependencies){
            classPool.appendClassPath(dependency);
        }

        classPool.appendSystemPath();

        return classPool;
    }

    private List<TestClass> initTestClasses() {
        final String[] extensions = {"class"};

        return FileUtils.listFiles(classDirectory, extensions, true).stream()
                .map(f -> extractClassNameFromFile(classDirectory, f))
                .filter(Objects::nonNull)
                .filter(classFilters::matches)
                .map(name -> getSourceFile(name)
                        .map(file -> TestClassFactory.create(this.logger, this.annotationFilters, this.methodFilters, name, this.classPool, file, this.classDirectory))
                        .orElse(null)
                )
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private Optional<File> getSourceFile(String className){
        return findFileFromClassName(sourceDirectory, className, ".java");
    }

    private Optional<File> getClassFile(String className){
        return findFileFromClassName(classDirectory, className, ".class");
    }

    private static Optional<File> findFileFromClassName(File baseDirectory, String className, String extension){
        final String relativePath = className.replace(".", File.separator) + extension;
        final File file = new File(baseDirectory, relativePath);

        return file.exists() ? Optional.of(file) : Optional.empty();
    }

    private String extractClassNameFromFile(final File parentDirectory, final File classFile) {
        if (null == classFile) {
            return null;
        }

        final String qualifiedFileName;

        try {
            qualifiedFileName = parentDirectory != null
                    ? classFile.getCanonicalPath().substring(parentDirectory.getCanonicalPath().length() + 1)
                    : classFile.getCanonicalPath();
        } catch (IOException e) {
            logger.warn(String.format("Failed to extract class name from file %s: %s",
                    classFile.getAbsolutePath(),
                    e.getMessage())
            );

            return null;
        }

        return removeExtension(qualifiedFileName.replace(File.separator, "."));
    }

    public List<TestClass> getTestClasses() {
        return testClasses;
    }

    public File getClassDirectory() {
        return classDirectory;
    }

    public File getSourceDirectory() {
        return sourceDirectory;
    }

    public ClassPool getClassPool() {
        return classPool;
    }
}
