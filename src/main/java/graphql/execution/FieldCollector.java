package graphql.execution;


import graphql.Directives;
import graphql.Internal;
import graphql.execution.defer.DeferSupport;
import graphql.language.*;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLUnionType;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static graphql.execution.MergedSelectionSet.newMergedSelectionSet;
import static graphql.execution.TypeFromAST.getTypeFromAST;

/**
 * A field collector can iterate over field selection sets and build out the sub fields that have been selected,
 * expanding named and inline fragments as it goes.s
 */
@Internal
public class FieldCollector {

    private final ConditionalNodes conditionalNodes = new ConditionalNodes();
    private final ValuesResolver valuesResolver = new ValuesResolver();
    private final DeferSupport deferSupport = new DeferSupport();

    public MergedSelectionSet collectFields(FieldCollectorParameters parameters, MergedField mergedField) {
        Map<String, MergedField> subFields = new LinkedHashMap<>();
        Set<String> visitedFragments = new LinkedHashSet<>();
        Set<DeferFragment> deferredFragments = new LinkedHashSet<>();
        for (Field field : mergedField.getFields()) {
            if (field.getSelectionSet() == null) {
                continue;
            }
            this.collectFields(parameters, field.getSelectionSet(), visitedFragments, subFields, deferredFragments);
        }
        return newMergedSelectionSet().subFields(subFields).deferredFragments(deferredFragments).build();
    }

    /**
     * Given a selection set this will collect the sub-field selections and return it as a map
     *
     * @param parameters   the parameters to this method
     * @param selectionSet the selection set to collect on
     *
     * @return a map of the sub field selections
     */
    public MergedSelectionSet collectFields(FieldCollectorParameters parameters, SelectionSet selectionSet) {
        Map<String, MergedField> subFields = new LinkedHashMap<>();
        Set<String> visitedFragments = new LinkedHashSet<>();
        Set<DeferFragment> deferredFragments = new LinkedHashSet<>();
        this.collectFields(parameters, selectionSet, visitedFragments, subFields, deferredFragments);
        return newMergedSelectionSet().subFields(subFields).deferredFragments(deferredFragments).build();
    }


    private void collectFields(FieldCollectorParameters parameters, SelectionSet selectionSet, Set<String> visitedFragments, Map<String, MergedField> fields, Set<DeferFragment> deferredFragments) {

        for (Selection selection : selectionSet.getSelections()) {
            if (selection instanceof Field) {
                collectField(parameters, fields, (Field) selection,  deferredFragments);
            } else if (selection instanceof InlineFragment) {
                collectInlineFragment(parameters, visitedFragments, fields, (InlineFragment) selection, deferredFragments);
            } else if (selection instanceof FragmentSpread) {
                collectFragmentSpread(parameters, visitedFragments, fields, (FragmentSpread) selection, deferredFragments);
            }
        }
    }

    private void collectFragmentSpread(FieldCollectorParameters parameters, Set<String> visitedFragments, Map<String, MergedField> fields, FragmentSpread fragmentSpread, Set<DeferFragment> deferredFragments) {
        if (visitedFragments.contains(fragmentSpread.getName())) {
            return;
        }
        if (!conditionalNodes.shouldInclude(parameters.getVariables(), fragmentSpread.getDirectives())) {
            return;
        }

        FragmentDefinition fragmentDefinition = parameters.getFragmentsByName().get(fragmentSpread.getName());

        if (!conditionalNodes.shouldInclude(parameters.getVariables(), fragmentDefinition.getDirectives())) {
            return;
        }
        if (!doesFragmentConditionMatch(parameters, fragmentDefinition)) {
            return;
        }

        if (deferSupport.checkForDeferDirective(fragmentSpread, parameters.getVariables())) {
            deferFragment(parameters, fragmentSpread, fragmentDefinition.getSelectionSet(), deferredFragments);
            return;
        }

        visitedFragments.add(fragmentSpread.getName());

        collectFields(parameters, fragmentDefinition.getSelectionSet(), visitedFragments, fields, deferredFragments);
    }

