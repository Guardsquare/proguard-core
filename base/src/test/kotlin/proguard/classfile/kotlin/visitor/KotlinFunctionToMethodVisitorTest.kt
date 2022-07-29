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

package proguard.classfile.kotlin.visitor

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.mockk.spyk
import io.mockk.verify
import proguard.classfile.Clazz
import proguard.classfile.Member
import proguard.classfile.ProgramClass
import proguard.classfile.kotlin.KotlinFunctionMetadata
import proguard.classfile.kotlin.KotlinMetadata
import proguard.classfile.visitor.MemberVisitor
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.KotlinSource

class KotlinFunctionToMethodVisitorTest : FreeSpec({

    "Given a Kotlin function" - {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            KotlinSource("Test.kt", """fun foo() = "bar"""".trimIndent())
        )

        val memberVisitor = spyk<MemberVisitor>(object : MemberVisitor {
            override fun visitAnyMember(clazz: Clazz, member: Member) { }
        })

        val programClass = programClassPool.getClass("TestKt") as ProgramClass

        programClass.kotlinMetadataAccept(AllFunctionVisitor(KotlinFunctionToMethodVisitor(memberVisitor)))

        "Then a member visitor should visit the referenced method" {
            verify(exactly = 1) {
                memberVisitor.visitProgramMember(
                    programClass,
                    withArg {
                        it.getName(programClass) shouldBe "foo"
                        it.getDescriptor(programClass) shouldBe "()Ljava/lang/String;"
                    }
                )
            }
        }
    }

    "Given a Kotlin function with an uninitialized referenced method" - {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            KotlinSource("Test.kt", """fun foo() = "bar"""".trimIndent())
        )

        val programClass = programClassPool.getClass("TestKt") as ProgramClass

        programClass.kotlinMetadataAccept(
            AllFunctionVisitor(object : KotlinFunctionVisitor {
                override fun visitAnyFunction(clazz: Clazz, metadata: KotlinMetadata, func: KotlinFunctionMetadata) {
                    func.referencedMethod = null
                }
            })
        )

        "Then KotlinFunctionToMethodVisitor should not throw an exception" {
            val memberVisitor = spyk<MemberVisitor>(object : MemberVisitor {
                override fun visitAnyMember(clazz: Clazz, member: Member) { }
            })

            shouldNotThrowAny {
                programClass.kotlinMetadataAccept(
                    AllFunctionVisitor(KotlinFunctionToMethodVisitor(memberVisitor))
                )
            }
        }

        "Then a member visitor should not visit the referenced method" {
            val memberVisitor = spyk<MemberVisitor>(object : MemberVisitor {
                override fun visitAnyMember(clazz: Clazz, member: Member) { }
            })

            verify(exactly = 0) {
                memberVisitor.visitProgramMember(
                    programClass,
                    withArg {
                        it.getName(programClass) shouldBe "foo"
                        it.getDescriptor(programClass) shouldBe "()Ljava/lang/String;"
                    }
                )
            }
        }
    }
})
