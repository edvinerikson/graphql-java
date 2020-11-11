package graphql.execution.defer


import graphql.execution.PatchExecutionResult
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription

import java.util.concurrent.atomic.AtomicBoolean

class CapturingSubscriber implements Subscriber<PatchExecutionResult> {
    Subscription subscription
    AtomicBoolean finished = new AtomicBoolean()
    Throwable throwable
    List<PatchExecutionResult> executionResults = []
    List<Object> executionResultData = []

    AtomicBoolean subscribeTo(Publisher<PatchExecutionResult> publisher) {
        publisher.subscribe(this)
        return finished
    }

    @Override
    void onSubscribe(Subscription s) {
        assert s != null, "subscription must not be null"
        this.subscription = s
        s.request(1)
    }

    @Override
    void onNext(PatchExecutionResult executionResult) {
        executionResults.add(executionResult)
        executionResultData.add(executionResult.getData())
        subscription.request(1)
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
