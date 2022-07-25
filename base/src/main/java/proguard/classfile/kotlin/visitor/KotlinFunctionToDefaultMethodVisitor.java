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
package proguard.classfile.kotlin.visitor;

import proguard.classfile.Clazz;
import proguard.classfile.kotlin.*;
import proguard.classfile.visitor.MemberVisitor;

/**
 * This class applies the given member visitor to the referenced default method of a Kotlin function.
 */
public class KotlinFunctionToDefaultMethodVisitor
implements   KotlinFunctionVisitor
{
    private final MemberVisitor memberVisitor;

    public KotlinFunctionToDefaultMethodVisitor(MemberVisitor memberVisitor) {
        this.memberVisitor = memberVisitor;
    }

    @Override
    public void visitAnyFunction(Clazz                  clazz,
                                 KotlinMetadata         kotlinMetadata,
                                 KotlinFunctionMetadata kotlinFunctionMetadata)
    {
        if (kotlinFunctionMetadata.referencedDefaultMethod != null)
        {
            kotlinFunctionMetadata.referencedDefaultMethod.accept(
                kotlinFunctionMetadata.referencedDefaultMethodClass, memberVisitor);
        }
    }
}
