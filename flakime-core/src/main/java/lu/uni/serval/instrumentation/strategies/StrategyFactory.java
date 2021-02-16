package lu.uni.serval.instrumentation.strategies;

import lu.uni.serval.instrumentation.strategies.bernoulli.BernoulliStrategy;
import lu.uni.serval.instrumentation.strategies.vocabulary.VocabularyStrategy;

import java.io.FileNotFoundException;
import java.util.Optional;
import java.util.Properties;

public class StrategyFactory {
    public static Strategy fromName(String name, Properties properties) throws ClassNotFoundException, FileNotFoundException {
        //TODO Take into account Strategy properties
        if(name.trim().equalsIgnoreCase("bernoulli")){
            return new BernoulliStrategy();
        }

        if (name.trim().equalsIgnoreCase("vocabulary")){
            boolean trainModel = Boolean.parseBoolean(Optional.of(properties.getProperty("trainModel")).orElse("true"));
            String nTrees = Optional.of(properties.getProperty("randomForestTrees")).orElse(String.valueOf(100));
            String nCores = Optional.of(properties.getProperty("randomForestThreads")).orElse(String.valueOf(1));
            String pathModel = "";
            if(!trainModel){
                pathModel = Optional.ofNullable(properties.getProperty("modelPath")).orElseThrow(() -> new FileNotFoundException("Path to the pre-trained model must be provided"));
            }

            return new VocabularyStrategy(pathModel,trainModel,Integer.parseInt(nTrees),Integer.parseInt(nCores));
        }

        throw new ClassNotFoundException(String.format("Cannot find strategy with name: %s", name));
    }
}
