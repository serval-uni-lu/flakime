package lu.uni.serval.flakime.core.data;

import java.util.List;

import javassist.CannotCompileException;
import javassist.CtClass;
import lu.uni.serval.flakime.core.utils.Logger;
import lu.uni.serval.flakime.core.utils.NameFilter;
import lu.uni.serval.flakime.core.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import java.util.stream.Collectors;

public class TestClass implements Iterable<TestMethod> {
    private final Logger logger;
    private final CtClass ctClass;
    private final File outputDirectory;
    private final List<TestMethod> testMethods;
    private final int nTestMethods;

    public TestClass(Logger logger, NameFilter annotationFilters, NameFilter methodFilters, CtClass ctClass, File sourceFile, File outputDirectory) {
        this.logger = logger;
        this.ctClass = ctClass;
        this.outputDirectory = outputDirectory;
        this.testMethods = Arrays.stream(ctClass.getDeclaredMethods())
                .filter(ctMethod -> Utils.isTest(ctMethod,methodFilters,annotationFilters))
                .map(m -> TestMethodFactory.create(logger, m, sourceFile, this.ctClass))
                .filter(Objects::nonNull)
                .filter(tm -> tm.getCtMethod().getMethodInfo().getCodeAttribute() != null)
                .collect(Collectors.toList());

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
