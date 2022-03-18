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

package proguard.analysis.cpa.jvm.domain.memory;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import proguard.analysis.cpa.interfaces.AbstractState;
import proguard.classfile.MethodSignature;
import proguard.analysis.cpa.defaults.LatticeAbstractState;
import proguard.analysis.cpa.domain.arg.ArgProgramLocationDependentAbstractState;
import proguard.analysis.cpa.interfaces.ProgramLocationDependent;
import proguard.analysis.cpa.jvm.cfa.edges.JvmCfaEdge;
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode;
import proguard.analysis.cpa.jvm.witness.JvmMemoryLocation;

/**
 * This {@link AbstractState} consists of a memory location with a set of sources contributed into its value.
 *
 * @author Dmitry Ivanov
 */
public class JvmMemoryLocationAbstractState
    implements LatticeAbstractState<JvmMemoryLocationAbstractState>,
               ProgramLocationDependent<JvmCfaNode, JvmCfaEdge, MethodSignature>
{

    public  static final JvmMemoryLocationAbstractState top = new JvmMemoryLocationAbstractState();
    private        final JvmMemoryLocation              memoryLocation;
    private        final Set<JvmMemoryLocation>         sourceLocations;

    /**
     * Create a memory location abstract state.
     *
     * @param memoryLocation a wrapped memory location pointing at an {@link AbstractState} encapsulated by the traced analysis
     */
    public JvmMemoryLocationAbstractState(JvmMemoryLocation memoryLocation)
    {
        this(memoryLocation, new HashSet<>());
    }

    /**
     * Create a memory location abstract state.
     *
     * @param memoryLocation  a wrapped memory location pointing at an {@link AbstractState} encapsulated by the traced analysis
     * @param sourceLocations source memory locations at CFA predecessors contributing into the value of the {@code memoryLocation} pointee
     */
    public JvmMemoryLocationAbstractState(JvmMemoryLocation memoryLocation,
                                          Set<JvmMemoryLocation> sourceLocations)
    {
        this.memoryLocation = memoryLocation;
        this.sourceLocations = sourceLocations;
    }

    private JvmMemoryLocationAbstractState()
    {
        this(null);
    }

    // implementations for LatticeAbstractState

    @Override
    public JvmMemoryLocationAbstractState join(JvmMemoryLocationAbstractState abstractState)
    {
        if (!memoryLocation.equals(abstractState.memoryLocation))
        {
            return top;
        }
        JvmMemoryLocationAbstractState result = copy();
        result.sourceLocations.addAll(abstractState.sourceLocations);
        return equals(result) ? this : result;
    }

    @Override
    public boolean isLessOrEqual(JvmMemoryLocationAbstractState abstractState)
    {
        return abstractState == top
               || memoryLocation.equals(abstractState.memoryLocation)
                  && abstractState.sourceLocations.containsAll(sourceLocations);
    }

    // implementations for ProgramLocationDependent

    @Override
    public JvmCfaNode getProgramLocation()
    {
        return memoryLocation.getProgramLocation();
    }

    @Override
    public void setProgramLocation(JvmCfaNode programLocation)
    {
        memoryLocation.setProgramLocation(programLocation);
    }

    public ArgProgramLocationDependentAbstractState<JvmCfaNode, JvmCfaEdge, MethodSignature> getArgNode()
    {
        return memoryLocation.getArgNode();
    }

    /**
     * Returns the memory location.
     */
    public JvmMemoryLocation getMemoryLocation()
    {
        return memoryLocation;
    }

    /**
     * Adds a source location to the source set.
     */
    public void addSourceLocation(JvmMemoryLocation sourceLocation)
    {
        sourceLocations.add(sourceLocation);
    }

    /**
     * Returns the source set.
     */
    public Set<JvmMemoryLocation> getSourceLocations()
    {
        return sourceLocations;
    }

    // implementations for AbstractState

    @Override
    public JvmMemoryLocationAbstractState copy()
    {
        return new JvmMemoryLocationAbstractState(memoryLocation, new HashSet<>(sourceLocations));
    }

    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof JvmMemoryLocationAbstractState))
        {
            return false;
        }
        JvmMemoryLocationAbstractState other = (JvmMemoryLocationAbstractState) obj;
        return memoryLocation.equals(other.memoryLocation) && sourceLocations.equals(other.sourceLocations);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(memoryLocation);
    }
}
