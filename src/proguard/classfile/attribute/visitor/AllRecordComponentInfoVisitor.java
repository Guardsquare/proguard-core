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
package proguard.classfile.attribute.visitor;

import proguard.classfile.Clazz;
import proguard.classfile.attribute.*;

/**
 * This {@link AttributeVisitor} lets a given {@link RecordComponentInfoVisitor} visit all
 * {@link RecordComponentInfo} instances of the {@link RecordAttribute} instances it visits.
 *
 * @author Eric Lafortune
 */
public class AllRecordComponentInfoVisitor
implements   AttributeVisitor
{
    private final RecordComponentInfoVisitor recordComponentInfoVisitor;


    public AllRecordComponentInfoVisitor(RecordComponentInfoVisitor recordComponentInfoVisitor)
    {
        this.recordComponentInfoVisitor = recordComponentInfoVisitor;
    }


    // Implementations for AttributeVisitor.

    public void visitAnyAttribute(Clazz clazz, Attribute attribute) {}


    public void visitRecordAttribute(Clazz clazz, RecordAttribute recordAttribute)
    {
        recordAttribute.componentsAccept(clazz, recordComponentInfoVisitor);
    }
}
