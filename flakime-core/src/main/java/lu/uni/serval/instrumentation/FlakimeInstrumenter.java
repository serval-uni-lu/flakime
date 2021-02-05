package lu.uni.serval.instrumentation;

import javassist.*;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.analysis.ControlFlow;
import lu.uni.serval.instrumentation.strategies.Strategy;
import lu.uni.serval.model.TestMethod;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Executor to perform transformation.
 *
 */
public class FlakimeInstrumenter {

    private final List<String> testAnnotations;
    private final float flakeRate;
    private final Strategy strategy;

    /**
     * Create an instrumenter with the specified parameters
     *
     * @param testAnnotations The annotation class which tests are annotated
     * @param flakeRate The flake rate of the tests
     * @param strategy The flakiness probability calculation strategy
     */
    public FlakimeInstrumenter(List<String> testAnnotations, float flakeRate, Strategy strategy) {
        this.testAnnotations = testAnnotations;
        this.flakeRate=flakeRate;
        this.strategy = strategy;
    }

    /**
     * Create new instance of a ClassPool.
     *
     * @return never {@code null}
     */
    protected ClassPool buildClassPool() {
        return new ClassPool(ClassPool.getDefault());
    }

    /**
     * Configure the passed instance of a ClassPool and append required class paths on it
     *
     * @param classPool the ClassPool instance to append
     * @param inputDir A Directory of classes
     * @return The modified ClassPool
     * @throws NotFoundException if passed {@code classPool} is {@code null} or if passed
     *          {@code inputDir} is a JAR or ZIP and not found
     */
    protected ClassPool configureClassPool(final ClassPool classPool, final String inputDir)
            throws NotFoundException {
        classPool.childFirstLookup = true;
        classPool.appendClassPath(inputDir);
        classPool.appendClassPath(new LoaderClassPath(Thread.currentThread().getContextClassLoader()));
        classPool.appendSystemPath();
//        debugClassLoader(classPool);
        return classPool;
    }

    /**
     * Evaluates and returns the output directory/
     *
     * If the passed {@code outputDir} is {@code null} or empty, the passed {@code inputDir} otherwise
     * the {@code outputDir} will returned.
     *
     * @param outputDir The class output directory
     * @param inputDir The class input directory
     * @return The computed output directory
     */
    protected String evaluateOutputDirectory(final String outputDir, final String inputDir) {
        return outputDir != null && !outputDir.trim().isEmpty() ? outputDir : inputDir.trim();
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

    /**
     * Insert a condition after each instruction that raises an exception based on probabilities.
     *
     * @param inDirectory Directory of the compiled test classes
     * @param outputDir Directory where the resulting modified test classes will be written
     * @param targetClass Test class name
     */
    public void instrument(String inDirectory,String outputDir,String targetClass){
        try {

            final ClassPool classPool = configureClassPool(buildClassPool(), inDirectory);
            final String outDirectory = evaluateOutputDirectory(outputDir, inDirectory);
            classPool.importPackage(targetClass);
            CtClass ctClass = classPool.get(targetClass);

            //Filter test methods
            List<CtMethod> ctMethods = Arrays.stream(ctClass.getDeclaredMethods()).filter(this::isTest).collect(Collectors.toList());

            //Iterate on all the test method
            for(CtMethod m: ctMethods){
                ControlFlow cf = new ControlFlow(m);
                TestMethod testMethod = new TestMethod();
                //Unique id used to create a local variable (should not be present in the initial source code)
                String uidRandomVariable = "flakimeRandomVar____";
                String uidFlakinessProbability = "flakimeFlakinessProbability____";
                m.addLocalVariable(uidRandomVariable,CtClass.doubleType);
                m.addLocalVariable(uidFlakinessProbability,CtClass.doubleType);

                //Initialize the random variable (should be set for each method) AND Insert computed flakiness probability
                m.insertAt(0,String.format("%s = %f;",uidRandomVariable,Math.random()));
                m.insertAt(0,String.format("%s = %f;",uidFlakinessProbability,this.strategy.computeProbability(testMethod)));
                ControlFlow.Block[] blocks = cf.basicBlocks();



                //Retrieve the next line after each statement blocks and insert the flakiness condition
                Arrays.stream(blocks)
                        .map(block -> m.getMethodInfo().getLineNumber(block.position())+1)
                        .collect(Collectors.toSet())
                        .forEach(n -> {
                            String function = "if("+uidRandomVariable+"< "+uidFlakinessProbability+"*"+this.flakeRate+"){throw new Exception(\"Test is flaky (flake rate: "+this.flakeRate+") :\"+"+uidRandomVariable+"+\" \");}";
                            try {
                                m.insertAt(n, function);
                            } catch (CannotCompileException e) {
                                e.printStackTrace();
                            }
                        });
            }
            //Write out the final modified class file
            ctClass.writeFile(outDirectory);
        } catch (NotFoundException | CannotCompileException | IOException | BadBytecode e) {
            e.printStackTrace();
        }

    }

}
