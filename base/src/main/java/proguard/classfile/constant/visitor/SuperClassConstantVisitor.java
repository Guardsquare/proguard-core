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
package proguard.classfile.constant.visitor;

import proguard.classfile.*;
import proguard.classfile.visitor.ClassVisitor;

/**
 * This {@link ClassVisitor} lets a given {@link ConstantVisitor} visit all the constant pool
 * entries of the super class and interfaces of the program classes it visits.
 *
 * @author Eric Lafortune
 */
public class SuperClassConstantVisitor implements ClassVisitor
{
    private final boolean         visitSuperClassConstants;
    private final boolean         visitInterfaceConstants;
    private final ConstantVisitor constantVisitor;


    /**
     * Creates a new SuperClassConstantVisitor.
     */
    public SuperClassConstantVisitor(boolean         visitSuperClassConstants,
                                     boolean         visitInterfaceConstants,
                                     ConstantVisitor constantVisitor)
    {
        this.visitSuperClassConstants = visitSuperClassConstants;
        this.visitInterfaceConstants  = visitInterfaceConstants;
        this.constantVisitor          = constantVisitor;
    }


    // Implementations for ClassVisitor.

    @Override
    public void visitAnyClass(Clazz clazz) { }


    @Override
    public void visitProgramClass(ProgramClass programClass)
    {
        if (visitSuperClassConstants)
        {
            programClass.superClassConstantAccept(constantVisitor);
        }

        if (visitInterfaceConstants)
        {
            programClass.interfaceConstantsAccept(constantVisitor);
        }
    }
}
