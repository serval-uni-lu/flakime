package lu.uni.serval.flakime.core.instrumentation.models.vocabulary;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Data implements Iterable<Data.Entry> {
    private final List<Entry> entries;

    public Data(InputStream in) throws IOException {
        entries = load(in);
    }

    public Data(List<Entry> entries){
        this.entries = entries;
    }

    public Data() {
        this.entries = new ArrayList<>();
    }

    public void addAll(Data data){
        this.entries.addAll(data.entries);
    }

    private List<Entry> load(InputStream in) throws IOException {
        return new ObjectMapper().readValue(in, new TypeReference<List<Entry>>(){});
    }

    public Pair<Data, Data> split(float trainRatio){
        final List<Entry> copy = new ArrayList<>(entries);
        Collections.shuffle(copy);

        int trainSize = Math.round(copy.size() * trainRatio);

        final Data train = new Data(copy.subList(0, trainSize));
        final Data test = new Data(copy.subList(trainSize, copy.size()));

        return Pair.of(train, test);
    }

    public int size() {
        return entries.size();
    }

    public Set<String> getProjects() {
        return entries.stream()
                .map(Entry::getProjectName)
                .collect(Collectors.toSet());
    }

    @Override
    public Iterator<Entry> iterator() {
        return entries.iterator();
    }

    public Stream<Entry> stream() {
        return entries.stream();
    }

    static class Entry{
        @JsonProperty(value = "Body")
        private String body;
        @JsonProperty(value = "ClassName")
        private String className;
        @JsonProperty(value = "MethodName")
        private String methodName;
        @JsonProperty(value = "ProjectName")
        private String projectName;
        @JsonProperty(value = "Label")
        private int label;

        public String getBody() {
            return body;
        }

        public void setBody(String body) {
            this.body = body;
        }

        public String getClassName() {
            return className;
        }

        public void setClassName(String className) {
            this.className = className;
        }

        public String getMethodName() {
            return methodName;
        }

        public void setMethodName(String methodName) {
            this.methodName = methodName;
        }

        public String getProjectName() {
            return projectName;
        }

        public void setProjectName(String projectName) {
            this.projectName = projectName;
        }

        public int getLabel() {
            return label;
        }

        public void setLabel(int label) {
            this.label = label;
        }
    }
}
