package lu.uni.serval.flakime.core.instrumentation;

import java.time.Instant;
import javassist.CannotCompileException;
import javassist.CtClass;
import lu.uni.serval.flakime.core.instrumentation.strategies.Strategy;
import lu.uni.serval.flakime.core.data.TestMethod;

/**
 * Executor to perform transformation.
 */
public class FlakimeInstrumenter {
    private static final String randomVariableName = "__FLAKIME_RANDOM_VARIABLE__" + Instant.now().getEpochSecond();
    private static final String disableFlagName = "__FLAKIME_DISABLE_FLAG__";

    /**
     * Method that will trigger the computation of the payload and the injection of the correct local variables.
     *
     * @param testMethod The targeted method.
     * @param strategy   The flakiness probability calculation strategy
     * @throws CannotCompileException Thrown if the local variables added do not respect the Java syntax
     */
    public static void instrument(TestMethod testMethod, Strategy strategy, String disableFlag) throws CannotCompileException {
//        testMethod.addLocalVariable(randomVariableName, CtClass.doubleType);
//        testMethod.insertBefore(String.format("%s = Math.random();", randomVariableName));

//        testMethod.addLocalVariable(flakimeDisableFlag, CtClass.booleanType);
//        testMethod.insertBefore(String.format("%s = Boolean.parseBoolean(System.getenv(\"%s\"));", disableFlagName, disableFlag));
        double randomDouble = Math.random();
        for (int lineNumber : testMethod.getStatementLineNumbers()) {
            final String payload = computePayload(testMethod, strategy, lineNumber,randomDouble, disableFlag);
            testMethod.insertAt(lineNumber+1, payload);
        }
    }

    /**
     * Method to compute the effective payload to be injected in the test methodpl
     *
     * @param testMethod The targeted test method
     * @param strategy   The flakiness probability calculation strategy
     * @param lineNumber The line number corresponding to the execution statement
     * @return The effective source code string to be injected.
     */
    private static String computePayload(TestMethod testMethod, Strategy strategy, int lineNumber, double randomDouble, String disableFlag) {
        final StringBuilder result = new StringBuilder();
        double probability = strategy.getTestFlakinessProbability(testMethod, lineNumber);
        //TODO Add environment var check to the inserted string
        if (probability > 0) {
            result.append("{if(!")
                    .append("Boolean.parseBoolean(System.getenv(\""+disableFlag+"\"))")
                    .append(" && (")
                    .append(randomDouble)
                    .append("<")
                    .append(probability)
                    .append("))")
                    .append("{")
                    .append("{")
                    .append("java.io.FileWriter fw = new java.io.FileWriter(\" output__"+testMethod.getName()+".out \",true);")
                    .append("fw.write(\"Flaked HERE:["+testMethod.getName()+"]["+lineNumber+"]["+probability+"]\\n\");")
                    .append("fw.close();")
                    .append("}")
                    .append("throw new Exception(\"")
                    .append("[flakinessProba:")
                    .append(probability)
                    .append("]\");}}");
        }

        return result.toString();
    }
}
