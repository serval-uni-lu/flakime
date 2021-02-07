package lu.uni.serval.data;

import javassist.*;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;

import static org.apache.commons.io.FilenameUtils.removeExtension;

public class Project implements Iterable<TestClass> {
    private final Set<String> testAnnotations;
    private final File classDirectory;
    private final File sourceDirectory;
    private final ClassPool classPool;

    private Set<String> classNames;

    public Project(Set<String> testAnnotations, File classDirectory, File sourceDirectory, List<String> dependencies) throws NotFoundException {
        this.testAnnotations = testAnnotations;
        this.classDirectory = classDirectory;
        this.sourceDirectory = sourceDirectory;
        this.classPool = configureClassPool(getDefaultClassPool(), this.classDirectory, dependencies);
    }

    public int getNumberClasses(){
        return getClassNames().size();
    }

    @Override
    public Iterator<TestClass> iterator(){
        return getClassNames().stream()
                .map(name -> getSourceFile(name)
                        .map(file -> TestClassFactory.create(this.testAnnotations, name, this.classPool, file, this.classDirectory))
                        .orElse(null)
                )
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
     * @param classDirectory A Directory of classes
     * @return The modified ClassPool
     * @throws NotFoundException if passed {@code classPool} is {@code null} or if passed
     *          {@code inputDir} is a JAR or ZIP and not found
     */
    private ClassPool configureClassPool(final ClassPool classPool, final File classDirectory, List<String> dependencies) throws NotFoundException {
        classPool.childFirstLookup = true;

        classPool.appendClassPath(classDirectory.getAbsolutePath());

        for(String dependency: dependencies){
            classPool.appendClassPath(dependency);
        }

        classPool.appendSystemPath();

        return classPool;
    }

    private Set<String> getClassNames() {
        if(classNames == null) {
            final String[] extensions = {"class"};

            classNames = FileUtils.listFiles(classDirectory, extensions, true).stream()
                    .map(f -> extractClassNameFromFile(classDirectory, f))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
        }

        return classNames;
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
