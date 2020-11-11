package graphql.execution;


import graphql.Directives;
import graphql.Internal;
import graphql.language.*;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLUnionType;

import java.util.*;

import static graphql.execution.FieldsAndPatches.newFieldsAndPatches;
import static graphql.execution.MergedSelectionSet.newMergedSelectionSet;
import static graphql.execution.Patch.newPatch;
import static graphql.execution.TypeFromAST.getTypeFromAST;

/**
 * A field collector can iterate over field selection sets and build out the sub fields that have been selected,
 * expanding named and inline fragments as it goes.s
 */
@Internal
public class FieldCollector {

    private final ConditionalNodes conditionalNodes = new ConditionalNodes();
    private final ValuesResolver valuesResolver = new ValuesResolver();

    public FieldsAndPatches collectFields(FieldCollectorParameters parameters, MergedField mergedField) {
        Map<String, MergedField> subFields = new LinkedHashMap<>();
        Set<String> visitedFragments = new LinkedHashSet<>();
        List<Patch> patches = new LinkedList<>();
        for (Field field : mergedField.getFields()) {
            if (field.getSelectionSet() == null) {
                continue;
            }
            this.collectFields(parameters, field.getSelectionSet(), visitedFragments, subFields, patches);
        }
        MergedSelectionSet fields = newMergedSelectionSet()
                .subFields(subFields)
                .build();

        return newFieldsAndPatches().fields(fields).patches(patches).build();
    }

    /**
     * Given a selection set this will collect the sub-field selections and return it as a map
     *
     * @param parameters   the parameters to this method
     * @param selectionSet the selection set to collect on
     *
     * @return a map of the sub field selections
     */
    public FieldsAndPatches collectFields(FieldCollectorParameters parameters, SelectionSet selectionSet) {
        Map<String, MergedField> subFields = new LinkedHashMap<>();
        Set<String> visitedFragments = new LinkedHashSet<>();
        List<Patch> patches = new LinkedList<>();
        this.collectFields(parameters, selectionSet, visitedFragments, subFields, patches);
        MergedSelectionSet fields = newMergedSelectionSet().subFields(subFields).build();

        return newFieldsAndPatches().fields(fields).patches(patches).build();
    }


    private void collectFields(FieldCollectorParameters parameters, SelectionSet selectionSet, Set<String> visitedFragments, Map<String, MergedField> fields, List<Patch> patches) {

        for (Selection selection : selectionSet.getSelections()) {
            if (selection instanceof Field) {
                collectField(parameters, fields, (Field) selection);
            } else if (selection instanceof InlineFragment) {
                collectInlineFragment(parameters, visitedFragments, fields, (InlineFragment) selection, patches);
            } else if (selection instanceof FragmentSpread) {
                collectFragmentSpread(parameters, visitedFragments, fields, (FragmentSpread) selection, patches);
            }
        }
    }

    private void collectFragmentSpread(FieldCollectorParameters parameters, Set<String> visitedFragments, Map<String, MergedField> fields, FragmentSpread fragmentSpread, List<Patch> patches) {
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

        if (isDeferred(parameters, fragmentSpread)) {
            deferFragment(parameters, fragmentSpread, fragmentDefinition.getSelectionSet(), patches);
            return;
        }

        visitedFragments.add(fragmentSpread.getName());

        collectFields(parameters, fragmentDefinition.getSelectionSet(), visitedFragments, fields, patches);
    }

    private void collectInlineFragment(FieldCollectorParameters parameters, Set<String> visitedFragments, Map<String, MergedField> fields, InlineFragment inlineFragment, List<Patch> patches) {
        if (!conditionalNodes.shouldInclude(parameters.getVariables(), inlineFragment.getDirectives()) ||
                !doesFragmentConditionMatch(parameters, inlineFragment)) {
            return;
        }

        if (isDeferred(parameters, inlineFragment)) {
            deferFragment(parameters, inlineFragment, inlineFragment.getSelectionSet(), patches);
            return;
        }

        collectFields(parameters, inlineFragment.getSelectionSet(), visitedFragments, fields, patches);
    }

    private void collectField(FieldCollectorParameters parameters, Map<String, MergedField> fields, Field field) {
        if (!conditionalNodes.shouldInclude(parameters.getVariables(), field.getDirectives())) {
            return;
        }
        String name = getFieldEntryKey(field);
        if (fields.containsKey(name)) {
            MergedField curFields = fields.get(name);
            fields.put(name, curFields.transform(builder -> builder.addField(field)));
        } else {
            fields.put(name, MergedField.newMergedField(field).build());
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

    private Boolean isDeferred(FieldCollectorParameters parameters, DirectivesContainer<?> container) {
        Directive directive = container.getDirective(Directives.DeferDirective.getName());
        if (directive != null) {
            Map<String, Object> values = valuesResolver.getArgumentValues(Directives.DeferDirective.getArguments(), directive.getArguments(), parameters.getVariables());
            Object ifValue = values.get("if");
            return !Boolean.FALSE.equals(ifValue);
        }

        return false;
    }

    private void deferFragment(FieldCollectorParameters parameters, DirectivesContainer<?> container, SelectionSet selectionSet, List<Patch> patches) {
        Directive directive = container.getDirective(Directives.DeferDirective.getName());
        Map<String, Object> values = valuesResolver.getArgumentValues(Directives.DeferDirective.getArguments(), directive.getArguments(), parameters.getVariables());

        FieldsAndPatches fieldsAndPatches = collectFields(parameters, selectionSet);

        Patch patch = newPatch()
                .fields(fieldsAndPatches.getFields())
                .label((String)values.get("label"))
                .build();

        patches.add(patch);
        patches.addAll(fieldsAndPatches.getPatches());
    }
}
