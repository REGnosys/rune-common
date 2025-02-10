package org.finos.rune.mapper.processor;

import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.meta.ReferenceWithMeta;

import java.util.HashSet;
import java.util.Set;

public class GlobalReferenceCollectorStrategy implements CollectorStrategy {
    private final Set<GlobalReferenceRecord> globalReferences = new HashSet<>();

    @Override
    public <R extends RosettaModelObject> void collect(R instance) {
        if (instance instanceof ReferenceWithMeta) {
            @SuppressWarnings("unchecked")
            ReferenceWithMeta<R> reference = (ReferenceWithMeta<R>) instance;
            Class<?> referenceValueType = reference.getValueType();
            String referenceKeyValue = reference.getGlobalReference();
            globalReferences.add(new GlobalReferenceRecord(referenceValueType, referenceKeyValue));
        }
    }

    public Set<GlobalReferenceRecord> getGlobalReferences() {
        return globalReferences;
    }
}
