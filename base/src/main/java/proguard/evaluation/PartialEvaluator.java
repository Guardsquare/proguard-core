/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2020 Guardsquare NV
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
package proguard.evaluation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import proguard.classfile.Clazz;
import proguard.classfile.Method;
import proguard.classfile.attribute.Attribute;
import proguard.classfile.attribute.CodeAttribute;
import proguard.classfile.attribute.ExceptionInfo;
import proguard.classfile.attribute.visitor.AttributeVisitor;
import proguard.classfile.attribute.visitor.ExceptionInfoVisitor;
import proguard.classfile.editor.ClassEstimates;
import proguard.classfile.instruction.BranchInstruction;
import proguard.classfile.instruction.ConstantInstruction;
import proguard.classfile.instruction.Instruction;
import proguard.classfile.instruction.InstructionFactory;
import proguard.classfile.instruction.LookUpSwitchInstruction;
import proguard.classfile.instruction.SimpleInstruction;
import proguard.classfile.instruction.TableSwitchInstruction;
import proguard.classfile.instruction.VariableInstruction;
import proguard.classfile.instruction.visitor.InstructionVisitor;
import proguard.classfile.util.BranchTargetFinder;
import proguard.classfile.visitor.ExceptionHandlerFilter;
import proguard.evaluation.exception.EmptyCodeAttributeException;
import proguard.evaluation.exception.ExcessiveComplexityException;
import proguard.exception.InstructionExceptionFormatter;
import proguard.evaluation.exception.StackGeneralizationException;
import proguard.evaluation.exception.VariablesGeneralizationException;
import proguard.evaluation.util.DebugPrinter;
import proguard.evaluation.util.PartialEvaluatorStateTracker;
import proguard.evaluation.value.BasicValueFactory;
import proguard.evaluation.value.InstructionOffsetValue;
import proguard.evaluation.value.Value;
import proguard.evaluation.value.ValueFactory;
import proguard.exception.ProguardCoreException;
import proguard.util.CircularIntBuffer;

import java.util.Arrays;

/**
 * This {@link AttributeVisitor} performs partial evaluation on the code attributes
 * that it visits.
 *
 * @author Eric Lafortune
 */
