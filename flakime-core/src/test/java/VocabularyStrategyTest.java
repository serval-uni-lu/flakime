import javassist.NotFoundException;
import lu.uni.serval.data.Project;
import lu.uni.serval.data.TestClass;
import lu.uni.serval.data.TestMethod;
import lu.uni.serval.instrumentation.strategies.vocabulary.TrainingData;
import lu.uni.serval.instrumentation.strategies.vocabulary.VocabularyStrategy;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class VocabularyStrategyTest {

    private VocabularyStrategy strategy;
    private MavenProject mavenProject;
    private Set<String> testAnnotations = new HashSet<>();
    private String testClassDirectory = "target/test-classes/" ;
    private String testSourceDirectory = "src/test/java/";
    private Project project;
    private TrainingData originalTrainingData;

    @BeforeAll
    public void setup() throws Exception {
        testAnnotations.add("@org.junit.jupiter.api.Test");
        String projectName = "test-project-1";
        URL resource = VocabularyStrategyTest.class.getClassLoader().getResource(projectName);

        assert resource != null;
        String pomfile = resource.getPath()+"/"+"pom.xml";
        this.mavenProject = loadMavenProject(pomfile);
//        initializeProject();

        final InputStream dataSource = VocabularyStrategyTest.class.getClassLoader().getResourceAsStream("data/vocabulary.json");
        this.originalTrainingData = new TrainingData(dataSource);

    }
    @Test
    public void ModelInitTest() {
        Set<String> additionalTrainingData = new HashSet<String>(Collections.singletonList("assertEqual(1,1)"));
        try {
            lu.uni.serval.instrumentation.strategies.vocabulary.Model rfcModel = new lu.uni.serval.instrumentation.strategies.vocabulary.Model();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public MavenProject loadMavenProject(String pomfile){
        this.strategy = new VocabularyStrategy();
        Model model = null;
        FileReader reader = null;
        MavenXpp3Reader mavenreader = new MavenXpp3Reader();
        try {
            reader = new FileReader(pomfile);
            model = mavenreader.read(reader);
            model.setPomFile(new File(pomfile));
        }catch(Exception ex){
            ex.printStackTrace();
        }
        return new MavenProject(model);
    }

    public void initializeProject() throws NotFoundException, DependencyResolutionRequiredException {
        System.out.println(getDirectory(testClassDirectory));
        Project p = new Project(
                testAnnotations,
                getDirectory(testClassDirectory),
                getDirectory(testSourceDirectory),
                (List<String>)this.mavenProject.getTestClasspathElements()
        );

        p.getClassNames().forEach(System.out::println);
        this.project = p;


    }

    private File getDirectory(String path) {
        final File directory = new File(path);

        if (directory.isAbsolute()) {
            return directory;
        } else {
            return new File(mavenProject.getBasedir(), path);
        }
    }

    @Test
    public void loadTrainingDataTest() throws Exception {
        final InputStream dataSource = VocabularyStrategyTest.class.getClassLoader().getResourceAsStream("data/vocabulary.json");
        final TrainingData originalTrainingData = new TrainingData(dataSource);
        int size = originalTrainingData.getEntries().size();
        assertEquals(16053,size);
    }

//    @Test
//    public void extractTestMethodBodyTest() throws IOException {
//        Set<String> additionalMethodsBody = new HashSet<>();
//        for(TestClass testClass: this.project){
//            for(TestMethod testMethod: testClass){
//                testMethod.getName();
//                File f = testMethod.getSourceCodeFile();
//                additionalMethodsBody.addAll(this.strategy.getTestMethodBodyText(f,testMethod));
//            }
//        }
//
//        assertNotEquals(0,additionalMethodsBody.size());
//    }

}
