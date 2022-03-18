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

package proguard.analysis.cpa.jvm.witness;

import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Collectors;
import proguard.classfile.MethodSignature;
import proguard.analysis.cpa.defaults.LatticeAbstractState;
import proguard.analysis.cpa.defaults.SetAbstractState;
import proguard.analysis.cpa.domain.arg.ArgProgramLocationDependentAbstractState;
import proguard.analysis.cpa.jvm.cfa.edges.JvmCfaEdge;
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode;
import proguard.analysis.cpa.jvm.domain.reference.Reference;
import proguard.analysis.cpa.jvm.state.JvmAbstractState;

/**
 * The {@link JvmHeapLocation} is a memory location corresponding to a dynamic memory entity at a certain program point.
 *
 * @author Dmitry Ivanov
 */
public class JvmHeapLocation
    extends JvmMemoryLocation
{

    public final SetAbstractState<Reference> reference;
    public final String                      field;

    /**
     * Create a heap location unspecific to a program location.
     *
     * @param reference a reference to dynamic memory
     */
    public JvmHeapLocation(SetAbstractState<Reference> reference, String field)
    {
        this(null, reference, field);
    }

    /**
     * Create a heap location at a specific program location.
     *
     * @param argNode   an ARG node
     * @param reference a reference to dynamic memory
     */
    public JvmHeapLocation(ArgProgramLocationDependentAbstractState<JvmCfaNode, JvmCfaEdge, MethodSignature> argNode, SetAbstractState<Reference> reference, String field)
    {
        this.argNode = argNode;
        this.reference = reference;
        this.field = field;
    }

    // implementations for JvmMemoryLocation

    @Override
    public JvmHeapLocation copy()
    {
        return new JvmHeapLocation(argNode, reference, field);
    }

    // implementations for MemoryLocation

    @Override
    public <T extends LatticeAbstractState> T extractValueOrDefault(JvmAbstractState abstractState, T defaultValue)
    {
        return (T) abstractState.getHeap().getField(reference, field, defaultValue);
    }

    // implementations for Object

    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof JvmHeapLocation))
        {
            return false;
        }
        JvmHeapLocation other = (JvmHeapLocation) obj;
        return super.equals(other) && reference.equals(other.reference);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(argNode, reference.hashCode());
    }

    @Override
    public String toString()
    {
        return "JvmHeapLocation(" + reference.stream()
                                             .sorted(Comparator.comparingInt(Reference::hashCode))
                                             .collect(Collectors.toList()) + ", " + field + ")" + (argNode == null
                                                                                                   ? ""
                                                                                                   : "@" + argNode.getProgramLocation().getSignature().getFqn() + ":" + argNode.getProgramLocation()
                                                                                                                                                                               .getOffset());
    }
}
