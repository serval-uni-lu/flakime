package lu.uni.serval.flakime.core.instrumentation.strategies.vocabulary;

import lu.uni.serval.flakime.core.utils.Logger;
import org.apache.commons.lang3.NotImplementedException;

public class ModelFactory {
    public static Model create(Model.Implementation implementation, Logger logger, int nTrees, int nThreads) {
        switch (implementation){
            case STANDFORD: return new StandfordModel(logger);
            case WEKA: return new WekaModel(logger, nTrees, nThreads);
        }

        throw new IllegalArgumentException(String.format("Failed to create model. Expecting '%s' or '%s' but got '%s' instead",
                Model.Implementation.STANDFORD,
                Model.Implementation.WEKA,
                implementation
        ));
    }

    public static Model load(Model.Implementation implementation, Logger logger, String pathToModel) throws Exception {
        switch (implementation){
            case STANDFORD: throw new NotImplementedException("Standford model not implemented yet");
            case WEKA: return WekaModel.load(logger, pathToModel);
        }

        throw new IllegalArgumentException(String.format("Failed to load model. Expecting '%s' or '%s' but got '%s' instead",
                Model.Implementation.STANDFORD,
                Model.Implementation.WEKA,
                implementation
        ));
    }
}
