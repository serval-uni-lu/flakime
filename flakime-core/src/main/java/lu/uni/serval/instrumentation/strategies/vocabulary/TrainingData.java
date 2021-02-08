package lu.uni.serval.instrumentation.strategies.vocabulary;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class TrainingData {
    private final List<Entry> entries;

    public TrainingData(InputStream in) throws IOException {
        entries = load(in);
    }

    private List<Entry> load(InputStream in) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(in, new TypeReference<List<Entry>>(){});
    }

    public List<Entry> getEntries() {
        return entries;
    }

    static class Entry{
        @JsonProperty(value = "Body")
        String body;
        @JsonProperty(value = "ClassName")
        String className;
        @JsonProperty(value = "MethodName")
        String methodName;
        @JsonProperty(value = "ProjectName")
        String projectName;
        @JsonProperty(value = "Label")
        int label;

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
