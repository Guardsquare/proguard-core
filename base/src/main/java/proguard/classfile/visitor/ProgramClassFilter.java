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
package proguard.classfile.visitor;

import proguard.classfile.*;


/**
 * This {@link ClassVisitor} delegates its visits to another given
 * {@link ClassVisitor}, but only when visiting program classes.
 *
 * @author Eric Lafortune
 */
public class ProgramClassFilter
implements   ClassVisitor
{
    private final ClassVisitor classVisitor;


    /**
     * Creates a new ProgramClassFilter.
     * @param classVisitor     the <code>ClassVisitor</code> to which visits
     *                         will be delegated.
     */
    public ProgramClassFilter(ClassVisitor classVisitor)
    {
        this.classVisitor = classVisitor;
    }


    // Implementations for ClassVisitor.

    @Override
    public void visitAnyClass(Clazz clazz) { }


    @Override
    public void visitProgramClass(ProgramClass programClass)
    {
        classVisitor.visitProgramClass(programClass);
    }
}
