package graphql.execution.defer;

import graphql.DeferredExecutionResult;
import graphql.DeferredExecutionResultImpl;
import graphql.ExecutionResult;
import graphql.GraphQLError;
import graphql.Internal;
import graphql.execution.ResultPath;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * This represents a deferred call (aka @defer) to get an execution result sometime after
 * the initial query has returned
 */
@Internal
public class DeferredCall {
    private final ResultPath path;
    private final Supplier<CompletableFuture<ExecutionResult>> call;
    private final String label;

    public DeferredCall(ResultPath path, Supplier<CompletableFuture<ExecutionResult>> call, String label) {
        this.path = path;
        this.call = call;
        this.label = label;
    }

    CompletableFuture<DeferredExecutionResult> invoke() {
        CompletableFuture<ExecutionResult> future = call.get();
        return future.thenApply(this::transformToDeferredResult);
    }

    private DeferredExecutionResult transformToDeferredResult(ExecutionResult executionResult) {
        DeferredExecutionResultImpl.Builder builder = DeferredExecutionResultImpl.newDeferredExecutionResult().from(executionResult);
        return builder
                .path(path)
                .label(label)
                .build();
    }
}
