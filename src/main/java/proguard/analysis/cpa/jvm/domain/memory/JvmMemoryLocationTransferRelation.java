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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import proguard.analysis.cpa.defaults.MemoryLocation;
import proguard.analysis.datastructure.callgraph.Call;
import proguard.classfile.Clazz;
import proguard.classfile.Method;
import proguard.classfile.MethodSignature;
import proguard.classfile.attribute.CodeAttribute;
import proguard.classfile.instruction.BranchInstruction;
import proguard.classfile.instruction.ConstantInstruction;
import proguard.classfile.instruction.Instruction;
import proguard.classfile.instruction.SimpleInstruction;
import proguard.classfile.instruction.SwitchInstruction;
import proguard.classfile.instruction.VariableInstruction;
import proguard.classfile.instruction.visitor.InstructionVisitor;
import proguard.classfile.util.ClassUtil;
import proguard.analysis.cpa.defaults.LatticeAbstractState;
import proguard.analysis.cpa.defaults.SetAbstractState;
import proguard.analysis.cpa.domain.arg.ArgProgramLocationDependentAbstractState;
import proguard.analysis.cpa.interfaces.AbstractState;
import proguard.analysis.cpa.interfaces.CfaNode;
import proguard.analysis.cpa.interfaces.Precision;
import proguard.analysis.cpa.interfaces.TransferRelation;
import proguard.analysis.cpa.jvm.cfa.edges.JvmCallCfaEdge;
import proguard.analysis.cpa.jvm.cfa.edges.JvmCfaEdge;
import proguard.analysis.cpa.jvm.cfa.edges.JvmInstructionCfaEdge;
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode;
import proguard.analysis.cpa.jvm.domain.reference.JvmReferenceAbstractState;
import proguard.analysis.cpa.jvm.domain.reference.Reference;
import proguard.analysis.cpa.jvm.state.JvmAbstractState;
import proguard.analysis.cpa.jvm.state.heap.tree.JvmTreeHeapFollowerAbstractState;
import proguard.analysis.cpa.jvm.util.ConstantLookupVisitor;
import proguard.analysis.cpa.jvm.witness.JvmHeapLocation;
import proguard.analysis.cpa.jvm.witness.JvmLocalVariableLocation;
import proguard.analysis.cpa.jvm.witness.JvmMemoryLocation;
import proguard.analysis.cpa.jvm.witness.JvmStackLocation;
import proguard.analysis.cpa.jvm.witness.JvmStaticFieldLocation;
import proguard.analysis.cpa.jvm.util.InstructionClassifier;
import proguard.analysis.cpa.util.StateNames;
import proguard.evaluation.ClassConstantValueFactory;
import proguard.evaluation.value.ParticularValueFactory;

/**
 * The {@link JvmMemoryLocationTransferRelation} computes the backward successors of an {@link JvmMemoryLocationAbstractState} for a given instruction. A backward successor
 * is a memory location which may have contributed to the value of the current {@link MemoryLocation}. The transfer
 * relation traverses over an ARG selecting the successor program locations from the ARG node parents and checking the reachability with the CFA.
 * The successor memory location is guaranteed to be greater than the threshold. Thus, the threshold defines the cut-off of the traces generated with {@link JvmMemoryLocationTransferRelation}.
 *
 * @author Dmitry Ivanov
 */
