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

package proguard.analysis.cpa.jvm.cfa.visitors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import proguard.analysis.cpa.jvm.cfa.JvmCfa;
import proguard.analysis.cpa.jvm.cfa.edges.JvmAssumeCaseCfaEdge;
import proguard.analysis.cpa.jvm.cfa.edges.JvmAssumeCfaEdge;
import proguard.analysis.cpa.jvm.cfa.edges.JvmAssumeDefaultCfaEdge;
import proguard.analysis.cpa.jvm.cfa.edges.JvmAssumeExceptionCfaEdge;
import proguard.analysis.cpa.jvm.cfa.edges.JvmCfaEdge;
import proguard.analysis.cpa.jvm.cfa.edges.JvmInstructionCfaEdge;
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCatchCfaNode;
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode;
import proguard.analysis.cpa.jvm.cfa.nodes.JvmUnknownCfaNode;
import proguard.classfile.Clazz;
import proguard.classfile.Method;
import proguard.classfile.MethodSignature;
import proguard.classfile.attribute.Attribute;
import proguard.classfile.attribute.CodeAttribute;
import proguard.classfile.attribute.ExceptionInfo;
import proguard.classfile.attribute.visitor.AttributeVisitor;
import proguard.classfile.instruction.BranchInstruction;
import proguard.classfile.instruction.ConstantInstruction;
import proguard.classfile.instruction.Instruction;
import proguard.classfile.instruction.LookUpSwitchInstruction;
import proguard.classfile.instruction.SimpleInstruction;
import proguard.classfile.instruction.SwitchInstruction;
import proguard.classfile.instruction.TableSwitchInstruction;
import proguard.classfile.instruction.VariableInstruction;
import proguard.classfile.instruction.visitor.InstructionVisitor;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This {@link AttributeVisitor} visits the {@link CodeAttribute} of a {@link Method} and performs two different tasks:
 *
 * <p>- for each exception handler (i.e. the beginning of a catch or finally block that can handle several {@link ExceptionInfo}) in the attribute exception table creates a {@link JvmCatchCfaNode}.
 * Consecutive nodes may be linked with a false {@link JvmAssumeExceptionCfaEdge} (that indicates that the exception was not caught by a catch statement and is then passed to the following one) if the
 * following catch block covers at least all the instruction offsets of the previous one (this includes also nested try/catch). If the exception is not caught by the last block of a chain an
 * additional a false {@link JvmAssumeExceptionCfaEdge} to the exception exit block is added. This is not necessary for finally blocks (i.e. with exception type = 0) since they always re-throw the
 * original exception once they're done, which will then be added as an edge to the first catch block that handles its offset by {@link JvmIntraproceduralCfaFillerVisitor}.
 *
 * <p>- sets the parameters for the current method for a {@link JvmIntraproceduralCfaFillerVisitor} and then acts similarly to an {@link proguard.classfile.instruction.visitor.AllInstructionVisitor}.
 * The visitor visits all the instructions of the {@link CodeAttribute} and creates the CFA for the current method.
 *
 * @author Carlo Alberto Pozzoli
 */

