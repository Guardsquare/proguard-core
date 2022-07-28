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
package proguard.classfile.attribute.visitor;

import proguard.classfile.Clazz;
import proguard.classfile.constant.ClassConstant;
import proguard.classfile.constant.Constant;
import proguard.classfile.constant.visitor.ConstantVisitor;
import proguard.classfile.visitor.ClassVisitor;
import proguard.classfile.visitor.MemberVisitor;

/**
 * This {@link ConstantVisitor} lets a given {@link ClassVisitor} visit the classes that are referenced by the visited
 * class constants.
 *
 * @author Joren Van Hecke
 */
public class ClassConstantToClassVisitor implements ConstantVisitor
{

    private final ClassVisitor classVisitor;

    public ClassConstantToClassVisitor(ClassVisitor classVisitor)
    {
        this.classVisitor = classVisitor;
    }

    public void visitAnyConstant(Clazz clazz, Constant constant) {}

    @Override
    public void visitClassConstant(Clazz clazz, ClassConstant classConstant)
    {
        if (this.classVisitor != null && classConstant.referencedClass != null)
        {
            classConstant.referencedClass.accept(this.classVisitor);
        }
    }
}
