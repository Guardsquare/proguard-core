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
package proguard.classfile.editor;

import proguard.classfile.*;
import proguard.classfile.attribute.*;
import proguard.classfile.attribute.annotation.*;
import proguard.classfile.attribute.annotation.target.*;
import proguard.classfile.attribute.annotation.target.visitor.*;
import proguard.classfile.attribute.annotation.visitor.TypeAnnotationVisitor;
import proguard.classfile.attribute.preverification.*;
import proguard.classfile.attribute.preverification.visitor.*;
import proguard.classfile.attribute.visitor.*;
import proguard.classfile.constant.Constant;
import proguard.classfile.instruction.*;
import proguard.classfile.instruction.visitor.InstructionVisitor;
import proguard.classfile.visitor.ClassPrinter;
import proguard.util.ArrayUtil;

import java.util.Arrays;

/**
 * This {@link AttributeVisitor} accumulates instructions, exceptions and line numbers,
 * and then adds them to a method or copies them into code attributes that it visits.
 * <p/>
 * The class supports composing
 *   instructions       ({@link #appendInstruction(Instruction)}),
 *   labels             ({@link #appendLabel(int)}),
 *   exception handlers ({@link #appendException(ExceptionInfo)}), and
 *   line numbers       ({@link #appendLineNumber(LineNumberInfo)}).
 * <p/>
 * The labels are numeric labels that you can choose freely, for example
 * instruction offsets from existing code that you are copying. You can then
 * refer to them in branches and exception handlers. You can compose the
 * code as a hierarchy of code fragments with their own local labels.
 * <p/>
 * You should provide an estimated maximum size (expressed in number of
 * bytes in the bytecode), so the implementation can efficiently allocate
 * the necessary internal buffers without reallocating them as the code
 * grows.
 * <p/>
 * For example:
 * <pre>
 *     ProgramClass  programClass  = ...
 *     ProgramMethod programMethod = ...
 *
 *     // Create any constants for the code.
 *     ConstantPoolEditor constantPoolEditor =
 *         new ConstantPoolEditor(programClass);
 *
 *     int exceptionType =
 *         constantPoolEditor.addClassConstant("java/lang/Exception", null);
 *
 *     // Compose the code.
 *     CodeAttributeComposer composer =
 *         new CodeAttributeComposer();
 *
 *     final int TRY_LABEL   =  0;
 *     final int IF_LABEL    =  1;
 *     final int THEN_LABEL  = 10;
 *     final int ELSE_LABEL  = 20;
 *     final int CATCH_LABEL = 30;
 *
 *     composer.beginCodeFragment(50);
 *     composer.appendLabel(TRY_LABEL);
 *     composer.appendInstruction(new SimpleInstruction(Instruction.OP_ICONST_1));
 *     composer.appendInstruction(new SimpleInstruction(Instruction.OP_ICONST_2));
 *     composer.appendLabel(IF_LABEL);
 *     composer.appendInstruction(new BranchInstruction(Instruction.OP_IFICMPLT, ELSE_LABEL - IF_LABEL));
 *
 *     composer.appendLabel(THEN_LABEL);
 *     composer.appendInstruction(new SimpleInstruction(Instruction.OP_ICONST_1));
 *     composer.appendInstruction(new SimpleInstruction(Instruction.OP_IRETURN));
 *
 *     composer.appendLabel(ELSE_LABEL);
 *     composer.appendInstruction(new SimpleInstruction(Instruction.OP_ICONST_2));
 *     composer.appendInstruction(new SimpleInstruction(Instruction.OP_IRETURN));
 *
 *     composer.appendLabel(CATCH_LABEL);
 *     composer.appendException(new ExceptionInfo(TRY_LABEL, CATCH_LABEL, CATCH_LABEL, exceptionType));
 *     composer.appendInstruction(new SimpleInstruction(Instruction.OP_ICONST_M1));
 *     composer.appendInstruction(new SimpleInstruction(Instruction.OP_IRETURN));
 *     composer.endCodeFragment();
 *
 *      // Add the code as a code attribute to the given method.
 *      composer.addCodeAttribute(programClass, programMethod, constantPoolEditor);
 * </pre>
 * <p/>
 * This class is mostly convenient to compose code based on existing code,
 * where the instructions are already available. For a more compact and
 * readable alternative to compose code programmatically from scratch,
 * see {@link CompactCodeAttributeComposer}.
 * <p/>
 * If you're building many method bodies, it is more efficient to reuse
 * a single instance of this composer for all methods that you add.
 *
 * @author Eric Lafortune
 * @author Joachim Vandersmissen
 */
