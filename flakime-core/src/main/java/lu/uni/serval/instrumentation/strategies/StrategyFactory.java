package lu.uni.serval.instrumentation.strategies;

import lu.uni.serval.instrumentation.strategies.bernoulli.BernoulliStrategy;
import lu.uni.serval.instrumentation.strategies.vocabulary.VocabularyStrategy;
import org.apache.maven.plugin.logging.Log;

import java.io.FileNotFoundException;
import java.util.Optional;
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
    public static Strategy fromName(String name, Properties properties, Log logger) throws ClassNotFoundException, FileNotFoundException {
        if(name.trim().equalsIgnoreCase("bernoulli")){
            return  new BernoulliStrategy(logger);
        }

        if (name.trim().equalsIgnoreCase("vocabulary")){
            boolean trainModel = Boolean.parseBoolean(Optional.ofNullable(properties.getProperty("trainModel")).orElse("true"));
            String nTrees = Optional.ofNullable(properties.getProperty("randomForestTrees")).orElse(String.valueOf(100));
            String nCores = Optional.ofNullable(properties.getProperty("randomForestThreads")).orElse(String.valueOf(1));
            String pathModel = "";
            if(!trainModel){
                pathModel = Optional.ofNullable(properties.getProperty("modelPath")).orElseThrow(() -> new FileNotFoundException("Path to the pre-trained model must be provided"));
            }
            VocabularyStrategy strategy = new VocabularyStrategy(logger);
            strategy.setnThreads(Integer.parseInt(nCores));
            strategy.setnTrees(Integer.parseInt(nTrees));
            strategy.setTrainModel(trainModel);
            strategy.setPathToModel(pathModel);

            return strategy;
        }

        throw new ClassNotFoundException(String.format("Cannot find strategy with name: %s", name));
    }
}
