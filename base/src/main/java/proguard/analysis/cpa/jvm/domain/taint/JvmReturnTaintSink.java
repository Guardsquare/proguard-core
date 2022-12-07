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
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import proguard.analysis.cpa.jvm.cfa.edges.JvmCfaEdge;
import proguard.analysis.cpa.jvm.cfa.edges.JvmInstructionCfaEdge;
import proguard.analysis.cpa.jvm.witness.JvmMemoryLocation;
import proguard.analysis.cpa.jvm.witness.JvmStackLocation;

/**
 * A {@link JvmTaintSink} triggered if the return value of
 * the specified method is tainted.
 *
 * @author Carlo Alberto Pozzoli
 */
public class JvmReturnTaintSink
    extends JvmTaintSink
{

    public JvmReturnTaintSink(String fqn)
    {
        super(fqn);
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
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof JvmReturnTaintSink))
        {
            return false;
        }
        JvmReturnTaintSink taintSink = (JvmReturnTaintSink) o;
        return Objects.equals(fqn, taintSink.fqn);
    }

    @Override
    public int hashCode()
    {
        return fqn.hashCode();
    }

    @Override
    public String toString()
    {
        return "[JvmReturnTaintSink] " + fqn;
    }
}
