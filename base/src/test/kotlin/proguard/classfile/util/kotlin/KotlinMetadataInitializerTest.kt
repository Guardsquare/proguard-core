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

package proguard.classfile.util.kotlin

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.mockk.spyk
import io.mockk.verify
import proguard.classfile.Clazz
import proguard.classfile.MethodSignature
import proguard.classfile.editor.MemberReferenceFixer
import proguard.classfile.kotlin.KotlinSyntheticClassKindMetadata
import proguard.classfile.kotlin.visitor.AllFunctionVisitor
import proguard.classfile.kotlin.visitor.KotlinFunctionVisitor
import proguard.classfile.kotlin.visitor.KotlinMetadataVisitor
import proguard.classfile.kotlin.visitor.ReferencedKotlinMetadataVisitor
import proguard.classfile.kotlin.visitor.filter.KotlinDeclarationContainerFilter
import proguard.classfile.util.ClassReferenceInitializer
import proguard.classfile.util.ClassRenamer
import proguard.classfile.visitor.MultiClassVisitor
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.JavaSource
import proguard.testutils.KotlinSource
import java.util.function.BiConsumer

class KotlinMetadataInitializerTest : FreeSpec({

    "Given a file facade containing 1 class" - {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            KotlinSource(
                "Test.kt",
                """
            val property: String = "FOO"

            class Foo
                """.trimIndent()
            )
        )

        "Then the file facade ownerClassName should be correct" {
            val declarationContainerVisitor = spyk<KotlinMetadataVisitor>()

            programClassPool.classesAccept(
                "TestKt",
                ReferencedKotlinMetadataVisitor(
                    KotlinDeclarationContainerFilter(declarationContainerVisitor)
                )
            )

            verify(exactly = 1) {
                declarationContainerVisitor.visitKotlinDeclarationContainerMetadata(
                    programClassPool.getClass("TestKt"),
                    withArg {
                        it.ownerClassName = "TestKt"
                    }
                )
            }
        }

        "Then the class ownerClassName should be correct" {
            val declarationContainerVisitor = spyk<KotlinMetadataVisitor>()

            programClassPool.classesAccept(
                "Foo",
                ReferencedKotlinMetadataVisitor(
                    KotlinDeclarationContainerFilter(declarationContainerVisitor)
                )
            )

            verify(exactly = 1) {
                declarationContainerVisitor.visitKotlinDeclarationContainerMetadata(
                    programClassPool.getClass("Foo"),
                    withArg {
                        it.ownerClassName = "Foo"
                    }
                )
            }
        }
    }

    "Given a class with Kotlin 1.4 metadata" - {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            JavaSource(
                "TestKotlin1dot4Metadata.java",
                """
                    @kotlin.Metadata(
                        bv = {1, 0, 3},
                        d1 = {"\u0000\n\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\u0018\u00002\u00020\u0001B\u0005¢\u0006\u0002\u0010\u0002"},
                        d2 = {"LTestKotlin1dot4Metadata;", "", "()V"},
                        k = 1,
                        mv = {1, 4, 0}
                    )
                    public class TestKotlin1dot4Metadata { }
                """.trimIndent()
            )
        )
        "Then the metadata should be parsed correctly" {
            val visitor = spyk<KotlinMetadataVisitor>()
            programClassPool.classesAccept(
                MultiClassVisitor(
                    KotlinMetadataInitializer { _, _ -> },
                    ReferencedKotlinMetadataVisitor(visitor)
                )
            )

            verify {
                visitor.visitKotlinClassMetadata(
                    programClassPool.getClass("TestKotlin1dot4Metadata"),
                    withArg {
                        it.className shouldBe "TestKotlin1dot4Metadata"
                        it.mv shouldBe arrayOf(1, 4, 0)
                    }
                )
            }
        }
    }

    "Given a class with Kotlin 9999 metadata" - {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            JavaSource(
                "TestKotlin9999Metadata.java",
                """
                    @kotlin.Metadata(
                        d1 = {"\u0000\n\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\u0018\u00002\u00020\u0001B\u0005¢\u0006\u0002\u0010\u0002"},
                        d2 = {"LTestKotlin9999Metadata;", "", "()V"},
                        k = 1,
                        mv = {9999, 0, 0}
                    )
                    public class TestKotlin9999Metadata { }
                """.trimIndent()
            ),
            initialize = false
        )
        "Then the metadata initializer should print a warning" {
            val visitor = spyk<KotlinMetadataVisitor>()
            lateinit var message: String
            programClassPool.classesAccept(
                MultiClassVisitor(
                    KotlinMetadataInitializer { _, s -> message = s },
                    ReferencedKotlinMetadataVisitor(visitor)
                )
            )
            message shouldBe "Encountered corrupt @kotlin/Metadata for class TestKotlin9999Metadata (version 9999.0.0)."
        }
    }

    "Given a class with Kotlin metadata version missing" - {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            JavaSource(
                "TestKotlinVersionMissingMetadata.java",
                """
                    @kotlin.Metadata(
                        d1 = {"\u0000\n\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\u0018\u00002\u00020\u0001B\u0005¢\u0006\u0002\u0010\u0002"},
                        d2 = {"LTestKotlinVersionMissingMetadata;", "", "()V"},
                        k = 1
                    )
                    public class TestKotlinVersionMissingMetadata { }
                """.trimIndent()
            ),
            initialize = false
        )
        "Then the metadata initializer should print a warning" {
            val visitor = spyk<KotlinMetadataVisitor>()
            lateinit var message: String
            programClassPool.classesAccept(
                MultiClassVisitor(
                    KotlinMetadataInitializer { _, s -> message = s },
                    ReferencedKotlinMetadataVisitor(visitor)
                )
            )
            message shouldBe "Encountered corrupt @kotlin/Metadata for class TestKotlinVersionMissingMetadata (version unknown)."
        }
    }

    "Given a class with Kotlin metadata unknown field" - {
        val (programClassPool, libraryClassPool) = ClassPoolBuilder.fromSource(
            JavaSource(
                "TestKotlin1dot4Metadata.java",
                """
                    @kotlin.Metadata(
                        bv = {1, 0, 3},
                        d1 = {"\u0000\n\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\u0018\u00002\u00020\u0001B\u0005¢\u0006\u0002\u0010\u0002"},
                        d2 = {"LTestKotlin1dot4Metadata;", "", "()V"},
                        k = 1,
                        mv = {1, 4, 0}
                    )
                    public class TestKotlin1dot4Metadata { }
                """.trimIndent()
            )
        )

        libraryClassPool.classAccept(
            "kotlin/Metadata",
            ClassRenamer(
                { clazz -> clazz.name },
                { clazz, member -> if (member.getName(clazz) == "k") "invalid" else member.getName(clazz) }
            )
        )

        programClassPool.classesAccept(MemberReferenceFixer(true))

        "Then the metadata initializer should print a warning" {
            val visitor = spyk<KotlinMetadataVisitor>()
            lateinit var message: String
            programClassPool.classesAccept(
                MultiClassVisitor(
                    KotlinMetadataInitializer { _, s -> message = s },
                    ReferencedKotlinMetadataVisitor(visitor)
                )
            )
            message shouldBe "Encountered corrupt Kotlin metadata in class TestKotlin1dot4Metadata. The metadata for this class will not be processed (Unknown Kotlin metadata field 'invalid')"
        }
    }

    "Given an anonymous lambda function" - {
        val (programClassPool, libraryClassPool) = ClassPoolBuilder.fromSource(
            KotlinSource(
                "Test.kt",
                """
                fun foo(x: () -> Unit) { println(x) }
                fun main() {
                    foo { }
                }
                """.trimIndent()
            )
        )

        "When the jvmSignature method name is <anonymous>" - {
            val visitor = spyk<KotlinFunctionVisitor>()
            val clazz = programClassPool.getClass("TestKt\$main\$1")

            clazz.kotlinMetadataAccept(
                AllFunctionVisitor(
                    { _, _, func -> func.jvmSignature = MethodSignature(func.jvmSignature.className, "<anonymous>", func.jvmSignature.descriptor) }
                )
            )
            val logger: BiConsumer<Clazz, String> = spyk()
            clazz.accept(ClassReferenceInitializer(programClassPool, libraryClassPool))
            clazz.kotlinMetadataAccept(AllFunctionVisitor(visitor))

            "Then the referencedMethod should be initialized" {
                verify(exactly = 1) {
                    visitor.visitSyntheticFunction(
                        clazz,
                        ofType<KotlinSyntheticClassKindMetadata>(),
                        withArg {
                            it.name shouldBe "<anonymous>"
                            it.referencedMethod shouldBe clazz.findMethod("invoke", null)
                        }
                    )
                }

                verify(exactly = 0) {
                    logger.accept(
                        ofType<Clazz>(),
                        ofType<String>()
                    )
                }
            }
        }
    }

    "Given a Kotlin metadata components" - {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            JavaSource(
                "Test.java",
                """
                public class Test { }
                """.trimIndent()
            )
        )
        "Then using the KotlinMetadataInitializer initialize function" - {
            val kotlinMetadataInitializer = KotlinMetadataInitializer { _, _ -> }
            kotlinMetadataInitializer.initialize(
                programClassPool.getClass("Test"),
                1,
                intArrayOf(1, 4, 0),
                arrayOf("\u0000\n\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\u0018\u00002\u00020\u0001B\u0005¢\u0006\u0002\u0010\u0002"),
                arrayOf("LTest;", "", "()V"),
                0,
                "",
                ""
            )

            "Then the metadata should be initialized correctly" {
                val visitor = spyk<KotlinMetadataVisitor>()
                programClassPool.classesAccept(
                    ReferencedKotlinMetadataVisitor(visitor)
                )

                verify {
                    visitor.visitKotlinClassMetadata(
                        programClassPool.getClass("Test"),
                        withArg {
                            it.className shouldBe "Test"
                            it.mv shouldBe arrayOf(1, 4, 0)
                        }
                    )
                }
            }
        }
    }
})
