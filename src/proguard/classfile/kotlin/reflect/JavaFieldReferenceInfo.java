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

package proguard.classfile.kotlin.reflect;

import proguard.classfile.*;

import static proguard.classfile.util.kotlin.KotlinNameUtil.generateGetterName;

/**
 * @author James Hamilton
 */
public class JavaFieldReferenceInfo
extends      JavaReferenceInfo
{
    public JavaFieldReferenceInfo(Clazz ownerClass, Clazz clazz, Member member)
    {
        super(ownerClass, clazz, member);
    }

    /**
     * If there is no getter method then the signature is the imaginary default getter that
     * would be generated otherwise e.g. "myProperty" -> "getMyProperty".
     *
     * @return the signature.
     */
    @Override
    public String getSignature()
    {
        return generateGetterName(this.getName()) + "()" + this.member.getDescriptor(this.clazz);
    }
}
