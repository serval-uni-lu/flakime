package lu.uni.serval.instrumentation.strategies.vocabulary;

import org.apache.maven.plugin.logging.Log;
import weka.classifiers.trees.RandomForest;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializationHelper;

import java.io.*;


public class Model {



    private final RandomForest randomForest;
    private final Log logger;
    private boolean trainNeededFlag = true;


    /**
     * Parameterized Constructor to create the model based on a pre-trained RandomForest
     *
     * @param modelPath The path to the pre-trained RandomForest
     *
     */
    public Model(Log logger,String modelPath)  {
        RandomForest randomForest1;
        this.logger = logger;
        InputStream inputStream = null;
        randomForest1 = new RandomForest();
        try {
//            inputStream = new FileInputStream(modelPath);
//            ObjectInputStream objectInputStream = SerializationHelper.getObjectInputStream(inputStream);
            randomForest1 = (RandomForest) SerializationHelper.read(modelPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.logger.info("Successfully loader model");
//        this.randomForest = (RandomForest) SerializationHelper.read(modelPath);
        this.randomForest = randomForest1;
        this.trainNeededFlag = false;
    }
    public Model(Log logger,int nTrees, int nThreads) {
        this.logger = logger;
        RandomForest forest = new RandomForest();
        forest.setNumIterations(nTrees);
        forest.setSeed(0);
        forest.setDebug(true);
        forest.setNumExecutionSlots(nThreads);
        this.randomForest = forest;
    }

    public RandomForest getRandomForest() {
        return randomForest;
    }



    /**
     * Non parameterized constructor that builds a RandomForest from scratch
     *
     */
    public Model(Log logger){
        this.logger = logger;
        int nbtrees = 100;
        int random_state = 0;
        int numberOfThreads = 12;

        RandomForest forest = new RandomForest();
        forest.setNumIterations(nbtrees);
        forest.setSeed(random_state);
        forest.setDebug(true);
        forest.setNumExecutionSlots(numberOfThreads);

        this.randomForest = forest;

    }


    /**
     * Method that will fit the classifier to the given Instances.
     *
     * @param trainingInstances Instances representing the training set.
     * @throws Exception Thrown by classifier build
     */
    public void trainModel(Instances trainingInstances) throws Exception {

        float startTime = System.nanoTime();
        this.randomForest.buildClassifier(trainingInstances);
        this.trainNeededFlag = false;
        float endTime = System.nanoTime();

        this.logger.info(String.format("%nRFC took %.1f seconds to train%n",(endTime-startTime)/1000000000));

    }

    /**
     * Method to return the probability of flakiness of the given instance
     *
     * @param instance The instance to be labeled
     * @return A probability between 0.0 and 1.0
     * @throws Exception Throw if model needs to be trained
     */
    public double classify(Instance instance) throws Exception {
        if(this.trainNeededFlag){
            throw new IllegalStateException("The model is not fitted");
        }

        return this.randomForest.classifyInstance(instance) ;
    }

    public void save(String path) throws Exception {
        SerializationHelper.write(path,this.randomForest);
    }
}
