/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2021 Guardsquare NV
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

package proguard.classfile

import io.kotest.core.spec.style.FreeSpec
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import proguard.classfile.AccessConstants.PUBLIC
import proguard.classfile.visitor.ClassVisitor
import proguard.util.StringMatcher

class ClassPoolTest : FreeSpec({
    "Given a ClassPool filled with classes" - {
        val classPool = ClassPool(
            setOf(
                LibraryClass(
                    PUBLIC,
                    "proguard/Classfile",
                    "java/lang/Object"
                ),
                LibraryClass(
                    PUBLIC,
                    "proguard/classfile/ClassMemberPair",
                    "java/lang/Object"
                ),
                LibraryClass(
                    PUBLIC,
                    "proguard/classfile/visitor/InjectedClassFilter",
                    "java/lang/Object"
                ),
                LibraryClass(
                    PUBLIC,
                    "proguard/DescriptorKeeper",
                    "java/lang/Object"
                ),
                LibraryClass(
                    PUBLIC,
                    "proguard/Targeter",
                    "java/lang/Object"
                )
            )
        )

        "When a ClassVisitor is applied to all matching classes in the class pool" - {
            val stringMatcher = mockk<StringMatcher>()
            val stringMatcherPrefix = "proguard/classfile/"
            every { stringMatcher.matches(any()) } answers {
                val string = firstArg<String>()
                string.startsWith(stringMatcherPrefix)
            }
            every { stringMatcher.prefix() } returns stringMatcherPrefix

            val visitor = mockk<ClassVisitor>()
            justRun { visitor.visitLibraryClass(any()) }

            classPool.classesAccept(stringMatcher, visitor)

            "Then each matching class must have been visited once" {
                verify(exactly = 1) {
                    classPool.getClass("proguard/classfile/ClassMemberPair").accept(visitor)
                    classPool.getClass("proguard/classfile/visitor/InjectedClassFilter").accept(visitor)
                }
            }

            "Then each non-matching class must not have been visited" {
                verify(exactly = 0) {
                    classPool.getClass("proguard/Classfile").accept(visitor)
                    classPool.getClass("proguard/DescriptorKeeper").accept(visitor)
                    classPool.getClass("proguard/Targeter").accept(visitor)
                }
            }
        }
    }
})