public class CodeAttributeComposer
implements   AttributeVisitor,
             InstructionVisitor,
             ExceptionInfoVisitor,
             StackMapFrameVisitor,
             VerificationTypeVisitor,
             LineNumberInfoVisitor,
             LocalVariableInfoVisitor,
             LocalVariableTypeInfoVisitor,
             TypeAnnotationVisitor,
             TargetInfoVisitor,
             LocalVariableTargetElementVisitor
{
    //*
    private static final boolean DEBUG = false;
    /*/
    public  static       boolean DEBUG = System.getProperty("cac") != null;
    //*/


    private static final int MAXIMUM_LEVELS = 32;
    private static final int INVALID        = -1;


    private final boolean allowExternalBranchTargets;
    private final boolean allowExternalExceptionOffsets;
    private final boolean shrinkInstructions;
    private final boolean absoluteBranchOffsets;

    private int maximumCodeLength;
    private int codeLength;
    private int exceptionTableLength;
    private int lineNumberTableLength;
    private int level = -1;

    private byte[]  code                  = new byte[ClassEstimates.TYPICAL_CODE_LENGTH];
    private int[]   oldInstructionOffsets = new int[ClassEstimates.TYPICAL_CODE_LENGTH];

    private final int[]   codeFragmentOffsets  = new int[MAXIMUM_LEVELS];
    private final int[]   codeFragmentLengths  = new int[MAXIMUM_LEVELS];
    private final int[][] instructionOffsetMap = new int[MAXIMUM_LEVELS][];

    private ExceptionInfo[]  exceptionTable  = new ExceptionInfo[ClassEstimates.TYPICAL_EXCEPTION_TABLE_LENGTH];
    private LineNumberInfo[] lineNumberTable = new LineNumberInfo[ClassEstimates.TYPICAL_LINE_NUMBER_TABLE_LENGTH];

    private int expectedStackMapFrameOffset;

    private final StackSizeUpdater    stackSizeUpdater    = new StackSizeUpdater();
    private final VariableSizeUpdater variableSizeUpdater = new VariableSizeUpdater();
    private final InstructionWriter   instructionWriter   = new InstructionWriter();

    // This field acts as a parameter for the visitor methods that construct
    // the code, so a given constant pool editor can be reused, for efficiency.
    private ConstantPoolEditor constantPoolEditor;


    /**
     * Creates a new CodeAttributeComposer that doesn't allow external branch
     * targets or exception offsets and that automatically shrinks
     * instructions.
     */
    public CodeAttributeComposer()
    {
        this(false, false, true);
    }


    /**
     * Creates a new CodeAttributeComposer.
     * @param allowExternalBranchTargets    specifies whether branch targets
     *                                      can lie outside the code fragment
     *                                      of the branch instructions.
     * @param allowExternalExceptionOffsets specifies whether exception
     *                                      offsets can lie outside the code
     *                                      fragment in which exceptions are
     *                                      defined.
     * @param shrinkInstructions            specifies whether instructions
     *                                      should automatically be shrunk
     *                                      before being written.
     */
    public CodeAttributeComposer(boolean allowExternalBranchTargets,
                                 boolean allowExternalExceptionOffsets,
                                 boolean shrinkInstructions)
    {
        this(allowExternalBranchTargets,
             allowExternalExceptionOffsets,
             shrinkInstructions,
             false);
    }


    /**
     * Creates a new CodeAttributeComposer.
     * @param allowExternalBranchTargets    specifies whether branch targets
     *                                      can lie outside the code fragment
     *                                      of the branch instructions.
     * @param allowExternalExceptionOffsets specifies whether exception
     *                                      offsets can lie outside the code
     *                                      fragment in which exceptions are
     *                                      defined.
     * @param shrinkInstructions            specifies whether instructions
     *                                      should automatically be shrunk
     *                                      before being written.
     * @param absoluteBranchOffsets         specifies whether offsets of
     *                                      appended branch instructions and
     *                                      switch instructions are absolute,
     *                                      that is, relative to the start of
     *                                      the code, instead of relative to
     *                                      the instructions. This may simplify
     *                                      creating code manually, assuming
     *                                      the offsets don't overflow.
     */
    public CodeAttributeComposer(boolean allowExternalBranchTargets,
                                 boolean allowExternalExceptionOffsets,
                                 boolean shrinkInstructions,
                                 boolean absoluteBranchOffsets)
    {
        this.allowExternalBranchTargets     = allowExternalBranchTargets;
        this.allowExternalExceptionOffsets  = allowExternalExceptionOffsets;
        this.shrinkInstructions             = shrinkInstructions;
        this.absoluteBranchOffsets          = absoluteBranchOffsets;
    }


    /**
     * Starts a new code definition.
     */
    public void reset()
    {
        maximumCodeLength     = 0;
        codeLength            = 0;
        exceptionTableLength  = 0;
        lineNumberTableLength = 0;
        level                 = -1;

        // Make sure the instruction writer has at least the same buffer size
        // as the local arrays.
        instructionWriter.reset(code.length);
    }


    /**
     * Starts a new code fragment. Branch instructions that are added are
     * assumed to be relative within such code fragments.
     * @param maximumCodeFragmentLength the maximum length of the code that will
     *                                  be added as part of this fragment (more
     *                                  precisely, the maximum old instruction
     *                                  offset or label that is specified, plus
     *                                  one).
     */
    public void beginCodeFragment(int maximumCodeFragmentLength)
    {
        level++;

        if (level >= MAXIMUM_LEVELS)
        {
            throw new IllegalArgumentException("Maximum number of code fragment levels exceeded ["+level+"]");
        }

        // Make sure there is sufficient space for adding the code fragment.
        // It's only a rough initial estimate for the code length, not even
        // necessarily a length expressed in bytes.
        maximumCodeLength += maximumCodeFragmentLength;

        ensureCodeLength(maximumCodeLength);

        // Try to reuse the previous array for this code fragment.
        if (instructionOffsetMap[level]        == null ||
            instructionOffsetMap[level].length <= maximumCodeFragmentLength)
        {
            if (maximumCodeFragmentLength < ClassEstimates.TYPICAL_CODE_LENGTH)
            {
                maximumCodeFragmentLength = ClassEstimates.TYPICAL_CODE_LENGTH;
            }

            instructionOffsetMap[level] = new int[maximumCodeFragmentLength + 1];
        }

        // Initialize the offset map.
        Arrays.fill(instructionOffsetMap[level], 0, maximumCodeFragmentLength + 1, INVALID);

        // Remember the location of the code fragment.
        codeFragmentOffsets[level] = codeLength;
        codeFragmentLengths[level] = maximumCodeFragmentLength;
    }


    /**
     * Returns the current length (in bytes) of the code attribute being composed.
     */
    public int getCodeLength()
    {
        return codeLength;
    }


    /**
     * Appends the given instruction with the given old offset.
     * Branch instructions must fit, for instance by enabling automatic
     * shrinking of instructions.
     * @param oldInstructionOffset the old offset of the instruction, to which
     *                             branches and other references in the current
     *                             code fragment are pointing.
     * @param instruction          the instruction to be appended.
     */
    public void appendInstruction(int         oldInstructionOffset,
                                  Instruction instruction)
    {
        if (shrinkInstructions)
        {
            instruction = instruction.shrink();
        }

        if (DEBUG)
        {
            println("["+codeLength+"] <- ", instruction.toString(oldInstructionOffset));
        }

        // Make sure the code and offset arrays are large enough.
        int newCodeLength = codeLength + instruction.length(codeLength);

        ensureCodeLength(newCodeLength);

        // Remember the old offset of the appended instruction.
        oldInstructionOffsets[codeLength] = oldInstructionOffset;

        // Fill out the new offset of the appended instruction.
        instructionOffsetMap[level][oldInstructionOffset] = codeLength;

        // Write the instruction. The instruction writer may widen it later on,
        // if necessary.
        instruction.accept(null,
                           null,
                           new CodeAttribute(0, 0, 0, 0, code, 0, null, 0, null),
                           codeLength,
                           instructionWriter);
        //instruction.write(code, codeLength);

        // Continue appending at the next instruction offset.
        codeLength = newCodeLength;
    }


    /**
     * Appends the given label with the given old offset.
     * @param oldInstructionOffset the old offset of the label, to which
     *                             branches and other references in the current
     *                             code fragment are pointing.
     */
    public void appendLabel(int oldInstructionOffset)
    {
        if (DEBUG)
        {
            println("["+codeLength+"] <- ", "[" + oldInstructionOffset + "] (label)");
        }

        // Make sure the code and offset arrays are large enough.
        ensureCodeLength(codeLength + 1);

        // Remember the old offset of the following instruction.
        oldInstructionOffsets[codeLength] = oldInstructionOffset;

        // Fill out the new offset of the following instruction.
        instructionOffsetMap[level][oldInstructionOffset] = codeLength;
    }


    /**
     * Appends the given instruction without defined offsets.
     * @param instructions the instructions to be appended.
     */
    public void appendInstructions(Instruction[] instructions)
    {
        for (int index = 0; index < instructions.length; index++)
        {
            appendInstruction(instructions[index]);
        }
    }


    /**
     * Appends the given instruction without a defined offset.
     * Branch instructions should have a label, to allow computing the
     * new relative offset.
     * Branch instructions must fit, for instance by enabling automatic
     * shrinking of instructions.
     * @param instruction the instruction to be appended.
     */
    public void appendInstruction(Instruction instruction)
    {
        if (shrinkInstructions)
        {
            instruction = instruction.shrink();
        }

        if (DEBUG)
        {
            println("["+codeLength+"] <- ", instruction.toString());
        }

        // Make sure the code array is large enough.
        int newCodeLength = codeLength + instruction.length(codeLength);

        ensureCodeLength(newCodeLength);

        // Clear the old offset of the appended instruction.
        oldInstructionOffsets[codeLength] = 0;

        // Write the instruction. The instruction writer may widen it later on,
        // if necessary.
        instruction.accept(null,
                           null,
                           new CodeAttribute(0, 0, 0, 0, code, 0, null, 0, null),
                           codeLength,
                           instructionWriter);
        //instruction.write(code, codeLength);

        // Continue appending at the next instruction offset.
        codeLength = newCodeLength;
    }


    /**
     * Appends the given exception to the exception table.
     * @param exceptionInfo the exception to be appended.
     */
    public void appendException(ExceptionInfo exceptionInfo)
    {
        if (DEBUG)
        {
            print("         ", "Exception ["+exceptionInfo.u2startPC+" -> "+exceptionInfo.u2endPC+": "+exceptionInfo.u2handlerPC+"]");
        }

        // Remap the exception right away.
        visitExceptionInfo(null, null, null, exceptionInfo);

        if (DEBUG)
        {
            System.out.println(" -> ["+exceptionInfo.u2startPC+" -> "+exceptionInfo.u2endPC+": "+exceptionInfo.u2handlerPC+"]");
        }

        // Don't add the exception if its instruction range is empty.
        if (exceptionInfo.u2startPC == exceptionInfo.u2endPC)
        {
            if (DEBUG)
            {
                println("         ", "  (not added because of empty instruction range)");
            }

            return;
        }

        // Add the exception.
        exceptionTable =
            ArrayUtil.add(exceptionTable,
                          exceptionTableLength++,
                          exceptionInfo);
    }


    /**
     * Inserts the given line number at the appropriate position in the line
     * number table.
     * @param lineNumberInfo the line number to be inserted.
     * @return the index where the line number was actually inserted.
     */
    public int insertLineNumber(LineNumberInfo lineNumberInfo)
    {
        return insertLineNumber(0, lineNumberInfo);
    }


    /**
     * Inserts the given line number at the appropriate position in the line
     * number table.
     * @param minimumIndex   the minimum index where the line number may be
     *                       inserted.
     * @param lineNumberInfo the line number to be inserted.
     * @return the index where the line number was inserted.
     */
    public int insertLineNumber(int minimumIndex, LineNumberInfo lineNumberInfo)
    {
        if (DEBUG)
        {
            print("         ", "Line number ["+lineNumberInfo.u2startPC+"]");
        }

        // Remap the line number right away.
        visitLineNumberInfo(null, null, null, lineNumberInfo);

        if (DEBUG)
        {
            System.out.println(" -> ["+lineNumberInfo.u2startPC+"] line "+lineNumberInfo.u2lineNumber+(lineNumberInfo.getSource()==null ? "":" ["+lineNumberInfo.getSource()+"]"));
        }

        lineNumberTable =
            ArrayUtil.extendArray(lineNumberTable,
                                  lineNumberTableLength + 1);

        // Find the insertion index, starting from the end.
        // Don't insert before a negative line number, in case of a tie.
        int index = lineNumberTableLength++;
        while (index > minimumIndex &&
               (lineNumberTable[index - 1].u2startPC    >  lineNumberInfo.u2startPC ||
                lineNumberTable[index - 1].u2startPC    >= lineNumberInfo.u2startPC &&
                lineNumberTable[index - 1].u2lineNumber >= 0))
        {
            lineNumberTable[index] = lineNumberTable[--index];
        }

        lineNumberTable[index] = lineNumberInfo;

        return index;
    }


    /**
     * Appends the given line number to the line number table.
     * @param lineNumberInfo the line number to be appended.
     */
    public void appendLineNumber(LineNumberInfo lineNumberInfo)
    {
        if (DEBUG)
        {
            print("         ", "Line number ["+lineNumberInfo.u2startPC+"]");
        }

        // Remap the line number right away.
        visitLineNumberInfo(null, null, null, lineNumberInfo);

        if (DEBUG)
        {
            System.out.println(" -> ["+lineNumberInfo.u2startPC+"] line "+lineNumberInfo.u2lineNumber+(lineNumberInfo.getSource()==null ? "":" ["+lineNumberInfo.getSource()+"]"));
        }

        // Add the line number.
        lineNumberTable =
            ArrayUtil.add(lineNumberTable,
                          lineNumberTableLength++,
                          lineNumberInfo);
    }


    /**
     * Wraps up the current code fragment, continuing with the previous one on
     * the stack.
     */
    public void endCodeFragment()
    {
        if (level < 0)
        {
            throw new IllegalArgumentException("Code fragment not begun ["+level+"]");
        }

        // Remap the instructions of the code fragment.
        int instructionOffset = codeFragmentOffsets[level];
        while (instructionOffset < codeLength)
        {
            // Get the next instruction.
            Instruction instruction = InstructionFactory.create(code, instructionOffset);

            // Does this instruction still have to be remapped?
            if (oldInstructionOffsets[instructionOffset] >= 0)
            {
                // Adapt the instruction for its new offset.
                instruction.accept(null, null, null, instructionOffset, this);

                // Write the instruction back. The instruction writer may still
                // widen it later on, if necessary.
                instruction.accept(null,
                                   null,
                                   new CodeAttribute(0, 0, 0, 0, code, 0, null, 0, null),
                                   instructionOffset,
                                   instructionWriter);
                //instruction.write(code, codeLength);
            }

            // Continue remapping at the next instruction offset.
            instructionOffset += instruction.length(instructionOffset);
        }

        // Correct the estimated maximum code length, now that we know the
        // actual length of this code fragment.
        maximumCodeLength += codeLength - codeFragmentOffsets[level] -
                             codeFragmentLengths[level];

        // Try to remap the exception offsets that couldn't be remapped before.
        if (allowExternalExceptionOffsets)
        {
            for (int index = 0; index < exceptionTableLength; index++)
            {
                ExceptionInfo exceptionInfo = exceptionTable[index];

                exceptionInfo.u2startPC =
                    remapExceptionOffset(exceptionInfo.u2startPC);
                exceptionInfo.u2endPC =
                    remapExceptionOffset(exceptionInfo.u2endPC);
                exceptionInfo.u2handlerPC =
                    remapExceptionOffset(exceptionInfo.u2handlerPC);
            }
        }

        level--;
    }


    /**
     * Adds the code that has been built as a code attribute to the given method.
     */
    public void addCodeAttribute(ProgramClass  programClass,
                                 ProgramMethod programMethod)
    {
        addCodeAttribute(programClass,
                         programMethod,
                         new ConstantPoolEditor(programClass));
    }


    /**
     * Adds the code that has been built as a code attribute to the given method.
     * Reuses the given constant pool editor, which may be more efficient.
     */
    public void addCodeAttribute(ProgramClass       programClass,
                                 ProgramMethod      programMethod,
                                 ConstantPoolEditor constantPoolEditor)
    {
        CodeAttribute codeAttribute =
            new CodeAttribute(constantPoolEditor.addUtf8Constant(Attribute.CODE));

        // Pass the given constant pool editor. for efficiency, and fill out
        // the attribute.
        this.constantPoolEditor = constantPoolEditor;
        visitCodeAttribute(programClass, programMethod, codeAttribute);
        this.constantPoolEditor = null;

        new AttributesEditor(programClass, programMethod, false)
            .addAttribute(codeAttribute);
    }


    // Implementations for AttributeVisitor.

    public void visitAnyAttribute(Clazz clazz, Attribute attribute) {}


    /**
     * Sets the code that has been built in the given code attribute.
     */
    public void visitCodeAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute)
    {
        if (DEBUG)
        {
            System.out.println("CodeAttributeComposer: putting results in ["+clazz.getName()+"."+method.getName(clazz)+method.getDescriptor(clazz)+"]");
        }

        if (level != -1)
        {
            throw new IllegalArgumentException("Code fragment not ended ["+level+"]");
        }

        level++;

        // Make sure the code attribute has sufficient space for the composed
        // code.
        if (codeAttribute.u4codeLength < codeLength)
        {
            codeAttribute.code = new byte[codeLength];
        }

        // Copy the composed code over into the code attribute.
        codeAttribute.u4codeLength = codeLength;
        System.arraycopy(code, 0, codeAttribute.code, 0, codeLength);

        // Remove exceptions with empty code blocks (done before).
        //exceptionTableLength =
        //    removeEmptyExceptions(exceptionTable, exceptionTableLength);

        // Make sure the exception table has sufficient space for the composed
        // exceptions.
        if (codeAttribute.exceptionTable.length < exceptionTableLength)
        {
            codeAttribute.exceptionTable = new ExceptionInfo[exceptionTableLength];
        }

        // Copy the exception table.
        codeAttribute.u2exceptionTableLength = exceptionTableLength;
        System.arraycopy(exceptionTable, 0, codeAttribute.exceptionTable, 0, exceptionTableLength);

        // Update the maximum stack size and local variable frame size.
        stackSizeUpdater.visitCodeAttribute(clazz, method, codeAttribute);
        variableSizeUpdater.visitCodeAttribute(clazz, method, codeAttribute);

        // Add a new line number table for the line numbers, if necessary.
        if (lineNumberTableLength > 0 &&
            codeAttribute.getAttribute(clazz, Attribute.LINE_NUMBER_TABLE) == null)
        {
            // This class generally doesn't interact with the constant pool
            // (its callers do), but unfortunately we now need to add the name
            // of the line number table attribute. Reuse the passed constant
            // pool editor if possible.
            ConstantPoolEditor constantPoolEditor = this.constantPoolEditor != null ?
                this.constantPoolEditor :
                new ConstantPoolEditor((ProgramClass)clazz);

            int attributeNameIndex =
                constantPoolEditor
                    .addUtf8Constant(Attribute.LINE_NUMBER_TABLE);

            new AttributesEditor((ProgramClass)clazz, (ProgramMember)method, codeAttribute, false)
                .addAttribute(new LineNumberTableAttribute(attributeNameIndex, 0, null));
        }

        // Copy the line number table and the local variable table.
        codeAttribute.attributesAccept(clazz, method, this);

        // Remap the exception table (done before).
        //codeAttribute.exceptionsAccept(clazz, method, this);

        // Remove exceptions with empty code blocks (done before).
        //codeAttribute.u2exceptionTableLength =
        //    removeEmptyExceptions(codeAttribute.exceptionTable,
        //                          codeAttribute.u2exceptionTableLength);

        // Make sure instructions are widened if necessary.
        instructionWriter.visitCodeAttribute(clazz, method, codeAttribute);

        level--;

        if (DEBUG)
        {
            codeAttribute.accept(clazz, method, new ClassPrinter());
        }
    }


    public void visitStackMapAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute, StackMapAttribute stackMapAttribute)
    {
        // Remap all stack map entries.
        expectedStackMapFrameOffset = -1;
        stackMapAttribute.stackMapFramesAccept(clazz, method, codeAttribute, this);
    }


    public void visitStackMapTableAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute, StackMapTableAttribute stackMapTableAttribute)
    {
        // Remap all stack map table entries.
        expectedStackMapFrameOffset = 0;
        stackMapTableAttribute.stackMapFramesAccept(clazz, method, codeAttribute, this);
    }


    public void visitLineNumberTableAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute, LineNumberTableAttribute lineNumberTableAttribute)
    {
        // Didn't we get line number new definitions?
        if (lineNumberTableLength == 0)
        {
            // Remap all line number table entries of the existing table.
            lineNumberTableAttribute.lineNumbersAccept(clazz, method, codeAttribute, this);
        }
        else
        {
            // Remove line numbers with empty code blocks.
            // Actually, we'll do this elsewhere, to allow processing the
            // line numbers of inlined methods.
            //lineNumberTableLength =
            //    removeEmptyLineNumbers(lineNumberTable,
            //                           lineNumberTableLength,
            //                           codeAttribute.u4codeLength);

            // Copy the line number table.
            lineNumberTableAttribute.lineNumberTable         = new LineNumberInfo[lineNumberTableLength];
            lineNumberTableAttribute.u2lineNumberTableLength = lineNumberTableLength;
            System.arraycopy(lineNumberTable, 0, lineNumberTableAttribute.lineNumberTable, 0, lineNumberTableLength);
        }
    }

    public void visitLocalVariableTableAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute, LocalVariableTableAttribute localVariableTableAttribute)
    {
        // Remap all local variable table entries.
        localVariableTableAttribute.localVariablesAccept(clazz, method, codeAttribute, this);

        // Remove local variables with empty code blocks.
        localVariableTableAttribute.u2localVariableTableLength =
            removeEmptyLocalVariables(localVariableTableAttribute.localVariableTable,
                                      localVariableTableAttribute.u2localVariableTableLength,
                                      codeAttribute.u2maxLocals);
    }


    public void visitLocalVariableTypeTableAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute, LocalVariableTypeTableAttribute localVariableTypeTableAttribute)
    {
        // Remap all local variable table entries.
        localVariableTypeTableAttribute.localVariablesAccept(clazz, method, codeAttribute, this);

        // Remove local variables with empty code blocks.
        localVariableTypeTableAttribute.u2localVariableTypeTableLength =
            removeEmptyLocalVariableTypes(localVariableTypeTableAttribute.localVariableTypeTable,
                                          localVariableTypeTableAttribute.u2localVariableTypeTableLength,
                                          codeAttribute.u2maxLocals);
    }


    public void visitAnyTypeAnnotationsAttribute(Clazz clazz, TypeAnnotationsAttribute typeAnnotationsAttribute)
    {
        typeAnnotationsAttribute.typeAnnotationsAccept(clazz, this);
    }


    public void visitRuntimeVisibleTypeAnnotationsAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute, RuntimeVisibleTypeAnnotationsAttribute runtimeVisibleTypeAnnotationsAttribute)
    {
        // Remap all type annotations.
        runtimeVisibleTypeAnnotationsAttribute.typeAnnotationsAccept(clazz, method, codeAttribute, this);
    }


    public void visitRuntimeInvisibleTypeAnnotationsAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute, RuntimeInvisibleTypeAnnotationsAttribute runtimeInvisibleTypeAnnotationsAttribute)
    {
        // Remap all type annotations.
        runtimeInvisibleTypeAnnotationsAttribute.typeAnnotationsAccept(clazz, method, codeAttribute, this);
    }


    // Implementations for InstructionVisitor.

    public void visitAnyInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, Instruction instruction) {}


    public void visitBranchInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, BranchInstruction branchInstruction)
    {
        try
        {
            // Adjust the branch offset.
            branchInstruction.branchOffset =
                newBranchOffset(offset, branchInstruction.branchOffset);

            // Don't remap this instruction again.
            oldInstructionOffsets[offset] = -1;
        }
        catch (IllegalArgumentException e)
        {
            if (level == 0 || !allowExternalBranchTargets)
            {
                 throw e;
            }
        }
    }


    public void visitAnySwitchInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, SwitchInstruction switchInstruction)
    {
        try
        {
            // TODO: We're assuming we can adjust no offsets or all offsets at once.
            // Adjust the default jump offset.
            switchInstruction.defaultOffset =
                newBranchOffset(offset, switchInstruction.defaultOffset);

            // Adjust the jump offsets.
            updateJumpOffsets(offset,
                              switchInstruction.jumpOffsets);

            // Don't remap this instruction again.
            oldInstructionOffsets[offset] = -1;
        }
        catch (IllegalArgumentException e)
        {
            if (level == 0 || !allowExternalBranchTargets)
            {
                 throw e;
            }
        }
    }


    // Implementations for ExceptionInfoVisitor.

    public void visitExceptionInfo(Clazz clazz, Method method, CodeAttribute codeAttribute, ExceptionInfo exceptionInfo)
    {
        // See if we can remap the start right away. Unmapped exception starts
        // are negated, in order to mark them as external.
        int startPC = exceptionInfo.u2startPC;
        exceptionInfo.u2startPC =
            !allowExternalExceptionOffsets ||
            remappableExceptionOffset(startPC) ?
                newInstructionOffset(startPC) :
                -startPC;

        // See if we can remap the end right away. Unmapped exception ends are
        // negated, in order to mark them as external.
        int endPC = exceptionInfo.u2endPC;
        exceptionInfo.u2endPC =
            !allowExternalExceptionOffsets ||
            remappableExceptionOffset(endPC) ?
                newInstructionOffset(endPC) :
                -endPC;

        // See if we can remap the handler right away. Unmapped exception
        // handlers are negated, in order to mark them as external.
        int handlerPC = exceptionInfo.u2handlerPC;
        exceptionInfo.u2handlerPC =
            !allowExternalExceptionOffsets ||
            remappableExceptionOffset(handlerPC) ?
                newInstructionOffset(handlerPC) :
                -handlerPC;
    }


    // Implementations for StackMapFrameVisitor.

    public void visitAnyStackMapFrame(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, StackMapFrame stackMapFrame)
    {
        // Remap the stack map frame offset.
        int stackMapFrameOffset = newInstructionOffset(offset);

        int offsetDelta = stackMapFrameOffset;

        // Compute the offset delta if the frame is part of a stack map frame
        // table (for JDK 6.0) instead of a stack map (for Java Micro Edition).
        if (expectedStackMapFrameOffset >= 0)
        {
            offsetDelta -= expectedStackMapFrameOffset;

            expectedStackMapFrameOffset = stackMapFrameOffset + 1;
        }

        stackMapFrame.u2offsetDelta = offsetDelta;
    }


    public void visitSameOneFrame(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, SameOneFrame sameOneFrame)
    {
        // Remap the stack map frame offset.
        visitAnyStackMapFrame(clazz, method, codeAttribute, offset, sameOneFrame);

        // Remap the verification type offset.
        sameOneFrame.stackItemAccept(clazz, method, codeAttribute, offset, this);
    }


    public void visitMoreZeroFrame(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, MoreZeroFrame moreZeroFrame)
    {
        // Remap the stack map frame offset.
        visitAnyStackMapFrame(clazz, method, codeAttribute, offset, moreZeroFrame);

        // Remap the verification type offsets.
        moreZeroFrame.additionalVariablesAccept(clazz, method, codeAttribute, offset, this);
    }


    public void visitFullFrame(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, FullFrame fullFrame)
    {
        // Remap the stack map frame offset.
        visitAnyStackMapFrame(clazz, method, codeAttribute, offset, fullFrame);

        // Remap the verification type offsets.
        fullFrame.variablesAccept(clazz, method, codeAttribute, offset, this);
        fullFrame.stackAccept(clazz, method, codeAttribute, offset, this);
    }


    // Implementations for VerificationTypeVisitor.

    public void visitAnyVerificationType(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, VerificationType verificationType) {}


    public void visitUninitializedType(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, UninitializedType uninitializedType)
    {
        // Remap the offset of the 'new' instruction.
        uninitializedType.u2newInstructionOffset = newInstructionOffset(uninitializedType.u2newInstructionOffset);
    }


    // Implementations for LineNumberInfoVisitor.

    public void visitLineNumberInfo(Clazz clazz, Method method, CodeAttribute codeAttribute, LineNumberInfo lineNumberInfo)
    {
        // Remap the code offset.
        lineNumberInfo.u2startPC = newInstructionOffset(lineNumberInfo.u2startPC);
    }


    // Implementations for LocalVariableInfoVisitor.

    public void visitLocalVariableInfo(Clazz clazz, Method method, CodeAttribute codeAttribute, LocalVariableInfo localVariableInfo)
    {
        // Remap the code offset and length.
        // TODO: The local variable frame might not be strictly preserved.
        int startPC = newInstructionOffset(localVariableInfo.u2startPC);
        int endPC   = newInstructionOffset(localVariableInfo.u2startPC +
                                           localVariableInfo.u2length);

        localVariableInfo.u2startPC = startPC;
        localVariableInfo.u2length  = endPC - startPC;
    }

    // Implementations for LocalVariableTypeInfoVisitor.

    public void visitLocalVariableTypeInfo(Clazz clazz, Method method, CodeAttribute codeAttribute, LocalVariableTypeInfo localVariableTypeInfo)
    {
        // Remap the code offset and length.
        // TODO: The local variable frame might not be strictly preserved.
        int startPC = newInstructionOffset(localVariableTypeInfo.u2startPC);
        int endPC   = newInstructionOffset(localVariableTypeInfo.u2startPC +
                                           localVariableTypeInfo.u2length);

        localVariableTypeInfo.u2startPC = startPC;
        localVariableTypeInfo.u2length  = endPC - startPC;
    }


    // Implementations for TypeAnnotationVisitor.

    public void visitTypeAnnotation(Clazz clazz, TypeAnnotation typeAnnotation)
    {
        // Remap the target info.
        typeAnnotation.targetInfoAccept(clazz, this);
    }


    public void visitTypeAnnotation(Clazz clazz, Method method, CodeAttribute codeAttribute, TypeAnnotation typeAnnotation)
    {
        // Remap the target info.
        typeAnnotation.targetInfoAccept(clazz, method, codeAttribute, this);
    }


    // Implementations for TargetInfoVisitor.

    public void visitAnyTargetInfo(Clazz clazz, TypeAnnotation typeAnnotation, TargetInfo targetInfo) {}


    public void visitLocalVariableTargetInfo(Clazz clazz, Method method, CodeAttribute codeAttribute, TypeAnnotation typeAnnotation, LocalVariableTargetInfo localVariableTargetInfo)
    {
        // Remap all local variable target info elements.
        localVariableTargetInfo.targetElementsAccept(clazz, method, codeAttribute, typeAnnotation, this);

        // Remove local variable target info elements with empty code blocks.
        localVariableTargetInfo.u2tableLength =
            removeEmptyLocalVariableTargetElements(localVariableTargetInfo.table,
                                                   localVariableTargetInfo.u2tableLength,
                                                   codeAttribute.u2maxLocals);
    }


    public void visitOffsetTargetInfo(Clazz clazz, Method method, CodeAttribute codeAttribute, TypeAnnotation typeAnnotation, OffsetTargetInfo offsetTargetInfo)
    {
        // Remap the code offset.
        offsetTargetInfo.u2offset = newInstructionOffset(offsetTargetInfo.u2offset);
    }


    public void visitTypeArgumentTargetInfo(Clazz clazz, Method method, CodeAttribute codeAttribute, TypeAnnotation typeAnnotation, TypeArgumentTargetInfo typeArgumentTargetInfo)
    {
        // Remap the code offset.
        typeArgumentTargetInfo.u2offset = newInstructionOffset(typeArgumentTargetInfo.u2offset);
    }


    // Implementations for LocalVariableTargetElementVisitor.

    public void visitLocalVariableTargetElement(Clazz clazz, Method method, CodeAttribute codeAttribute, TypeAnnotation typeAnnotation, LocalVariableTargetInfo localVariableTargetInfo, LocalVariableTargetElement localVariableTargetElement)
    {
        // Remap the code offset and length.
        // TODO: The local variable frame might not be strictly preserved.
        int startPC = newInstructionOffset(localVariableTargetElement.u2startPC);
        int endPC   = newInstructionOffset(localVariableTargetElement.u2startPC +
                                           localVariableTargetElement.u2length);

        localVariableTargetElement.u2startPC = startPC;
        localVariableTargetElement.u2length  = endPC - startPC;
    }


    // Small utility methods.

    /**
     * Make sure the code arrays have at least the given size.
     */
    private void ensureCodeLength(int newCodeLength)
    {
        if (code.length < newCodeLength)
        {
            // Add 20% to avoid extending the arrays too often.
            newCodeLength = newCodeLength * 6 / 5;

            code                  = ArrayUtil.extendArray(code,                  newCodeLength);
            oldInstructionOffsets = ArrayUtil.extendArray(oldInstructionOffsets, newCodeLength);

            instructionWriter.extend(newCodeLength);
        }
    }


    /**
     * Adjusts the given jump offsets for the instruction at the given offset.
     */
    private void updateJumpOffsets(int offset, int[] jumpOffsets)
    {
        for (int index = 0; index < jumpOffsets.length; index++)
        {
            jumpOffsets[index] = newBranchOffset(offset, jumpOffsets[index]);
        }
    }


    /**
     * Computes the new branch offset for the instruction at the given new offset
     * with the given old branch offset.
     */
    private int newBranchOffset(int newInstructionOffset, int oldBranchOffset)
    {
        if (newInstructionOffset < 0 ||
            newInstructionOffset > codeLength)
        {
            throw new IllegalArgumentException("Invalid instruction offset ["+newInstructionOffset +"] in code with length ["+codeLength+"]");
        }

        // Are the input branch offsets relative to the start of the code
        // or relative to the instruction?
        int oldInstructionOffset = absoluteBranchOffsets ?
            0 :
            oldInstructionOffsets[newInstructionOffset];

        // Compute the new branch offset, always relative to the instruction.
        return newInstructionOffset(oldInstructionOffset + oldBranchOffset) -
               newInstructionOffset;
    }


    /**
     * Computes the new instruction offset for the instruction at the given old
     * offset.
     */
    private int newInstructionOffset(int oldInstructionOffset)
    {
        if (oldInstructionOffset < 0 ||
            oldInstructionOffset > codeFragmentLengths[level])
        {
            throw new IllegalArgumentException("Instruction offset ["+oldInstructionOffset +"] out of range in code fragment with length ["+codeFragmentLengths[level]+"] at level "+level);
        }

        int newInstructionOffset = instructionOffsetMap[level][oldInstructionOffset];
        if (newInstructionOffset == INVALID)
        {
            throw new IllegalArgumentException("Invalid instruction offset ["+oldInstructionOffset +"] in code fragment at level "+level);
        }

        return newInstructionOffset;
    }


    /**
     * Computes the new instruction offset for an exception start, end, or
     * handler, if the old instruction offset is negated.
     */
    private int remapExceptionOffset(int oldInstructionOffset)
    {
        // Unmapped exception offsets are still negated.
        if (oldInstructionOffset < 0)
        {
            oldInstructionOffset = -oldInstructionOffset;
            if (remappableExceptionOffset(oldInstructionOffset))
            {
                return newInstructionOffset(oldInstructionOffset);
            }
            else if (level == 0)
            {
                throw new IllegalStateException("Couldn't remap exception offset ["+oldInstructionOffset+"]");
            }
        }

        return oldInstructionOffset;
    }


    /**
     * Returns whether the given old exception offset can be remapped in the
     * current code fragment.
     */
    private boolean remappableExceptionOffset(int oldInstructionOffset)
    {
        // Can we index in the array?
        if (oldInstructionOffset > codeFragmentLengths[level])
        {
            return false;
        }

        // Do we have a valid new instruction offset?
        int newInstructionOffset =
            instructionOffsetMap[level][oldInstructionOffset];

        return newInstructionOffset > INVALID;
    }


    /**
     * Returns the given list of exceptions, without the ones that have empty
     * code blocks.
     */
    private int removeEmptyExceptions(ExceptionInfo[] exceptionInfos,
                                      int             exceptionInfoCount)
    {
        // Overwrite all empty exceptions.
        int newIndex = 0;
        for (int index = 0; index < exceptionInfoCount; index++)
        {
            ExceptionInfo exceptionInfo = exceptionInfos[index];
            if (exceptionInfo.u2startPC < exceptionInfo.u2endPC)
            {
                exceptionInfos[newIndex++] = exceptionInfo;
            }
        }

        // Clear the unused array entries.
        Arrays.fill(exceptionInfos, newIndex, exceptionInfoCount, null);

        return newIndex;
    }


    /**
     * Returns the given list of line numbers, without the ones that have empty
     * code blocks or that exceed the code size.
     */
    private int removeEmptyLineNumbers(LineNumberInfo[] lineNumberInfos,
                                       int              lineNumberInfoCount,
                                       int              codeLength)
    {
        // Overwrite all empty line number entries.
        int newIndex = 0;
        for (int index = 0; index < lineNumberInfoCount; index++)
        {
            LineNumberInfo lineNumberInfo = lineNumberInfos[index];
            int startPC = lineNumberInfo.u2startPC;
            if (startPC < codeLength &&
                (index == 0 || startPC > lineNumberInfos[index-1].u2startPC))
            {
                lineNumberInfos[newIndex++] = lineNumberInfo;
            }
        }

        // Clear the unused array entries.
        Arrays.fill(lineNumberInfos, newIndex, lineNumberInfoCount, null);

        return newIndex;
    }


    /**
     * Returns the given list of local variables, without the ones that have empty
     * code blocks or that exceed the actual number of local variables.
     */
    private int removeEmptyLocalVariables(LocalVariableInfo[] localVariableInfos,
                                          int                 localVariableInfoCount,
                                          int                 maxLocals)
    {
        // Overwrite all empty local variable entries.
        int newIndex = 0;
        for (int index = 0; index < localVariableInfoCount; index++)
        {
            LocalVariableInfo localVariableInfo = localVariableInfos[index];
            if (localVariableInfo.u2length > 0 &&
                localVariableInfo.u2index < maxLocals)
            {
                localVariableInfos[newIndex++] = localVariableInfo;
            }
        }

        // Clear the unused array entries.
        Arrays.fill(localVariableInfos, newIndex, localVariableInfoCount, null);

        return newIndex;
    }


    /**
     * Returns the given list of local variable types, without the ones that
     * have empty code blocks or that exceed the actual number of local variables.
     */
    private int removeEmptyLocalVariableTypes(LocalVariableTypeInfo[] localVariableTypeInfos,
                                              int                     localVariableTypeInfoCount,
                                              int                     maxLocals)
    {
        // Overwrite all empty local variable type entries.
        int newIndex = 0;
        for (int index = 0; index < localVariableTypeInfoCount; index++)
        {
            LocalVariableTypeInfo localVariableTypeInfo = localVariableTypeInfos[index];
            if (localVariableTypeInfo.u2length > 0 &&
                localVariableTypeInfo.u2index < maxLocals)
            {
                localVariableTypeInfos[newIndex++] = localVariableTypeInfo;
            }
        }

        // Clear the unused array entries.
        Arrays.fill(localVariableTypeInfos, newIndex, localVariableTypeInfoCount, null);

        return newIndex;
    }


    /**
     * Returns the given list of local variable target elements, without the ones
     * that have empty code blocks or that exceed the actual number of local variables.
     */
    private int removeEmptyLocalVariableTargetElements(LocalVariableTargetElement[] localVariableTargetElements,
                                                       int                          localVariableTargetElementCount,
                                                       int                          maxLocals)
    {
        // Overwrite all empty local variable target elements.
        int newIndex = 0;
        for (int index = 0; index < localVariableTargetElementCount; index++)
        {
            LocalVariableTargetElement localVariableTargetElement = localVariableTargetElements[index];
            if (localVariableTargetElement.u2length > 0 &&
                localVariableTargetElement.u2index < maxLocals)
            {
                localVariableTargetElements[newIndex++] = localVariableTargetElement;
            }
        }

        // Clear the unused array entries.
        Arrays.fill(localVariableTargetElements, newIndex, localVariableTargetElementCount, null);

        return newIndex;
    }


    private void println(String string1, String string2)
    {
        print(string1, string2);

        System.out.println();
    }

    private void print(String string1, String string2)
    {
        System.out.print(string1);

        for (int index = 0; index < level; index++)
        {
            System.out.print("  ");
        }

        System.out.print(string2);
    }


    /**
     * Small sample application that illustrates the use of this class.
     */
    public static void main(String[] args)
    {
        // Create an empty class.
        ProgramClass programClass =
            new ProgramClass(VersionConstants.CLASS_VERSION_1_8,
                             1,
                             new Constant[10],
                             AccessConstants.PUBLIC,
                             0,
                             0);

        // Add its name and superclass.
        ConstantPoolEditor constantPoolEditor =
            new ConstantPoolEditor(programClass);

        programClass.u2thisClass  = constantPoolEditor.addClassConstant("com/example/Test", programClass);
        programClass.u2superClass = constantPoolEditor.addClassConstant(ClassConstants.NAME_JAVA_LANG_OBJECT, null);

        // Create an empty method.
        ProgramMethod programMethod =
            new ProgramMethod(AccessConstants.PUBLIC,
                              constantPoolEditor.addUtf8Constant("test"),
                              constantPoolEditor.addUtf8Constant("()I"),
                              null);

        // Add the method to the class.
        ClassEditor classEditor =
            new ClassEditor(programClass);

        classEditor.addMethod(programMethod);

        // Create any constants for the code.
        int exceptionType =
            constantPoolEditor.addClassConstant("java/lang/Exception", null);

        // Compose the code -- the equivalent of this java code:
        //     try
        //     {
        //         if (1 < 2) return 1; else return 2;
        //     }
        //     catch (Exception e)
        //     {
        //         return -1;
        //     }
        CodeAttributeComposer composer =
            new CodeAttributeComposer();

        final int TRY_LABEL   =  0;
        final int IF_LABEL    =  1;
        final int THEN_LABEL  = 10;
        final int ELSE_LABEL  = 20;
        final int CATCH_LABEL = 30;

        composer.beginCodeFragment(50);
        composer.appendLabel(TRY_LABEL);
        composer.appendInstruction(new SimpleInstruction(Instruction.OP_ICONST_1));
        composer.appendInstruction(new SimpleInstruction(Instruction.OP_ICONST_2));
        composer.appendLabel(IF_LABEL);
        composer.appendInstruction(new BranchInstruction(Instruction.OP_IFICMPLT, ELSE_LABEL - IF_LABEL));

        composer.appendLabel(THEN_LABEL);
        composer.appendInstruction(new SimpleInstruction(Instruction.OP_ICONST_1));
        composer.appendInstruction(new SimpleInstruction(Instruction.OP_IRETURN));

        composer.appendLabel(ELSE_LABEL);
        composer.appendInstruction(new SimpleInstruction(Instruction.OP_ICONST_2));
        composer.appendInstruction(new SimpleInstruction(Instruction.OP_IRETURN));

        composer.appendLabel(CATCH_LABEL);
        composer.appendException(new ExceptionInfo(TRY_LABEL, CATCH_LABEL, CATCH_LABEL, exceptionType));
        composer.appendInstruction(new SimpleInstruction(Instruction.OP_ICONST_M1));
        composer.appendInstruction(new SimpleInstruction(Instruction.OP_IRETURN));
        composer.endCodeFragment();

        // Add the code as a code attribute to the given method.
        composer.addCodeAttribute(programClass, programMethod, constantPoolEditor);

        // Print out the result.
        programClass.accept(new ClassPrinter());
    }
}
