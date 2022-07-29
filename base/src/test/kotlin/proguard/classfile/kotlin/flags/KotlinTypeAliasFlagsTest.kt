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

package proguard.classfile.kotlin.flags

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.mockk.spyk
import io.mockk.verify
import proguard.classfile.kotlin.KotlinDeclarationContainerMetadata
import proguard.classfile.kotlin.visitor.AllTypeAliasVisitor
import proguard.classfile.kotlin.visitor.KotlinTypeAliasVisitor
import proguard.classfile.kotlin.visitor.ReferencedKotlinMetadataVisitor
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.KotlinSource
import proguard.testutils.ReWritingMetadataVisitor

class KotlinTypeAliasFlagsTest : FreeSpec({

    "Given a public type alias without annotation" - {
        val clazz = ClassPoolBuilder.fromSource(
            KotlinSource("Test.kt", "typealias privateAlias = String")
        ).programClassPool.getClass("TestKt")

        "Then the flags should be initialized correctly" {
            val typeAliasVisitor = spyk<KotlinTypeAliasVisitor>()
            clazz.kotlinMetadataAccept(AllTypeAliasVisitor(typeAliasVisitor))

            verify {
                typeAliasVisitor.visitTypeAlias(
                    clazz,
                    ofType(KotlinDeclarationContainerMetadata::class),
                    withArg {
                        it.flags.visibility.isPublic shouldBe true
                        it.flags.visibility.isPrivate shouldBe false
                        it.flags.visibility.isProtected shouldBe false
                        it.flags.visibility.isPrivateToThis shouldBe false
                        it.flags.visibility.isInternal shouldBe false
                        it.flags.visibility.isLocal shouldBe false

                        it.flags.common.hasAnnotations shouldBe false
                    }
                )
            }
        }

        "Then the flags should be written and re-initialized correctly" {
            val typeAliasVisitor = spyk<KotlinTypeAliasVisitor>()
            clazz.accept(ReWritingMetadataVisitor(AllTypeAliasVisitor(typeAliasVisitor)))

            verify {
                typeAliasVisitor.visitTypeAlias(
                    clazz,
                    ofType(KotlinDeclarationContainerMetadata::class),
                    withArg {
                        it.flags.visibility.isPublic shouldBe true
                        it.flags.visibility.isPrivate shouldBe false
                        it.flags.visibility.isProtected shouldBe false
                        it.flags.visibility.isPrivateToThis shouldBe false
                        it.flags.visibility.isInternal shouldBe false
                        it.flags.visibility.isLocal shouldBe false

                        it.flags.common.hasAnnotations shouldBe false
                    }
                )
            }
        }
    }

    "Given a private type alias without annotation" - {
        val clazz = ClassPoolBuilder.fromSource(
            KotlinSource("Test.kt", "private typealias privateAlias = String")
        ).programClassPool.getClass("TestKt")

        "Then the flags should be initialized correctly" {
            val typeAliasVisitor = spyk<KotlinTypeAliasVisitor>()
            clazz.kotlinMetadataAccept(AllTypeAliasVisitor(typeAliasVisitor))

            verify {
                typeAliasVisitor.visitTypeAlias(
                    clazz,
                    ofType(KotlinDeclarationContainerMetadata::class),
                    withArg {
                        it.flags.visibility.isPublic shouldBe false
                        it.flags.visibility.isPrivate shouldBe true
                        it.flags.visibility.isProtected shouldBe false
                        it.flags.visibility.isPrivateToThis shouldBe false
                        it.flags.visibility.isInternal shouldBe false
                        it.flags.visibility.isLocal shouldBe false

                        it.flags.common.hasAnnotations shouldBe false
                    }
                )
            }
        }

        "Then the flags should be written and re-initialized correctly" {
            val typeAliasVisitor = spyk<KotlinTypeAliasVisitor>()
            clazz.accept(ReWritingMetadataVisitor(AllTypeAliasVisitor(typeAliasVisitor)))

            verify {
                typeAliasVisitor.visitTypeAlias(
                    clazz,
                    ofType(KotlinDeclarationContainerMetadata::class),
                    withArg {
                        it.flags.visibility.isPublic shouldBe false
                        it.flags.visibility.isPrivate shouldBe true
                        it.flags.visibility.isProtected shouldBe false
                        it.flags.visibility.isPrivateToThis shouldBe false
                        it.flags.visibility.isInternal shouldBe false
                        it.flags.visibility.isLocal shouldBe false

                        it.flags.common.hasAnnotations shouldBe false
                    }
                )
            }
        }
    }

    "Given an annotated type alias" - {
        val clazz = ClassPoolBuilder.fromSource(
            KotlinSource(
                "Test.kt",
                """
                @Target(AnnotationTarget.TYPEALIAS)
                annotation class MyTypeAliasAnnotation
                @MyTypeAliasAnnotation
                typealias privateAlias = String
                """.trimIndent()
            )
        ).programClassPool.getClass("TestKt")

        "Then the hasAnnotation common flag should be initialized correctly" {
            val typeAliasVisitor = spyk<KotlinTypeAliasVisitor>()
            clazz.accept(ReferencedKotlinMetadataVisitor(AllTypeAliasVisitor(typeAliasVisitor)))

            verify {
                typeAliasVisitor.visitTypeAlias(
                    clazz,
                    ofType(KotlinDeclarationContainerMetadata::class),
                    withArg {
                        it.flags.common.hasAnnotations shouldBe true
                    }
                )
            }
        }

        "Then the hasAnnotation common flag should be written and re-initialized correctly" {
            val typeAliasVisitor = spyk<KotlinTypeAliasVisitor>()
            clazz.accept(ReWritingMetadataVisitor(AllTypeAliasVisitor(typeAliasVisitor)))

            verify {
                typeAliasVisitor.visitTypeAlias(
                    clazz,
                    ofType(KotlinDeclarationContainerMetadata::class),
                    withArg {
                        it.flags.common.hasAnnotations shouldBe true
                    }
                )
            }
        }
    }
})
