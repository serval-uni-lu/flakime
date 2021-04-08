package lu.uni.serval.flakime.core.data;

import java.util.List;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TestClass implements Iterable<TestMethod> {
    private final Logger logger;
    private final Set<String> testAnnotations;
    private final CtClass ctClass;
    private final File outputDirectory;
    private final List<TestMethod> testMethods;
    private int nTestMethods = 0;

    public TestClass(Logger logger, Set<String> testAnnotations, CtClass ctClass, File sourceFile,
            File outputDirectory) {
        this.logger = logger;
        this.testAnnotations = testAnnotations;
        this.ctClass = ctClass;
        this.outputDirectory = outputDirectory;
        this.testMethods = Arrays.stream(ctClass.getDeclaredMethods()).filter(this::isTest)
                .map(m -> TestMethodFactory.create(logger, m, sourceFile, this.ctClass)).filter(Objects::nonNull)
                .filter(tm -> tm.getCtMethod().getMethodInfo().getCodeAttribute() != null).collect(Collectors.toList());

        nTestMethods = this.testMethods.size();

    }

    public void write() throws IOException, CannotCompileException {
        this.logger.debug(String.format("Write class to %s", outputDirectory.getAbsolutePath()));
        this.ctClass.writeFile(outputDirectory.getAbsolutePath());
    }

    @Override
    public Iterator<TestMethod> iterator() {
        return this.testMethods.iterator();
    }

    /**
     * Check whether a CtMethod is a test by analysing its annotation.
     *
     * @param m method that is evaluated
     * @return True if the method annotation is in {@code testAnnotations}
     */
    private boolean isTest(CtMethod m) {
        String methodName = m.getName();
        Pattern pattern = Pattern.compile("^test", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(methodName);
        boolean matchFound = matcher.find();
        if(matchFound)
            return true;

        String runtimeAnnotation = "RuntimeVisibleAnnotations";
        return m.getMethodInfo().getAttributes().stream()
                .anyMatch(attributeInfo -> attributeInfo.getName().equals(runtimeAnnotation)
                        && this.testAnnotations.contains(attributeInfo.toString()));

    }

    public String getName() {
        return this.ctClass.getName();
    }

    public List<TestMethod> getTestMethods() {
        return testMethods;
    }

    public int getnTestMethods() {
        return nTestMethods;
    }
}
