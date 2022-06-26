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
import java.util.LinkedList;
import java.util.Objects;
import java.util.Set;
import proguard.analysis.cpa.bam.BamLocationDependent;
import proguard.analysis.cpa.defaults.LatticeAbstractState;
import proguard.analysis.cpa.defaults.ProgramLocationDependentReachedSet;
import proguard.analysis.cpa.interfaces.AbstractState;
import proguard.analysis.cpa.interfaces.ProgramLocationDependent;
import proguard.analysis.cpa.jvm.cfa.edges.JvmCfaEdge;
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode;
import proguard.analysis.cpa.jvm.witness.JvmMemoryLocation;
import proguard.classfile.MethodSignature;

/**
 * This {@link AbstractState} consists of a {@link BamLocationDependentJvmMemoryLocation} with a set of sources contributed into its value and the call
 * stack that generated it.
 *
 * @param <AbstractStateT> The type of the abstract states in the BAM cache.
 * 
 * @author Dmitry Ivanov
 */
public class JvmMemoryLocationAbstractState<AbstractStateT extends AbstractState & ProgramLocationDependent<JvmCfaNode, JvmCfaEdge, MethodSignature>>
    implements LatticeAbstractState<JvmMemoryLocationAbstractState<AbstractStateT>>,
               ProgramLocationDependent<JvmCfaNode, JvmCfaEdge, MethodSignature>,
               BamLocationDependent<JvmCfaNode, JvmCfaEdge, AbstractStateT, MethodSignature>
{

    public static final JvmMemoryLocationAbstractState                             top = new JvmMemoryLocationAbstractState();
    private final       BamLocationDependentJvmMemoryLocation<AbstractStateT>      locationDependentMemoryLocation;
    private final       Set<BamLocationDependentJvmMemoryLocation<AbstractStateT>> sourceLocations;
    // LinkedList is used here because the data structure needs to support both stack operations and an equals operator that compares all the members (which is not a case for ArrayDeque)
    private final       LinkedList<StackEntry>                                     callStack;

    private JvmMemoryLocationAbstractState()
    {
        this((BamLocationDependentJvmMemoryLocation) null, null, null);
    }

    /**
     * Create a {@link JvmMemoryLocationAbstractState} with empty source locations and call stack.
     *
     * @param locationDependentMemoryLocation a {@link JvmMemoryLocation} in a specified program location coming from a specific reached set.
     */
    public JvmMemoryLocationAbstractState(BamLocationDependentJvmMemoryLocation<AbstractStateT> locationDependentMemoryLocation)
    {
        this(locationDependentMemoryLocation, new HashSet<>(), new LinkedList<>());
    }

    /**
     * Create a {@link JvmMemoryLocationAbstractState} with empty source locations and call stack.
     *
     * @param memoryLocation   a memory location.
     * @param programLocation  the program location of the memory location.
     * @param sourceReachedSet the reached set of the traced analysis from which the analyzed state comes from.
     */
    public JvmMemoryLocationAbstractState(JvmMemoryLocation memoryLocation,
                                          JvmCfaNode programLocation,
                                          ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, AbstractStateT, MethodSignature> sourceReachedSet)
    {
        this(memoryLocation, programLocation, sourceReachedSet, new HashSet<>());
    }

    /**
     * Create a {@link JvmMemoryLocationAbstractState} with empty call stack.
     *
     * @param memoryLocation   a memory location.
     * @param programLocation  the program location of the memory location.
     * @param sourceReachedSet the reached set of the traced analysis from which the analyzed state comes from.
     * @param sourceLocations  the succcessor memory locations.
     */
    public JvmMemoryLocationAbstractState(JvmMemoryLocation memoryLocation,
                                          JvmCfaNode programLocation,
                                          ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, AbstractStateT, MethodSignature> sourceReachedSet,
                                          Set<BamLocationDependentJvmMemoryLocation<AbstractStateT>> sourceLocations)
    {
        this(memoryLocation, programLocation, sourceReachedSet, sourceLocations, new LinkedList<>());
    }

    /**
     * Create a {@link JvmMemoryLocationAbstractState} with empty source locations.
     *
     * @param memoryLocation   a memory location.
     * @param programLocation  the program location of the memory location.
     * @param sourceReachedSet the reached set of the traced analysis from which the analyzed state comes from.
     * @param callStack        the call stack.
     */
    public JvmMemoryLocationAbstractState(JvmMemoryLocation memoryLocation,
                                          JvmCfaNode programLocation,
                                          ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, AbstractStateT, MethodSignature> sourceReachedSet,
                                          LinkedList<StackEntry> callStack)
    {
        this(memoryLocation, programLocation, sourceReachedSet, new HashSet<>(), callStack);
    }

    /**
     * Create a {@link JvmMemoryLocationAbstractState} with source locations.
     *
     * @param memoryLocation   a memory location.
     * @param programLocation  the program location of the memory location.
     * @param sourceReachedSet the reached set of the traced analysis from which the analyzed state comes from.
     * @param sourceLocations  the succcessor memory locations.
     * @param callStack        the call stack.
     */
    public JvmMemoryLocationAbstractState(JvmMemoryLocation memoryLocation,
                                          JvmCfaNode programLocation,
                                          ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, AbstractStateT, MethodSignature> sourceReachedSet,
                                          Set<BamLocationDependentJvmMemoryLocation<AbstractStateT>> sourceLocations,
                                          LinkedList<StackEntry> callStack)
    {
        this(new BamLocationDependentJvmMemoryLocation<>(memoryLocation, programLocation, sourceReachedSet),
             sourceLocations,
             callStack);
    }

    private JvmMemoryLocationAbstractState(BamLocationDependentJvmMemoryLocation<AbstractStateT> locationDependentMemoryLocation,
                                           Set<BamLocationDependentJvmMemoryLocation<AbstractStateT>> sourceLocations,
                                           LinkedList<StackEntry> callStack)
    {
        this.locationDependentMemoryLocation = locationDependentMemoryLocation;
        this.sourceLocations = sourceLocations;
        this.callStack = callStack;
    }

    public BamLocationDependentJvmMemoryLocation<AbstractStateT> getLocationDependentMemoryLocation()
    {
        return locationDependentMemoryLocation;
    }

    /**
     * Returns the information of the caller, null if the caller of the method the state belongs to is unknown.
     */
    public StackEntry peekCallStack()
    {
        return callStack.peek();
    }

    /**
     * Returns true if a method is present in the call stack.
     */
    public boolean callStackContains(MethodSignature signature)
    {
        return callStack.stream().anyMatch(e -> signature.equals(e.signature));
    }

    /**
     * Returns a shallow copy of the call stack.
     */
    public LinkedList<StackEntry> copyStack()
    {
        return new LinkedList<>(callStack);
    }

    // implementations for LatticeAbstractState

    @Override
    public JvmMemoryLocationAbstractState join(JvmMemoryLocationAbstractState abstractState)
    {
        if (!locationDependentMemoryLocation.equals(abstractState.locationDependentMemoryLocation))
        {
            return top;
        }
        JvmMemoryLocationAbstractState result = copy();
        result.sourceLocations.addAll(abstractState.sourceLocations);
        return equals(result) ? this : result;
    }

    @Override
    public boolean isLessOrEqual(JvmMemoryLocationAbstractState<AbstractStateT> abstractState)
    {
        return abstractState == top
               || locationDependentMemoryLocation.equals(abstractState.locationDependentMemoryLocation)
                  && callStack.equals(abstractState.callStack)
                  && abstractState.sourceLocations.containsAll(sourceLocations);
    }

    // implementations for ProgramLocationDependent

    @Override
    public JvmCfaNode getProgramLocation()
    {
        return locationDependentMemoryLocation.getProgramLocation();
    }

    @Override
    public void setProgramLocation(JvmCfaNode programLocation)
    {
        locationDependentMemoryLocation.setProgramLocation(programLocation);
    }

    @Override
    public ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, AbstractStateT, MethodSignature> getSourceReachedSet()
    {
        return locationDependentMemoryLocation.getSourceReachedSet();
    }

    @Override
    public void setSourceReachedSet(ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, AbstractStateT, MethodSignature> sourceReachedSet)
    {
        locationDependentMemoryLocation.setSourceReachedSet(sourceReachedSet);
    }

    /**
     * Adds a source location to the source set.
     */
    public void addSourceLocation(BamLocationDependentJvmMemoryLocation<AbstractStateT> sourceLocation)
    {
        sourceLocations.add(sourceLocation);
    }

    /**
     * Returns the source set.
     */
    public Set<BamLocationDependentJvmMemoryLocation<AbstractStateT>> getSourceLocations()
    {
        return sourceLocations;
    }

    // implementations for AbstractState

    @Override
    public JvmMemoryLocationAbstractState copy()
    {
        return new JvmMemoryLocationAbstractState(locationDependentMemoryLocation.copy(),
                                                  new HashSet<>(sourceLocations),
                                                  callStack);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof JvmMemoryLocationAbstractState))
        {
            return false;
        }
        JvmMemoryLocationAbstractState other = (JvmMemoryLocationAbstractState) obj;
        return Objects.equals(locationDependentMemoryLocation, other.locationDependentMemoryLocation)
               && Objects.equals(callStack, other.callStack)
               && Objects.equals(sourceLocations, other.sourceLocations);
    }


    @Override
    public int hashCode()
    {
        return Objects.hash(locationDependentMemoryLocation);
    }

    /**
     * An entry of the call stack of the state.
     */
    public static class StackEntry
    {

        public final MethodSignature                    signature;
        public final ProgramLocationDependentReachedSet reachedSet;
        public final AbstractState                      callerState;

        public StackEntry(MethodSignature signature,
            ProgramLocationDependentReachedSet reachedSet,
            AbstractState callerState)
        {
            this.signature = signature;
            this.reachedSet = reachedSet;
            this.callerState = callerState;
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(signature, reachedSet, callerState);
        }

        @Override
        public boolean equals(Object obj)
        {
            if (!(obj instanceof StackEntry))
            {
                return false;
            }

            StackEntry other = (StackEntry) obj;
            return reachedSet == other.reachedSet && Objects.equals(signature, other.signature) && Objects.equals(callerState, other.callerState);
        }
    }

}