public class JvmMemoryLocationTransferRelation<AbstractStateT extends LatticeAbstractState<AbstractStateT>>
    implements TransferRelation
{

    private final AbstractStateT threshold;

    /**
     * Create a memory location transfer relation.
     *
     * @param threshold a cut-off threshold
     */
    public JvmMemoryLocationTransferRelation(AbstractStateT threshold)
    {
        this.threshold = threshold;
    }

    // implementations for TransferRelation

    @Override
    public Collection<? extends AbstractState> getAbstractSuccessors(AbstractState abstractState, Precision precision)
    {
        if (!(abstractState instanceof JvmMemoryLocationAbstractState))
        {
            throw new IllegalArgumentException(getClass().getName() + " does not support " + abstractState.getClass().getName());
        }
        JvmMemoryLocationAbstractState state = (JvmMemoryLocationAbstractState) abstractState.copy();

        List<JvmMemoryLocationAbstractState> successors = new ArrayList<>();

        for (ArgProgramLocationDependentAbstractState<JvmCfaNode, JvmCfaEdge, MethodSignature> parent : state.getArgNode().getParents())
        {
            if (parent.getProgramLocation().equals(state.getProgramLocation())) // the state was created after combining other states
            {
                JvmMemoryLocation memoryLocation = state.getMemoryLocation().copy();
                memoryLocation.setArgNode(parent);
                successors.add(new JvmMemoryLocationAbstractState(memoryLocation));
                continue;
            }
            MethodSignature parentSignature = parent.getProgramLocation().getSignature();
            MethodSignature currentSignature = state.getProgramLocation().getSignature();
            Optional<JvmCfaEdge> optionalEdge = parent.getProgramLocation().getLeavingEdges().stream().filter(e -> state.getProgramLocation().getEnteringEdges().contains(e)).findFirst();
            if (optionalEdge.isPresent()) // the state is the result of a transfer relation or a call
            {
                JvmCfaEdge edge = optionalEdge.get();
                JvmAbstractState<AbstractStateT> predecessorState = (JvmAbstractState<AbstractStateT>) (parent.getWrappedState().getStateByName(StateNames.Jvm));
                List<JvmMemoryLocation> successorMemoryLocations = new ArrayList<>();
                if (edge instanceof JvmInstructionCfaEdge) // trace back the intraprocedural transfer relation
                {
                    successorMemoryLocations.addAll(getSuccessorMemoryLocationsForInstruction(state,
                                                                                              parent,
                                                                                              ((JvmInstructionCfaEdge) edge).getInstruction(),
                                                                                              state.getProgramLocation().getClazz(),
                                                                                              precision).stream()
                                                                                                        .map(JvmMemoryLocation::copy)
                                                                                                        .collect(Collectors.toList()));
                }
                else if (edge instanceof JvmCallCfaEdge) // trace back the call
                {
                    if (state.getProgramLocation().getOffset() != 0) // do not trace the unknown caller
                    {
                        continue;
                    }
                    if (state.getMemoryLocation() instanceof JvmLocalVariableLocation) // trace the argument back to the caller
                    {
                        JvmLocalVariableLocation argumentLocation = (JvmLocalVariableLocation) state.getMemoryLocation();
                        Call call = ((JvmCallCfaEdge) edge).getCall();
                        boolean isStatic = call.invocationOpcode == Instruction.OP_INVOKESTATIC || call.invocationOpcode == Instruction.OP_INVOKEDYNAMIC;
                        String currentDescriptor = currentSignature.descriptor.toString();
                        int parameterNumber = ClassUtil.internalMethodParameterNumber(currentDescriptor, isStatic, argumentLocation.index);
                        int parameterSize = ClassUtil.internalMethodParameterSize(currentDescriptor, isStatic);
                        boolean isCategory2 = ClassUtil.isInternalCategory2Type(ClassUtil.internalMethodParameterType(currentDescriptor, parameterNumber));
                        JvmStackLocation operandLocation = new JvmStackLocation(parameterSize
                                                                                - argumentLocation.index
                                                                                - (isCategory2
                                                                                                     ? parameterNumber == ClassUtil.internalMethodParameterNumber(currentDescriptor,
                                                                                                                                                                  isStatic,
                                                                                                                                                                  argumentLocation.index + 1)
                                                                                                       ? 2
                                                                                                       : 0
                                                                                                     : 1));
                        successorMemoryLocations.add(operandLocation);
                    }
                    else // all other memory locations are preserved
                    {
                        successorMemoryLocations.add(state.getMemoryLocation().copy());
                    }
                }
                else // the catch edge preserves the memory location
                {
                    successorMemoryLocations.add(state.getMemoryLocation().copy());
                }
                successors.addAll(successorMemoryLocations.stream()
                                                          .filter(l -> !l.extractValueOrDefault(predecessorState, threshold).isLessOrEqual(threshold))
                                                          .peek(l -> l.setArgNode(parent))
                                                          .map(JvmMemoryLocationAbstractState::new)
                                                          .collect(Collectors.toList()));
            }
            else // the state was calculated without having a CFA edge, e.g., after a method return or upon entering a catch block from a method call
            {
                if (!parentSignature.equals(state.getProgramLocation().getSignature())) // method call
                {
                    if (state.getMemoryLocation() instanceof JvmLocalVariableLocation) // local variables aren't preserved across methods
                    {
                        continue;
                    }
                    if (state.getMemoryLocation() instanceof JvmStackLocation) // trace the return value back to the callee
                    {
                        JvmStackLocation stackLocation = (JvmStackLocation) state.getMemoryLocation();
                        int index = stackLocation.getIndex();
                        if (!(parent.getProgramLocation().getOffset() == CfaNode.RETURN_EXIT_NODE_OFFSET
                              && index <= ClassUtil.internalTypeSize(parentSignature.descriptor.returnType)
                              || parent.getProgramLocation().getOffset() == CfaNode.EXCEPTION_EXIT_NODE_OFFSET
                                 && index == 0))
                        {
                            continue;
                        }
                    }
                }
                else if (state.getMemoryLocation() instanceof JvmStackLocation
                         && ((JvmStackLocation) state.getMemoryLocation()).getIndex() != 0) // the stack isn't preserved except for its top
                {
                    continue;
                }
                AbstractStateT value = state.getMemoryLocation().extractValueOrDefault((JvmAbstractState<AbstractStateT>) parent.getWrappedState().getStateByName(StateNames.Jvm), threshold);

                if (value.isLessOrEqual(threshold))
                {
                    continue;
                }
                JvmMemoryLocation parentLocation = state.getMemoryLocation().copy();
                parentLocation.setArgNode(parent);
                successors.add(new JvmMemoryLocationAbstractState(parentLocation));
            }
        }
        successors.forEach(s -> state.addSourceLocation(s.getMemoryLocation()));
        successors.add(state);
        return successors;
    }

    private List<JvmMemoryLocation> getSuccessorMemoryLocationsForInstruction(JvmMemoryLocationAbstractState abstractState,
                                                                              ArgProgramLocationDependentAbstractState<JvmCfaNode, JvmCfaEdge, MethodSignature> parentState,
                                                                                              Instruction instruction,
                                                                                              Clazz clazz,
                                                                                              Precision precision)
    {
        List<JvmMemoryLocation> answer = new ArrayList<>();
        instruction.accept(clazz, null, null, 0, new InstructionAbstractInterpreter(answer, abstractState.getMemoryLocation(), parentState));
        return answer;
    }

    private List<JvmMemoryLocation> backtraceStackLocation(JvmMemoryLocation memoryLocation,
                                                           Instruction instruction,
                                                           Clazz clazz)
    {
        List<JvmMemoryLocation> result = new ArrayList<>();
        if (!(memoryLocation instanceof JvmStackLocation))
        {
            result.add(memoryLocation);
            return result;
        }
        int index = ((JvmStackLocation) memoryLocation).getIndex();
        int pushCount = instruction.stackPushCount(clazz);
        int popCount = instruction.stackPopCount(clazz);
        if (index >= pushCount)
        {
            result.add(new JvmStackLocation(index - pushCount + popCount));
            return result;
        }
        result.addAll(getPoppedLocations(popCount));
        return result;
    }

    private List<JvmMemoryLocation> getPoppedLocations(int popCount)
    {
        List<JvmMemoryLocation> result = new ArrayList<>();
        for (int i = 0; i < popCount; ++i)
        {
            result.add(new JvmStackLocation(i));
        }
        return result;
    }

    /**
     * The default implementation traces the return value back to the method arguments and the instance.
     */
    private List<JvmMemoryLocation> processCall(JvmMemoryLocation memoryLocation,
                                                ConstantInstruction callInstruction,
                                                Clazz clazz)
    {
        return memoryLocation instanceof JvmLocalVariableLocation
               ? Collections.singletonList(memoryLocation) // local variable locations aren't affected by calls
               : memoryLocation instanceof JvmStackLocation
                 && isStackLocationTooDeep((JvmStackLocation) memoryLocation, callInstruction, clazz)
                 || doesMemoryLocationDependOnReturnValue(memoryLocation)
                 ? backtraceStackLocation(memoryLocation, callInstruction, clazz) // trace deep stack locations and intraprocedurally analyzed calls intraprocedurally
                 : Collections.emptyList(); // do not trace intraprocedurally calls which were analyzed interprocedurally
    }

    private boolean isStackLocationTooDeep(JvmStackLocation stackLocation, Instruction instruction, Clazz clazz)
    {
        return stackLocation.index >= instruction.stackPushCount(clazz);
    }

    private boolean doesMemoryLocationDependOnReturnValue(JvmMemoryLocation memoryLocation)
    {
        return memoryLocation.getArgNode().getParents().stream().noneMatch(p -> p.getProgramLocation().isExitNode());
    }

    /**
     * This {@link InstructionVisitor} performs generic operations (e.g., loads, stores) parametrized by the specific behavior of
     * {@link JvmMemoryLocationTransferRelation} for instruction applications, method invocations, and constructing literals.
     */
    private class InstructionAbstractInterpreter
        implements InstructionVisitor
    {

        private final List<JvmMemoryLocation>                                                           answer;
        private final JvmMemoryLocation                                                                 memoryLocation;
        private final ArgProgramLocationDependentAbstractState<JvmCfaNode, JvmCfaEdge, MethodSignature> parentState;
        private final ClassConstantValueFactory                                                         classConstantValueFactory = new ClassConstantValueFactory(new ParticularValueFactory());
        private final ConstantLookupVisitor                                                             constantLookupVisitor     = new ConstantLookupVisitor();

        public InstructionAbstractInterpreter(List<JvmMemoryLocation> answer,
                                              JvmMemoryLocation memoryLocation,
                                              ArgProgramLocationDependentAbstractState<JvmCfaNode, JvmCfaEdge, MethodSignature> parentState)
        {
            this.answer = answer;
            this.memoryLocation  = memoryLocation;
            this.parentState = parentState;
        }

        // implementations for InstructionVisitor

        @Override
        public void visitSimpleInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, SimpleInstruction simpleInstruction)
        {
            if (simpleInstruction.opcode >= Instruction.OP_IASTORE && simpleInstruction.opcode <= Instruction.OP_SASTORE && memoryLocation instanceof JvmHeapLocation)
            {
                // only stack instructions affect heap locations
                answer.add(memoryLocation);
                if (((JvmAbstractState<AbstractStateT>) parentState.getWrappedState().getStateByName("Jvm")).getHeap() instanceof JvmTreeHeapFollowerAbstractState)
                {
                    SetAbstractState<Reference> arrayReference = ((JvmReferenceAbstractState) parentState.getWrappedState().getStateByName("Reference")).peek(simpleInstruction.isCategory2()
                                                                                                                                                              ? 3
                                                                                                                                                              : 2);
                    JvmHeapLocation heapLocation = (JvmHeapLocation) memoryLocation;
                    if (heapLocation.reference.stream().anyMatch(arrayReference::contains) && heapLocation.field.equals("[]"))
                    {
                        answer.add(new JvmStackLocation(0));
                    }
                }
                return;
            }
            if (!(memoryLocation instanceof JvmStackLocation) || InstructionClassifier.isReturn(simpleInstruction.opcode))
            {
                // non-stack locations aren't affected by simple instructions
                answer.add(memoryLocation);
                return;
            }
            int index = ((JvmStackLocation) memoryLocation).getIndex();
            if (isStackLocationTooDeep((JvmStackLocation) memoryLocation, simpleInstruction, clazz))
            {
                // if the location is too deep in the stack, offset the location by the instruction pop/push difference
                answer.addAll(backtraceStackLocation(memoryLocation, simpleInstruction, clazz));
                return;
            }
            switch (simpleInstruction.opcode)
            {
                case Instruction.OP_DUP:
                    answer.add(new JvmStackLocation(0));
                    break;
                case Instruction.OP_DUP_X1:
                {
                    answer.add(index == 2 ? new JvmStackLocation(0) : memoryLocation);
                    break;
                }
                case Instruction.OP_DUP_X2:
                {
                    answer.add(index == 3 ? new JvmStackLocation(0) : memoryLocation);
                    break;
                }
                case Instruction.OP_DUP2:
                {
                    answer.add(index > 1 ? new JvmStackLocation(index - 2) : memoryLocation);
                    break;
                }
                case Instruction.OP_DUP2_X1:
                {
                    answer.add(index > 2 ? new JvmStackLocation(index - 3) : memoryLocation);
                    break;
                }
                case Instruction.OP_DUP2_X2:
                {
                    answer.add(index > 3 ? new JvmStackLocation(index - 4) : memoryLocation);
                    break;
                }
                case Instruction.OP_SWAP:
                {
                    answer.add(new JvmStackLocation(1 - index));
                    break;
                }
                case Instruction.OP_IALOAD:
                case Instruction.OP_FALOAD:
                case Instruction.OP_AALOAD:
                case Instruction.OP_BALOAD:
                case Instruction.OP_CALOAD:
                case Instruction.OP_SALOAD:
                case Instruction.OP_LALOAD:
                case Instruction.OP_DALOAD:
                {
                    answer.add(new JvmStackLocation(0));
                    answer.add(new JvmStackLocation(1));
                    if (((JvmAbstractState<AbstractStateT>) parentState.getWrappedState().getStateByName(StateNames.Jvm)).getHeap() instanceof JvmTreeHeapFollowerAbstractState)
                    {
                        answer.add(new JvmHeapLocation(((JvmReferenceAbstractState) parentState.getWrappedState().getStateByName(StateNames.Reference)).peek(1),
                                                       "[]"));
                    }
                    break;
                }
                default: // arithmetic and literal instructions
                {
                    answer.addAll(backtraceStackLocation(memoryLocation, simpleInstruction, clazz));
                }
            }
        }

        @Override
        public void visitVariableInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, VariableInstruction variableInstruction)
        {
            if (variableInstruction.opcode == Instruction.OP_IINC)
            {
                // increment does not affect the memory location
                answer.add(memoryLocation);
                return;
            }
            if (variableInstruction.isLoad())
            {
                answer.addAll(backtraceStackLocation(memoryLocation, variableInstruction, clazz));
                if (memoryLocation instanceof JvmStackLocation
                    && !isStackLocationTooDeep((JvmStackLocation) memoryLocation, variableInstruction, clazz))
                {
                    // the loaded stack location maps to the corresponding local variable array cell
                    answer.add(new JvmLocalVariableLocation(variableInstruction.variableIndex + ((JvmStackLocation) memoryLocation).index));
                }
                return;
            }
            if (!(memoryLocation instanceof JvmLocalVariableLocation) || ((JvmLocalVariableLocation) memoryLocation).index != variableInstruction.variableIndex)
            {
                // stores affect the corresponding variable array entry only
                answer.add(memoryLocation);
            }
            answer.add(new JvmStackLocation(0));
            if (variableInstruction.isCategory2())
            {
                answer.add(new JvmStackLocation(1));
            }
        }

        @Override
        public void visitConstantInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, ConstantInstruction constantInstruction)
        {
            switch (constantInstruction.opcode)
            {
                case Instruction.OP_GETSTATIC:
                {
                    constantLookupVisitor.isStatic = true;
                    clazz.constantPoolEntryAccept(constantInstruction.constantIndex, constantLookupVisitor);
                    answer.addAll(backtraceStackLocation(memoryLocation, constantInstruction, clazz));
                    if (memoryLocation instanceof JvmStackLocation
                        && !isStackLocationTooDeep((JvmStackLocation) memoryLocation, constantInstruction, clazz))
                    {
                        answer.add(new JvmStaticFieldLocation(constantLookupVisitor.result));
                    }
                    break;
                }
                case Instruction.OP_PUTSTATIC:
                    constantLookupVisitor.isStatic = true;
                    clazz.constantPoolEntryAccept(constantInstruction.constantIndex, constantLookupVisitor);
                    if (memoryLocation instanceof JvmStackLocation)
                    {
                        answer.add(new JvmStackLocation(((JvmStackLocation) memoryLocation).getIndex() + constantLookupVisitor.resultSize));
                        break;
                    }
                    if (memoryLocation instanceof JvmStaticFieldLocation)
                    {
                        answer.add(new JvmStackLocation(0));
                        if (constantLookupVisitor.resultSize == 2)
                        {
                            answer.add(new JvmStackLocation(1));
                        }
                        break;
                    }
                    answer.add(memoryLocation);
                    break;
                case Instruction.OP_GETFIELD:
                {
                    constantLookupVisitor.isStatic = false;
                    clazz.constantPoolEntryAccept(constantInstruction.constantIndex, constantLookupVisitor);
                    answer.addAll(backtraceStackLocation(memoryLocation, constantInstruction, clazz));
                    if (memoryLocation instanceof JvmStackLocation
                        && !isStackLocationTooDeep((JvmStackLocation) memoryLocation, constantInstruction, clazz)
                        && ((JvmAbstractState<AbstractStateT>) parentState.getWrappedState().getStateByName("Jvm")).getHeap() instanceof JvmTreeHeapFollowerAbstractState)
                    {
                        // if the heap model is nontrivial, backtrace to the heap location
                        answer.add(new JvmHeapLocation(((JvmReferenceAbstractState) parentState.getWrappedState().getStateByName("Reference")).peek(),
                                                       constantLookupVisitor.result));
                    }
                    break;
                }
                case Instruction.OP_PUTFIELD:
                {
                    constantLookupVisitor.isStatic = false;
                    clazz.constantPoolEntryAccept(constantInstruction.constantIndex, constantLookupVisitor);
                    if (memoryLocation instanceof JvmStackLocation)
                    {
                        answer.add(new JvmStackLocation(((JvmStackLocation) memoryLocation).index + constantLookupVisitor.resultSize + 1));
                        break;
                    }
                    if (!(memoryLocation instanceof JvmHeapLocation))
                    {
                        answer.add(memoryLocation);
                        break;
                    }
                    JvmHeapLocation heapLocation = (JvmHeapLocation) memoryLocation;
                    if (!constantLookupVisitor.result.equals(heapLocation.field)
                        || !(((JvmAbstractState<AbstractStateT>) parentState.getWrappedState().getStateByName("Jvm")).getHeap() instanceof JvmTreeHeapFollowerAbstractState))
                    {
                        answer.add(memoryLocation);
                        break;
                    }
                    SetAbstractState<Reference> reference = ((JvmReferenceAbstractState) parentState.getWrappedState().getStateByName("Reference")).peek(constantLookupVisitor.resultSize);
                    SetAbstractState<Reference> referenceIntersection = heapLocation.reference.stream().filter(reference::contains).collect(Collectors.toCollection(SetAbstractState::new));
                    if (reference.size() != 1 || reference.equals(heapLocation.reference) || constantLookupVisitor.result.endsWith("[]"))
                    {
                        answer.add(memoryLocation);
                    }
                    if (referenceIntersection.size() > 0)
                    {
                        answer.add(new JvmStackLocation(0));
                        if (constantLookupVisitor.resultSize > 1)
                        {
                            answer.add(new JvmStackLocation(1));
                        }
                    }
                    break;
                }
                case Instruction.OP_INVOKESTATIC:
                case Instruction.OP_INVOKEDYNAMIC:
                case Instruction.OP_INVOKEVIRTUAL:
                case Instruction.OP_INVOKESPECIAL:
                case Instruction.OP_INVOKEINTERFACE:
                    answer.addAll(processCall(memoryLocation, constantInstruction, clazz));
                    break;
                case Instruction.OP_NEW: // TODO creating objects on the heap is not yet modeled
                case Instruction.OP_NEWARRAY:
                case Instruction.OP_ANEWARRAY:
                case Instruction.OP_MULTIANEWARRAY:
                default:
                    answer.addAll(backtraceStackLocation(memoryLocation, constantInstruction, clazz));
            }
        }

        @Override
        public void visitBranchInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, BranchInstruction branchInstruction)
        {
            answer.addAll(backtraceStackLocation(memoryLocation, branchInstruction, clazz));
        }

        @Override
        public void visitAnySwitchInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, SwitchInstruction switchInstruction)
        {
            answer.addAll(backtraceStackLocation(memoryLocation, switchInstruction, clazz));
        }
    }
}
