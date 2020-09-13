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
import proguard.classfile.constant.*;
import proguard.classfile.constant.visitor.ConstantVisitor;
import proguard.classfile.visitor.ClassVisitor;
import proguard.util.ArrayUtil;

/**
 * This {@link ConstantVisitor} and {@link ClassVisitor} adds the class constants or the
 * classes that it visits to the given target permitted classes attribute.
 */
public class PermittedSubclassAdder
implements   ConstantVisitor,
             ClassVisitor

{
    private final ConstantPoolEditor   constantPoolEditor;
    private final PermittedSubclassesAttribute targetPermittedSubclassesAttribute;


    /**
     * Creates a new PermittedSubclassAdder that will add classes to the
     * given target nest members attribute.
     */
    public PermittedSubclassAdder(ProgramClass                 targetClass,
                                  PermittedSubclassesAttribute targetPermittedSubclassesAttribute)
    {
        this.constantPoolEditor                 = new ConstantPoolEditor(targetClass);
        this.targetPermittedSubclassesAttribute = targetPermittedSubclassesAttribute;
    }


    // Implementations for ConstantVisitor.

    public void visitAnyConstant(Clazz clazz, Constant constant) {}

    public void visitClassConstant(Clazz clazz, ClassConstant classConstant)
    {
        targetPermittedSubclassesAttribute.u2classes =
            ArrayUtil.add(targetPermittedSubclassesAttribute.u2classes,
                          targetPermittedSubclassesAttribute.u2classesCount++,
                          constantPoolEditor.addClassConstant(classConstant.getName(clazz),
                                                              classConstant.referencedClass));
    }


    // Implementations for ClassVisitor.

    @Override
    public void visitAnyClass(Clazz clazz)
    {
        targetPermittedSubclassesAttribute.u2classes =
            ArrayUtil.add(targetPermittedSubclassesAttribute.u2classes,
                          targetPermittedSubclassesAttribute.u2classesCount++,
                          constantPoolEditor.addClassConstant(clazz));
    }
}