public class JvmIntraproceduralCfaFillerAllInstructionVisitor
    implements AttributeVisitor
{

    private final        JvmCfa cfa;
    private static final Logger log = LogManager.getLogger(JvmIntraproceduralCfaFillerAllInstructionVisitor.class);

    public JvmIntraproceduralCfaFillerAllInstructionVisitor(JvmCfa cfa)
    {
        this.cfa = cfa;
    }

    @Override
    public void visitAnyAttribute(Clazz clazz, Attribute attribute)
    {
    }

    @Override
    public void visitCodeAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute)
    {
        MethodSignature signature = MethodSignature.computeIfAbsent(clazz, method);

        generateCatchNodes(signature, codeAttribute, clazz);

        // visit all the instructions of the current method
        codeAttribute.instructionsAccept(clazz, method, new JvmIntraproceduralCfaFillerVisitor(signature, cfa));
    }

    private void generateCatchNodes(MethodSignature signature, CodeAttribute codeAttribute, Clazz clazz)
    {
        // create a node for each catch/finally statement
        // if more than one catch block target the same try block each subsequent node is linked to the previous one with an edge that assumes the exception was not caught
        if (codeAttribute.exceptionTable != null)
        {
            // get the position of all handlers, use a LinkedHashSet to remove duplicates and keep order
            Set<Integer> handlersOffset =
                    Arrays.stream(codeAttribute.exceptionTable)
                          .map(x -> x.u2handlerPC)
                          .collect(Collectors.toCollection(LinkedHashSet::new));

            // create the nodes for code locations at beginning of catch/finally statement
            for (int handlerOffset : handlersOffset)
            {
                JvmCatchCfaNode catchNode = new JvmCatchCfaNode(signature,
                                                                handlerOffset,
                                                                Arrays.stream(codeAttribute.exceptionTable)
                                                                      .filter(e -> e.u2handlerPC == handlerOffset)
                                                                      .findFirst()
                                                                      .get().u2catchType,
                                                                clazz);
                cfa.addFunctionCatchNode(signature, catchNode, handlerOffset);

                // link the catch node to the starting node of the handler block with a true exception assumption
                new JvmAssumeExceptionCfaEdge(catchNode,
                                              cfa.addNodeIfAbsent(signature, handlerOffset, clazz),
                                              true,
                                              catchNode.getCatchType());
            }

            for (int currHandlerOffset : handlersOffset)
            {
                Set<ExceptionInfo> handledBlocks = Arrays.stream(codeAttribute.exceptionTable)
                                                         .filter(e -> e.u2handlerPC == currHandlerOffset)
                                                         .collect(Collectors.toSet());

                // there will be only one in most cases (can be more if there is a finally block)
                for (ExceptionInfo currBlock : handledBlocks)
                {
                    int pos = Arrays.asList(codeAttribute.exceptionTable).indexOf(currBlock);

                    // check if there is a following catch handler that handles the same code block
                    Optional<ExceptionInfo> followingCatch = Arrays.stream(codeAttribute.exceptionTable)
                                                                   .skip(pos + 1)
                                                                   .filter(e -> e.u2startPC <= currBlock.u2startPC
                                                                                && e.u2endPC >= currBlock.u2endPC)
                                                                   .findFirst();

                    if (followingCatch.isPresent())
                    {
                        // if there is a following catch block we link it with a false assume exception edge with the previous catch node type
                        // i.e. the following catch block is considered only if the exception is not caught by the block before
                        JvmCatchCfaNode currNode = cfa.getFunctionCatchNode(signature, currHandlerOffset);
                        JvmCatchCfaNode nextNode = cfa.getFunctionCatchNode(signature, followingCatch.get().u2handlerPC);

                        new JvmAssumeExceptionCfaEdge(currNode,
                                                      nextNode,
                                                      false,
                                                      currNode.getCatchType());
                        break;
                    }
                }
            }
        }

        // if the last node of a catch chain (i.e. linked only to the beginning of the corresponding catch block and not to other catch nodes)is not a `finally` (catch type == 0) which always throws
        // an exception in the end, add an edge to the exception exit block, since this is an intra-procedural implementation
        for (JvmCatchCfaNode node : cfa.getFunctionCatchNodes(signature))
        {
            if (node.getLeavingEdges().size() == 1 && node.getCatchType() != 0)
            {
                JvmCfaNode exitNode = cfa.getFunctionExceptionExitNode(signature, clazz);

                // add edge for exception not caught
                new JvmAssumeExceptionCfaEdge(node, exitNode, false, node.getCatchType());
            }
        }
    }

    /**
     * This {@link InstructionVisitor} creates nodes and edges of a {@link JvmCfa} for every instruction it visits. For every instruction the node for its offset is added (if it did not exist before)
     * plus eventual additional nodes (i.e. in case of a branch instruction a node for the code location of the jump is added).
     *
     * <p>The edge from the previously visited instruction is linked to the current instruction if the control flows from one to the other (e.g. the previous edge is linked if it represents an iadd
     * instruction but not if it represents a goto instruction).
     *
     * @author Carlo Alberto Pozzoli
     */
    private static class JvmIntraproceduralCfaFillerVisitor
        implements InstructionVisitor
    {

        private final JvmCfa          cfa;
        private final MethodSignature signature;
        private       JvmCfaEdge      previousEdge;

        public JvmIntraproceduralCfaFillerVisitor(MethodSignature methodSignature, JvmCfa cfa)
        {
            this.signature = methodSignature;
            this.cfa       = cfa;
        }

        @Override
        public void visitAnyInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, Instruction instruction)
        {
        }

        @Override
        public void visitSimpleInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, SimpleInstruction simpleInstruction)
        {
            switch (simpleInstruction.opcode)
            {
                case Instruction.OP_IRETURN:
                case Instruction.OP_LRETURN:
                case Instruction.OP_FRETURN:
                case Instruction.OP_DRETURN:
                case Instruction.OP_ARETURN:
                case Instruction.OP_RETURN:
                    connectReturn(offset, clazz, codeAttribute);
                    break;
                case Instruction.OP_ATHROW:
                    connectThrow(offset, clazz, codeAttribute);
                    break;
                default:
                    connectStatement(offset, clazz, codeAttribute);
            }
        }

        @Override
        public void visitVariableInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, VariableInstruction variableInstruction)
        {
            switch (variableInstruction.opcode)
            {
                case Instruction.OP_RET:
                    connectRet(offset, clazz, codeAttribute);
                    break;
                default:
                    connectStatement(offset, clazz, codeAttribute);
            }
        }

        @Override
        public void visitConstantInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, ConstantInstruction constantInstruction)
        {
            // TODO: maybe method calls should have their unique edge type even for intra-procedural implementation?
            connectStatement(offset, clazz, codeAttribute);
        }

        @Override
        public void visitBranchInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, BranchInstruction branchInstruction)
        {
            switch (branchInstruction.opcode)
            {
                case Instruction.OP_GOTO:
                case Instruction.OP_GOTO_W:
                case Instruction.OP_JSR:
                case Instruction.OP_JSR_W:
                    connectGoto(offset, clazz, codeAttribute, branchInstruction);
                    break;
                default:
                    connectBranch(offset, clazz, codeAttribute, branchInstruction);
            }
        }

        @Override
        public void visitAnySwitchInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, SwitchInstruction tableSwitchInstruction)
        {
            try
            {
                connectSwitch(offset, clazz, codeAttribute, tableSwitchInstruction);
            }
            // should never happen
            catch (IllegalArgumentException e)
            {
                log.error("Unknown switch instruction", e);
            }
        }

        /**
         * Performs the default operations needed for each instruction, must be called by all the other specific addNode methods:
         *
         * <p>- Creates a new node for the current location if it does not exist.
         *
         * <p>- Links the previous edge if the previous instruction indicated that the control flows through the two instructions.
         */
        private JvmCfaNode connect(int offset, Clazz clazz)
        {
            JvmCfaNode currentNode = cfa.addNodeIfAbsent(signature, offset, clazz);

            if (previousEdge != null)
            {
                // connect the previous edge if it exist (i.e. not first node and previous instruction is the antecedent in the control flow)
                previousEdge.setTarget(currentNode);
            }

            return currentNode;
        }

        /**
         * A normal instruction, the control flows to the next one. This also includes function calls since it's an intra-procedural implementation.
         */
        private void connectStatement(int offset, Clazz clazz, CodeAttribute methodCode)
        {
            JvmCfaNode currentNode = connect(offset, clazz);

            // create the new outgoing edge that will be connected to next node when the next instruction is visited
            previousEdge = new JvmInstructionCfaEdge(methodCode, offset);
            previousEdge.setSource(currentNode);
        }

        /**
         * Add an edge from current location to the exit node.
         */
        private void connectReturn(int offset, Clazz clazz, CodeAttribute methodCode)
        {
            JvmCfaNode currentNode = connect(offset, clazz);

            // next instruction will not continue the control flow
            previousEdge = null;

            JvmCfaNode exitNode = cfa.getFunctionReturnExitNode(signature, clazz);

            // create and add edge to nodes
            new JvmInstructionCfaEdge(currentNode, exitNode, methodCode, offset);
        }

        /**
         * we are not able to reconstruct the correct flow of the ret operation without knowing the value of the variable on the stack, the best we can do is adding an edge to an unknown state that
         * might be modified when the information is available.
         */
        private void connectRet(int offset, Clazz clazz, CodeAttribute methodCode)
        {
            JvmCfaNode currentNode = connect(offset, clazz);

            // next instruction will not continue the control flow
            previousEdge = null;

            // add an edge to the unknown node
            new JvmInstructionCfaEdge(currentNode, JvmUnknownCfaNode.INSTANCE, methodCode, offset);
        }

        /**
         * Links the current node with the node at the jump offset.
         */
        private void connectGoto(int offset, Clazz clazz, CodeAttribute methodCode, BranchInstruction instruction)
        {
            JvmCfaNode currentNode = connect(offset, clazz);

            // next instruction will not continue the control flow
            previousEdge = null;

            int branchTarget = offset + instruction.branchOffset;

            // get the goto location node
            JvmCfaNode jumpNode = cfa.addNodeIfAbsent(signature, branchTarget, clazz);

            // create and add edge to nodes
            new JvmInstructionCfaEdge(currentNode, jumpNode, methodCode, offset);
        }

        /**
         * Creates two {@link JvmAssumeCfaEdge}: a true one to the branch target and a false one to the next instruction.
         */
        private void connectBranch(int offset, Clazz clazz, CodeAttribute methodCode, BranchInstruction instruction)
        {
            JvmCfaNode currentNode = connect(offset, clazz);

            // create the branch not taken edge that will be connected to next node when the next instruction is visited
            previousEdge = new JvmAssumeCfaEdge(methodCode, offset, false);
            previousEdge.setSource(currentNode);

            // create the branch taken edge with the corresponding node
            int branchTarget = offset + instruction.branchOffset;

            JvmCfaNode jumpNode = cfa.addNodeIfAbsent(signature, branchTarget, clazz);

            new JvmAssumeCfaEdge(currentNode, jumpNode, methodCode, offset, true);
        }

        /**
         * Creates a {@link JvmAssumeCaseCfaEdge} for each case of a switch statement and a {@link JvmAssumeDefaultCfaEdge} to the default location node.
         */
        private void connectSwitch(int offset, Clazz clazz, CodeAttribute methodCode, SwitchInstruction instruction) throws IllegalArgumentException
        {
            // check just because the subsequent check on type, should never happen
            if (!(instruction instanceof TableSwitchInstruction || instruction instanceof LookUpSwitchInstruction))
            {
                throw new IllegalArgumentException("Unexpected switch instruction type");
            }

            JvmCfaNode currentNode = connect(offset, clazz);

            // we already link the following node since its location belongs to a case/default (there is always a default in the table)
            previousEdge = null;

            // add the default node/edge pair
            int branchTarget = offset + instruction.defaultOffset;

            JvmCfaNode jumpNode = cfa.addNodeIfAbsent(signature, branchTarget, clazz);

            new JvmAssumeDefaultCfaEdge(currentNode, jumpNode, methodCode, offset);

            // create a node/edge pair for each case in the table
            for (int index = 0; index < instruction.jumpOffsets.length; index++)
            {
                branchTarget = offset + instruction.jumpOffsets[index];
                int caseAssumption = instruction instanceof TableSwitchInstruction ? ((TableSwitchInstruction) instruction).lowCase + index : ((LookUpSwitchInstruction) instruction).cases[index];

                jumpNode = cfa.addNodeIfAbsent(signature, branchTarget, clazz);

                new JvmAssumeCaseCfaEdge(currentNode, jumpNode, methodCode, offset, caseAssumption);
            }
        }

        /**
         * Add a node to the catch node at the handler location of the first {@link ExceptionInfo} in the {@link CodeAttribute} exception table that is applicable to the current instruction's offset.
         */
        private void connectThrow(int offset, Clazz clazz, CodeAttribute methodCode)
        {
            JvmCfaNode currentNode = connect(offset, clazz);

            // next instruction will not continue the control flow
            previousEdge = null;

            // add an edge to the first catch block node that may handle the exception
            // if there is none add an edge to the exit node
            Optional<ExceptionInfo> firstCatch = Optional.empty();

            if (methodCode.exceptionTable != null)
            {
                // find the first catch handler of the inner try block
                // i.e. the first catch handler that is applicable to current offset since the jvm just looks up the exception table in order
                // traversing the assume exception edges you can reach the correct catch block once the exception type is known
                firstCatch = Arrays.stream(methodCode.exceptionTable)
                                   .filter(e -> e.isApplicable(offset))
                                   .findFirst();
            }

            // link edge to the first catch block
            if (firstCatch.isPresent())
            {
                new JvmInstructionCfaEdge(currentNode, cfa.getFunctionCatchNode(signature, firstCatch.get().u2handlerPC), methodCode, offset);
            }
            // since this is an intra-procedural implementation if the thrown exception is not caught just add an edge to the exception exit node
            else
            {
                JvmCfaNode exitNode = cfa.getFunctionExceptionExitNode(signature, clazz);

                // add edge for the throw instruction
                new JvmInstructionCfaEdge(currentNode, exitNode, methodCode, offset);
            }
        }
    }
}
