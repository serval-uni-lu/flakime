package lu.uni.serval.flakime.core.data;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtMethod;
import lu.uni.serval.flakime.core.utils.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class TestClass implements Iterable<TestMethod> {
    private final Logger logger;
    private final Set<String> testAnnotations;
    private final CtClass ctClass;
    private final File sourceFile;
    private final File outputDirectory;

    public TestClass(Logger logger, Set<String> testAnnotations, CtClass ctClass, File sourceFile, File outputDirectory) {
        this.logger = logger;
        this.testAnnotations = testAnnotations;
        this.ctClass = ctClass;
        this.sourceFile = sourceFile;
        this.outputDirectory = outputDirectory;
    }

    public void write() throws IOException, CannotCompileException {
        this.logger.debug(String.format("Write class to %s", outputDirectory.getAbsolutePath()));
        this.ctClass.writeFile(outputDirectory.getAbsolutePath());
    }

    @Override
    public Iterator<TestMethod> iterator(){
        return Arrays.stream(ctClass.getDeclaredMethods())
                .filter(TestClass.this::isTest)
                .map(m -> TestMethodFactory.create(logger, m, sourceFile))
                .filter(Objects::nonNull)
                .collect(Collectors.toList())
                .iterator();
    }

    /**
     * Check whether a CtMethod is a test by analysing its annotation.
     *
     * @param m method that is evaluated
     * @return True if the method annotation is in {@code testAnnotations}
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
