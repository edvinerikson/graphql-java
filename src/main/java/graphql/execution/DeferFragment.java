package graphql.execution;

import graphql.Assert;
import graphql.language.SelectionSet;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLType;

public class DeferFragment {
    private GraphQLObjectType objectType;
    private String label;
    private MergedSelectionSet selectionSet;

    DeferFragment(String label, MergedSelectionSet selectionSet, GraphQLObjectType objectType) {
        this.label = label;
        this.selectionSet = Assert.assertNotNull(selectionSet);
        // this.objectType = Assert.assertNotNull(objectType);
    }

    public MergedSelectionSet getSelectionSet() {
        return selectionSet;
    }

    public String getLabel() {
        return label;
    }

    public GraphQLObjectType getObjectType() {
        return objectType;
    }

    public static Builder newDeferFragment() {
        return new Builder();
    }

    public static class Builder {
        private String label;
        private MergedSelectionSet selectionSet;
        private GraphQLObjectType objectType;

        public Builder() {

        }

        public Builder label(String label) {
            this.label = label;
            return this;
        }

        public Builder selectionSet(MergedSelectionSet selectionSet) {
            this.selectionSet = selectionSet;
            return this;
        }

        public Builder objectType(GraphQLObjectType objectType) {
            this.objectType = objectType;
            return this;
        }

        public DeferFragment build() {
            return new DeferFragment(label, selectionSet, objectType);
        }
    }
}
