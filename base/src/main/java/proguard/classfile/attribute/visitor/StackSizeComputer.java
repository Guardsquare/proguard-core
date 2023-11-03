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
package proguard.classfile.attribute.visitor;

import proguard.classfile.Clazz;
import proguard.classfile.Method;
import proguard.classfile.attribute.Attribute;
import proguard.classfile.attribute.CodeAttribute;
import proguard.classfile.editor.ClassEstimates;
import proguard.util.ArrayUtil;

/**
 * This {@link AttributeVisitor} computes the stack sizes at all instruction offsets
 * of the code attributes that it visits.
 * <p/>
 * If only the maximum stack size is required, then prefer {@link MaxStackSizeComputer}
 * since this class uses more memory because it stores the sizes for each offset.
 */
public class StackSizeComputer
implements   AttributeVisitor
{

    private final MaxStackSizeComputer maxSackSizeComputer;
    private int[] stackSizesBefore = new int[ClassEstimates.TYPICAL_CODE_LENGTH];
    private int[] stackSizesAfter = new int[ClassEstimates.TYPICAL_CODE_LENGTH];

    /**
     * Construct a {@link StackSizeComputer} that keeps track of sizes
     * at each offset.
     */
    public StackSizeComputer()
    {
        this.maxSackSizeComputer = new MaxStackSizeComputer((offset, stackSizeBefore, stackSizeAfter) -> {
            stackSizesBefore[offset] = stackSizeBefore;
            stackSizesAfter[offset] = stackSizeAfter;
        });
    }

    /**
     * Returns whether the instruction at the given offset is reachable in the
     * most recently visited code attribute.
     */
    public boolean isReachable(int instructionOffset)
    {
        return this.maxSackSizeComputer.isReachable(instructionOffset);
    }


    /**
     * Returns the stack size before the given instruction offset of the most
     * recently visited code attribute.
     */
    public int getStackSizeBefore(int instructionOffset)
    {
        if (!this.maxSackSizeComputer.isReachable(instructionOffset))
        {
            throw new IllegalArgumentException("Unknown stack size before unreachable instruction offset ["+instructionOffset+"]");
        }

        return stackSizesBefore[instructionOffset];
    }


    /**
     * Returns the stack size after the given instruction offset of the most
     * recently visited code attribute.
     */
    public int getStackSizeAfter(int instructionOffset)
    {
        if (!this.maxSackSizeComputer.isReachable((instructionOffset)))
        {
            throw new IllegalArgumentException("Unknown stack size after unreachable instruction offset ["+instructionOffset+"]");
        }

        return stackSizesAfter[instructionOffset];
    }


    /**
     * Returns the maximum stack size of the most recently visited code attribute.
     */
    public int getMaxStackSize()
    {
        return this.maxSackSizeComputer.getMaxStackSize();
    }


    // Implementations for AttributeVisitor.

    public void visitAnyAttribute(Clazz clazz, Attribute attribute) {}


    public void visitCodeAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute)
    {
        int codeLength = codeAttribute.u4codeLength;

        stackSizesBefore = ArrayUtil.ensureArraySize(stackSizesBefore, codeLength, 0);
        stackSizesAfter = ArrayUtil.ensureArraySize(stackSizesAfter, codeLength, 0);

        codeAttribute.accept(clazz, method, maxSackSizeComputer);
    }
}
