package lu.uni.serval.instrumentation.strategies.vocabulary;

import lu.uni.serval.data.Project;
import lu.uni.serval.data.TestClass;
import lu.uni.serval.data.TestMethod;
import lu.uni.serval.instrumentation.strategies.Strategy;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class VocabularyStrategy implements Strategy {
    private Model model;
    @Override
    public void preProcess(Project p) throws Exception {

        System.out.println("Entered preProcess()");
        final InputStream dataSource = VocabularyStrategy.class.getClassLoader().getResourceAsStream("data/vocabulary.json");
        final TrainingData originalTrainingData = new TrainingData(dataSource);
        Set<String> additionalMethodsBody = new HashSet<>();
        for(TestClass testClass: p){
            for(TestMethod testMethod: testClass){
                testMethod.getName();
                File f = testMethod.getSourceCodeFile();
                additionalMethodsBody.addAll(this.getTestMethodBodyText(f,testMethod));
            }
        }


        this.model = new Model(originalTrainingData,additionalMethodsBody);

        System.out.println("Registered body ");

    }

    @Override
    public String getProbabilityFunction(TestMethod test, int lineNumber) {

        return "Math.random()";
    }

    @Override
    public void postProcess() {

    }

    /**
     * Retrieve the text set corresponding to a method in the source file
     *
     * @param f the source file
     * @param method the corresponding {@code TestMethod} instance
     * @return the set of text corresponding to the method
     * @throws IOException
     */
    public Set<String> getTestMethodBodyText(File f,TestMethod method) throws IOException {
        List<String> sb = new ArrayList<>();
        Set<String> resultBody = new HashSet<>();
        BufferedReader br = new BufferedReader(new FileReader(f));
        String line;
        while ((line = br.readLine()) != null) {
            sb.add(line);
        }
        int size = method.getControlFlow().basicBlocks().length;

        for(int i = 0 ; i <size ; i++){
            String res = sb.get(method.getCtMethod().getMethodInfo().getLineNumber(i));
            resultBody.add(res);
//            System.out.printf("[%s] Result: ----[%s]----%n",method.getName(),res);
        }

        return resultBody;
    }
}
