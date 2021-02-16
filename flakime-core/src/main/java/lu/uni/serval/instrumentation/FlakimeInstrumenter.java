package lu.uni.serval.instrumentation;

import javassist.CannotCompileException;
import lu.uni.serval.data.TestMethod;
import lu.uni.serval.instrumentation.strategies.Strategy;

import java.time.Instant;

/**
 * Executor to perform transformation.
 *
 */
public class FlakimeInstrumenter {
    private static final String randomVariableName = "__FLAKIME_RANDOM_VARIABLE__" + Instant.now().getEpochSecond();

    public static void instrument(TestMethod testMethod, float flakeRate, Strategy strategy) throws CannotCompileException {
        testMethod.addLocalVariableDouble(randomVariableName);
        testMethod.insertAt(0,String.format("%s = %f;",randomVariableName, Math.random()));

        for(int lineNumber: testMethod.getStatementLineNumbers()){
            final String payload = computePayload(testMethod, strategy, flakeRate, lineNumber);
            System.out.println("Inserting ***"+payload+"***");
            testMethod.insertAt(lineNumber + 1, payload);
        }
    }

    private static String computePayload(TestMethod testMethod, Strategy strategy, float flakeRate, int lineNumber){
        //Rewrite so as the flakerate is taken in account only if the calculated flakiness proba is > 0

        StringBuilder result = new StringBuilder();
        String probability = strategy.getProbabilityFunction(testMethod, lineNumber);

        if(Double.parseDouble(probability) > 0){
            result.append("if(")
//                    .append("+")
                    .append(randomVariableName)
//                    .append("+")
                    .append("<")
                    .append(probability)
                    .append( "){throw new Exception(\"" )
                    .append("[flakinessProba:")
                    .append(probability)
                    .append("]\");}");

        }

        return result.toString();
    }

}
