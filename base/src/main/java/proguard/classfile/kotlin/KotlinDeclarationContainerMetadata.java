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

import proguard.classfile.Clazz;
import proguard.classfile.kotlin.visitor.*;
import proguard.resources.kotlinmodule.KotlinModule;
import proguard.resources.kotlinmodule.visitor.KotlinModuleVisitor;

import java.util.*;

/**
 * This class is named after Kotlin's own naming scheme. A declaration container is a type that
 * can define functions, properties and delegated properties, and that can also define type aliases.
 */
public abstract class KotlinDeclarationContainerMetadata
extends KotlinMetadata
{
    public List<KotlinPropertyMetadata> properties;

    public List<KotlinFunctionMetadata> functions;

    public List<KotlinTypeAliasMetadata> typeAliases;

    public String ownerClassName;
    public Clazz  ownerReferencedClass;

    // Extensions.
    public List<KotlinPropertyMetadata> localDelegatedProperties;

    // A reference to the module that this declaration container was defined in.
    public KotlinModule referencedModule;

    public KotlinDeclarationContainerMetadata(int    k,
                                              int[]  mv,
                                              int    xi,
                                              String xs,
                                              String pn)
    {
        super(k, mv, xi, xs, pn);
    }


    public void propertiesAccept(Clazz clazz, KotlinPropertyVisitor kotlinPropertyVisitor)
    {
        for (KotlinPropertyMetadata property : properties)
        {
            property.accept(clazz, this, kotlinPropertyVisitor);
        }
    }


    public void delegatedPropertiesAccept(Clazz clazz, KotlinPropertyVisitor kotlinPropertyVisitor)
    {
        for (KotlinPropertyMetadata localDelegatedProperty : localDelegatedProperties)
        {
            localDelegatedProperty.acceptAsDelegated(clazz, this, kotlinPropertyVisitor);
        }
    }


    public void functionsAccept(Clazz clazz, KotlinFunctionVisitor kotlinFunctionVisitor)
    {
        for (KotlinFunctionMetadata function : functions)
        {
            function.accept(clazz, this, kotlinFunctionVisitor);
        }
    }


    public void typeAliasesAccept(Clazz clazz, KotlinTypeAliasVisitor kotlinTypeAliasVisitor)
    {
        for (KotlinTypeAliasMetadata typeAlias : typeAliases)
        {
            typeAlias.accept(clazz, this, kotlinTypeAliasVisitor);
        }
    }

    public void moduleAccept(KotlinModuleVisitor kotlinModuleVisitor)
    {
        if (this.referencedModule != null)
        {
            this.referencedModule.accept(kotlinModuleVisitor);
        }
    }
}
