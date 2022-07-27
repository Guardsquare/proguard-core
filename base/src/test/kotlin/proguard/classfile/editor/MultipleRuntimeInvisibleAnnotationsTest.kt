/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2021 Guardsquare NV
 */

package proguard.classfile.editor

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import proguard.classfile.Clazz
import proguard.classfile.attribute.Attribute.RUNTIME_INVISIBLE_ANNOTATIONS
import proguard.classfile.attribute.annotation.Annotation
import proguard.classfile.attribute.annotation.visitor.AllAnnotationVisitor
import proguard.classfile.attribute.annotation.visitor.AnnotationTypeFilter
import proguard.classfile.attribute.annotation.visitor.AnnotationVisitor
import proguard.classfile.attribute.visitor.AllAttributeVisitor
import proguard.classfile.attribute.visitor.AttributeCounter
import proguard.classfile.attribute.visitor.AttributeNameFilter
import proguard.classfile.util.ClassRenamer
import proguard.classfile.visitor.AllMemberVisitor
import proguard.classfile.visitor.MemberNameFilter
import proguard.classfile.visitor.MultiClassVisitor
import testutils.ClassPoolBuilder
import testutils.KotlinSource

class MultipleRuntimeInvisibleAnnotationsTest : FreeSpec({

    "Given class with RuntimeInvisibleAnnotation" - {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            KotlinSource(
                "Test.kt",
                """
                class TestClass {
                    val property: String
                    @JvmName("bar") get() = "foo"
                }
                """.trimIndent()
            )
        )

        "When the getter method is renamed" - {
            programClassPool.classAccept(
                "TestClass",
                MultiClassVisitor(
                    ClassRenamer(
                        { clazz -> clazz.name },
                        { clazz, member -> if (member.getName(clazz) == "bar") "ObfuscatedName" else "bar" }
                    ),
                    ClassReferenceFixer(false)
                )
            )

            "Then there should be exactly 1 RuntimeInvisibleAnnotations attribute" {
                val counter = AttributeCounter()
                programClassPool.classAccept(
                    "TestClass",
                    AllMemberVisitor(
                        MemberNameFilter(
                            "ObfuscatedName",
                            AllAttributeVisitor(
                                AttributeNameFilter(RUNTIME_INVISIBLE_ANNOTATIONS, counter)
                            )
                        )
                    )
                )

                counter.count shouldBe 1
            }

            "Then there should be exactly 1 JvmName annotation" {
                var count = 0
                programClassPool.classAccept(
                    "TestClass",
                    AllMemberVisitor(
                        MemberNameFilter(
                            "ObfuscatedName",
                            AllAttributeVisitor(
                                AllAnnotationVisitor(
                                    AnnotationTypeFilter(
                                        "Lkotlin/jvm/JvmName;",
                                        object : AnnotationVisitor {
                                            override fun visitAnnotation(clazz: Clazz?, annotation: Annotation?) {
                                                count++
                                            }
                                        }
                                    )
                                )
                            )
                        )
                    )
                )

                count shouldBe 1
            }
        }
    }
})
