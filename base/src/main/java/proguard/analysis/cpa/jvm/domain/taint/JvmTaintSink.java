/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2022 Guardsquare NV
 *
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
 */

package proguard.analysis.cpa.jvm.domain.taint;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import proguard.analysis.cpa.domain.taint.TaintSource;
import proguard.analysis.cpa.jvm.cfa.edges.JvmCfaEdge;
import proguard.analysis.cpa.domain.taint.TaintSink;
import proguard.analysis.cpa.jvm.witness.JvmMemoryLocation;
import proguard.classfile.Signature;

/**
 * The {@link JvmTaintSink} adds an interface for extracting sensitive JVM memory locations and to check
 * if the sink matches a given cfa edge.
 */
public abstract class JvmTaintSink
    extends TaintSink
{

    public JvmTaintSink(Signature signature)
    {
        super(signature);
    }

    public JvmTaintSink(Signature signature, Predicate<TaintSource> isValidForSource)
    {
        super(signature, isValidForSource);
    }

    /**
     * Returns memory locations which trigger this taint sink.
     */
    public abstract Set<JvmMemoryLocation> getMemoryLocations();

    /**
     * Returns whether the sink matches a given CFA edge.
     */
    public abstract boolean matchCfaEdge(JvmCfaEdge edge);

    /**
     * Helper method taking a collection of sinks and converting it to a mapping that associates each sink
     * with the memory locations which triggers it. For convenience the sinks are further grouped
     * by their method signature.
     */
    public static Map<Signature, Map<JvmTaintSink, Set<JvmMemoryLocation>>> convertSinksToMemoryLocations(Collection<? extends JvmTaintSink> taintSinks)
    {
        Map<Signature, Map<JvmTaintSink, Set<JvmMemoryLocation>>> result = new HashMap<>();
        for (JvmTaintSink taintSink : taintSinks)
        {
            Set<JvmMemoryLocation> memoryLocations = taintSink.getMemoryLocations();
            result.computeIfAbsent(taintSink.signature, x -> new HashMap<>()).put(taintSink, memoryLocations);
        }
        return result;
    }

    @Override
    public boolean equals(Object other)
    {
        return super.equals(other);
    }

    @Override
    public int hashCode()
    {
        return super.hashCode();
    }
}
