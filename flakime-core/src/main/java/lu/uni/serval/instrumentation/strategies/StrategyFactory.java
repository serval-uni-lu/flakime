package lu.uni.serval.instrumentation.strategies;

import lu.uni.serval.instrumentation.strategies.bernoulli.BernoulliStrategy;
import lu.uni.serval.instrumentation.strategies.vocabulary.VocabularyStrategy;

public class StrategyFactory {
    public static Strategy fromName(String name) throws ClassNotFoundException {
        if(name.trim().equalsIgnoreCase("bernoulli")){
            return new BernoulliStrategy();
        }

        if (name.trim().equalsIgnoreCase("vocabulary")){
            return new VocabularyStrategy();
        }

        throw new ClassNotFoundException(String.format("Cannot find strategy with name: %s", name));
    }
}
