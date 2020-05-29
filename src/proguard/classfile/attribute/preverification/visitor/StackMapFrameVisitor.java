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
package proguard.classfile.attribute.preverification.visitor;

import proguard.classfile.*;
import proguard.classfile.attribute.CodeAttribute;
import proguard.classfile.attribute.preverification.*;

/**
 * This interface specifies the methods for a visitor of
 * {@link StackMapFrame} instances.
 *
 * @author Eric Lafortune
 */
public interface StackMapFrameVisitor
{
    /**
     * Visits any StackMapFrame instance. The more specific default implementations of
     * this interface delegate to this method.
     */
    default void visitAnyStackMapFrame(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, StackMapFrame stackMapFrame)
    {
        throw new UnsupportedOperationException(this.getClass().getName()+" does not support "+stackMapFrame.getClass().getName());
    }


    default void visitSameZeroFrame(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, SameZeroFrame sameZeroFrame)
    {
        visitAnyStackMapFrame(clazz, method, codeAttribute, offset, sameZeroFrame);
    }


    default void visitSameOneFrame(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, SameOneFrame sameOneFrame)
    {
        visitAnyStackMapFrame(clazz, method, codeAttribute, offset, sameOneFrame);
    }


    default void visitLessZeroFrame(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, LessZeroFrame lessZeroFrame)
    {
        visitAnyStackMapFrame(clazz, method, codeAttribute, offset, lessZeroFrame);
    }


    default void visitMoreZeroFrame(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, MoreZeroFrame moreZeroFrame)
    {
        visitAnyStackMapFrame(clazz, method, codeAttribute, offset, moreZeroFrame);
    }


    default void visitFullFrame(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, FullFrame fullFrame)
    {
        visitAnyStackMapFrame(clazz, method, codeAttribute, offset, fullFrame);
    }
}
