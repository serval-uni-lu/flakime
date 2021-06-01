package lu.uni.serval.flakime.core.instrumentation;

import java.io.File;
import java.time.Instant;
import java.util.Set;
import javassist.CannotCompileException;
import javassist.CtClass;
import lu.uni.serval.flakime.core.instrumentation.models.Model;
import lu.uni.serval.flakime.core.data.TestMethod;
import lu.uni.serval.flakime.core.instrumentation.models.uniform.UniformDistrubtionModel;
import lu.uni.serval.flakime.core.instrumentation.models.vocabulary.VocabularyModel;

/**
 * Executor to perform transformation.
 */
public class FlakimeInstrumenter {

    private FlakimeInstrumenter() throws IllegalAccessException {
        throw new IllegalAccessException("FlakimeInstrumenter should not be instantiated");
    }

    private static final String FLAKIME_OUTPUT_DIR = "__FLAKIME_OUTPUTDIR_" + Instant.now().getEpochSecond();
    private static final String RANDOM_VARIABLE_NAME = "__FLAKIME_RANDOM_VARIABLE__" + Instant.now().getEpochSecond();
    private static final String FLAKIME_DISABLE_FLAG = "__FLAKIME_DISABLE_FLAG__";


    /**
     * Method that triggers the computation of the payload and insert it at the given source code position
     *
     * @param testMethod The targeted test method
     * @param model The strategy to use (see {@link VocabularyModel}, {@link UniformDistrubtionModel}
     * @param outputDir The directory where the flake position report file will be written
     * @param disableFlag The environment variable name that control if the flake will occur
     * @param flakeRate The flake rate
     * @param disableReport flag to disable report or not.
     * @throws CannotCompileException if the source code compilation Fails
     */
    public static void instrument(TestMethod testMethod, Model model, File outputDir, String disableFlag, double flakeRate, boolean disableReport)
            throws CannotCompileException{
        testMethod.addLocalVariable(RANDOM_VARIABLE_NAME, CtClass.doubleType);
        testMethod.insertBefore(String.format("%s = Math.random();", RANDOM_VARIABLE_NAME));

        testMethod.addLocalVariable(FLAKIME_DISABLE_FLAG, CtClass.booleanType);
        testMethod.insertBefore(String.format("%s = Boolean.parseBoolean(System.getenv(\"%s\"));", FLAKIME_DISABLE_FLAG,disableFlag));

        Set<Integer> statementLineNumbers = testMethod.getStatementLineNumbers();
            for (int lineNumber : statementLineNumbers) {
                final String payload = computePayload(testMethod, model, lineNumber,outputDir,flakeRate,disableReport);
                testMethod.insertAt(lineNumber+1,payload);
            }

    }

    private static String prettyName(String name){
        return name.replace('.','_').replace("(","").replace(")","").replace("#","_");
    }

    /**
     * Method to compute the effective payload to be injected in the test method
     *
     * @param testMethod The targeted test method
     * @param model   The flakiness probability calculation strategy
     * @param lineNumber The line number corresponding to the execution statement
     * @return The effective source code string to be injected.
     */
    private static String computePayload(TestMethod testMethod, Model model, int lineNumber, File outputDir, double flakeRate, boolean disableReport) {
        final StringBuilder result = new StringBuilder();
        double probability = model.getTestFlakinessProbability(testMethod, lineNumber,flakeRate);
        String path = outputDir.getAbsolutePath().replace("\\","\\\\");

        String fileWritterString = "";
        if (!disableReport) fileWritterString = writeFileString(prettyName(testMethod.getLongName()),probability,path,lineNumber);

        String flakeCondition = flakeConditionString(probability);

        if (probability > 0) {
            result.append("if(")
                    .append(flakeCondition)
                    .append(")")
                    .append("{")
                    .append(fileWritterString)
                    .append("\n")
                    .append("throw new Exception(\"Flakime Exception\");")
                    .append("}")
                    .append("\n");
        }

        return result.toString();
    }

    private static String flakeConditionString(double probability){
       return String.format("(!%s) && (%s < %f)", FLAKIME_DISABLE_FLAG, RANDOM_VARIABLE_NAME,probability);
    }

    private static String writeFileString(String methodName,double probability,String path,int lineNumber){
        final String folderInitialization = String.format("java.io.File %s = new java.io.File(\"%s\");\n%s.mkdir();",
                FLAKIME_OUTPUT_DIR,
                path,
                FLAKIME_OUTPUT_DIR
        );

        final String fileName = String.format("_output_%s.out",
                methodName
        );

        final String declaration = String.format(
                "java.io.FileWriter fw = new java.io.FileWriter(new java.io.File(%s,\"%s\"),true);",
                FLAKIME_OUTPUT_DIR,
                fileName
        );

        final String filePayload = String.format("String.valueOf(%s)+\",%d,%.2f\\n\"","System.currentTimeMillis()",
                lineNumber,
                probability
        );

        return folderInitialization +
                "\n" +
                declaration +
                "\n" +
                "fw.write(" +
                filePayload +
                ");" +
                "\n" +
                "fw.close();";
    }
}
