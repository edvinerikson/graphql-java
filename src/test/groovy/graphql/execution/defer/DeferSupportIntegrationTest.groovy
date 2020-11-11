package graphql.execution.defer


import graphql.Directives
import graphql.ExecutionInput
import graphql.ExecutionResult
import graphql.GraphQL
import graphql.TestUtil
import graphql.execution.PatchExecutionResult
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import graphql.schema.idl.RuntimeWiring
import org.awaitility.Awaitility
import org.reactivestreams.Publisher
import spock.lang.Specification

import java.time.Duration
import java.util.concurrent.CompletableFuture

import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring

class DeferSupportIntegrationTest extends Specification {
    def then = 0

    def sentAt() {
        def seconds = Duration.ofMillis(System.currentTimeMillis() - then).toMillis()
        "T+" + seconds
    }

    def sleepSome(DataFetchingEnvironment env) {
        Integer sleepTime = env.getArgument("sleepTime")
        sleepTime = Optional.ofNullable(sleepTime).orElse(0)
        Thread.sleep(sleepTime)
    }

    def schemaSpec = '''
            type Query {
                post : Post 
                mandatoryReviews : [Review]!
            }
            
            type Mutation {
                mutate(arg : String) : String
            }
            
            type Post {
                postText : String
                sentAt : String
                echo(text : String = "echo") : String
                comments(sleepTime : Int, prefix :String) : [Comment]
                reviews(sleepTime : Int) : [Review]
            }
            
            type Comment {
                commentText : String
                sentAt : String
                comments(sleepTime : Int, prefix :String) : [Comment]
                goes : Bang
            }
            
            type Review {
                reviewText : String
                sentAt : String
                comments(sleepTime : Int, prefix :String) : [Comment]
                goes : Bang
            }       
            
            type Bang {
                bang : String
            }     
        '''

    DataFetcher postFetcher = new DataFetcher() {
        @Override
        Object get(DataFetchingEnvironment environment) {
            return CompletableFuture.supplyAsync({
                [postText: "post_data", sentAt: sentAt()]
            })
        }
    }
    DataFetcher commentsFetcher = new DataFetcher() {
        @Override
        Object get(DataFetchingEnvironment env) {
            return CompletableFuture.supplyAsync({
                sleepSome(env)

                def prefix = env.getArgument("prefix")
                prefix = prefix == null ? "" : prefix

                def result = []
                for (int i = 0; i < 3; i++) {
                    result.add([commentText: prefix + "comment" + i, sentAt: sentAt(), goes: "goes"])
                }
                return result
            })
        }

    }
    DataFetcher reviewsFetcher = new DataFetcher() {
        @Override
        Object get(DataFetchingEnvironment env) {
            return CompletableFuture.supplyAsync({
                sleepSome(env)
                def result = []
                for (int i = 0; i < 3; i++) {
                    result.add([reviewText: "review" + i, sentAt: sentAt(), goes: "goes"])
                }
                return result
            })
        }
    }

    DataFetcher bangDataFetcher = new DataFetcher() {
        @Override
        Object get(DataFetchingEnvironment environment) {
            throw new RuntimeException("Bang!")
        }
    }
    DataFetcher echoDataFetcher = new DataFetcher() {
        @Override
        Object get(DataFetchingEnvironment environment) {
            return environment.getArgument("text")
        }
    }

    GraphQL graphQL = null

    void setup() {
        then = System.currentTimeMillis()

        def runtimeWiring = RuntimeWiring.newRuntimeWiring()
                .type(newTypeWiring("Query").dataFetcher("post", postFetcher))
                .type(newTypeWiring("Post").dataFetcher("comments", commentsFetcher))
                .type(newTypeWiring("Post").dataFetcher("echo", echoDataFetcher))
                .type(newTypeWiring("Post").dataFetcher("reviews", reviewsFetcher))
                .type(newTypeWiring("Bang").dataFetcher("bang", bangDataFetcher))

                .type(newTypeWiring("Comment").dataFetcher("comments", commentsFetcher))
                .type(newTypeWiring("Review").dataFetcher("comments", commentsFetcher))
                .build()

        def schema = TestUtil.schema(schemaSpec, runtimeWiring)
                .transform({ builder -> builder.additionalDirective(Directives.DeferDirective) })
        this.graphQL = GraphQL.newGraphQL(schema).build()
    }

