package graphql.execution;

import graphql.ExecutionResult;
import graphql.GraphQLError;
import graphql.execution.instrumentation.InstrumentationContext;
import graphql.execution.reactive.SingleSubscriberPublisher;
import org.reactivestreams.Publisher;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class Dispatcher {

    private SingleSubscriberPublisher<PatchExecutionResult> publisher = new SingleSubscriberPublisher<>();
    private List<CompletableFuture<PatchExecutionResult>> subsequentPayloads = new LinkedList<>();

    private int expectedPayloads = 0;
    private int happenedPayloads = 0;
    boolean hasSubsequentPayloads() {
        return expectedPayloads > 0;
    }

    boolean hasNext() {
        return expectedPayloads > happenedPayloads;
    }

    void increaseExpectedPayloads(int count) {
        expectedPayloads += count;
    }

    void increaseHappenedPayloads() {
        happenedPayloads += 1;
    }

    void addFields(String label, ResultPath path, CompletableFuture<ExecutionResult> futureExecutionResult, InstrumentationContext<PatchExecutionResult> instrumentationContext) {
        CompletableFuture<PatchExecutionResult> futureResult = new CompletableFuture<>();

        futureResult.whenComplete(instrumentationContext::onCompleted);
        futureExecutionResult.whenComplete((dataResult, throwable) -> {
                    increaseHappenedPayloads();
                    boolean hasNext = hasNext();
                    if (throwable == null) {
                        PatchExecutionResult patch = PatchExecutionResultImpl.newPatchExecutionResult()
                                .hasNext(hasNext)
                                .label(label)
                                .path(path)
                                .data(dataResult.getData())
                                .extensions(dataResult.getExtensions())
                                .errors(dataResult.getErrors())
                                .build();
                        futureResult.complete(patch);
                        publisher.offer(patch);
                    } else {
                        futureResult.completeExceptionally(throwable);
                        publisher.offerError(throwable);
                    }

                    if (!hasNext) {
                        publisher.noMoreData();
                    }
                });
        instrumentationContext.onDispatched(futureResult);
    }

    Publisher<PatchExecutionResult> getPublisher() {
        return publisher;
    }
}
