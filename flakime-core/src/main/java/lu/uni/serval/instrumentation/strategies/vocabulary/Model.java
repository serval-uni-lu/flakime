package lu.uni.serval.instrumentation.strategies.vocabulary;

import org.apache.commons.lang3.ArrayUtils;
import org.deeplearning4j.nn.modelimport.keras.preprocessing.text.KerasTokenizer;
import org.deeplearning4j.nn.modelimport.keras.preprocessing.text.TokenizerMode;
import org.javatuples.Pair;
import weka.classifiers.Evaluation;
import weka.classifiers.trees.RandomForest;
import weka.core.*;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;


public class Model {

    private final RandomForest randomForest;
    private boolean trainNeededFlag = true;


    /**
     * Parameterized Constructor to create the model based on a pre-trained RandomForest
     *
     * @param modelPath The path to the pre-trained RandomForest
     * @throws Exception
     */
    public Model(String modelPath) throws Exception {
        this.randomForest = (RandomForest) SerializationHelper.read(modelPath);
        this.trainNeededFlag = false;
    }


    /**
     * Non parameterized constructor that builds a RandomForest from scratch
     *
     * @throws Exception
     */
    public Model() throws Exception {
        int nbtrees = 5;
        int random_state = 0;
        int numberOfThreads = 5;

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
     * @throws Exception
     */
    public void trainModel(Instances trainingInstances) throws Exception {
        float startTime = System.nanoTime();
        this.randomForest.buildClassifier(trainingInstances);
        this.trainNeededFlag = false;
        float endTime = System.nanoTime();

        System.out.printf("%nRFC took %.1f seconds to train%n",(endTime-startTime)/1000000000);

    }

    /**
     * Method to return the probability of flakiness of the given instance
     *
     * @param instance The instance to be labeled
     * @return A probability between 0.0 and 1.0
     * @throws Exception
     */
    public double classify(Instance instance) throws Exception {
        if(this.trainNeededFlag){
            throw new IllegalStateException("The model is not fitted");
        }

        return this.randomForest.classifyInstance(instance) ;
    }
}
