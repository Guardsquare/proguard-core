/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2021 Guardsquare NV
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

package proguard.analysis;

import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import proguard.classfile.Clazz;
import proguard.classfile.Method;
import proguard.classfile.attribute.*;
import proguard.classfile.attribute.visitor.AttributeVisitor;
import proguard.classfile.instruction.BranchInstruction;
import proguard.classfile.instruction.Instruction;
import proguard.classfile.instruction.InstructionFactory;
import proguard.classfile.instruction.SwitchInstruction;

/**
 * Calculate the dominator tree of any method, making
 * it possible to determine which instructions are
 * guaranteed to be executed before others.
 *
 * <p>
 * This is useful for applications like the {@link CallResolver}
 * that would like to know whether an instruction,
 * e.g. a method call, is always guaranteed to be executed
 * assuming the containing method is invoked, or if
 * its execution requires specific branches in the
 * method to be taken.
 * </p>
 *
 * <p>
 * In principle, dominator analysis is based on a simple equation:
 * <ol>
 *     <li>The entry node dominates only itself</li>
 *     <li>
 *         Any other node's dominator set is calculated by
 *         forming the intersection of the dominator sets of
 *         all its control flow predecessors. Afterwards, the node
 *         itself is also added to its dominator set.
 *     </li>
 * </ol>
 * Like this, the dominator information is propagated through the
 * control flow graph one by one, potentially requiring several
 * iterations until the solution is stable, as the dominator sets
 * of some predecessors might still be uninitialized when used.
 * </p>
 *
 * <p>
 * The implementation here is based on an algorithm
 * that solves the underlying dataflow equation using optimized
 * {@link BitSet} objects instead of normal sets.
 * </p>
 *
 * @author Samuel Hopstock
 */
