package graphql.execution.defer;

import graphql.*;
import graphql.execution.MergedField;
import graphql.execution.ValuesResolver;
import graphql.execution.reactive.SingleSubscriberPublisher;
import graphql.language.Directive;
import graphql.language.Field;
import graphql.language.FragmentSpread;
import graphql.language.InlineFragment;
import org.reactivestreams.Publisher;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;

import static graphql.Directives.*;

/**
 * This provides support for @defer directives on fields that mean that results will be sent AFTER
 * the main result is sent via a Publisher stream.
 */
@Internal
public class DeferSupport {

    private final AtomicBoolean deferDetected = new AtomicBoolean(false);
    private final Deque<CompletableFuture<DeferredExecutionResult>> deferredCalls = new ConcurrentLinkedDeque<>();
    private final SingleSubscriberPublisher<DeferredExecutionResult> publisher = new SingleSubscriberPublisher<>();
    private final ValuesResolver valuesResolver = new ValuesResolver();

    public boolean checkForDeferDirective(FragmentSpread fragmentSpread, Map<String,Object> variables) {
        return shouldDefer(fragmentSpread.getDirective(DeferDirective.getName()), variables);
    }

    public boolean checkForDeferDirective(InlineFragment inlineFragment, Map<String, Object> variables) {
        return shouldDefer(inlineFragment.getDirective(DeferDirective.getName()), variables);
    }

    private boolean shouldDefer(Directive directive, Map<String, Object> variables) {
        if (directive != null) {
            Map<String, Object> argumentValues = valuesResolver.getArgumentValues(DeferDirective.getArguments(), directive.getArguments(), variables);
            return (Boolean) argumentValues.getOrDefault("if", true);
        }
        return false;
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    private void drainDeferredCalls() {


    }


    public void enqueue(DeferredCall deferredCall) {
        deferDetected.set(true);
        CompletableFuture<DeferredExecutionResult> future = deferredCall.invoke();
        deferredCalls.offer(future);
    }

    public boolean isDeferDetected() {
        return deferDetected.get();
    }

    /**
     * When this is called the deferred execution will begin
     *
     * @return the publisher of deferred results
     */
    public Publisher<DeferredExecutionResult> startDeferredCalls() {
        drainDeferredCalls();
        return publisher;
    }
}
