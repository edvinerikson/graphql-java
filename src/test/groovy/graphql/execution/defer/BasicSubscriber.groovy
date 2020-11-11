package graphql.execution.defer


import graphql.execution.PatchExecutionResult
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription

import java.util.concurrent.atomic.AtomicBoolean

class BasicSubscriber implements Subscriber<PatchExecutionResult> {
    Subscription subscription
    AtomicBoolean finished = new AtomicBoolean()
    Throwable throwable

    @Override
    void onSubscribe(Subscription s) {
        assert s != null, "subscription must not be null"
        this.subscription = s
        s.request(1)
    }

    @Override
    void onNext(PatchExecutionResult executionResult) {
    }

    @Override
    void onError(Throwable t) {
        finished.set(true)
    }

    @Override
    void onComplete() {
        finished.set(true)
    }
}
