package lu.uni.serval.data;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;

import java.io.File;
import java.util.Set;

public class TestClassFactory {
    public static TestClass create(Set<String> testAnnotations, String className, ClassPool classPool, File sourceFile, File outputDirectory) {
        try {
            classPool.importPackage(className);
            CtClass ctClass = classPool.get(className);
            return new TestClass(testAnnotations, ctClass, sourceFile, outputDirectory);
        } catch (NotFoundException e) {
            return null;
        }
    }
}
