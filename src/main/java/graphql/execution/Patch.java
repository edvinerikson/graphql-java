package graphql.execution;

import java.util.List;

public class Patch {
    private String label;
    private MergedSelectionSet fields;

    Patch(String label, MergedSelectionSet fields) {
        this.label = label;
        this.fields = fields;
    }

    public String getLabel() {
        return label;
    }

    public MergedSelectionSet getFields() {
        return fields;
    }

    static class Builder {
        private String label;
        private MergedSelectionSet fields;

        public Builder label(String label) {
            this.label = label;
            return this;
        }

        public Builder fields(MergedSelectionSet fields) {
            this.fields = fields;
            return this;
        }

        public Patch build() {
            return new Patch(label, fields);
        }
    }

    static Builder newPatch() {
        return new Builder();
    }
}
