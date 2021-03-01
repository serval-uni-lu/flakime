package lu.uni.serval.flakime.core.instrumentation.strategies;

import java.util.Optional;
import lu.uni.serval.flakime.core.instrumentation.strategies.bernoulli.BernoulliStrategy;
import lu.uni.serval.flakime.core.instrumentation.strategies.vocabulary.VocabularyStrategy;
import lu.uni.serval.flakime.core.utils.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Properties;

public class StrategyFactory {
    /**
     * Factory pattern implementation. Returns the strategy instance from the corresponding type.
     *
     * @param name The strategy name
     * @param properties The strategy properties
     * @param logger The logger instance
     * @return The strategy instance
     * @throws ClassNotFoundException Thrown if the requested strategy does not exists
     * @throws FileNotFoundException Thrown if the file corresponding to the model is not found.
     */
    public static Strategy fromName(String name, Properties properties, Logger logger) throws ClassNotFoundException, FileNotFoundException {
        if(name.trim().equalsIgnoreCase("bernoulli")){
            return  new BernoulliStrategy(logger);
        }

        if (name.trim().equalsIgnoreCase("vocabulary")){
            final String nTrees = properties.getProperty("randomForestTrees", String.valueOf(50));
            final String nCores = properties.getProperty("randomForestThreads", String.valueOf(Runtime.getRuntime().availableProcessors()));
            boolean trainModel = Boolean.parseBoolean(properties.getProperty("trainModel", "true"));

            String pathModel = "";

            pathModel = Optional.ofNullable(properties.getProperty("modelPath")).orElse("rfc_classifier");
            if(!trainModel){

                if(!new File(pathModel).exists()){
                    throw new FileNotFoundException(String.format("Cannot find modelPath set at '%s'", pathModel));
                }
            }

            final VocabularyStrategy strategy = new VocabularyStrategy(logger);
            strategy.setnThreads(Integer.parseInt(nCores));
            strategy.setnTrees(Integer.parseInt(nTrees));
            strategy.setTrainModel(trainModel);
            strategy.setPathToModel(pathModel);

            return strategy;
        }

        throw new ClassNotFoundException(String.format("Cannot find strategy with name: %s", name));
    }
}
