package graphql;

import graphql.execution.ResultPath;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static graphql.Assert.assertNotNull;

/**
 * Results that come back from @defer fields have an extra path property that tells you where
 * that deferred result came in the original query
 */
@PublicApi
public class DeferredExecutionResultImpl extends ExecutionResultImpl implements DeferredExecutionResult {

    private final List<Object> path;
    private final String label;

    private DeferredExecutionResultImpl(List<Object> path, String label, ExecutionResultImpl executionResult) {
        super(executionResult);
        this.path = assertNotNull(path);
        this.label = label;
    }

    /**
     * @return the execution path of this deferred result in the original query
     */
    public List<Object> getPath() {
        return path;
    }

    /**
     * @return the label of this deferred result
     */
    public String getLabel() {
        return label;
    }

    @Override
    public Map<String, Object> toSpecification() {
        Map<String, Object> map = new LinkedHashMap<>(super.toSpecification());
        if (path != null) {
            map.put("path", path);
        }

        if (label != null) {
            map.put("label", label);
        }

        return map;
    }

    @Override
    public String toString() {
        return "DeferredExecutionResultImpl{" +
                "errors=" + getErrors() +
                ", data=" + getData() +
                ", dataPresent=" + isDataPresent() +
                ", extensions=" + getExtensions() +
                ", hasNext=" + getHasNext() +
                ", label=" + label +
                ", path=" + path +
                '}';
    }

    public static Builder newDeferredExecutionResult() {
        return new Builder();
    }

    public static class Builder {
        private List<Object> path = Collections.emptyList();
        private String label;
        private ExecutionResultImpl.Builder builder = ExecutionResultImpl.newExecutionResult();

        public Builder path(ResultPath path) {
            this.path = assertNotNull(path).toList();
            return this;
        }

        public Builder path(List<Object> path) {
            this.path = path;
            return this;
        }

        public Builder from(ExecutionResult executionResult) {
            builder.from(executionResult);
            return this;
        }

        public Builder label(String label) {
            this.label = label;
            return this;
        }

        public Builder hasNext(Boolean hasNext) {
            builder.hasNext(hasNext);
            return this;
        }

        public DeferredExecutionResult build() {
            ExecutionResultImpl build = builder.build();
            return new DeferredExecutionResultImpl(path, label, build);
        }
    }
}
