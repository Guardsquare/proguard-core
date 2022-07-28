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
package proguard.classfile.editor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import proguard.classfile.Clazz;
import proguard.classfile.attribute.Attribute;
import proguard.classfile.attribute.InnerClassesAttribute;
import proguard.classfile.attribute.InnerClassesInfo;
import proguard.classfile.attribute.visitor.AttributeVisitor;
import proguard.classfile.attribute.visitor.InnerClassesInfoVisitor;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * This {@link AttributeVisitor} and {@link InnerClassesInfoVisitor} removes a given {@link Clazz} from all visited
 * {@link InnerClassesAttribute}s.
 *
 * @author Joren Van Hecke
 */
public class InnerClassRemover implements AttributeVisitor, InnerClassesInfoVisitor
{
    private final Clazz classToBeRemoved;
    private final Set<InnerClassesInfo> innerClassesEntriesToBeRemoved = new HashSet<>();
    private static final Logger logger = LogManager.getLogger(InnerClassRemover.class);

    public InnerClassRemover(Clazz clazz)
    {
        this.classToBeRemoved = clazz;
    }

    @Override
    public void visitAnyAttribute(Clazz clazz, Attribute attribute) {}

    @Override
    public void visitInnerClassesAttribute(Clazz clazz, InnerClassesAttribute innerClassesAttribute)
    {
        innerClassesAttribute.innerClassEntriesAccept(clazz, this);
        InnerClassesAttributeEditor editor = new InnerClassesAttributeEditor(innerClassesAttribute);
        logger.trace("{} inner class entries are removed from class {}",
                innerClassesEntriesToBeRemoved.size(), clazz);
        for (InnerClassesInfo entry : innerClassesEntriesToBeRemoved)
        {
            editor.removeInnerClassesInfo(entry);
        }
    }

    @Override
    public void visitInnerClassesInfo(Clazz clazz, InnerClassesInfo innerClassesInfo)
    {
        String innerClassName = clazz.getClassName(innerClassesInfo.u2innerClassIndex);
        if (Objects.equals(innerClassName, this.classToBeRemoved.getName()))
        {
            logger.trace("Removing inner classes entry of class {} enqueued to be removed from class {}",
                    innerClassName, clazz);
            innerClassesEntriesToBeRemoved.add(innerClassesInfo);
        }
    }
}
