package graphql;


import graphql.language.Description;
import graphql.language.DirectiveDefinition;
import graphql.language.StringValue;
import graphql.schema.GraphQLDirective;

import static graphql.Scalars.*;
import static graphql.introspection.Introspection.DirectiveLocation.ENUM_VALUE;
import static graphql.introspection.Introspection.DirectiveLocation.FIELD;
import static graphql.introspection.Introspection.DirectiveLocation.FIELD_DEFINITION;
import static graphql.introspection.Introspection.DirectiveLocation.FRAGMENT_SPREAD;
import static graphql.introspection.Introspection.DirectiveLocation.INLINE_FRAGMENT;
import static graphql.introspection.Introspection.DirectiveLocation.SCALAR;
import static graphql.language.DirectiveLocation.newDirectiveLocation;
import static graphql.language.InputValueDefinition.newInputValueDefinition;
import static graphql.language.NonNullType.newNonNullType;
import static graphql.language.TypeName.newTypeName;
import static graphql.schema.GraphQLArgument.newArgument;
import static graphql.schema.GraphQLNonNull.nonNull;

/**
 * The directives that are understood by graphql-java
 */
@PublicApi
public class Directives {
    private static final String SPECIFIED_BY = "specifiedBy";
    private static final String DEPRECATED = "deprecated";

    public static final String NO_LONGER_SUPPORTED = "No longer supported";
    public static final DirectiveDefinition DEPRECATED_DIRECTIVE_DEFINITION;
    public static final DirectiveDefinition SPECIFIED_BY_DIRECTIVE_DEFINITION;


    static {
        DEPRECATED_DIRECTIVE_DEFINITION = DirectiveDefinition.newDirectiveDefinition()
                .name(DEPRECATED)
                .directiveLocation(newDirectiveLocation().name(FIELD_DEFINITION.name()).build())
                .directiveLocation(newDirectiveLocation().name(ENUM_VALUE.name()).build())
                .description(createDescription("Marks the field or enum value as deprecated"))
                .inputValueDefinition(
                        newInputValueDefinition()
                                .name("reason")
                                .description(createDescription("The reason for the deprecation"))
                                .type(newTypeName().name("String").build())
                                .defaultValue(StringValue.newStringValue().value(NO_LONGER_SUPPORTED).build())
                                .build())
                .build();

        SPECIFIED_BY_DIRECTIVE_DEFINITION = DirectiveDefinition.newDirectiveDefinition()
                .name(SPECIFIED_BY)
                .directiveLocation(newDirectiveLocation().name(SCALAR.name()).build())
                .description(createDescription("Exposes a URL that specifies the behaviour of this scalar."))
                .inputValueDefinition(
                        newInputValueDefinition()
                                .name("url")
                                .description(createDescription("The URL that specifies the behaviour of this scalar."))
                                .type(newNonNullType(newTypeName().name("String").build()).build())
                                .build())
                .build();
    }

    public static final GraphQLDirective DeferDirective = GraphQLDirective.newDirective()
            .name("defer")
            .description("Directs the executor to defer this fragment when the `if` argument is true or undefined.")
            .argument(newArgument()
                    .name("if")
                    .type(GraphQLBoolean)
                    .description("Deferred when true or undefined."))
            .argument(newArgument()
                    .name("label")
                    .type(GraphQLString)
                    .description("Unique name"))
            .validLocations(FRAGMENT_SPREAD, INLINE_FRAGMENT)
            .build();

    public static final GraphQLDirective StreamDirective = GraphQLDirective.newDirective()
            .name("stream")
            .description("Directs the executor to stream plural fields when the `if` argument is true or undefined.")
            .argument(newArgument()
                    .name("if")
                    .type(GraphQLBoolean)
                    .description("Stream when true or undefined."))
            .argument(newArgument()
                    .name("label")
                    .type(GraphQLString)
                    .description("Unique name"))
            .argument(newArgument()
                    .name("initialCount")
                    .type(nonNull(GraphQLInt))
                    .description("Number of items to return immediately"))
            .validLocations(FIELD)
            .build();

    public static final GraphQLDirective IncludeDirective = GraphQLDirective.newDirective()
            .name("include")
            .description("Directs the executor to include this field or fragment only when the `if` argument is true")
            .argument(newArgument()
                    .name("if")
                    .type(nonNull(GraphQLBoolean))
                    .description("Included when true."))
            .validLocations(FRAGMENT_SPREAD, INLINE_FRAGMENT, FIELD)
            .build();

    public static final GraphQLDirective SkipDirective = GraphQLDirective.newDirective()
            .name("skip")
            .description("Directs the executor to skip this field or fragment when the `if`'argument is true.")
            .argument(newArgument()
                    .name("if")
                    .type(nonNull(GraphQLBoolean))
                    .description("Skipped when true."))
            .validLocations(FRAGMENT_SPREAD, INLINE_FRAGMENT, FIELD)
            .build();

    /**
     * The "deprecated" directive is special and is always available in a graphql schema
     * <p>
     * See https://graphql.github.io/graphql-spec/June2018/#sec--deprecated
     */
    public static final GraphQLDirective DeprecatedDirective = GraphQLDirective.newDirective()
            .name(DEPRECATED)
            .description("Marks the field or enum value as deprecated")
            .argument(newArgument()
                    .name("reason")
                    .type(GraphQLString)
                    .defaultValue(NO_LONGER_SUPPORTED)
                    .description("The reason for the deprecation"))
            .validLocations(FIELD_DEFINITION, ENUM_VALUE)
            .definition(DEPRECATED_DIRECTIVE_DEFINITION)
            .build();

    /**
     * The "specifiedBy" directive allows to provide a specification URL for a Scalar
     */
    public static final GraphQLDirective SpecifiedByDirective = GraphQLDirective.newDirective()
            .name(SPECIFIED_BY)
            .description("Exposes a URL that specifies the behaviour of this scalar.")
            .argument(newArgument()
                    .name("url")
                    .type(nonNull(GraphQLString))
                    .description("The URL that specifies the behaviour of this scalar."))
            .validLocations(SCALAR)
            .definition(SPECIFIED_BY_DIRECTIVE_DEFINITION)
            .build();

    private static Description createDescription(String s) {
        return new Description(s, null, false);
    }

}
