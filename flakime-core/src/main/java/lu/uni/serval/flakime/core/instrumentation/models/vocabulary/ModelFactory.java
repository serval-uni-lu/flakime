package anonymised.flakime.core.instrumentation.models.vocabulary;

import anonymised.flakime.core.utils.Logger;

public class ModelFactory {

    private ModelFactory() throws IllegalAccessException {
        throw new IllegalAccessException("Model factory should not be instantiated");
    }

    public static Model create(Model.Implementation implementation, Logger logger, int nTrees, int nThreads) {
        switch (implementation){
            case WEKA: return new WekaModel(logger, nTrees, nThreads);
            default: throw new IllegalArgumentException(String.format("Failed to create model. Expecting '%s' or '%s' but got '%s' instead",
                    Model.Implementation.STANDFORD,
                    Model.Implementation.WEKA,
                    implementation
            ));
        }
    }

    public static Model load(Model.Implementation implementation, Logger logger, String pathToModel) throws Exception {
        switch (implementation){
            case WEKA: return WekaModel.load(logger, pathToModel);
            default: throw new IllegalArgumentException(String.format("Failed to create model. Expecting '%s' or '%s' but got '%s' instead",
                    Model.Implementation.STANDFORD,
                    Model.Implementation.WEKA,
                    implementation
            ));
        }
    }
}
