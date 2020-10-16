package graphql.execution;

import graphql.Assert;
import graphql.PublicApi;

import java.util.*;


@PublicApi
public class MergedSelectionSet {

    private final Map<String, MergedField> subFields;
    private final Set<DeferFragment> deferredFragments;

    private MergedSelectionSet(Map<String, MergedField> subFields, Set<DeferFragment> deferredFragments) {
        this.subFields = Assert.assertNotNull(subFields);
        this.deferredFragments = Assert.assertNotNull(deferredFragments);
    }

    public Map<String, MergedField> getSubFields() {
        return subFields;
    }

    public List<MergedField> getSubFieldsList() {
        return new ArrayList<>(subFields.values());
    }

    public int size() {
        return subFields.size();
    }

    public Set<String> keySet() {
        return subFields.keySet();
    }

    public MergedField getSubField(String key) {
        return subFields.get(key);
    }

    public List<String> getKeys() {
        return new ArrayList<>(keySet());
    }

    public boolean isEmpty() {
        return subFields.isEmpty();
    }

    public Set<DeferFragment> getDeferredFragments() {
        return deferredFragments;
    }

    public static Builder newMergedSelectionSet() {
        return new Builder();
    }

    public static class Builder {
        private Map<String, MergedField> subFields = new LinkedHashMap<>();
        private Set<DeferFragment> deferredFragments = new LinkedHashSet<>();

        private Builder() {

        }

        public Builder subFields(Map<String, MergedField> subFields) {
            this.subFields = subFields;
            return this;
        }

        public Builder deferredFragments(Set<DeferFragment> deferredFragments) {
            this.deferredFragments = deferredFragments;
            return this;
        }

        public MergedSelectionSet build() {
            return new MergedSelectionSet(subFields, deferredFragments);
        }

    }

}
