package lu.uni.serval.flakime.core.instrumentation;

import java.io.File;
import java.time.Instant;
import java.util.Set;
import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.bytecode.BadBytecode;
import lu.uni.serval.flakime.core.instrumentation.strategies.Strategy;
import lu.uni.serval.flakime.core.data.TestMethod;

/**
 * Executor to perform transformation.
 */
public class FlakimeInstrumenter {
    private static final String randomVariableName = "__FLAKIME_RANDOM_VARIABLE__" + Instant.now().getEpochSecond();
    private static final String flakimeDisableFlag = "__FLAKIME_DISABLE_FLAG__";


    /**
     * Method that triggers the computation of the payload and insert it at the given source code position
     *
     * @param testMethod The targeted test method
     * @param strategy The strategy to use (see {@link lu.uni.serval.flakime.core.instrumentation.strategies.vocabulary.VocabularyStrategy}, {@link lu.uni.serval.flakime.core.instrumentation.strategies.bernoulli.BernoulliStrategy}
     * @param outputDir The directory where the flake position report file will be written
     * @param disableFlag The environment variable name that control if the flake will occur
     * @param flakeRate The flake rate
     * @throws CannotCompileException if the source code compilation Fails
     */
    public static void instrument(TestMethod testMethod, Strategy strategy, File outputDir,String disableFlag,double flakeRate)
            throws CannotCompileException{
        testMethod.addLocalVariable(randomVariableName, CtClass.doubleType);
        testMethod.insertBefore(String.format("%s = Math.random();", randomVariableName));

        testMethod.addLocalVariable(flakimeDisableFlag, CtClass.booleanType);
        testMethod.insertBefore(String.format("%s = Boolean.parseBoolean(System.getenv(\"%s\"));", flakimeDisableFlag,disableFlag));

        double randomDouble = Math.random();
        Set<Integer> statementLineNumbers = testMethod.getStatementLineNumbers();
        for (int lineNumber : statementLineNumbers) {
            final String payload = computePayload(testMethod, strategy, lineNumber,outputDir,flakeRate);
//            System.out.println("Inserting #["+payload+"]#");
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
     * @param strategy   The flakiness probability calculation strategy
     * @param lineNumber The line number corresponding to the execution statement
     * @return The effective source code string to be injected.
     */
    private static String computePayload(TestMethod testMethod, Strategy strategy, int lineNumber,File outputDir,double flakeRate) {
        final StringBuilder result = new StringBuilder();
        double probability = strategy.getTestFlakinessProbability(testMethod, lineNumber,flakeRate);
        String path = outputDir.getAbsolutePath().replace("\\","\\\\");
        outputDir.mkdir();

        String fileWritterString = writeFileString(prettyName(testMethod.getName()),probability,path,lineNumber);
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
        String s = String.format("(!%s) && (%s < %f)",flakimeDisableFlag,randomVariableName,probability);
        return s;
    }

    private static String writeFileString(String methodName,double probability,String path,int lineNumber){
        StringBuilder result = new StringBuilder();
        String fileName = String.format("_output_%s.out",methodName);
        String declaration = String.format(
                "java.io.FileWriter fw = new java.io.FileWriter(new java.io.File(\"%s\",\"%s\"),true);",
                path, fileName);
        result.append(declaration);
        result.append("\n");
        result.append("fw.write(");
        String filePayload = String.format("String.valueOf(%s)+\",%d,%.2f\\n\"","System.currentTimeMillis()",lineNumber,probability);
        result.append(filePayload).append(");");
        result.append("\n");
        result.append("fw.close();");
        return result.toString();
    }

//    private static String computePayload_2(TestMethod testMethod, Strategy strategy, int lineNumber,double randomDouble) {
//        final StringBuilder result = new StringBuilder();
//        double probability = strategy.getTestFlakinessProbability(testMethod, lineNumber);
//
//        if (probability > 0) {
//            result.append("{if(!")
//                    .append("Boolean.parseBoolean(System.getenv(\"FLAKIME_DISABLE\"))")
//                    .append(" && (")
//                    .append(randomDouble)
//                    .append("<")
//                    .append(probability)
//                    .append("))")
//                    .append("{")
//                    .append("{")
//                    .append("java.io.FileWriter fw = new java.io.FileWriter(\" output__"+testMethod.getName()+".out \",true);")
//                    .append("fw.write(\"Flaked HERE:["+testMethod.getName()+"]["+lineNumber+"]["+probability+"]\\n\");")
//                    .append("fw.close();")
//                    .append("}")
//                    .append("System.err.println(\"flakked\")")
//                    .append("")
//                    .append("")
//                    .append(";}}");
//        }
//
//        return result.toString();
//    }
//
//    private static String computePayload_3(TestMethod testMethod, Strategy strategy, int lineNumber,double randomDouble) {
//        final StringBuilder result = new StringBuilder();
//        double probability = strategy.getTestFlakinessProbability(testMethod, lineNumber);
//
//        if (probability > 0) {
//            result
//
//                    .append(";{")
////                    .append("Object flakime = null;")
//                    .append("System.err.println(\"Testhere "+lineNumber+"\");")
//                    .append("}");
//
//        }
//
//        return result.toString();
//    }
}