public class PartialEvaluator
implements   AttributeVisitor,
             ExceptionInfoVisitor
{
    private static final boolean DEBUG        = false;
    private static final boolean DEBUG_RESULTS = false;

    /**
     * Enables new exceptions to be thrown during evaluation.
     * These are exceptions that are not thrown by the original ProGuard code.
     * This is a temporary flag to allow the new exceptions to be tested.
     * TODO: Remove this flag when the new exceptions are stable and will not break any dependent code.
     */
    public static boolean ENABLE_NEW_EXCEPTIONS = System.getProperty("proguard.pe.newexceptions") != null;

    private final static Logger logger = LogManager.getLogger(PartialEvaluator.class);

    // The analysis will generalize stack/vars after visiting an instruction this many times.
    private static final int GENERALIZE_AFTER_N_EVALUATIONS = 5;
    // If the analysis visits an instruction this many times (this can happen e.g. for big switches),
    // the analysis of this method is forcibly stopped and a ExcessiveComplexityException is thrown.
    // By default (value set to -1), the analysis is not forcibly stopped.
    private int              stopAnalysisAfterNEvaluations  = -1;

    public static final int NONE            = -2;
    public static final int AT_METHOD_ENTRY = -1;
    public static final int AT_CATCH_ENTRY  = -1;

    private final ValueFactory       valueFactory;
    private final InvocationUnit     invocationUnit;
    private final boolean            evaluateAllCode;
    private final int                prettyInstructionBuffered;
    private final InstructionVisitor extraInstructionVisitor;

    private InstructionOffsetValue[]     branchOriginValues  = new InstructionOffsetValue[ClassEstimates.TYPICAL_CODE_LENGTH];
    private InstructionOffsetValue[]     branchTargetValues  = new InstructionOffsetValue[ClassEstimates.TYPICAL_CODE_LENGTH];
    private TracedVariables[]            variablesBefore     = new TracedVariables[ClassEstimates.TYPICAL_CODE_LENGTH];
    private TracedStack[]                stacksBefore        = new TracedStack[ClassEstimates.TYPICAL_CODE_LENGTH];
    private TracedVariables[]            variablesAfter      = new TracedVariables[ClassEstimates.TYPICAL_CODE_LENGTH];
    private TracedStack[]                stacksAfter         = new TracedStack[ClassEstimates.TYPICAL_CODE_LENGTH];
    private boolean[]                    generalizedContexts = new boolean[ClassEstimates.TYPICAL_CODE_LENGTH];
    private int[]                        evaluationCounts    = new int[ClassEstimates.TYPICAL_CODE_LENGTH];
    private boolean                      evaluateExceptions;
    private int                          codeLength;
    private PartialEvaluatorStateTracker stateTracker;

    private final BasicBranchUnit    branchUnit;
    private final BranchTargetFinder branchTargetFinder;

    private final java.util.Stack<InstructionBlock> callingInstructionBlockStack;
    private final java.util.Stack<InstructionBlock> instructionBlockStack = new java.util.Stack<>();


    /**
     * Creates a simple PartialEvaluator.
     */
    public PartialEvaluator()
    {
        this(new BasicValueFactory());
    }


    /**
     * Creates a new PartialEvaluator.
     * @param valueFactory    the value factory that will create all values
     *                        during evaluation.
     */
    public PartialEvaluator(ValueFactory valueFactory)
    {
        this(valueFactory,
             new BasicInvocationUnit(valueFactory),
             true);
    }


    /**
     * Creates a new PartialEvaluator.
     * @param valueFactory    the value factory that will create all values
     *                        during the evaluation.
     * @param invocationUnit  the invocation unit that will handle all
     *                        communication with other fields and methods.
     * @param evaluateAllCode a flag that specifies whether all casts, branch
     *                        targets, and exception handlers should be
     *                        evaluated, even if they are unnecessary or
     *                        unreachable.
     */
    public PartialEvaluator(ValueFactory   valueFactory,
                            InvocationUnit invocationUnit,
                            boolean        evaluateAllCode)
    {
        this(valueFactory,
             invocationUnit,
             evaluateAllCode,
             null);
    }


    /**
     * Creates a new PartialEvaluator.
     * @param valueFactory            the value factory that will create all
     *                                values during the evaluation.
     * @param invocationUnit          the invocation unit that will handle all
     *                                communication with other fields and
     *                                methods.
     * @param evaluateAllCode         a flag that specifies whether all branch
     *                                targets and exception handlers should be
     *                                evaluated, even if they are unreachable.
     * @param extraInstructionVisitor an optional extra visitor for all
     *                                instructions right before they are
     *                                executed.
     */
    public PartialEvaluator(ValueFactory       valueFactory,
                            InvocationUnit     invocationUnit,
                            boolean            evaluateAllCode,
                            InstructionVisitor extraInstructionVisitor)
    {
        this(valueFactory,
             invocationUnit,
             evaluateAllCode,
             extraInstructionVisitor,
             evaluateAllCode ?
                 new BasicBranchUnit() :
                 new TracedBranchUnit(),
             new BranchTargetFinder(),
             null);
    }

    /**
     * Creates a new PartialEvaluator.
     * @param valueFactory                 the value factory that will create
     *                                     all values during evaluation.
     * @param invocationUnit               the invocation unit that will handle
     *                                     all communication with other fields
     *                                     and methods.
     * @param evaluateAllCode              a flag that specifies whether all
     *                                     casts, branch targets, and exception
     *                                     handlers should be evaluated, even
     *                                     if they are unnecessary or
     *                                     unreachable.
     * @param branchUnit                   the branch unit that will handle all
     *                                     branches.
     * @param branchTargetFinder           the utility class that will find all
     *                                     branches.
     * @param callingInstructionBlockStack the stack of instruction blocks to
     *                                     be evaluated
     */
    private PartialEvaluator(ValueFactory                      valueFactory,
                             InvocationUnit                    invocationUnit,
                             boolean                           evaluateAllCode,
                             InstructionVisitor                extraInstructionVisitor,
                             BasicBranchUnit                   branchUnit,
                             BranchTargetFinder                branchTargetFinder,
                             java.util.Stack<InstructionBlock> callingInstructionBlockStack)
    {
        this.valueFactory                 = valueFactory;
        this.invocationUnit               = invocationUnit;
        this.evaluateAllCode              = evaluateAllCode;
        // To always support pretty printing, set this default value larger than 0.
        this.prettyInstructionBuffered    = 0;
        this.extraInstructionVisitor      = extraInstructionVisitor;
        this.branchUnit                   = branchUnit;
        this.branchTargetFinder           = branchTargetFinder;
        this.callingInstructionBlockStack = callingInstructionBlockStack == null ?
            this.instructionBlockStack :
            callingInstructionBlockStack;
        if (DEBUG || DEBUG_RESULTS)
        {
            this.stateTracker = new DebugPrinter(DEBUG, DEBUG_RESULTS);
        }
    }

    /**
     * Builds this PartialEvaluator using the (partly) filled Builder.
     */
    private PartialEvaluator(Builder builder)
    {
        this.valueFactory                 = builder.valueFactory == null ? new BasicValueFactory(): builder.valueFactory;
        this.invocationUnit               = builder.invocationUnit == null ? new BasicInvocationUnit(valueFactory) : builder.invocationUnit;
        this.evaluateAllCode              = builder.evaluateAllCode;
        this.prettyInstructionBuffered    = builder.prettyInstructionBuffered;
        this.extraInstructionVisitor      = builder.extraInstructionVisitor;
        this.branchUnit                   = builder.branchUnit == null ? ( evaluateAllCode ?
                                                                           new BasicBranchUnit() :
                                                                           new TracedBranchUnit())
                                                                        : builder.branchUnit;
        this.branchTargetFinder           = builder.branchTargetFinder == null ? new BranchTargetFinder() : builder.branchTargetFinder;
        this.callingInstructionBlockStack = builder.callingInstructionBlockStack == null ? this.instructionBlockStack : builder.callingInstructionBlockStack;
        this.stopAnalysisAfterNEvaluations = builder.stopAnalysisAfterNEvaluations;
        if (builder.stateTracker == null && (DEBUG || DEBUG_RESULTS))
        {
            this.stateTracker = new DebugPrinter(DEBUG, DEBUG_RESULTS);
        }
        else
        {
            this.stateTracker = builder.stateTracker;
        }
    }

    public static class Builder {
        private ValueFactory                      valueFactory;
        private InvocationUnit                    invocationUnit;
        private boolean                           evaluateAllCode               = true;
        // To always support pretty printing, set this default value larger than 0.
        private int                               prettyInstructionBuffered     = 0;
        private InstructionVisitor                extraInstructionVisitor;
        private BasicBranchUnit                   branchUnit;
        private BranchTargetFinder                branchTargetFinder;
        private java.util.Stack<InstructionBlock> callingInstructionBlockStack;
        private int                               stopAnalysisAfterNEvaluations = -1; // disabled by default
        private PartialEvaluatorStateTracker      stateTracker;

        public static Builder create()
        {
            return new Builder();
        }
        private Builder() {}

        public PartialEvaluator build()
        {
            return new PartialEvaluator(this);
        }

        public Builder setStateTracker(PartialEvaluatorStateTracker stateTracker)
        {
            this.stateTracker = stateTracker;
            return this;
        }

        /**
         * the value factory that will create all values during evaluation.
         */
        public Builder setValueFactory(ValueFactory valueFactory)
        {
            this.valueFactory = valueFactory;
            return this;
        }

        /**
         * The invocation unit that will handle all communication with other fields and methods.
         */
        public Builder setInvocationUnit(InvocationUnit invocationUnit)
        {
            this.invocationUnit = invocationUnit;
            return this;
        }

        /**
         * Specifies whether all casts, branch targets, and exceptionhandlers should be evaluated,
         * even if they are unnecessary or unreachable.
         */
        public Builder setEvaluateAllCode(boolean evaluateAllCode)
        {
            this.evaluateAllCode = evaluateAllCode;
            return this;
        }

        /**
         * Specifies how many instructions should be considered in the context of a pretty message.
         * When <= 0, no pretty printing is applied.
         */
        public Builder setPrettyPrinting(int prettyInstructionBuffered)
        {
            this.prettyInstructionBuffered = prettyInstructionBuffered;
            return this;
        }

        /**
         * Enable pretty printing with a default buffer size of 7.
         */
        public Builder setPrettyPrinting()
        {
            return this.setPrettyPrinting(7);
        }

        /**
         *  an optional extra visitor for all instructions right before they are executed.
         */
        public Builder setExtraInstructionVisitor(InstructionVisitor extraInstructionVisitor)
        {
            this.extraInstructionVisitor = extraInstructionVisitor;
            return this;
        }

        /**
         * The branch unit that will handle all branches.
         */
        public Builder setBranchUnit(BasicBranchUnit branchUnit)
        {
            this.branchUnit = branchUnit;
            return this;
        }

        /**
         *  The utility class that will find all branches.
         */
        public Builder setBranchTargetFinder(BranchTargetFinder branchTargetFinder)
        {
            this.branchTargetFinder = branchTargetFinder;
            return this;
        }

        /**
         * the stack of instruction blocks to be evaluated.
         */
        public Builder setCallingInstructionBlockStack(java.util.Stack<InstructionBlock> callingInstructionBlockStack)
        {
            this.callingInstructionBlockStack = callingInstructionBlockStack;
            return this;
        }

        /**
         * The analysis of one method will forcibly stop (throwing a ExcessiveComplexityException)
         * after this many evaluations of a single instruction.
         */
        public Builder stopAnalysisAfterNEvaluations(int stopAnalysisAfterNEvaluations)
        {
            this.stopAnalysisAfterNEvaluations = stopAnalysisAfterNEvaluations;
            return this;
        }
    }


    // Implementations for AttributeVisitor.

    public void visitAnyAttribute(Clazz clazz, Attribute attribute) {}


    public void visitCodeAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute)
    {
//        DEBUG = DEBUG_RESULTS =
//            clazz.getName().equals("abc/Def") &&
//            method.getName(clazz).equals("abc");

        // TODO: Remove this when the partial evaluator has stabilized.
        // Catch any unexpected exceptions from the actual visiting method.
        try
        {
            // Process the code.
            visitCodeAttribute0(clazz, method, codeAttribute);
        }
        catch (RuntimeException ex)
        {
            logger.error("Unexpected error while performing partial evaluation:");
            logger.error("  Class       = [{}]", clazz.getName());
            logger.error("  Method      = [{}{}]", method.getName(clazz), method.getDescriptor(clazz));
            logger.error("  Exception   = [{}] ({})", ex.getClass().getName(), ex.getMessage());

            if (stateTracker != null) stateTracker.registerException(clazz, method, codeAttribute, this, ex);

            throw ex;
        }
    }


    public void visitCodeAttribute0(Clazz clazz, Method method, CodeAttribute codeAttribute)
    {
        // Evaluate the instructions, starting at the entry point.

        // Empty code attribute, do not analyze
        if (codeAttribute.code.length == 0)
        {
            throw new EmptyCodeAttributeException("Empty code attribute found during partial evaluation");
        }
        // Reuse the existing variables and stack objects, ensuring the right size.
        TracedVariables variables = new TracedVariables(codeAttribute.u2maxLocals);
        TracedStack     stack     = new TracedStack(codeAttribute.u2maxStack);

        // Initialize the reusable arrays and variables.
        initializeArrays(codeAttribute);
        initializeParameters(clazz, method, codeAttribute, variables);

        if (stateTracker != null) stateTracker.startCodeAttribute(clazz, method, codeAttribute, variables);

        // Reset stacks.
        instructionBlockStack.clear();
        callingInstructionBlockStack.clear();

        // Find all instruction offsets,...
        codeAttribute.accept(clazz, method, branchTargetFinder);

        // Start executing the first instruction block.
        evaluateInstructionBlockAndExceptionHandlers(clazz,
                                                     method,
                                                     codeAttribute,
                                                     variables,
                                                     stack,
                                                     0,
                                                     codeAttribute.u4codeLength);

        if (stateTracker != null) stateTracker.evaluationResults(clazz, method, codeAttribute, this);
    }


    /**
     * Returns whether a block of instructions is ever used.
     */
    public boolean isTraced(int startOffset, int endOffset)
    {
        for (int index = startOffset; index < endOffset; index++)
        {
            if (isTraced(index))
            {
                return true;
            }
        }

        return false;
    }


    /**
     * Returns whether the instruction at the given offset has ever been
     * executed during the partial evaluation.
     */
    public boolean isTraced(int instructionOffset)
    {
        return evaluationCounts[instructionOffset] > 0;
    }


    /**
     * Returns whether there is an instruction at the given offset.
     */
    public boolean isInstruction(int instructionOffset)
    {
        return branchTargetFinder.isInstruction(instructionOffset);
    }


    /**
     * Returns whether the instruction at the given offset is the target of
     * any kind.
     */
    public boolean isTarget(int instructionOffset)
    {
        return branchTargetFinder.isTarget(instructionOffset);
    }


    /**
     * Returns whether the instruction at the given offset is the origin of a
     * branch instruction.
     */
    public boolean isBranchOrigin(int instructionOffset)
    {
        return branchTargetFinder.isBranchOrigin(instructionOffset);
    }


    /**
     * Returns whether the instruction at the given offset is the target of a
     * branch instruction.
     */
    public boolean isBranchTarget(int instructionOffset)
    {
        return branchTargetFinder.isBranchTarget(instructionOffset);
    }


    /**
     * Returns whether the instruction at the given offset is the target of a
     * branch instruction or an exception.
     */
    public boolean isBranchOrExceptionTarget(int instructionOffset)
    {
        return branchTargetFinder.isBranchTarget(instructionOffset) ||
               branchTargetFinder.isExceptionHandler(instructionOffset);
    }


    /**
     * Returns whether the instruction at the given offset is the start of
     * an exception handler.
     */
    public boolean isExceptionHandler(int instructionOffset)
    {
        return branchTargetFinder.isExceptionHandler(instructionOffset);
    }


    /**
     * Returns whether the instruction at the given offset is the start of a
     * subroutine.
     */
    public boolean isSubroutineStart(int instructionOffset)
    {
        return branchTargetFinder.isSubroutineStart(instructionOffset);
    }


    /**
     * Returns whether the instruction at the given offset is a subroutine
     * invocation.
     */
    public boolean isSubroutineInvocation(int instructionOffset)
    {
        return branchTargetFinder.isSubroutineInvocation(instructionOffset);
    }


    /**
     * Returns whether the instruction at the given offset is part of a
     * subroutine.
     */
    public boolean isSubroutine(int instructionOffset)
    {
        return branchTargetFinder.isSubroutine(instructionOffset);
    }


    /**
     * Returns whether the subroutine at the given offset is ever returning
     * by means of a regular 'ret' instruction.
     */
    public boolean isSubroutineReturning(int instructionOffset)
    {
        return branchTargetFinder.isSubroutineReturning(instructionOffset);
    }


    /**
     * Returns the offset after the subroutine that starts at the given
     * offset.
     */
    public int subroutineEnd(int instructionOffset)
    {
        return branchTargetFinder.subroutineEnd(instructionOffset);
    }


