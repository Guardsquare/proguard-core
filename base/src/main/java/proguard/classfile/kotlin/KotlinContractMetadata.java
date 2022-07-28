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
package proguard.classfile.kotlin;

import proguard.classfile.*;
import proguard.classfile.kotlin.visitor.*;
import proguard.util.*;

import java.util.*;


public class KotlinContractMetadata
extends      SimpleProcessable
implements   Processable
{
    public List<KotlinEffectMetadata> effects;


    public void accept(Clazz                  clazz,
                       KotlinMetadata         kotlinMetadata,
                       KotlinFunctionMetadata kotlinFunctionMetadata,
                       KotlinContractVisitor  kotlinContractVisitor)
    {
        kotlinContractVisitor.visitContract(clazz,
                                            kotlinMetadata,
                                            kotlinFunctionMetadata,
                                            this);
    }


    public void effectsAccept(Clazz                 clazz,
                             KotlinMetadata         kotlinMetadata,
                             KotlinFunctionMetadata kotlinFunctionMetadata,
                             KotlinEffectVisitor    kotlinEffectVisitor)
    {
        for (KotlinEffectMetadata effect : effects)
        {
            effect.accept(clazz, kotlinMetadata, kotlinFunctionMetadata, this, kotlinEffectVisitor);
        }
    }


    // Implementations for Object.
    @Override
    public String toString()
    {
        return "Kotlin contract";
    }
}
