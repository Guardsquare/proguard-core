/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2021 Guardsquare NV
 */

package proguard.classfile.kotlin

import io.kotest.core.spec.style.FreeSpec
import io.mockk.mockk
import io.mockk.verify
import proguard.classfile.Clazz
import proguard.classfile.ProgramClass
import proguard.classfile.attribute.Attribute.RUNTIME_VISIBLE_ANNOTATIONS
import proguard.classfile.attribute.annotation.Annotation
import proguard.classfile.attribute.annotation.ArrayElementValue
import proguard.classfile.attribute.annotation.ConstantElementValue
import proguard.classfile.attribute.annotation.ElementValue
import proguard.classfile.attribute.annotation.visitor.AllAnnotationVisitor
import proguard.classfile.attribute.annotation.visitor.AnnotationTypeFilter
import proguard.classfile.attribute.annotation.visitor.AnnotationVisitor
import proguard.classfile.attribute.annotation.visitor.ElementValueVisitor
import proguard.classfile.attribute.visitor.AllAttributeVisitor
import proguard.classfile.attribute.visitor.AttributeNameFilter
import proguard.classfile.editor.ConstantPoolEditor
import proguard.classfile.kotlin.KotlinConstants.TYPE_KOTLIN_METADATA
import proguard.classfile.kotlin.visitor.KotlinMetadataVersionFilter
import proguard.classfile.util.kotlin.KotlinMetadataInitializer
import proguard.classfile.visitor.ClassVisitor
import testutils.ClassPoolBuilder
import testutils.KotlinSource

class KotlinMetadataVersionFilterTest : FreeSpec({
    val (programClassPool, _) = ClassPoolBuilder.fromSource(
        KotlinSource(
            "Test.kt",
            """
                    class Person
            """.trimIndent()
        )
    )

    val supportedMetadataVisitor = mockk<ClassVisitor>(relaxed = true)
    val unsupportedMetadataVisitor = mockk<ClassVisitor>(relaxed = true)

    "Given a class with a supported metadata version" - {
        val supportedClazz = programClassPool.getClass("Person") as ProgramClass

        "Then the KotlinMetadataVersionFilter accepts this class" {
            supportedClazz.accept(KotlinMetadataVersionFilter(KotlinMetadataInitializer::isSupportedMetadataVersion, supportedMetadataVisitor, unsupportedMetadataVisitor))

            verify(exactly = 1) {
                supportedClazz.accept(supportedMetadataVisitor)
            }
        }
    }

    "Given a class with an unsupported metadata version" - {
        val unsupportedClazz = programClassPool.getClass("Person") as ProgramClass
        // This visitor updates the mv field in the kotlin metadata annotation.
        unsupportedClazz.accept(
            AllAttributeVisitor(
                AttributeNameFilter(
                    RUNTIME_VISIBLE_ANNOTATIONS,
                    AllAnnotationVisitor(
                        AnnotationTypeFilter(
                            TYPE_KOTLIN_METADATA,
                            object : AnnotationVisitor {
                                override fun visitAnnotation(clazz: Clazz, annotation: Annotation) {
                                    annotation.elementValuesAccept(
                                        clazz,
                                        object : ElementValueVisitor {
                                            override fun visitAnyElementValue(clazz: Clazz, annotation: Annotation, elementValue: ElementValue) {}
                                            override fun visitArrayElementValue(clazz: Clazz, annotation: Annotation, arrayElementValue: ArrayElementValue) {
                                                arrayElementValue.elementValueAccept(
                                                    clazz, annotation, 0,
                                                    object : ElementValueVisitor {
                                                        override fun visitConstantElementValue(clazz: Clazz, annotation: Annotation, constantElementValue: ConstantElementValue) {
                                                            val index = ConstantPoolEditor(clazz as ProgramClass).addIntegerConstant(9001)
                                                            constantElementValue.u2constantValueIndex = index
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        )
                    )
                )
            )
        )

        "Then the KotlinMetadataVersionFilter rejects the class" {
            unsupportedClazz.accept(KotlinMetadataVersionFilter(KotlinMetadataInitializer::isSupportedMetadataVersion, supportedMetadataVisitor, unsupportedMetadataVisitor))

            verify(exactly = 1) {
                unsupportedMetadataVisitor.visitProgramClass(unsupportedClazz)
            }
        }
    }
})