public class DominatorCalculator
implements   AttributeVisitor
{

    /**
     * Virtual instruction offset modelling the method
     * exit, i.e. all return instructions.
     */
    public static final int EXIT_NODE_OFFSET  = -1;
    /**
     * Virtual instruction offset modelling the method
     * entry. This is needed such that the method entry
     * is guaranteed to have no incoming control flow edges,
     * as this would prevent the algorithm from converging
     * properly without complicated alternative measures.
     */
    public static final int ENTRY_NODE_OFFSET = -2;

    private static final List<Byte> UNCONDITIONAL_BRANCHES = Arrays.asList(Instruction.OP_GOTO,
                                                                           Instruction.OP_GOTO_W,
                                                                           Instruction.OP_JSR,
                                                                           Instruction.OP_JSR_W);
    private static final List<Byte> RETURN_INSTRUCTIONS    = Arrays.asList(Instruction.OP_RETURN,
                                                                           Instruction.OP_IRETURN,
                                                                           Instruction.OP_LRETURN,
                                                                           Instruction.OP_FRETURN,
                                                                           Instruction.OP_DRETURN,
                                                                           Instruction.OP_ARETURN);

    /**
     * Maps an offset to its dominator set, represented by
     * a {@link BitSet}: This data structure contains at most one
     * bit per offset in the method, which is set to 1 if the offset
     * dominates the current instruction or 0 otherwise.
     */
    private final Map<Integer, BitSet> dominatorMap = new HashMap<>();
    private       int                  bitSetSize   = 0;

    private final boolean ignoreExceptions;

    /**
     * Creates a new DominatorCalculator. The default behavior is to ignore exceptions.
     */
    public DominatorCalculator()
    {
        this(true);
    }

    /**
     * Creates a new DominatorCalculator.
     * @param ignoreExceptions If false, exceptions will be taken into account in the analysis.
     */
    public DominatorCalculator(boolean ignoreExceptions)
    {
        this.ignoreExceptions = ignoreExceptions;
    }

    /**
     * Check if one instruction dominates another one.
     * If this is the case, the dominating instruction
     * is guaranteed to be executed before the inferior
     * instruction. Should you wish to check whether an
     * instruction is guaranteed to be executed once the
     * containing method is invoked, you can use the
     * virtual inferior {@link #EXIT_NODE_OFFSET} as a
     * collection for all return instructions.
     *
     * @param dominator The potentially dominating instruction's
     *                  offset
     * @param inferior  The potentially dominated instruction's
     *                  offset
     * @return true if the potential dominator is indeed
     *     guaranteed to be executed before the inferior
     */
    public boolean dominates(int dominator, int inferior)
    {
        BitSet dominators = dominatorMap.get(inferior);
        if (dominators == null)
        {
            throw new IllegalStateException("No dominator information known for offset " + inferior);
        }
        return dominators.get(offsetToIndex(dominator));
    }

    /**
     * As we introduced {@link #ENTRY_NODE_OFFSET} and
     * {@link #EXIT_NODE_OFFSET} as virtual instruction
     * offsets, the rest of the instructions are shifted
     * by those two places. In order to transparently handle
     * this, this method converts actual method offsets
     * to their internal index counterparts.
     */
    private int offsetToIndex(int offset)
    {
        return offset + 2;
    }

    // Implementations for AttributeVisitor

    @Override
    public void visitAnyAttribute(Clazz clazz, Attribute attribute)
    {
        // Not interested in other attributes
    }

    @Override
    public void visitCodeAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute)
    {
        dominatorMap.clear();
        bitSetSize = offsetToIndex(codeAttribute.u4codeLength);

        // Entry node only dominates itself,
        // all other nodes are initialized to have all nodes dominate them
        // (this is necessary due to the fact that the propagation uses
        // intersection, requiring the full universe for uninitialized nodes)
        BitSet entryDominators = new BitSet(bitSetSize);
        entryDominators.set(offsetToIndex(ENTRY_NODE_OFFSET));
        dominatorMap.put(ENTRY_NODE_OFFSET, entryDominators);
        dominatorMap.put(EXIT_NODE_OFFSET, initBitSet());

        propagateToSuccessor(ENTRY_NODE_OFFSET, 0);
        for (int handler : findExceptionHandlers(codeAttribute, 0))
        {
            propagateToSuccessor(ENTRY_NODE_OFFSET, handler);
        }
        run(codeAttribute);
    }

    /**
     * Iterate through the whole method code one time, in the
     * order of a breadth-first search algorithm over the control
     * flow graph. I.e. handle direct child nodes before grand
     * children.
     */
    private void run(CodeAttribute codeAttribute)
    {
        if (codeAttribute.u4codeLength == 0)
        {
            // Skip the method if it doesn't contain any code (e.g. if it was skipped during Dex2Pro conversion).
            return;
        }
        LinkedHashSet<Integer> workList = new LinkedHashSet<>();
        workList.add(0);
        workList.addAll(findExceptionHandlers(codeAttribute, 0));

        while (!workList.isEmpty())
        {
            int offset = workList.stream()
                                 .skip(workList.size() - 1)
                                 .findFirst()
                                 .orElseThrow(() -> new IllegalStateException("Can't get last element in non-empty work list"));
            workList.remove(offset);

            Instruction instruction       = InstructionFactory.create(codeAttribute.code, offset);
            int         instructionLength = instruction.length(offset);
            int         nextOffset        = offset + instructionLength;
            boolean     nextOffsetExists  = nextOffset < codeAttribute.u4codeLength;

            Set<Integer> successors = new HashSet<>();
            if (instruction instanceof BranchInstruction)
            {
                BranchInstruction branch = (BranchInstruction) instruction;
                successors.add(offset + branch.branchOffset);
                if (nextOffsetExists && !UNCONDITIONAL_BRANCHES.contains(branch.opcode))
                {
                    successors.add(nextOffset);
                }
            }
            else if (instruction instanceof SwitchInstruction)
            {
                SwitchInstruction switchInstruction = (SwitchInstruction) instruction;
                successors.add(offset + switchInstruction.defaultOffset);
                for (int jumpOffset : switchInstruction.jumpOffsets)
                {
                    successors.add(offset + jumpOffset);
                }
            }
            else if (RETURN_INSTRUCTIONS.contains(instruction.opcode))
            {
                propagateToSuccessor(offset, EXIT_NODE_OFFSET);
            }
            else if (nextOffsetExists)
            {
                successors.add(nextOffset);
            }

            Set<Integer> exceptionSuccessors = new HashSet<>();
            for (int successor : successors)
            {
                exceptionSuccessors.addAll(findExceptionHandlers(codeAttribute, successor));
            }
            successors.addAll(exceptionSuccessors);

            for (int successor : successors)
            {
                if (propagateToSuccessor(offset, successor))
                {
                    workList.add(successor);
                }
            }
        }
    }

    /**
     * Create a {@link BitSet} where all bits representing
     * offsets in the method are set to true.
     */
    private BitSet initBitSet()
    {
        BitSet result = new BitSet(bitSetSize);
        result.set(0, bitSetSize);
        return result;
    }

    /**
     * Propagate dominator information to a successor of the
     * current instruction.
     *
     * @return true if this propagation step changed the
     *     successor's dominator information
     */
    private boolean propagateToSuccessor(int curr, int successor)
    {
        BitSet currDominators      = dominatorMap.computeIfAbsent(curr, o -> initBitSet());
        BitSet successorDominators = dominatorMap.computeIfAbsent(successor, o -> initBitSet());
        BitSet beforePropagation   = (BitSet) successorDominators.clone();

        successorDominators.and(currDominators);
        successorDominators.set(offsetToIndex(successor));

        return !beforePropagation.equals(successorDominators);
    }

    /**
     * Find the start of all exception handlers in a code attribute that are
     * associated with a try block covering the given offset.
     * @param codeAttribute
     * @param offset
     * @return
     */
    private Set<Integer> findExceptionHandlers(CodeAttribute codeAttribute, int offset)
    {
        Set<Integer> handlers = new HashSet<>();
        if (ignoreExceptions)
        {
            return handlers;
        }
        for (ExceptionInfo exceptionInfo : codeAttribute.exceptionTable)
        {
            if (exceptionInfo.isApplicable(offset))
            {
                handlers.add(exceptionInfo.u2handlerPC);
            }
        }
        return handlers;
    }
}
