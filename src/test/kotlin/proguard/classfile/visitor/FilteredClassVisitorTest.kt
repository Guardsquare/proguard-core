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

package proguard.classfile.visitor

import io.kotest.assertions.any
import io.kotest.core.spec.style.FreeSpec
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import proguard.classfile.ClassPool
import proguard.util.StringMatcher

class FilteredClassVisitorTest : FreeSpec({
    "Given a FilteredClassVisitor with a non-null filter" - {
        val visitor = mockk<ClassVisitor>()
        val filteredClassVisitor = FilteredClassVisitor("proguard/classfile/", visitor)
        "When the FilteredClassVisitor visits a ClassPool" - {
            val classPool = mockk<ClassPool>()
            justRun { classPool.classesAccept(any<StringMatcher>(), visitor) }

            filteredClassVisitor.visitClassPool(classPool)

            "Then the ClassPool accepts the visitor and filter" {
                verify(exactly = 1) {
                    classPool.classesAccept(any<StringMatcher>(), visitor)
                }
            }
        }
    }

    "Given a FilteredClassVisitor with a string matcher" - {
        val visitor = mockk<ClassVisitor>()
        val alwaysMatchingMatcher = mockk<StringMatcher>()
        every { alwaysMatchingMatcher.matches(any()) } returns true
        every { alwaysMatchingMatcher.prefix() } returns ""

        val filteredClassVisitor = FilteredClassVisitor(alwaysMatchingMatcher, visitor)
        "When the FilteredClassVisitor visits a ClassPool" - {
            val classPool = mockk<ClassPool>()
            every { classPool.classesAccept(alwaysMatchingMatcher, visitor) } answers {
                alwaysMatchingMatcher.prefix()
            }

            filteredClassVisitor.visitClassPool(classPool)

            "Then the matcher's prefix is checked at least once" {
                verify(atLeast = 1) {
                    alwaysMatchingMatcher.prefix()
                }
            }
        }
    }
})
