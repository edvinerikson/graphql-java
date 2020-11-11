package graphql;


import graphql.execution.PatchExecutionResult;
import org.reactivestreams.Publisher;
import com.google.common.collect.ImmutableList;
import graphql.collect.ImmutableKit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static graphql.collect.ImmutableKit.map;

@Internal
public class ExecutionResultImpl implements ExecutionResult {

    private final List<GraphQLError> errors;
    private final Object data;
    private final transient Map<Object, Object> extensions;
    private final transient boolean dataPresent;
    private final Boolean hasNext;
    private final Publisher<PatchExecutionResult> patchPublisher;

    public ExecutionResultImpl(GraphQLError error) {
        this(false, null, Collections.singletonList(error), null, null, null);
    }

    public ExecutionResultImpl(List<? extends GraphQLError> errors) {
        this(false, null, errors, null, null, null);
    }

    public ExecutionResultImpl(Object data, List<? extends GraphQLError> errors) {
        this(true, data, errors, null, null, null);
    }
    public ExecutionResultImpl(Object data, List<? extends GraphQLError> errors, Boolean hasNext) {
        this(true, data, errors, null, hasNext, null);
    }

    public ExecutionResultImpl(Object data, List<? extends GraphQLError> errors, Map<Object, Object> extensions) {
        this(true, data, errors, extensions, null, null);
    }

    public ExecutionResultImpl(ExecutionResultImpl other) {
        this(other.dataPresent, other.data, other.errors, other.extensions, other.hasNext, other.patchPublisher);
    }

    private ExecutionResultImpl(boolean dataPresent, Object data, List<? extends GraphQLError> errors, Map<Object, Object> extensions, Boolean hasNext, Publisher<PatchExecutionResult> patchPublisher) {
        this.dataPresent = dataPresent;
        this.data = data;

        if (errors != null && !errors.isEmpty()) {
            this.errors = ImmutableList.copyOf(errors);
        } else {
            this.errors = ImmutableKit.emptyList();
        }

        this.extensions = extensions;
        this.hasNext = hasNext;
        this.patchPublisher = patchPublisher;
    }

    public boolean isDataPresent() {
        return dataPresent;
    }

    @Override
    public List<GraphQLError> getErrors() {
        return errors;
    }

    @Override
    @SuppressWarnings("TypeParameterUnusedInFormals")
    public <T> T getData() {
        //noinspection unchecked
        return (T) data;
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

        return result;
    }

    @Override
    public Boolean getHasNext() {
        return hasNext;
    }

    @Override
    public Publisher<PatchExecutionResult> getPatchPublisher() {
        return patchPublisher;
    }

    private Object errorsToSpec(List<GraphQLError> errors) {
        return map(errors, GraphQLError::toSpecification);
    }

    @Override
    public String toString() {
        return "ExecutionResultImpl{" +
                "errors=" + errors +
                ", data=" + data +
                ", dataPresent=" + dataPresent +
                ", extensions=" + extensions +
                ", hasNext=" + hasNext +
                '}';
    }

    public ExecutionResultImpl transform(Consumer<Builder> builderConsumer) {
        Builder builder = newExecutionResult().from(this);
        builderConsumer.accept(builder);
        return builder.build();
    }

    public static Builder newExecutionResult() {
        return new Builder();
    }

    public static class Builder {
        private boolean dataPresent;
        private Object data;
        private List<GraphQLError> errors = new ArrayList<>();
        private Map<Object, Object> extensions;
        private Boolean hasNext;
        private Publisher<PatchExecutionResult> patchPublisher;

        public Builder from(ExecutionResult executionResult) {
            dataPresent = executionResult.isDataPresent();
            data = executionResult.getData();
            errors = new ArrayList<>(executionResult.getErrors());
            extensions = executionResult.getExtensions();
            hasNext = executionResult.getHasNext();
            patchPublisher = executionResult.getPatchPublisher();
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

        public Builder addErrors(List<GraphQLError> errors) {
            this.errors.addAll(errors);
            return this;
        }

        public Builder addError(GraphQLError error) {
            this.errors.add(error);
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

        public Builder patchPublisher(Publisher<PatchExecutionResult> patchPublisher) {
            this.patchPublisher = patchPublisher;
            return this;
        }

        public ExecutionResultImpl build() {
            return new ExecutionResultImpl(dataPresent, data, errors, extensions, hasNext, patchPublisher);
        }
    }
}
