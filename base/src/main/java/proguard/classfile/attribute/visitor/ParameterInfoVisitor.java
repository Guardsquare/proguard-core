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

import proguard.classfile.*;
import proguard.classfile.attribute.ParameterInfo;

/**
 * This interface specifies the method for a visitor of
 * {@link ParameterInfo} instances. Note that there is only a single
 * implementation of {@link ParameterInfo}, such that this interface
 * is not strictly necessary as a visitor.
 *
 * @author Eric Lafortune
 */
public interface ParameterInfoVisitor
{
    public void visitParameterInfo(Clazz clazz, Method method, int parameterIndex, ParameterInfo parameterInfo);
}