    def "test defer support end to end"() {

        def query = '''
            query {
                post {
                    postText
                    ... on Post @defer(label: "a") {
                       a :comments(sleepTime:200) {
                          commentText
                       }  
                    }
                    
                    ... on Post @defer(label: "b") {
                        b : reviews(sleepTime:100) {
                            reviewText
                            ... on Review @defer(label: "b_comments") {
                                comments(prefix : "b_") {
                                    commentText
                                }
                            }
                        }
                    }
                    
                    ... on Post @defer(label: "c") {
                        c: reviews {
                            goes {
                                bang
                            }
                        }
                    }
                }
            }
        '''

        when:
        def initialResult = graphQL.execute(ExecutionInput.newExecutionInput().query(query).build())

        then:
        initialResult.errors.isEmpty()
        initialResult.data == ["post": ["postText": "post_data"]]

        when:

        Publisher<PatchExecutionResult> deferredResultStream = initialResult.patchPublisher;

        def subscriber = new CapturingSubscriber()
        subscriber.subscribeTo(deferredResultStream)
        Awaitility.await().untilTrue(subscriber.finished)

        List<ExecutionResult> resultList = subscriber.executionResults

        then:

        assertDeferredData(resultList)
    }

    def "test defer support keeps the fields named correctly when interspersed in the query"() {

        def query = '''
            query {
                post {
                    interspersedA: echo(text:"before a:")
                    ... on Post @defer {
                        a: comments(sleepTime:200) {
                            commentText
                        }
                    }
                      
                    interspersedB: echo(text:"before b:")
                    
                    ... on Post @defer {
                        b : reviews(sleepTime:100) {
                            reviewText
                            ... on Review @defer {
                                comments(prefix : "b_") {
                                    commentText
                                }
                            }
                        }
                    }
                    
                    interspersedC: echo(text:"before c:")

                    ... on Post @defer {
                        c: reviews {
                            goes {
                                bang
                            }
                        }
                    }
                    
                    
                    interspersedD: echo(text:"after c:")
                }
            }
        '''

        when:
        def initialResult = graphQL.execute(ExecutionInput.newExecutionInput().query(query).build())

        then:
        initialResult.errors.isEmpty()
        initialResult.data == ["post": [
                "interspersedA": "before a:",

                "interspersedB": "before b:",

                "interspersedC": "before c:",

                "interspersedD": "after c:",
        ]]

        when:

        Publisher<PatchExecutionResult> deferredResultStream = initialResult.patchPublisher;

        def subscriber = new CapturingSubscriber()
        subscriber.subscribeTo(deferredResultStream);
        Awaitility.await().untilTrue(subscriber.finished)

        List<PatchExecutionResult> resultList = subscriber.executionResults

        then:

        assertDeferredData(resultList)
    }

    def assertDeferredData(List<PatchExecutionResult> resultList) {
        resultList.size() == 6
        assert resultList[0].hasNext == true
        assert resultList[1].hasNext == true
        assert resultList[2].hasNext == true
        assert resultList[3].hasNext == true
        assert resultList[4].hasNext == true
        assert resultList[5].hasNext == false


        /*
        assert resultList[0].data == [a:[[commentText: "comment0"], [commentText: "comment1"], [commentText: "comment2"]]]
        assert resultList[0].errors == []
        assert resultList[0].path.toList() == ["post"]

        assert resultList[1].data == [comments:[[commentText: "b_comment0"], [commentText: "b_comment1"], [commentText: "b_comment2"]]]
        assert resultList[1].errors == []
        assert resultList[1].path.toList() == ["post", "b", 0]

        // exceptions in here
        assert resultList[2].errors.size() == 3
        assert resultList[2].errors[0].getMessage() == "Exception while fetching data (/post/c[0]/goes/bang) : Bang!"
        assert resultList[2].errors[1].getMessage() == "Exception while fetching data (/post/c[1]/goes/bang) : Bang!"
        assert resultList[2].errors[2].getMessage() == "Exception while fetching data (/post/c[2]/goes/bang) : Bang!"
        assert resultList[2].path.toList() == ["post"]

        assert resultList[3].data == [b:[[reviewText: "review0"], [reviewText: "review1"], [reviewText: "review2"]]]
        assert resultList[3].errors == []
        assert resultList[3].path.toList() == ["post"]

        assert resultList[4].data == [comments: [[commentText: "b_comment0"], [commentText: "b_comment1"], [commentText: "b_comment2"]]]
        assert resultList[4].errors == []
        assert resultList[4].path.toList() == ["post", "b", 1]

        assert resultList[5].data == [comments: [[commentText: "b_comment0"], [commentText: "b_comment1"], [commentText: "b_comment2"]]]
        assert resultList[5].errors == []
        assert resultList[5].path.toList() == ["post", "b", 2]
       */
        true
    }
}
