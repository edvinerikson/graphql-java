package graphql.execution;

import graphql.ExecutionResult;
import graphql.ExecutionResultImpl;
import graphql.GraphQLError;
import org.reactivestreams.Publisher;

import java.util.*;

import static java.util.stream.Collectors.toList;

public class PatchExecutionResultImpl implements PatchExecutionResult {

    private String label;
    private ResultPath path;
    private Boolean hasNext;
    private Object data;
    private List<GraphQLError> errors;
    private Map<Object, Object> extensions;
    private boolean dataPresent;

    PatchExecutionResultImpl(boolean dataPresent, String label, ResultPath path, Boolean hasNext, Object data, List<GraphQLError> errors, Map<Object, Object> extensions) {
        this.label = label;
        this.path = path;
        this.dataPresent = dataPresent;
        this.data = data;

        if (errors != null && !errors.isEmpty()) {
            this.errors = Collections.unmodifiableList(new ArrayList<>(errors));
        } else {
            this.errors = Collections.emptyList();
        }

        this.extensions = extensions;
        this.hasNext = hasNext;

    }

    @Override
    public List<GraphQLError> getErrors() {
        return errors;
    }

    @Override
    public <T> T getData() {
        return (T) data;
    }

    @Override
    public boolean isDataPresent() {
        return data != null;
    }

    @Override
    public Map<Object, Object> getExtensions() {
        return extensions;
    }

    @Override
    public Map<String, Object> toSpecification() {
        Map<String, Object> result = new LinkedHashMap<>();
        if (errors != null && !errors.isEmpty()) {
            result.put("errors", errorsToSpec(errors));
        }
        if (dataPresent) {
            result.put("data", data);
        }
        if (extensions != null) {
            result.put("extensions", extensions);
        }

        if (hasNext != null) {
            result.put("hasNext", hasNext);
        }

        if (label != null) {
            result.put("label", label);
        }

        if (path != null) {
            result.put("path", path.toList());
        }

        return result;
    }

    @Override
    public Boolean getHasNext() {
        return hasNext;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public ResultPath getPath() {
        return path;
    }

    private Object errorsToSpec(List<GraphQLError> errors) {
        return errors.stream().map(GraphQLError::toSpecification).collect(toList());
    }


    public static Builder newPatchExecutionResult() {
        return new Builder();
    }

    public static class Builder {
        private boolean dataPresent;
        private String label;
        private ResultPath path;
        private Object data;
        private List<GraphQLError> errors = new ArrayList<>();
        private Map<Object, Object> extensions;
        private Boolean hasNext;

        public Builder from(ExecutionResult executionResult) {
            dataPresent = executionResult.isDataPresent();
            data = executionResult.getData();
            errors = new ArrayList<>(executionResult.getErrors());
            extensions = executionResult.getExtensions();
            hasNext = executionResult.getHasNext();
            return this;
        }

        public Builder data(Object data) {
            dataPresent = true;
            this.data = data;
            return this;
        }

        public Builder errors(List<GraphQLError> errors) {
            this.errors = errors;
            return this;
        }

        public Builder extensions(Map<Object, Object> extensions) {
            this.extensions = extensions;
            return this;
        }

        public Builder addExtension(String key, Object value) {
            this.extensions = (this.extensions == null ? new LinkedHashMap<>() : this.extensions);
            this.extensions.put(key, value);
            return this;
        }

        public Builder hasNext(Boolean hasNext) {
            this.hasNext = hasNext;
            return this;
        }

        public Builder path(ResultPath path) {
            this.path = path;
            return this;
        }

        public Builder label(String label) {
            this.label = label;
            return this;
        }

        public PatchExecutionResultImpl build() {
            return new PatchExecutionResultImpl(dataPresent, label, path, hasNext, data, errors, extensions);
        }
    }
}
