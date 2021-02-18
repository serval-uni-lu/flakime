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

    public static void instrument(TestMethod testMethod, float flakeRate, Strategy strategy) throws CannotCompileException {
        testMethod.addLocalVariable(randomVariableName, CtClass.doubleType);
        testMethod.addLocalVariable(flakimeInjectorFlag,CtClass.booleanType);
        testMethod.insertBefore(String.format("%s = %f;",randomVariableName, Math.random()));
        testMethod.insertBefore(String.format("%s = Boolean.parseBoolean(System.getenv(\"FLAKE_FLAG_FLAKIME\"));",flakimeInjectorFlag));

        for(int lineNumber: testMethod.getStatementLineNumbers()){
            final String payload = computePayload(testMethod, strategy, flakeRate, lineNumber);
            testMethod.insertAt(lineNumber, payload);
        }
    }

    private static String computePayload(TestMethod testMethod, Strategy strategy, float flakeRate, int lineNumber){
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
