package lu.uni.serval;

import javassist.*;
import javassist.bytecode.AnnotationsAttribute;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FlakimeInstrumenter {


    private List<CtMethod> ctMethods;
    public static final Set<String> testAnnotations = new HashSet<>();
    static {
        testAnnotations.add("@org.junit.jupiter.api.Test");
        testAnnotations.add("@org.junit.Test");
    }

    public FlakimeInstrumenter() {
    }

    protected ClassPool buildClassPool() {
        // create new classpool for transform; don't blow up the default
        return new ClassPool(ClassPool.getDefault());
    }

    protected ClassPool configureClassPool(final ClassPool classPool, final String inputDir)
            throws NotFoundException {
        classPool.childFirstLookup = true;
        classPool.appendClassPath(inputDir);
        classPool.appendClassPath(new LoaderClassPath(Thread.currentThread().getContextClassLoader()));
        classPool.appendSystemPath();
//        debugClassLoader(classPool);
        return classPool;
    }

    protected String evaluateOutputDirectory(final String outputDir, final String inputDir) {
        return outputDir != null && !outputDir.trim().isEmpty() ? outputDir : inputDir.trim();
    }

    private boolean isTest(CtMethod m) {
        String runtimeAnnotation = "RuntimeVisibleAnnotations";
//        System.out.println(m.getMethodInfo().getAttribute(runtimeAnnotation).toString());
        return m.getMethodInfo().getAttributes().stream().anyMatch(attributeInfo -> {
//                System.out.println("Attribute info name"+attributeInfo.getName());
//                System.out.println("Attribute info toString"+attributeInfo.toString());
                return attributeInfo.getName().equals(runtimeAnnotation) &&
                        testAnnotations.contains(attributeInfo.toString());
                }
//                )
        );

    }

    public void instrument(String inDirectory,String outputDir,String targetClass){
        try {

            final ClassPool classPool = configureClassPool(buildClassPool(), inDirectory);
            final String outDirectory = evaluateOutputDirectory(outputDir, inDirectory);
            classPool.importPackage(targetClass);
            CtClass ctClass = classPool.get(targetClass);


            this.ctMethods = Arrays.stream(ctClass.getDeclaredMethods()).filter(this::isTest).collect(Collectors.toList());

            for(CtMethod m: ctMethods){
                m.setBody(getNewBody(m));
            }

//            List<String> res = ctMethods.stream().map(m -> m.getMethodInfo().getName()).collect(Collectors.toList());

            ctClass.writeFile(outDirectory);
        } catch (NotFoundException | CannotCompileException | IOException e) {
            e.printStackTrace();
        }

    }

    public String getNewBody(CtMethod ctMethod){
        return "throw new Exception(\"Custom exception\");";
    }
}