    private void collectInlineFragment(FieldCollectorParameters parameters, Set<String> visitedFragments, Map<String, MergedField> fields, InlineFragment inlineFragment, Set<DeferFragment> deferredFragments) {
        if (!conditionalNodes.shouldInclude(parameters.getVariables(), inlineFragment.getDirectives()) ||
                !doesFragmentConditionMatch(parameters, inlineFragment)) {
            return;
        }

        if (deferSupport.checkForDeferDirective(inlineFragment, parameters.getVariables())) {
            deferFragment(parameters, inlineFragment, deferredFragments);
            return;
        }

        collectFields(parameters, inlineFragment.getSelectionSet(), visitedFragments, fields, deferredFragments);
    }

    private void collectField(FieldCollectorParameters parameters, Map<String, MergedField> fields, Field field, Set<DeferFragment> deferredFragments) {
        if (!conditionalNodes.shouldInclude(parameters.getVariables(), field.getDirectives())) {
            return;
        }
        String name = getFieldEntryKey(field);
        if (fields.containsKey(name)) {
            MergedField curFields = fields.get(name);
            deferredFragments.addAll(curFields.getDeferredFragments());
            fields.put(name, curFields.transform(builder -> builder.addField(field).deferredFragments(deferredFragments)));
        } else {
            fields.put(name, MergedField.newMergedField(field).deferredFragments(deferredFragments).build());
        }
    }

    private String getFieldEntryKey(Field field) {
        if (field.getAlias() != null) {
            return field.getAlias();
        } else {
            return field.getName();
        }
    }


    private boolean doesFragmentConditionMatch(FieldCollectorParameters parameters, InlineFragment inlineFragment) {
        if (inlineFragment.getTypeCondition() == null) {
            return true;
        }
        GraphQLType conditionType;
        conditionType = getTypeFromAST(parameters.getGraphQLSchema(), inlineFragment.getTypeCondition());
        return checkTypeCondition(parameters, conditionType);
    }

    private boolean doesFragmentConditionMatch(FieldCollectorParameters parameters, FragmentDefinition fragmentDefinition) {
        GraphQLType conditionType;
        conditionType = getTypeFromAST(parameters.getGraphQLSchema(), fragmentDefinition.getTypeCondition());
        return checkTypeCondition(parameters, conditionType);
    }

    private boolean checkTypeCondition(FieldCollectorParameters parameters, GraphQLType conditionType) {
        GraphQLObjectType type = parameters.getObjectType();
        if (conditionType.equals(type)) {
            return true;
        }

        if (conditionType instanceof GraphQLInterfaceType) {
            List<GraphQLObjectType> implementations = parameters.getGraphQLSchema().getImplementations((GraphQLInterfaceType) conditionType);
            return implementations.contains(type);
        } else if (conditionType instanceof GraphQLUnionType) {
            return ((GraphQLUnionType) conditionType).getTypes().contains(type);
        }
        return false;
    }

    private void deferFragment(FieldCollectorParameters parameters, FragmentSpread fragmentSpread, SelectionSet selectionSet, Set<DeferFragment> deferredFragments) {
        Directive directive = fragmentSpread.getDirective(Directives.DeferDirective.getName());
        deferFragment(parameters, directive.getArguments(), selectionSet, deferredFragments);
    }

    private void deferFragment(FieldCollectorParameters parameters, InlineFragment inlineFragment, Set<DeferFragment> deferredFragments) {
        Directive directive = inlineFragment.getDirective(Directives.DeferDirective.getName());
        deferFragment(parameters, directive.getArguments(), inlineFragment.getSelectionSet(), deferredFragments);
    }

    private void deferFragment(FieldCollectorParameters parameters, List<Argument> arguments, SelectionSet selectionSet, Set<DeferFragment> deferredFragments) {
        Map<String, Object> values = valuesResolver.getArgumentValues(Directives.DeferDirective.getArguments(), arguments, parameters.getVariables());
        DeferFragment fragment = DeferFragment.newDeferFragment()
                .label((String)values.get("label"))
                .selectionSet(collectFields(parameters, selectionSet))
                .build();
        deferredFragments.add(fragment);
    }
}
