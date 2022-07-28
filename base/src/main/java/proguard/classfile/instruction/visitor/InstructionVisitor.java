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
package proguard.classfile.instruction.visitor;

import proguard.classfile.*;
import proguard.classfile.attribute.CodeAttribute;
import proguard.classfile.instruction.*;


/**
 * This interface specifies the methods for a visitor of
 * {@link Instruction} instances.
 *
 * @author Eric Lafortune
 */
public interface InstructionVisitor
{
    /**
     * Visits any Instruction instance. The more specific default implementations of
     * this interface delegate to this method.
     */
    default void visitAnyInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, Instruction instruction)
    {
        throw new UnsupportedOperationException(this.getClass().getName()+" does not support "+instruction.getClass().getName());
    }


    default void visitSimpleInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, SimpleInstruction simpleInstruction)
    {
        visitAnyInstruction(clazz, method, codeAttribute, offset, simpleInstruction);
    }


    default void visitVariableInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, VariableInstruction variableInstruction)
    {
        visitAnyInstruction(clazz, method, codeAttribute, offset, variableInstruction);
    }


    default void visitConstantInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, ConstantInstruction constantInstruction)
    {
        visitAnyInstruction(clazz, method, codeAttribute, offset, constantInstruction);
    }


    default void visitBranchInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, BranchInstruction branchInstruction)
    {
        visitAnyInstruction(clazz, method, codeAttribute, offset, branchInstruction);
    }



    /**
     * Visits any SwitchInstruction instance. The more specific default implementations of
     * this interface delegate to this method.
     */
    default void visitAnySwitchInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, SwitchInstruction switchInstruction)
    {
        visitAnyInstruction(clazz, method, codeAttribute, offset, switchInstruction);
    }


    default void visitTableSwitchInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, TableSwitchInstruction tableSwitchInstruction)
    {
        visitAnySwitchInstruction(clazz, method, codeAttribute, offset, tableSwitchInstruction);
    }


    default void visitLookUpSwitchInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, LookUpSwitchInstruction lookUpSwitchInstruction)
    {
        visitAnySwitchInstruction(clazz, method, codeAttribute, offset, lookUpSwitchInstruction);
    }
}
