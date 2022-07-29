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

package proguard.testutils.cpa

import proguard.classfile.Clazz
import proguard.classfile.ProgramClass
import proguard.classfile.ProgramMethod
import proguard.classfile.attribute.visitor.AttributeVisitor
import proguard.classfile.visitor.ClassVisitor
import proguard.classfile.visitor.MemberVisitor

class NamedMember(val memberName: String, val descriptor: String) : ProgramMethod() {

    override fun getName(clazz: Clazz?): String {
        return memberName
    }

    override fun getDescriptor(clazz: Clazz?): String {
        return descriptor
    }

    override fun accept(programClass: ProgramClass?, memberVisitor: MemberVisitor?) {
    }

    override fun referencedClassesAccept(classVisitor: ClassVisitor?) {
    }

    override fun attributesAccept(programClass: ProgramClass?, attributeVisitor: AttributeVisitor?) {
    }
}
