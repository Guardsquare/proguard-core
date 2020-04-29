/*
 * ProGuard Core -- library to process Java bytecode.
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
import proguard.classfile.attribute.visitor.AttributeVisitor;

/**
 * This {@link AttributeVisitor} resets it {@link CodeAttributeEditor} whenever it visits a
 * code attribute.
 *
 * @author Eric Lafortune
 */
public class CodeAttributeEditorResetter
implements   AttributeVisitor
{
    private final CodeAttributeEditor codeAttributeEditor;


    /**
     * Creates a new CodeAttributeEditorResetter.
     * @param codeAttributeEditor the code attribute editor that will be reset.
     */
    public CodeAttributeEditorResetter(CodeAttributeEditor codeAttributeEditor)
    {
        this.codeAttributeEditor = codeAttributeEditor;
    }


    // Implementations for AttributeVisitor.

    public void visitAnyAttribute(Clazz clazz, Attribute attribute) {}


    public void visitCodeAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute)
    {
        codeAttributeEditor.reset(codeAttribute.u4codeLength);
    }
}
