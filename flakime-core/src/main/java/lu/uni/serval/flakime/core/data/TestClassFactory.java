package lu.uni.serval.flakime.core.data;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import lu.uni.serval.flakime.core.utils.Logger;
import lu.uni.serval.flakime.core.utils.NameFilter;

import java.io.File;
import java.util.Set;

public class TestClassFactory {

    private TestClassFactory() throws IllegalAccessException {
        throw new IllegalAccessException("TestClassFactory should not be instantiated");
    }

    public static TestClass create(Logger logger, NameFilter annotationFilters, NameFilter methodFilters, String className, ClassPool classPool, File sourceFile, File outputDirectory) {
        try {
            classPool.importPackage(className);
            CtClass ctClass = classPool.get(className);
            return new TestClass(logger, annotationFilters, methodFilters, ctClass, sourceFile, outputDirectory);
        } catch (NotFoundException e) {
            return null;
        }
    }
}