//    /**
//     * Returns the instruction offset at which the object instance that is
//     * created at the given 'new' instruction offset is initialized, or
//     * <code>NONE</code> if it is not being created.
//     */
//    public int initializationOffset(int instructionOffset)
//    {
//        return branchTargetFinder.initializationOffset(instructionOffset);
//    }


//    /**
//     * Returns whether the method is an instance initializer.
//     */
//    public boolean isInitializer()
//    {
//        return branchTargetFinder.isInitializer();
//    }


//    /**
//     * Returns the instruction offset at which this initializer is calling
//     * the "super" or "this" initializer method, or <code>NONE</code> if it is
//     * not an initializer.
//     */
//    public int superInitializationOffset()
//    {
//        return branchTargetFinder.superInitializationOffset();
//    }


    /**
     * Returns whether the instruction at the given offset creates a new,
     * uninitialized instance.
     */
    public boolean isCreation(int offset)
    {
        return branchTargetFinder.isCreation(offset);
    }


    /**
     * Returns whether the instruction at the given offset is the special
     * invocation of an instance initializer.
     */
    public boolean isInitializer(int offset)
    {
        return branchTargetFinder.isInitializer(offset);
    }


    /**
     * Returns the variables before execution of the instruction at the given
     * offset.
     */
    public TracedVariables getVariablesBefore(int instructionOffset)
    {
        return variablesBefore[instructionOffset];
    }


    /**
     * Returns the variables after execution of the instruction at the given
     * offset.
     */
    public TracedVariables getVariablesAfter(int instructionOffset)
    {
        return variablesAfter[instructionOffset];
    }


    /**
     * Returns the stack before execution of the instruction at the given
     * offset.
     */
    public TracedStack getStackBefore(int instructionOffset)
    {
        return stacksBefore[instructionOffset];
    }


    /**
     * Returns the stack after execution of the instruction at the given
     * offset.
     */
    public TracedStack getStackAfter(int instructionOffset)
    {
        return stacksAfter[instructionOffset];
    }


    /**
     * Returns the instruction offsets that branch to the given instruction
     * offset.
     */
    public InstructionOffsetValue branchOrigins(int instructionOffset)
    {
        return branchOriginValues[instructionOffset];
    }


    /**
     * Returns the instruction offsets to which the given instruction offset
     * branches.
     */
    public InstructionOffsetValue branchTargets(int instructionOffset)
    {
        return branchTargetValues[instructionOffset];
    }


    /**
     * Returns a filtering version of the given instruction visitor that only
     * visits traced instructions.
     */
    public InstructionVisitor tracedInstructionFilter(InstructionVisitor instructionVisitor)
    {
        return tracedInstructionFilter(true, instructionVisitor);
    }


    /**
     * Returns a filtering version of the given instruction visitor that only
     * visits traced or untraced instructions.
     */
    public InstructionVisitor tracedInstructionFilter(boolean            traced,
                                                      InstructionVisitor instructionVisitor)
    {
        return new MyTracedInstructionFilter(traced, instructionVisitor);
    }


    // Utility methods to evaluate instruction blocks.

    /**
     * Pushes block of instructions to be executed in the calling partial
     * evaluator.
     */
    private void pushCallingInstructionBlock(TracedVariables variables,
                                             TracedStack     stack,
                                             int             startOffset)
    {
        callingInstructionBlockStack.push(new InstructionBlock(variables,
                                                                 stack,
                                                                 startOffset));
    }


    /**
     * Pushes block of instructions to be executed in this partial evaluator.
     */
    private void pushInstructionBlock(TracedVariables variables,
                                      TracedStack     stack,
                                      int             startOffset)
    {
        instructionBlockStack.push(new InstructionBlock(variables,
                                                          stack,
                                                          startOffset));
    }


    /**
     * Evaluates the instruction block and the exception handlers covering the
     * given instruction range in the given code.
     */
    private void evaluateInstructionBlockAndExceptionHandlers(Clazz           clazz,
                                                              Method          method,
                                                              CodeAttribute   codeAttribute,
                                                              TracedVariables variables,
                                                              TracedStack     stack,
                                                              int             startOffset,
                                                              int             endOffset)
    {
        evaluateInstructionBlock(clazz,
                                 method,
                                 codeAttribute,
                                 variables,
                                 stack,
                                 startOffset);

        evaluateExceptionHandlers(clazz,
                                  method,
                                  codeAttribute,
                                  startOffset,
                                  endOffset);
    }


    /**
     * Evaluates a block of instructions, starting at the given offset and ending
     * at a branch instruction, a return instruction, or a throw instruction.
     */
    private void evaluateInstructionBlock(Clazz           clazz,
                                          Method          method,
                                          CodeAttribute   codeAttribute,
                                          TracedVariables variables,
                                          TracedStack     stack,
                                          int             startOffset)
    {
        // Execute the initial instruction block.
        evaluateSingleInstructionBlock(clazz,
                                       method,
                                       codeAttribute,
                                       variables,
                                       stack,
                                       startOffset);

        // Execute all resulting instruction blocks on the execution stack.
        while (!instructionBlockStack.empty())
        {
            if (stateTracker != null) stateTracker.startBranchCodeBlockEvaluation(instructionBlockStack);

            InstructionBlock instructionBlock = instructionBlockStack.pop();

            evaluateSingleInstructionBlock(clazz,
                                           method,
                                           codeAttribute,
                                           instructionBlock.variables,
                                           instructionBlock.stack,
                                           instructionBlock.startOffset);
        }
    }


    /**
     * Evaluates a block of instructions, starting at the given offset and ending
     * at a branch instruction, a return instruction, or a throw instruction.
     * Instruction blocks that are to be evaluated as a result are pushed on
     * the given stack.
     */
    private void evaluateSingleInstructionBlock(Clazz            clazz,
                                                Method           method,
                                                CodeAttribute    codeAttribute,
                                                TracedVariables  variables,
                                                TracedStack      stack,
                                                int              startOffset)
    {
        byte[] code = codeAttribute.code;
        InstructionExceptionFormatter formatter = prettyInstructionBuffered > 0 ?
                new InstructionExceptionFormatter(
                        logger,
                        new CircularIntBuffer(prettyInstructionBuffered),
                        code,
                        clazz,
                        method)
                : null;

        if (stateTracker != null)
        {
             stateTracker.startInstructionBlock(clazz, method, codeAttribute, variables, stack, startOffset);
        }

        Processor processor = new Processor(variables,
                                            stack,
                                            valueFactory,
                                            branchUnit,
                                            invocationUnit,
                                            evaluateAllCode);

        int instructionOffset = startOffset;

        int maxOffset = startOffset;

        // Evaluate the subsequent instructions.
        while (true)
        {
            try
            {

                if (formatter != null)
                {
                    formatter.registerInstructionOffset(instructionOffset);
                }

                if (maxOffset < instructionOffset)
                {
                    maxOffset = instructionOffset;
                }

                // Decode the instruction.
                Instruction instruction = InstructionFactory.create(code, instructionOffset);

                // Maintain a generalized local variable frame and stack at this
                // instruction offset, before execution.
                int evaluationCount = evaluationCounts[instructionOffset];
                if (evaluationCount == 0)
                {
                    // First time we're passing by this instruction.
                    if (variablesBefore[instructionOffset] == null)
                    {
                        // There's not even a context at this index yet.
                        variablesBefore[instructionOffset] = new TracedVariables(variables);
                        stacksBefore[instructionOffset] = new TracedStack(stack);
                    }
                    else
                    {
                        // Reuse the context objects at this index.
                        variablesBefore[instructionOffset].initialize(variables);
                        stacksBefore[instructionOffset].copy(stack);
                    }

                    // We'll execute in the generalized context, because it is
                    // the same as the current context.
                    generalizedContexts[instructionOffset] = true;
                }
                else
                {
                    // Merge in the current context.
                    boolean variablesChanged;
                    boolean stackChanged;
                    try
                    {
                        variablesChanged=variablesBefore[instructionOffset].generalize(variables, true);;
                    }
                    catch (IllegalArgumentException ex)
                    {
                        throw new VariablesGeneralizationException(ex, variablesBefore[instructionOffset], variables);
                    }
                    try
                    {
                        stackChanged=stacksBefore[instructionOffset].generalize(stack);
                    }
                    catch (IllegalArgumentException ex)
                    {
                        throw new StackGeneralizationException(ex, stacksBefore[instructionOffset], stack);
                    }

                    //System.out.println("GVars:  "+variablesBefore[instructionOffset]);
                    //System.out.println("GStack: "+stacksBefore[instructionOffset]);

                    // Bail out if the current context is the same as last time.
                    if (!variablesChanged &&
                            !stackChanged &&
                            generalizedContexts[instructionOffset])
                    {
                        if (stateTracker != null) stateTracker.skipInstructionBlock(clazz, method, instructionOffset,
                            instruction, variablesBefore[instructionOffset], stacksBefore[instructionOffset], evaluationCount);

                        break;
                    }

                    // See if this instruction has been evaluated an excessive number
                    // of times.
                    if (evaluationCount >= GENERALIZE_AFTER_N_EVALUATIONS)
                    {
                        if (stateTracker != null) stateTracker.generalizeInstructionBlock(clazz, method, instructionOffset,
                                instruction, variables, stack, evaluationCount);

                        if (stopAnalysisAfterNEvaluations != -1 && evaluationCount >= stopAnalysisAfterNEvaluations)
                        {
                            throw new ExcessiveComplexityException("Stopping evaluation after "+evaluationCount+" evaluations.");
                        }

                        // Continue, but generalize the current context.
                        // Note that the most recent variable values have to remain
                        // last in the generalizations, for the sake of the ret
                        // instruction.
                        variables.generalize(variablesBefore[instructionOffset], false);
                        stack.generalize(stacksBefore[instructionOffset]);

                        // We'll execute in the generalized context.
                        generalizedContexts[instructionOffset] = true;
                    }
                    else
                    {
                        // We'll execute in the current context.
                        generalizedContexts[instructionOffset] = false;
                    }
                }

                // We'll evaluate this instruction.
                evaluationCounts[instructionOffset]++;

                // Remember this instruction's offset with any stored value.
                Value storeValue = new InstructionOffsetValue(instructionOffset);
                variables.setProducerValue(storeValue);
                stack.setProducerValue(storeValue);

                // Reset the branch unit.
                branchUnit.reset();

                if (stateTracker != null) stateTracker.startInstructionEvaluation(clazz, method, instructionOffset, instruction, variables, stack, evaluationCount);

                if (extraInstructionVisitor != null)
                {
                    // Visit the instruction with the optional visitor.
                    instruction.accept(clazz,
                            method,
                            codeAttribute,
                            instructionOffset,
                            extraInstructionVisitor);
                }

                try
                {
                    // Process the instruction. The processor may modify the
                    // variables and the stack, and it may call the branch unit
                    // and the invocation unit.
                    instruction.accept(clazz,
                            method,
                            codeAttribute,
                            instructionOffset,
                            processor);
                }
                catch (RuntimeException ex)
                {
                    // Fallback to the default exception formatter.
                    if (formatter == null)
                    {
                        logger.error("Unexpected error while evaluating instruction:");
                        logger.error("  Class       = [{}]", clazz.getName());
                        logger.error("  Method      = [{}{}]", method.getName(clazz), method.getDescriptor(clazz));
                        logger.error("  Instruction = {}", instruction.toString(clazz, instructionOffset));
                        logger.error("  Exception   = [{}] ({})", ex.getClass().getName(), ex.getMessage());
                    }

                    throw ex;
                }

                // Collect the branch targets from the branch unit.
                InstructionOffsetValue branchTargets = branchUnit.getTraceBranchTargets();
                int branchTargetCount = branchTargets.instructionOffsetCount();

                if (stateTracker != null) stateTracker.afterInstructionEvaluation(clazz, method, instructionOffset, instruction,
                    variables, stack, branchUnit, branchTargets(instructionOffset));

                // Maintain a generalized local variable frame and stack at this
                // instruction offset, after execution.
                if (evaluationCount == 0)
                {
                    // First time we're passing by this instruction.
                    if (variablesAfter[instructionOffset] == null)
                    {
                        // There's not even a context at this index yet.
                        variablesAfter[instructionOffset] = new TracedVariables(variables);
                        stacksAfter[instructionOffset] = new TracedStack(stack);
                    }
                    else
                    {
                        // Reuse the context objects at this index.
                        variablesAfter[instructionOffset].initialize(variables);
                        stacksAfter[instructionOffset].copy(stack);
                    }
                }
                else
                {
                    // Merge in the current context.
                    variablesAfter[instructionOffset].generalize(variables, true);
                    stacksAfter[instructionOffset].generalize(stack);
                }

                // Did the branch unit get called?
                if (branchUnit.wasCalled())
                {
                    // Accumulate the branch targets at this offset.
                    branchTargetValues[instructionOffset] = branchTargetValues[instructionOffset] == null ?
                            branchTargets :
                            branchTargetValues[instructionOffset].generalize(branchTargets);

                    // Are there no branch targets at all?
                    if (branchTargetCount == 0)
                    {
                        // Exit from this code block.
                        break;
                    }

                    // Accumulate the branch origins at the branch target offsets.
                    InstructionOffsetValue instructionOffsetValue = new InstructionOffsetValue(instructionOffset);
                    for (int index = 0; index < branchTargetCount; index++)
                    {
                        int branchTarget = branchTargets.instructionOffset(index);
                        branchOriginValues[branchTarget] = branchOriginValues[branchTarget] == null ?
                                instructionOffsetValue :
                                branchOriginValues[branchTarget].generalize(instructionOffsetValue);
                    }

                    // Are there multiple branch targets?
                    if (branchTargetCount > 1)
                    {
                        // Push them on the execution stack and exit from this block.
                        for (int index = 0; index < branchTargetCount; index++)
                        {
                            if (stateTracker != null)  stateTracker.registerAlternativeBranch(clazz, method,
                                instructionOffset,  instruction, variables, stack,
                                index, branchTargetCount, branchTargets.instructionOffset(index));

                            pushInstructionBlock(new TracedVariables(variables),
                                    new TracedStack(stack),
                                    branchTargets.instructionOffset(index));
                        }

                        break;
                    }

                    if (stateTracker != null) stateTracker.definitiveBranch(clazz, method, instructionOffset, instruction, variables, stack, branchTargets);

                    // Continue at the definite branch target.
                    instructionOffset = branchTargets.instructionOffset(0);
                }
                else
                {
                    // Just continue with the next instruction.
                    instructionOffset += instruction.length(instructionOffset);
                }

                // Is this a subroutine invocation?
                if (instruction.opcode == Instruction.OP_JSR ||
                        instruction.opcode == Instruction.OP_JSR_W)
                {
                    // Evaluate the subroutine in another partial evaluator.
                    evaluateSubroutine(clazz,
                            method,
                            codeAttribute,
                            variables,
                            stack,
                            instructionOffset);

                    break;
                }
                else if (instruction.opcode == Instruction.OP_RET)
                {
                    // Let the partial evaluator that has called the subroutine
                    // handle the evaluation after the return.
                    if (stateTracker != null) stateTracker.registerSubroutineReturn(clazz, method, instructionOffset, variables, stack);

                    pushCallingInstructionBlock(new TracedVariables(variables),
                            new TracedStack(stack),
                            instructionOffset);
                    break;
                }
            }
            catch (ProguardCoreException ex)
            {
                if (formatter != null)
                {
                    formatter.printException(ex, variablesBefore[instructionOffset], stacksBefore[instructionOffset]);
                }
                throw ex;
            }
        }

        if (stateTracker != null) stateTracker.instructionBlockDone(clazz, method, codeAttribute, variables, stack, startOffset);
    }


    /**
     * Evaluates a subroutine and its exception handlers, starting at the given
     * offset and ending at a subroutine return instruction.
     */
    private void evaluateSubroutine(Clazz           clazz,
                                    Method          method,
                                    CodeAttribute   codeAttribute,
                                    TracedVariables variables,
                                    TracedStack     stack,
                                    int             subroutineStart)
    {
        int subroutineEnd = branchTargetFinder.subroutineEnd(subroutineStart);

        if (stateTracker != null) stateTracker.startSubroutine(clazz, method, variables, stack, subroutineStart, subroutineEnd);

        // Create a temporary partial evaluator, so there are no conflicts
        // with variables that are alive across subroutine invocations, between
        // different invocations.
        PartialEvaluator subroutinePartialEvaluator = subRoutineEvaluator();

        subroutinePartialEvaluator.initializeArrays(codeAttribute);

        // Evaluate the subroutine.
        subroutinePartialEvaluator.evaluateInstructionBlockAndExceptionHandlers(clazz,
                                                                                method,
                                                                                codeAttribute,
                                                                                variables,
                                                                                stack,
                                                                                subroutineStart,
                                                                                subroutineEnd);

        // Merge back the temporary partial evaluator. This way, we'll get
        // the lowest common denominator of stacks and variables.
        if (stateTracker != null) stateTracker.generalizeSubroutine(clazz, method, variables, stack, subroutineStart, subroutineEnd);
        generalize(subroutinePartialEvaluator, 0, codeAttribute.u4codeLength);

        if (stateTracker != null) stateTracker.endSubroutine(clazz, method, variables, stack, subroutineStart, subroutineEnd);
    }

    /**
     * Creates a new PartialEvaluator, based on this one.
     * This partial evaluator is the subroutine calling partial evaluator.
     */
    private PartialEvaluator subRoutineEvaluator()
    {
        return Builder.create()
                .setValueFactory(valueFactory)
                .setInvocationUnit(invocationUnit)
                .setEvaluateAllCode(evaluateAllCode)
                .setExtraInstructionVisitor(extraInstructionVisitor)
                .setBranchUnit(branchUnit)
                .setBranchTargetFinder(branchTargetFinder)
                .setCallingInstructionBlockStack(instructionBlockStack)
                .setPrettyPrinting(prettyInstructionBuffered)
                .setStateTracker(stateTracker)
                .build();
    }


    /**
     * Generalizes the results of this partial evaluator with those of another
     * given partial evaluator, over a given range of instructions.
     */
    private void generalize(PartialEvaluator other,
                            int              codeStart,
                            int              codeEnd)
    {
        for (int offset = codeStart; offset < codeEnd; offset++)
        {
            if (other.branchOriginValues[offset] != null)
            {
                branchOriginValues[offset] = branchOriginValues[offset] == null ?
                    other.branchOriginValues[offset] :
                    branchOriginValues[offset].generalize(other.branchOriginValues[offset]);
            }

            if (other.isTraced(offset))
            {
                if (other.branchTargetValues[offset] != null)
                {
                    branchTargetValues[offset] = branchTargetValues[offset] == null ?
                        other.branchTargetValues[offset] :
                        branchTargetValues[offset].generalize(other.branchTargetValues[offset]);
                }

                if (evaluationCounts[offset] == 0)
                {
                    variablesBefore[offset]     = other.variablesBefore[offset];
                    stacksBefore[offset]        = other.stacksBefore[offset];
                    variablesAfter[offset]      = other.variablesAfter[offset];
                    stacksAfter[offset]         = other.stacksAfter[offset];
                    generalizedContexts[offset] = other.generalizedContexts[offset];
                    evaluationCounts[offset]    = other.evaluationCounts[offset];
                }
                else
                {
                    variablesBefore[offset].generalize(other.variablesBefore[offset], false);
                    stacksBefore[offset]   .generalize(other.stacksBefore[offset]);
                    variablesAfter[offset] .generalize(other.variablesAfter[offset], false);
                    stacksAfter[offset]    .generalize(other.stacksAfter[offset]);
                    //generalizedContexts[offset]
                    evaluationCounts[offset] += other.evaluationCounts[offset];
                }
            }
        }
    }


    /**
     * Evaluates the exception handlers covering and targeting the given
     * instruction range in the given code.
     */
    private void evaluateExceptionHandlers(Clazz         clazz,
                                           Method        method,
                                           CodeAttribute codeAttribute,
                                           int           startOffset,
                                           int           endOffset)
    {
        if (stateTracker != null) stateTracker.startExceptionHandlingForBlock(clazz, method, startOffset, endOffset);

        ExceptionHandlerFilter exceptionEvaluator =
            new ExceptionHandlerFilter(startOffset,
                                       endOffset,
                                       this);

        // Evaluate the exception catch blocks, until their entry variables
        // have stabilized.
        do
        {
            // Reset the flag to stop evaluating.
            evaluateExceptions = false;

            // Evaluate all relevant exception catch blocks once.
            codeAttribute.exceptionsAccept(clazz,
                                           method,
                                           startOffset,
                                           endOffset,
                                           exceptionEvaluator);
        }
        while (evaluateExceptions);
    }


    // Implementations for ExceptionInfoVisitor.

    public void visitExceptionInfo(Clazz clazz, Method method, CodeAttribute codeAttribute, ExceptionInfo exceptionInfo)
    {
        int startPC = exceptionInfo.u2startPC;
        int endPC   = exceptionInfo.u2endPC;

        // Do we have to evaluate this exception catch block?
        if (mayThrowExceptions(clazz, method, codeAttribute, startPC, endPC))
        {
            int handlerPC = exceptionInfo.u2handlerPC;
            int catchType = exceptionInfo.u2catchType;

            if (stateTracker != null) stateTracker.registerExceptionHandler(clazz, method, startPC, endPC, exceptionInfo);

            // Reuse the existing variables and stack objects, ensuring the
            // right size.
            TracedVariables variables = new TracedVariables(codeAttribute.u2maxLocals);
            TracedStack     stack     = new TracedStack(codeAttribute.u2maxStack);

            // Initialize the trace values.
            Value storeValue = new InstructionOffsetValue(handlerPC | InstructionOffsetValue.EXCEPTION_HANDLER);
            variables.setProducerValue(storeValue);
            stack.setProducerValue(storeValue);

            // Initialize the variables by generalizing the variables of the
            // try block. Make sure to include the results of the last
            // instruction for preverification.
            generalizeVariables(startPC,
                                endPC,
                                evaluateAllCode,
                                variables);

            // Initialize the stack.
            invocationUnit.enterExceptionHandler(clazz,
                                                 method,
                                                 codeAttribute,
                                                 handlerPC,
                                                 catchType,
                                                 stack);

            int evaluationCount = evaluationCounts[handlerPC];

            // Evaluate the instructions, starting at the entry point.
            evaluateInstructionBlock(clazz,
                                     method,
                                     codeAttribute,
                                     variables,
                                     stack,
                                     handlerPC);

            // Remember to evaluate all exception handlers once more.
            if (!evaluateExceptions)
            {
                evaluateExceptions = evaluationCount < evaluationCounts[handlerPC];
            }
        }
//        else if (evaluateAllCode)
//        {
//            if (DEBUG) System.out.println("No information for partial evaluation of exception ["+startPC +" -> "+endPC +": "+exceptionInfo.u2handlerPC+"] yet");
//
//            // We don't have any information on the try block yet, but we do
//            // have to evaluate the exception handler.
//            // Remember to evaluate all exception handlers once more.
//            evaluateExceptions = true;
//        }
        else
        {
            if (stateTracker != null) stateTracker.registerUnusedExceptionHandler(clazz, method, startPC, endPC, exceptionInfo);
        }
    }


    // Small utility methods.

    /**
     * Initializes the data structures for the variables, stack, etc.
     */
    private void initializeArrays(CodeAttribute codeAttribute)
    {
        int newCodeLength = codeAttribute.u4codeLength;

        // Create new arrays for storing information at each instruction offset.
        if (branchOriginValues.length < newCodeLength)
        {
            // Create new arrays.
            branchOriginValues  = new InstructionOffsetValue[newCodeLength];
            branchTargetValues  = new InstructionOffsetValue[newCodeLength];
            variablesBefore     = new TracedVariables[newCodeLength];
            stacksBefore        = new TracedStack[newCodeLength];
            variablesAfter      = new TracedVariables[newCodeLength];
            stacksAfter         = new TracedStack[newCodeLength];
            generalizedContexts = new boolean[newCodeLength];
            evaluationCounts    = new int[newCodeLength];
        }
        else
        {
            // Reset the old arrays.
            Arrays.fill(branchOriginValues,  0, codeLength, null);
            Arrays.fill(branchTargetValues,  0, codeLength, null);
            Arrays.fill(generalizedContexts, 0, codeLength, false);
            Arrays.fill(evaluationCounts,    0, codeLength, 0);

            for (int index = 0; index < newCodeLength; index++)
            {
                if (variablesBefore[index] != null)
                {
                    variablesBefore[index].reset(codeAttribute.u2maxLocals);
                }

                if (stacksBefore[index] != null)
                {
                    stacksBefore[index].reset(codeAttribute.u2maxStack);
                }

                if (variablesAfter[index] != null)
                {
                    variablesAfter[index].reset(codeAttribute.u2maxLocals);
                }

                if (stacksAfter[index] != null)
                {
                    stacksAfter[index].reset(codeAttribute.u2maxStack);
                }
            }

            for (int index = newCodeLength; index < codeLength; index++)
            {
                if (variablesBefore[index] != null)
                {
                    variablesBefore[index].reset(0);
                }

                if (stacksBefore[index] != null)
                {
                    stacksBefore[index].reset(0);
                }

                if (variablesAfter[index] != null)
                {
                    variablesAfter[index].reset(0);
                }

                if (stacksAfter[index] != null)
                {
                    stacksAfter[index].reset(0);
                }
            }
        }

        codeLength = newCodeLength;
    }


    /**
     * Initializes the data structures for the variables, stack, etc.
     */
    private void initializeParameters(Clazz           clazz,
                                      Method          method,
                                      CodeAttribute   codeAttribute,
                                      TracedVariables variables)
    {
//        // Create the method parameters.
//        TracedVariables parameters = new TracedVariables(codeAttribute.u2maxLocals);
//
//        // Remember this instruction's offset with any stored value.
//        Value storeValue = new InstructionOffsetValue(AT_METHOD_ENTRY);
//        parameters.setProducerValue(storeValue);

        // Create the method parameters.
        Variables parameters = new Variables(codeAttribute.u2maxLocals);

        // Initialize the method parameters.
        invocationUnit.enterMethod(clazz, method, parameters);

        // Initialize the variables with the parameters.
        variables.initialize(parameters);

        // Set the store value of each parameter variable. We store the
        // variable indices of the parameters. These parameter offsets take
        // into account Category 2 types.
        for (int index = 0; index < parameters.size(); index++)
        {
            InstructionOffsetValue producerValue =
                new InstructionOffsetValue(index | InstructionOffsetValue.METHOD_PARAMETER);

            variables.setProducerValue(index, producerValue);
        }
    }


    /**
     * Returns whether a block of instructions may ever throw an exception.
     */
    private boolean mayThrowExceptions(Clazz         clazz,
                                       Method        method,
                                       CodeAttribute codeAttribute,
                                       int           startOffset,
                                       int           endOffset)
    {
        for (int index = startOffset; index < endOffset; index++)
        {
            if (isTraced(index) &&
                (evaluateAllCode ||
                 InstructionFactory.create(codeAttribute.code, index).mayInstanceThrowExceptions(clazz)))
            {
                return true;
            }
        }

        return false;
    }


    /**
     * Generalize the local variable frames of a block of instructions.
     */
    private void generalizeVariables(int             startOffset,
                                     int             endOffset,
                                     boolean         includeAfterLastInstruction,
                                     TracedVariables generalizedVariables)
    {
        boolean first     = true;
        int     lastIndex = -1;

        // Generalize the variables before each of the instructions in the block.
        for (int index = startOffset; index < endOffset; index++)
        {
            if (isTraced(index))
            {
                TracedVariables tracedVariables = variablesBefore[index];

                if (first)
                {
                    // Initialize the variables with the first traced local
                    // variable frame.
                    generalizedVariables.initialize(tracedVariables);

                    first = false;
                }
                else
                {
                    // Generalize the variables with the traced local variable
                    // frame. We can't use the return value, because local
                    // generalization can be different a couple of times,
                    // with the global generalization being the same.
                    generalizedVariables.generalize(tracedVariables, false);
                }

                lastIndex = index;
            }
        }

        // Generalize the variables after the last instruction in the block,
        // if required.
        if (includeAfterLastInstruction &&
            lastIndex >= 0)
        {
            TracedVariables tracedVariables = variablesAfter[lastIndex];

            if (first)
            {
                // Initialize the variables with the local variable frame.
                generalizedVariables.initialize(tracedVariables);
            }
            else
            {
                // Generalize the variables with the local variable frame.
                generalizedVariables.generalize(tracedVariables, false);
            }
        }

        // Just clear the variables if there aren't any traced instructions
        // in the block.
        if (first)
        {
            generalizedVariables.reset(generalizedVariables.size());
        }
    }

    /**
     * It the analysis visits an instruction this many times (this can happen e.g. for big switches),
     * the analysis of this method is forcibly stopped and a ExcessiveComplexityException is thrown.
     */
    public PartialEvaluator stopAnalysisAfterNEvaluations(int stopAnalysisAfterNEvaluations)
    {
        this.stopAnalysisAfterNEvaluations = stopAnalysisAfterNEvaluations;
        return this;
    }

    /**
     * This class represents an instruction block that has to be executed,
     * starting with a given state at a given instruction offset.
     */
    public static class InstructionBlock
    {
        private final TracedVariables variables;
        private final TracedStack     stack;
        private final int             startOffset;


        private InstructionBlock(TracedVariables variables,
                                 TracedStack     stack,
                                 int             startOffset)
        {
            this.variables   = variables;
            this.stack       = stack;
            this.startOffset = startOffset;
        }
    }


   /**
     * This InstructionVisitor delegates its visits to a given
     * InstructionVisitor, but only if the instruction has been traced (or not).
     */
    private class MyTracedInstructionFilter implements InstructionVisitor
    {
        private final boolean            traced;
        private final InstructionVisitor instructionVisitor;


        public MyTracedInstructionFilter(boolean            traced,
                                            InstructionVisitor instructionVisitor)
        {
            this.traced          = traced;
            this.instructionVisitor = instructionVisitor;
        }


        // Implementations for InstructionVisitor.

        public void visitSimpleInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, SimpleInstruction simpleInstruction)
        {
            if (shouldVisit(offset))
            {
                instructionVisitor.visitSimpleInstruction(clazz, method, codeAttribute, offset, simpleInstruction);
            }
        }


        public void visitVariableInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, VariableInstruction variableInstruction)
        {
            if (shouldVisit(offset))
            {
                instructionVisitor.visitVariableInstruction(clazz, method, codeAttribute, offset, variableInstruction);
            }
        }


        public void visitConstantInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, ConstantInstruction constantInstruction)
        {
            if (shouldVisit(offset))
            {
                instructionVisitor.visitConstantInstruction(clazz, method, codeAttribute, offset, constantInstruction);
            }
        }


        public void visitBranchInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, BranchInstruction branchInstruction)
        {
            if (shouldVisit(offset))
            {
                instructionVisitor.visitBranchInstruction(clazz, method, codeAttribute, offset, branchInstruction);
            }
        }


        public void visitTableSwitchInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, TableSwitchInstruction tableSwitchInstruction)
        {
            if (shouldVisit(offset))
            {
                instructionVisitor.visitTableSwitchInstruction(clazz, method, codeAttribute, offset, tableSwitchInstruction);
            }
        }


        public void visitLookUpSwitchInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, LookUpSwitchInstruction lookUpSwitchInstruction)
        {
            if (shouldVisit(offset))
            {
                instructionVisitor.visitLookUpSwitchInstruction(clazz, method, codeAttribute, offset, lookUpSwitchInstruction);
            }
        }


        private boolean shouldVisit(int offset)
        {
            return isTraced(offset) == traced;
        }
   }
}
