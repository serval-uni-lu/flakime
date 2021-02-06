package lu.uni.serval.data;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtMethod;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class TestClass implements Iterable<TestMethod> {
    private final Set<String> testAnnotations;
    private final CtClass ctClass;
    private final File sourceDirectory;
    private final File classDirectory;

    public TestClass(Set<String> testAnnotations, CtClass ctClass, File sourceDirectory, File classDirectory) {
        this.testAnnotations = testAnnotations;
        this.ctClass = ctClass;
        this.sourceDirectory = sourceDirectory;
        this.classDirectory = classDirectory;
    }

    public void write() throws IOException, CannotCompileException {
        this.ctClass.writeFile(classDirectory.getAbsolutePath());
    }

    @Override
    public Iterator<TestMethod> iterator(){
        return Arrays.stream(ctClass.getDeclaredMethods())
                .filter(TestClass.this::isTest)
                .map(m -> TestMethodFactory.create(m, sourceDirectory))
                .filter(Objects::nonNull)
                .collect(Collectors.toList())
                .iterator();
    }

    /**
     * Check whether a CtMethod is a test by analysing its annotation.
     *
     * @param m method that is evaluated
     * @return True if the method is indeed a test
     */
    private boolean isTest(CtMethod m) {
        String runtimeAnnotation = "RuntimeVisibleAnnotations";
        return m.getMethodInfo().getAttributes().stream().anyMatch(attributeInfo -> attributeInfo.getName().equals(runtimeAnnotation) &&
                this.testAnnotations.contains(attributeInfo.toString())
        );

    }

    public String getName() {
        return this.ctClass.getName();
    }
}
