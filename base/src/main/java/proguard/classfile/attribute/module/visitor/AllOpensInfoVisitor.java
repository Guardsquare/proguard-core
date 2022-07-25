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
package proguard.classfile.attribute.module.visitor;

import proguard.classfile.Clazz;
import proguard.classfile.attribute.Attribute;
import proguard.classfile.attribute.module.ModuleAttribute;
import proguard.classfile.attribute.visitor.AttributeVisitor;

/**
 * This {@link AttributeVisitor} lets a given {@link OpensInfoVisitor} visit all
 * {@link OpensInfo} instances of the {@link ModuleAttribute} instances it visits.
 *
 * @author Joachim Vandersmissen
 */
public class AllOpensInfoVisitor
implements   AttributeVisitor
{
    private final OpensInfoVisitor opensInfoVisitor;


    public AllOpensInfoVisitor(OpensInfoVisitor opensInfoVisitor)
    {
        this.opensInfoVisitor = opensInfoVisitor;
    }


    // Implementations for AttributeVisitor.

    public void visitAnyAttribute(Clazz clazz, Attribute attribute) {}


    public void visitModuleAttribute(Clazz clazz, ModuleAttribute moduleAttribute)
    {
        moduleAttribute.opensAccept(clazz, opensInfoVisitor);
    }
}
