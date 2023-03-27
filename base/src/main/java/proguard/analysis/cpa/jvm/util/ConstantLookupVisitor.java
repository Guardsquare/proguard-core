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

package proguard.analysis.cpa.jvm.util;

import proguard.classfile.Clazz;
import proguard.classfile.constant.ClassConstant;
import proguard.classfile.constant.FieldrefConstant;
import proguard.classfile.constant.visitor.ConstantVisitor;
import proguard.classfile.util.ClassUtil;

/**
 * This {@link ConstantVisitor} is used for field operations and the {@code instanceOf} predicate.
 * The {@link  #result} stores the fully qualified name for either a class or a field.
 * The {@link  #resultSize} is the size of the referenced type in bytes.
 *
 * @author Dmitry Ivanov
 */
public class ConstantLookupVisitor implements ConstantVisitor
{
    public String  result      = null;
    public Clazz   resultClazz = null;
    public int     resultSize  = -1;
    public boolean isStatic    = false;

    public void resetResult()
    {
        result      = null;
        resultClazz = null;
        resultSize  = -1;
    }

    // implementations for ConstantVisitor

    @Override
    public void visitFieldrefConstant(Clazz clazz, FieldrefConstant fieldrefConstant)
    {
        resultSize = ClassUtil.internalTypeSize(fieldrefConstant.getType(clazz));
        result = fieldrefConstant.getClassName(clazz)
                 + (isStatic ? "." : "#")
                 + fieldrefConstant.getName(clazz)
                 + ":"
                 + fieldrefConstant.getType(clazz);
    }

    @Override
    public void visitClassConstant(Clazz clazz, ClassConstant classConstant)
    {
        resultSize  = 1;
        result      = classConstant.getName(clazz);
        resultClazz = classConstant.referencedClass;
    }
}
