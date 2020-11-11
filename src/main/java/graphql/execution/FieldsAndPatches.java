package graphql.execution;

import java.util.List;

public class FieldsAndPatches {
    private MergedSelectionSet fields;
    private List<Patch> patches;

    FieldsAndPatches(MergedSelectionSet fields, List<Patch> patches) {
        this.fields = fields;
        this.patches = patches;
    }

    public List<Patch> getPatches() {
        return patches;
    }

    public MergedSelectionSet getFields() {
        return fields;
    }

    static class Builder {
        private MergedSelectionSet fields;
        private List<Patch> patches;


        public Builder fields(MergedSelectionSet fields) {
            this.fields = fields;
            return this;
        }

        public Builder patches(List<Patch> patches) {
            this.patches = patches;
            return this;
        }

        public FieldsAndPatches build() {
            return new FieldsAndPatches(fields, patches);
        }
    }

    public static Builder newFieldsAndPatches() {
        return new Builder();
    }
}
