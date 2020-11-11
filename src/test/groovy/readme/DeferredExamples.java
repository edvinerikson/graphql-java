package readme;

import graphql.Directives;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.execution.PatchExecutionResult;
import graphql.schema.GraphQLSchema;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import javax.servlet.http.HttpServletResponse;
import java.util.Map;

@SuppressWarnings({"unused", "ConstantConditions", "UnusedAssignment", "unchecked"})
public class DeferredExamples {

    GraphQLSchema buildSchemaWithDirective() {

        GraphQLSchema schema = buildSchema();
        schema = schema.transform(builder ->
                builder.additionalDirective(Directives.DeferDirective)
        );
        return schema;
    }

    void basicExample(HttpServletResponse httpServletResponse, String deferredQuery) {
        GraphQLSchema schema = buildSchemaWithDirective();
        GraphQL graphQL = GraphQL.newGraphQL(schema).build();

        //
        // deferredQuery contains the query with @defer directives in it
        //
        ExecutionResult initialResult = graphQL.execute(ExecutionInput.newExecutionInput().query(deferredQuery).build());

        //
        // then initial results happen first, the deferred ones will begin AFTER these initial
        // results have completed
        //
        sendMultipartHttpResult(httpServletResponse, initialResult);

        Map<Object, Object> extensions = initialResult.getExtensions();
        Publisher<PatchExecutionResult> deferredResults = initialResult.getPatchPublisher();

        //
        // you subscribe to the deferred results like any other reactive stream
        //
        deferredResults.subscribe(new Subscriber<PatchExecutionResult>() {

            Subscription subscription;

            @Override
            public void onSubscribe(Subscription s) {
                subscription = s;
                //
                // how many you request is up to you
                subscription.request(10);
            }

            @Override
            public void onNext(PatchExecutionResult executionResult) {
                //
                // as each deferred result arrives, send it to where it needs to go
                //
                sendMultipartHttpResult(httpServletResponse, executionResult);
                subscription.request(10);
            }

            @Override
            public void onError(Throwable t) {
                handleError(httpServletResponse, t);
            }

            @Override
            public void onComplete() {
                completeResponse(httpServletResponse);
            }
        });
    }

    private void completeResponse(HttpServletResponse httpServletResponse) {
    }

    private void handleError(HttpServletResponse httpServletResponse, Throwable t) {
    }

    private void sendMultipartHttpResult(HttpServletResponse httpServletResponse, PatchExecutionResult patch) {
    }

    private void sendMultipartHttpResult(HttpServletResponse httpServletResponse, ExecutionResult initialResult) {
    }


    private GraphQLSchema buildSchema() {
        return null;
    }

}
