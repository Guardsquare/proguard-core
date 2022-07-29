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

package proguard.classfile.kotlin

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.spyk
import io.mockk.verify
import proguard.classfile.ProgramClass
import proguard.classfile.editor.ClassReferenceFixer
import proguard.classfile.kotlin.visitor.AllPropertyVisitor
import proguard.classfile.kotlin.visitor.KotlinPropertyVisitor
import proguard.classfile.kotlin.visitor.ReferencedKotlinMetadataVisitor
import proguard.classfile.kotlin.visitor.filter.KotlinPropertyFilter
import proguard.classfile.util.ClassReferenceInitializer
import proguard.classfile.util.ClassRenamer
import proguard.classfile.util.MemberFinder
import proguard.classfile.visitor.AllMemberVisitor
import proguard.classfile.visitor.MemberNameFilter
import proguard.classfile.visitor.MultiClassVisitor
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.KotlinSource
import proguard.testutils.ReWritingMetadataVisitor

class KotlinSyntheticDelegateTest : FreeSpec({
    "Given a property delegate" - {
        val (programClassPool, libraryClassPool) = ClassPoolBuilder.fromSource(
            KotlinSource(
                "Test.kt",
                """
                var x = 1
                var y by ::x
                """.trimIndent()
            )
        )

        val testKt = programClassPool.getClass("TestKt") as ProgramClass
        val memberFinder = MemberFinder()
        val delegateMethod = memberFinder.findMethod(testKt, "getY\$delegate", null)

        "Then a \$delegate method should be created" {
            delegateMethod shouldNotBe null
        }

        "Then the syntheticMethodForDelegate field should be correct" {
            val visitor = spyk<KotlinPropertyVisitor>()
            programClassPool.classesAccept(
                ReferencedKotlinMetadataVisitor(
                    AllPropertyVisitor(KotlinPropertyFilter({ prop -> prop.name == "y" }, visitor))
                )
            )

            verify(exactly = 1) {
                visitor.visitAnyProperty(
                    testKt,
                    ofType<KotlinDeclarationContainerMetadata>(),
                    withArg {
                        it.syntheticMethodForDelegate?.method shouldBe "getY\$delegate"
                        it.syntheticMethodForDelegate?.descriptor.toString() shouldBe "()Ljava/lang/Object;"
                    }
                )
            }
        }

        "Then the syntheticMethodForDelegate references should be initialized" {
            val visitor = spyk<KotlinPropertyVisitor>()
            programClassPool.classesAccept(
                ReferencedKotlinMetadataVisitor(
                    AllPropertyVisitor(KotlinPropertyFilter({ prop -> prop.name == "y" }, visitor))
                )
            )

            verify(exactly = 1) {
                visitor.visitAnyProperty(
                    testKt,
                    ofType<KotlinDeclarationContainerMetadata>(),
                    withArg {
                        it.referencedSyntheticMethodForDelegateClass shouldBe testKt
                        it.referencedSyntheticMethodForDelegateMethod shouldBe delegateMethod
                    }
                )
            }
        }

        "Then re-writing the metadata should preserve the field" {
            val visitor = spyk<KotlinPropertyVisitor>()
            programClassPool.classesAccept(
                MultiClassVisitor(
                    ReWritingMetadataVisitor(
                        AllPropertyVisitor(KotlinPropertyFilter({ prop -> prop.name == "y" }, visitor))
                    ),
                    ClassReferenceInitializer(programClassPool, libraryClassPool)
                )
            )

            verify(exactly = 1) {
                visitor.visitAnyProperty(
                    testKt,
                    ofType<KotlinDeclarationContainerMetadata>(),
                    withArg {
                        it.syntheticMethodForDelegate?.method shouldBe "getY\$delegate"
                        it.syntheticMethodForDelegate?.descriptor.toString() shouldBe "()Ljava/lang/Object;"
                    }
                )
            }
        }

        "Then renaming the delegate method should update the field" {
            val visitor = spyk<KotlinPropertyVisitor>()

            programClassPool.classesAccept(
                MultiClassVisitor(
                    AllMemberVisitor(
                        MemberNameFilter(
                            "getY\$delegate",
                            ClassRenamer({ it.name }) { _, _ ->
                                "obfuscated"
                            }
                        )
                    ),
                    ClassReferenceFixer(false),
                    ReWritingMetadataVisitor(
                        AllPropertyVisitor(KotlinPropertyFilter({ prop -> prop.name == "y" }, visitor))
                    ),
                )
            )

            memberFinder.findMethod(testKt, "obfuscated", null) shouldNotBe null

            verify(exactly = 1) {
                visitor.visitAnyProperty(
                    testKt,
                    ofType<KotlinDeclarationContainerMetadata>(),
                    withArg {
                        it.syntheticMethodForDelegate?.method shouldBe "obfuscated"
                        it.syntheticMethodForDelegate?.descriptor.toString() shouldBe "()Ljava/lang/Object;"
                    }
                )
            }
        }
    }
})
