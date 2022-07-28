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
package proguard.classfile.editor;

import proguard.classfile.*;
import proguard.classfile.visitor.ClassVisitor;

/**
 * This {@link ClassVisitor} sorts the various elements of the classes that it visits:
 * interfaces, constants, fields, methods, and attributes.
 *
 * @author Eric Lafortune
 */
public class ClassElementSorter
implements   ClassVisitor
{
    private final ClassVisitor interfaceSorter    = new InterfaceSorter();
    private final ClassVisitor constantPoolSorter = new ConstantPoolSorter();
    private final ClassVisitor classMemberSorter  = new ClassMemberSorter();
    private final ClassVisitor attributeSorter    = new AttributeSorter();

    private boolean sortInterfaces;
    private boolean sortConstants;
    private boolean sortMembers;
    private boolean sortAttributes;

    /**
     * Creates a default `ClassElementSorter` that sorts interfaces, constants and attributes.
     */
    public ClassElementSorter()
    {
        this(true, true, false, true);
    }

    public ClassElementSorter(boolean sortInterfaces, boolean sortConstants, boolean sortMembers, boolean sortAttributes)
    {
        this.sortInterfaces = sortInterfaces;
        this.sortConstants  = sortConstants;
        this.sortMembers    = sortMembers;
        this.sortAttributes = sortAttributes;
    }

    // Implementations for ClassVisitor.

    @Override
    public void visitAnyClass(Clazz clazz)
    {
    }

    @Override
    public void visitProgramClass(ProgramClass programClass)
    {
        if (sortConstants)  programClass.accept(constantPoolSorter);
        if (sortInterfaces) programClass.accept(interfaceSorter);
        if (sortMembers)    programClass.accept(classMemberSorter);
        if (sortAttributes) programClass.accept(attributeSorter);
    }
}
