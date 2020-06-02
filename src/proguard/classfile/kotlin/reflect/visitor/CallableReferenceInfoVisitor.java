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

package proguard.classfile.kotlin.reflect.visitor;

import proguard.classfile.kotlin.reflect.*;

/**
 * @author James Hamilton
 */
public interface CallableReferenceInfoVisitor
{
    void visitAnyCallableReferenceInfo(CallableReferenceInfo callableReferenceInfo);

    default void visitFunctionReferenceInfo(FunctionReferenceInfo functionReferenceInfo)
    {
        this.visitAnyCallableReferenceInfo(functionReferenceInfo);
    }

    default void visitJavaReferenceInfo(JavaReferenceInfo javaReferenceInfo)
    {
        this.visitAnyCallableReferenceInfo(javaReferenceInfo);
    }

    default void visitLocalVariableReferenceInfo(LocalVariableReferenceInfo localVariableReferenceInfo)
    {
        this.visitAnyCallableReferenceInfo(localVariableReferenceInfo);
    }

    default void visitPropertyReferenceInfo(PropertyReferenceInfo propertyReferenceInfo)
    {
        this.visitAnyCallableReferenceInfo(propertyReferenceInfo);
    }
}
