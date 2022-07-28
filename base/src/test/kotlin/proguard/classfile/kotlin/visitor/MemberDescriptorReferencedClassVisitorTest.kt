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

package proguard.classfile.kotlin.visitor

import io.kotest.core.spec.style.FreeSpec
import io.mockk.spyk
import io.mockk.verify
import proguard.classfile.visitor.AllMemberVisitor
import proguard.classfile.visitor.ClassVisitor
import proguard.classfile.visitor.MemberDescriptorReferencedClassVisitor
import testutils.ClassPoolBuilder
import testutils.KotlinSource

class MemberDescriptorReferencedClassVisitorTest : FreeSpec({
    "Given an inline class" - {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            KotlinSource(
                "Test.kt",
                """
                @JvmInline
                value class Password(val s: String)
                
                fun login(password: Password) {
                    println(password);
                }
                """.trimIndent()
            )
        )

        "Then visiting the referenced descriptor methods of login with includeKotlinMetadata true" - {
            val visitor = spyk<ClassVisitor>()
            programClassPool.classesAccept(
                "TestKt",
                AllMemberVisitor(MemberDescriptorReferencedClassVisitor(true, visitor))
            )

            "Should visit the Password class" {
                verify(exactly = 1) {
                    visitor.visitAnyClass(programClassPool.getClass("Password"))
                }
            }
        }

        "Then visiting the referenced descriptor methods of login with includeKotlinMetadata false" - {
            val visitor = spyk<ClassVisitor>()
            programClassPool.classesAccept(
                "TestKt",
                AllMemberVisitor(MemberDescriptorReferencedClassVisitor(false, visitor))
            )

            "Should not visit the Password class" {
                verify(exactly = 0) {
                    visitor.visitAnyClass(programClassPool.getClass("Password"))
                }
            }
        }
    }
})
