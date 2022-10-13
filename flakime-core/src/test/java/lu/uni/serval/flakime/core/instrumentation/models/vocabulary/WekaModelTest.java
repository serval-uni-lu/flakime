package lu.uni.serval.flakime.core.instrumentation.models.vocabulary;

import lu.uni.serval.flakime.core.helpers.TestLogger;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.stream.Collectors;

class WekaModelTest {
    private static final TestLogger logger = new TestLogger();

    @Test
    void extractOverall() throws Exception {
        final String csvOutputFolder = System.getProperty("csvOutputFolder");

        if(csvOutputFolder == null || csvOutputFolder.isEmpty() || !new File(csvOutputFolder).exists()){
            return;
        }

        final String output = new File(new File(csvOutputFolder), "all.csv").getAbsolutePath();
        final InputStream dataSource = WekaModel.class.getClassLoader().getResourceAsStream("data/vocabulary.json");
        final Data data = new Data(dataSource);
        final WekaModel model = new WekaModel(logger, 100, Runtime.getRuntime().availableProcessors());

        final Data training = new Data();
        final Data test = new Data();
        for (String project : data.getProjects()) {
            final Data projectData = new Data(
                    data.stream()
                            .filter(e -> e.getProjectName().equals(project))
                            .collect(Collectors.toList())
            );

            final Pair<Data, Data> split = projectData.split(0.8f);

            training.addAll(split.getLeft());
            test.addAll(split.getRight());
        }

        model.setData(training, Collections.emptySet());
        model.train();

        try (
                BufferedWriter writer = Files.newBufferedWriter(Paths.get(output));
                CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader("project", "class", "method", "y_true", "probability", "y_pred"))
        ) {

            for (Data.Entry entry : test) {
                model.computeProbability(entry.getBody());

                csvPrinter.printRecord(
                        entry.getProjectName(),
                        entry.getClassName(),
                        entry.getMethodName(),
                        entry.getLabel(),
                        model.computeProbability(entry.getBody()),
                        model.computeClass(entry.getBody())
                );

            }
        }
    }

    @Test
    void extractInterProject() throws Exception {
        final String csvOutputFolder = System.getProperty("csvOutputFolder");

        if(csvOutputFolder == null || csvOutputFolder.isEmpty() || !new File(csvOutputFolder).exists()){
            return;
        }

        final InputStream dataSource = WekaModel.class.getClassLoader().getResourceAsStream("data/vocabulary.json");
        final Data data = new Data(dataSource);
        final WekaModel model = new WekaModel(logger, 100, Runtime.getRuntime().availableProcessors());

        for(String project: data.getProjects()){
            final String output = new File(new File(csvOutputFolder), "inter-" + project + ".csv").getAbsolutePath();

            final Data training = new Data(
                    data.stream()
                    .filter(e -> !e.getProjectName().equals(project))
                    .collect(Collectors.toList())
            );

            final Data testing = new Data(
                    data.stream()
                            .filter(e -> e.getProjectName().equals(project))
                            .collect(Collectors.toList())
            );

            model.setData(training, Collections.emptySet());
            model.train();

            try (
                    BufferedWriter writer = Files.newBufferedWriter(Paths.get(output));
                    CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader("project", "class", "method", "y_true", "probability", "y_pred"))
            ) {

                for (Data.Entry entry : testing) {
                    model.computeProbability(entry.getBody());

                    csvPrinter.printRecord(
                            entry.getProjectName(),
                            entry.getClassName(),
                            entry.getMethodName(),
                            entry.getLabel(),
                            model.computeProbability(entry.getBody()),
                            model.computeClass(entry.getBody())
                    );

                }
            }
        }
    }

    @Test
    void extractIntraProject() throws Exception {
        final String csvOutputFolder = System.getProperty("csvOutputFolder");

        if (csvOutputFolder == null || csvOutputFolder.isEmpty() || !new File(csvOutputFolder).exists()) {
            return;
        }

        final InputStream dataSource = WekaModel.class.getClassLoader().getResourceAsStream("data/vocabulary.json");
        final Data data = new Data(dataSource);
        final WekaModel model = new WekaModel(logger, 100, Runtime.getRuntime().availableProcessors());

        for (String project : data.getProjects()) {
            final String output = new File(new File(csvOutputFolder), "intra-" + project + ".csv").getAbsolutePath();

            final Data projectData = new Data(
                    data.stream()
                            .filter(e -> e.getProjectName().equals(project))
                            .collect(Collectors.toList())
            );

            final Pair<Data, Data> split = projectData.split(0.8f);

            model.setData(split.getLeft(), Collections.emptySet());
            model.train();

            try (
                    BufferedWriter writer = Files.newBufferedWriter(Paths.get(output));
                    CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader("project", "class", "method", "y_true", "probability", "y_pred"))
            ) {

                for (Data.Entry entry : split.getRight()) {
                    model.computeProbability(entry.getBody());

                    csvPrinter.printRecord(
                            entry.getProjectName(),
                            entry.getClassName(),
                            entry.getMethodName(),
                            entry.getLabel(),
                            model.computeProbability(entry.getBody()),
                            model.computeClass(entry.getBody())
                    );

                }
            }
        }
    }
}