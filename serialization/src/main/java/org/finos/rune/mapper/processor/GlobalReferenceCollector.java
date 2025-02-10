package org.finos.rune.mapper.processor;

/*-
 * ==============
 * Rune Common
 * ==============
 * Copyright (C) 2018 - 2025 REGnosys
 * ==============
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ==============
 */

import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.meta.ReferenceWithMeta;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.AttributeMeta;
import com.rosetta.model.lib.process.Processor;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GlobalReferenceCollector implements Processor {
    private final Set<GlobalReferenceRecord> globalReferences = new HashSet<>();

    @Override
    public <R extends RosettaModelObject> boolean processRosetta(RosettaPath path, Class<? extends R> rosettaType, R instance, RosettaModelObject parent, AttributeMeta... metas) {
        if (instance instanceof ReferenceWithMeta) {
            @SuppressWarnings("unchecked")
            ReferenceWithMeta<R> reference = (ReferenceWithMeta<R>) instance;
            Class<?> referenceValueType = reference.getValueType();
            String referenceKeyValue = reference.getGlobalReference();
            globalReferences.add(new GlobalReferenceRecord(referenceValueType, referenceKeyValue));
        }
        return true;
    }

    @Override
    public <R extends RosettaModelObject> boolean processRosetta(RosettaPath path, Class<? extends R> rosettaType, List<? extends R> instances, RosettaModelObject parent, AttributeMeta... metas) {
        if (instances == null) {
            return false;
        }
        boolean result = true;
        for (int i = 0; i < instances.size(); i++) {
            R instance = instances.get(i);
            path = path.withIndex(i);
            result &= processRosetta(path, rosettaType, instance, parent, metas);
        }
        return result;
    }

    @Override
    public <T> void processBasic(RosettaPath path, Class<? extends T> rosettaType, T instance, RosettaModelObject parent, AttributeMeta... metas) {
        //No references on basic types on their corresponding wrapper type
    }

    @Override
    public <T> void processBasic(RosettaPath path, Class<? extends T> rosettaType, Collection<? extends T> instances, RosettaModelObject parent, AttributeMeta... metas) {
        if (instances == null)
            return;
        for (T instance : instances) {
            processBasic(path, rosettaType, instance, parent, metas);
        }
    }

    public Set<GlobalReferenceRecord> getGlobalReferences() {
        return globalReferences;
    }

    @Override
    public Report report() {
        throw new UnsupportedOperationException("Report not supported for UnreferencedKeyCollector");
    }
}
