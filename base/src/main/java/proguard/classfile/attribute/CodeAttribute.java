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
package proguard.classfile.attribute;

import proguard.classfile.*;
import proguard.classfile.attribute.visitor.*;
import proguard.classfile.instruction.*;
import proguard.classfile.instruction.visitor.InstructionVisitor;

/**
 * This {@link Attribute} represents a code attribute.
 *
 * @author Eric Lafortune
 */
public class CodeAttribute extends Attribute
{
    private static final byte[]          EMPTY_CODE            = new byte[0];
    private static final ExceptionInfo[] EMPTY_EXCEPTION_TABLE = new ExceptionInfo[0];
    private static final Attribute[]     EMPTY_ATTRIBUTES      = new Attribute[0];


    public int             u2maxStack;
    public int             u2maxLocals;
    public int             u4codeLength;
    public byte[]          code;
    public int             u2exceptionTableLength;
    public ExceptionInfo[] exceptionTable;
    public int             u2attributesCount;
    public Attribute[]     attributes;


    /**
     * Creates an uninitialized CodeAttribute.
     */
    public CodeAttribute()
    {
    }


    /**
     * Creates a partially initialized CodeAttribute without code, exceptions,
     * or attributes.
     */
    public CodeAttribute(int u2attributeNameIndex)
    {
        this(u2attributeNameIndex,
             0,
             0,
             0,
             EMPTY_CODE);
    }


    /**
     * Creates an initialized CodeAttribute without exceptions or attributes.
     */
    public CodeAttribute(int             u2attributeNameIndex,
                         int             u2maxStack,
                         int             u2maxLocals,
                         int             u4codeLength,
                         byte[]          code)
    {
        this(u2attributeNameIndex,
             u2maxStack,
             u2maxLocals,
             u4codeLength,
             code,
             0,
             EMPTY_EXCEPTION_TABLE,
             0,
             EMPTY_ATTRIBUTES);
    }


    /**
     * Creates an initialized CodeAttribute.
     */
    public CodeAttribute(int             u2attributeNameIndex,
                         int             u2maxStack,
                         int             u2maxLocals,
                         int             u4codeLength,
                         byte[]          code,
                         int             u2exceptionTableLength,
                         ExceptionInfo[] exceptionTable,
                         int             u2attributesCount,
                         Attribute[]     attributes)
    {
        super(u2attributeNameIndex);

        this.u2maxStack             = u2maxStack;
        this.u2maxLocals            = u2maxLocals;
        this.u4codeLength           = u4codeLength;
        this.code                   = code;
        this.u2exceptionTableLength = u2exceptionTableLength;
        this.exceptionTable         = exceptionTable;
        this.u2attributesCount      = u2attributesCount;
        this.attributes             = attributes;
    }


    /**
     * Returns the (first) attribute with the given name.
     */
    public Attribute getAttribute(Clazz clazz, String name)
    {
        for (int index = 0; index < u2attributesCount; index++)
        {
            Attribute attribute = attributes[index];
            if (attribute.getAttributeName(clazz).equals(name))
            {
                return attribute;
            }
        }

        return null;
    }


    // Implementations for Attribute.

    public void accept(Clazz clazz, Method method, AttributeVisitor attributeVisitor)
    {
        attributeVisitor.visitCodeAttribute(clazz, method, this);
    }


    /**
     * Applies the given instruction visitor to all instructions.
     */
    public void instructionsAccept(Clazz clazz, Method method, InstructionVisitor instructionVisitor)
    {
        instructionsAccept(clazz, method, 0, u4codeLength, instructionVisitor);
    }


    /**
     * Applies the given instruction visitor to the instruction at the specified
     * offset.
     */
    public void instructionAccept(Clazz clazz, Method method, int offset, InstructionVisitor instructionVisitor)
    {
        Instruction instruction = InstructionFactory.create(code, offset);
        instruction.accept(clazz, method, this, offset, instructionVisitor);
    }


    /**
     * Applies the given instruction visitor to all instructions in the
     * specified range of offsets.
     */
    public void instructionsAccept(Clazz clazz, Method method, int startOffset, int endOffset, InstructionVisitor instructionVisitor)
    {
        int offset = startOffset;

        while (offset < endOffset)
        {
            // Note that the instruction is only volatile.
            Instruction instruction = InstructionFactory.create(code, offset);
            int instructionLength = instruction.length(offset);
            instruction.accept(clazz, method, this, offset, instructionVisitor);
            offset += instructionLength;
        }
    }


    /**
     * Applies the given exception visitor to all exceptions.
     */
    public void exceptionsAccept(Clazz clazz, Method method, ExceptionInfoVisitor exceptionInfoVisitor)
    {
        for (int index = 0; index < u2exceptionTableLength; index++)
        {
            // We don't need double dispatching here, since there is only one
            // type of ExceptionInfo.
            exceptionInfoVisitor.visitExceptionInfo(clazz, method, this, exceptionTable[index]);
        }
    }


    /**
     * Applies the given exception visitor to all exceptions that are applicable
     * to the instruction at the specified offset.
     */
    public void exceptionsAccept(Clazz clazz, Method method, int offset, ExceptionInfoVisitor exceptionInfoVisitor)
    {
        for (int index = 0; index < u2exceptionTableLength; index++)
        {
            ExceptionInfo exceptionInfo = exceptionTable[index];
            if (exceptionInfo.isApplicable(offset))
            {
                exceptionInfoVisitor.visitExceptionInfo(clazz, method, this, exceptionInfo);
            }
        }
    }


    /**
     * Applies the given exception visitor to all exceptions that are applicable
     * to any of the instructions in the specified range of offsets.
     */
    public void exceptionsAccept(Clazz clazz, Method method, int startOffset, int endOffset, ExceptionInfoVisitor exceptionInfoVisitor)
    {
        for (int index = 0; index < u2exceptionTableLength; index++)
        {
            ExceptionInfo exceptionInfo = exceptionTable[index];
            if (exceptionInfo.isApplicable(startOffset, endOffset))
            {
                exceptionInfoVisitor.visitExceptionInfo(clazz, method, this, exceptionInfo);
            }
        }
    }


    /**
     * Applies the given attribute visitor to all attributes.
     */
    public void attributesAccept(Clazz clazz, Method method, AttributeVisitor attributeVisitor)
    {
        for (int index = 0; index < u2attributesCount; index++)
        {
            attributes[index].accept(clazz, method, this, attributeVisitor);
        }
    }
}
