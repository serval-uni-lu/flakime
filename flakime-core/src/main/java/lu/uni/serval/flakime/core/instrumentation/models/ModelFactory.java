package lu.uni.serval.flakime.core.instrumentation.models;

import lu.uni.serval.flakime.core.instrumentation.models.uniform.UniformDistrubtionModel;
import lu.uni.serval.flakime.core.instrumentation.models.vocabulary.VocabularyModel;
import lu.uni.serval.flakime.core.utils.Logger;

import java.io.FileNotFoundException;
import java.util.Properties;

public class ModelFactory {

    private ModelFactory() throws IllegalAccessException {
        throw new IllegalAccessException("Strategy Factory should not be instantiated");
    }


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
    public static Model fromName(String name, Properties properties, Logger logger) throws ClassNotFoundException, FileNotFoundException {
        if(name.trim().equalsIgnoreCase("uniformDistribution")){
            return  new UniformDistrubtionModel(logger);
        }

        if (name.trim().equalsIgnoreCase("vocabulary")){
            final String nTrees = properties.getProperty("randomForestTrees", String.valueOf(100));
            final String nCores = properties.getProperty("randomForestThreads", String.valueOf(Runtime.getRuntime().availableProcessors()));
            boolean trainModel = Boolean.parseBoolean(properties.getProperty("forceTraining", "false"));

            String pathModel = "";

            pathModel = properties.getProperty("modelPath");

            final VocabularyModel strategy = new VocabularyModel(logger);
            strategy.setnThreads(Integer.parseInt(nCores));
            strategy.setnTrees(Integer.parseInt(nTrees));
            strategy.setTrainModel(trainModel);
            strategy.setPathToModel(pathModel);

            return strategy;
        }

        throw new ClassNotFoundException(String.format("Cannot find strategy with name: %s", name));
    }
}
