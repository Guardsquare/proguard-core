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
package proguard.classfile.visitor;

import proguard.classfile.*;
import proguard.classfile.attribute.*;
import proguard.classfile.attribute.visitor.ExceptionInfoVisitor;
import proguard.classfile.constant.visitor.ConstantVisitor;

/**
 * This {@link ExceptionInfoVisitor} lets a given
 * {@link ConstantVisitor} visit all catch class constants of exceptions
 * that it visits.
 *
 * @author Eric Lafortune
 */
public class ExceptionHandlerConstantVisitor
implements   ExceptionInfoVisitor
{
    private final ConstantVisitor constantVisitor;


    /**
     * Creates a new ExceptionHandlerConstantVisitor.
     * @param constantVisitor the ConstantVisitor that will visit the catch
     *                        class constants.
     */
    public ExceptionHandlerConstantVisitor(ConstantVisitor constantVisitor)
    {
        this.constantVisitor = constantVisitor;
    }


    // Implementations for ExceptionInfoVisitor.

    public void visitExceptionInfo(Clazz clazz, Method method, CodeAttribute codeAttribute, ExceptionInfo exceptionInfo)
    {
        int catchType = exceptionInfo.u2catchType;
        if (catchType != 0)
        {
            clazz.constantPoolEntryAccept(catchType, constantVisitor);
        }
    }
}
