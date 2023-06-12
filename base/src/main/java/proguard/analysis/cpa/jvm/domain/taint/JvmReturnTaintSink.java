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

import java.util.Collections;
import java.util.Set;
import java.util.function.Predicate;
import proguard.analysis.cpa.domain.taint.TaintSource;
import proguard.analysis.cpa.jvm.cfa.edges.JvmCfaEdge;
import proguard.analysis.cpa.jvm.cfa.edges.JvmInstructionCfaEdge;
import proguard.analysis.cpa.jvm.witness.JvmMemoryLocation;
import proguard.analysis.cpa.jvm.witness.JvmStackLocation;
import proguard.classfile.Signature;

/**
 * A {@link JvmTaintSink} triggered if the return value of
 * the specified method is tainted.
 */
public class JvmReturnTaintSink
    extends JvmTaintSink
{

    public JvmReturnTaintSink(Signature signature)
    {
        super(signature);
    }

    public JvmReturnTaintSink(Signature signature, Predicate<TaintSource> isValidForSource)
    {
        super(signature, isValidForSource);
    }

    // Implementations for JvmTaintSink

    /**
     * The location of values returned by a method is the top of the stack.
     * Since in our convention just the top value is tainted for category 2
     * types just the top is enough.
     */
    @Override
    public Set<JvmMemoryLocation> getMemoryLocations()
    {
        return Collections.singleton(new JvmStackLocation(0));
    }

    /**
     * Returns true on the return edge of the sink method.
     */
    @Override
    public boolean matchCfaEdge(JvmCfaEdge edge)
    {
        return edge instanceof JvmInstructionCfaEdge && edge.getTarget().isReturnExitNode();
    }

    // Implementations for Object

    @Override
    public boolean equals(Object o)
    {
        return super.equals(o);
    }

    @Override
    public int hashCode()
    {
        return super.hashCode();
    }

    @Override
    public String toString()
    {
        StringBuilder result = new StringBuilder("[JvmReturnTaintSink] ").append(signature);
        if (!IS_VALID_FOR_SOURCE_DEFAULT.equals(isValidForSource))
        {
            result.append(", filtered by source: ").append(isValidForSource);
        }

        return result.toString();
    }
}
