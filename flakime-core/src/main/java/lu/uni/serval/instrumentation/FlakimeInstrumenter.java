package lu.uni.serval.instrumentation;

import javassist.CannotCompileException;
import javassist.CtClass;
import lu.uni.serval.data.TestMethod;
import lu.uni.serval.instrumentation.strategies.Strategy;

import java.time.Instant;

/**
 * Executor to perform transformation.
 *
 */
public class FlakimeInstrumenter {
    private static final String randomVariableName = "__FLAKIME_RANDOM_VARIABLE__" + Instant.now().getEpochSecond();
    private static final String flakimeInjectorFlag = "__FLAKIME_INJECTOR_FLAG__";

    /**
     * Method that will trigger the computation of the payload and the injection of the correct local variables.
     *
     * @param testMethod The targeted method.
     * @param strategy The flakiness probability calculation strategy
     * @throws CannotCompileException Thrown if the local variables added do not respect the Java syntax
     */
    public static void instrument(TestMethod testMethod, Strategy strategy) throws CannotCompileException {
        testMethod.addLocalVariable(randomVariableName, CtClass.doubleType);
        testMethod.addLocalVariable(flakimeInjectorFlag,CtClass.booleanType);
        testMethod.insertBefore(String.format("%s = %f;",randomVariableName, Math.random()));
        testMethod.insertBefore(String.format("%s = Boolean.parseBoolean(System.getenv(\"FLAKE_FLAG_FLAKIME\"));",flakimeInjectorFlag));

        for(int lineNumber: testMethod.getStatementLineNumbers()){
            final String payload = computePayload(testMethod, strategy, lineNumber);
            testMethod.insertAt(lineNumber, payload);
        }
    }

    /**
     * Method to compute the effective payload to be injected in the test method
     *
     * @param testMethod The targeted test method
     * @param strategy The flakiness probability calculation strategy
     * @param lineNumber The line number corresponding to the execution statement
     * @return The effective source code string to be injected.
     */
    private static String computePayload(TestMethod testMethod, Strategy strategy, int lineNumber){
        StringBuilder result = new StringBuilder();
        String probability = strategy.getProbabilityFunction(testMethod, lineNumber);

        //TODO Add environment var check to the inserted string
        if(Double.parseDouble(probability) > 0){
            result.append("if( ")
                    .append(flakimeInjectorFlag)
                    .append(" && (")
                    .append(randomVariableName)
                    .append("<")
                    .append(probability)
                    .append( ")){throw new Exception(\"" )
                    .append("[flakinessProba:")
                    .append(probability)
                    .append("]\");}");

        }

        return result.toString();
    }

}
