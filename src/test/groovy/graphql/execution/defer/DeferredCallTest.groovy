package graphql.execution.defer


import graphql.ExecutionResultImpl
import graphql.validation.ValidationError
import graphql.validation.ValidationErrorType
import spock.lang.Specification

import static graphql.execution.ResultPath.parse
import static java.util.concurrent.CompletableFuture.completedFuture

class DeferredCallTest extends Specification {

    def "test call capture gives a CF"() {
        given:
        DeferredCall call = new DeferredCall(parse("/path"), {
            completedFuture(new ExecutionResultImpl("some data", Collections.emptyList()))
        }, null)

        when:
        def future = call.invoke()
        then:
        future.join().data == "some data"
        future.join().path == ["path"]
    }

    def "test error capture happens via CF"() {
        given:
        DeferredCall call = new DeferredCall(parse("/path"), {
            completedFuture(new ExecutionResultImpl("some data", [new ValidationError(ValidationErrorType.FieldUndefined)]))
        }, null)

        when:
        def future = call.invoke()
        def er = future.join()

        then:
        er.errors.size() == 1
        er.errors[0].message.contains("Validation error of type FieldUndefined")
        er.path == ["path"]
    }
}
