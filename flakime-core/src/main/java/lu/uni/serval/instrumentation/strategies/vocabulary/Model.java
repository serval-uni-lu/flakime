package lu.uni.serval.instrumentation.strategies.vocabulary;

import org.apache.commons.lang3.ArrayUtils;
import org.deeplearning4j.nn.modelimport.keras.preprocessing.text.KerasTokenizer;
import org.deeplearning4j.nn.modelimport.keras.preprocessing.text.TokenizerMode;
import org.javatuples.Pair;
import weka.classifiers.Evaluation;
import weka.classifiers.trees.RandomForest;
import weka.core.*;

import java.util.*;
import java.util.stream.Collectors;

public class Model {

    private String result;
    private final RandomForest randomForest;
    private boolean trainNeededFlag = true;

    public String getResult() {
        return result;

    }

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

    public void trainModel(Instances trainingInstances) throws Exception {
        float startTime = System.nanoTime();
        this.randomForest.buildClassifier(trainingInstances);
        this.trainNeededFlag = false;
        float endTime = System.nanoTime();

        System.out.printf("%nRFC took %.1f seconds to train%n",(endTime-startTime)/1000000000);

    }

    public double classify(Instance instance) throws Exception {
        if(this.trainNeededFlag){
            throw new IllegalStateException("The model is not fitted");
        }

        return this.randomForest.classifyInstance(instance) ;
    }
}
