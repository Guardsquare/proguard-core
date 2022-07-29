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
import io.kotest.core.spec.style.funSpec
import io.kotest.matchers.shouldBe
import io.mockk.spyk
import io.mockk.verify
import proguard.classfile.Clazz
import proguard.classfile.kotlin.KotlinClassKindMetadata
import proguard.classfile.kotlin.KotlinDeclarationContainerMetadata
import proguard.classfile.kotlin.visitor.AllConstructorVisitor
import proguard.classfile.kotlin.visitor.AllFunctionVisitor
import proguard.classfile.kotlin.visitor.AllPropertyVisitor
import proguard.classfile.kotlin.visitor.AllTypeAliasVisitor
import proguard.classfile.kotlin.visitor.AllTypeParameterVisitor
import proguard.classfile.kotlin.visitor.AllValueParameterVisitor
import proguard.classfile.kotlin.visitor.KotlinConstructorVisitor
import proguard.classfile.kotlin.visitor.KotlinFunctionVisitor
import proguard.classfile.kotlin.visitor.KotlinMetadataVisitor
import proguard.classfile.kotlin.visitor.KotlinPropertyVisitor
import proguard.classfile.kotlin.visitor.KotlinTypeAliasVisitor
import proguard.classfile.kotlin.visitor.KotlinTypeParameterVisitor
import proguard.classfile.kotlin.visitor.KotlinValueParameterVisitor
import proguard.classfile.kotlin.visitor.MultiKotlinMetadataVisitor
import proguard.classfile.kotlin.visitor.ReferencedKotlinMetadataVisitor
import proguard.classfile.visitor.ClassVisitor
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.KotlinSource
import proguard.testutils.ReWritingMetadataVisitor

class KotlinCommonFlagsTest : FreeSpec({

    val (programClassPool, _) = ClassPoolBuilder.fromSource(
        KotlinSource(
            "Test.kt",
            """
            import kotlin.annotation.AnnotationTarget.*

            @Target(
                CLASS,
                CONSTRUCTOR,
                PROPERTY,
                FUNCTION,
                FIELD,
                TYPE_PARAMETER,
                VALUE_PARAMETER,
                PROPERTY_GETTER,
                PROPERTY_SETTER,
                TYPE,
                TYPEALIAS
            )
            annotation class MyAnnotation

            class NotAnnotatedClass {
                @Suppress("TOPLEVEL_TYPEALIASES_ONLY")
                typealias NotAnnotatedTypeAlias = String

                var propertyWithoutAnnotation: String = "foo"
                    get() = "foo"
                    set(value) { field = value }

                fun <T> funWithoutAnnotation(@Suppress("UNUSED_PARAMETER") param: String) { }
            }

            @MyAnnotation
            class AnnotatedClass @MyAnnotation constructor() {
                @MyAnnotation
                var propertyWithAnnotation: String = "foo"
                    @MyAnnotation get() = "foo"
                    @MyAnnotation set(value) { field = value }

                @MyAnnotation
                fun <@MyAnnotation T> funWithAnnotation(@Suppress("UNUSED_PARAMETER") @MyAnnotation param: String) { }

                @Suppress("TOPLEVEL_TYPEALIASES_ONLY")
                @MyAnnotation
                typealias AnnotatedTypeAlias = String
            }
            """.trimIndent()
        )
    )

    // Run the test twice, check a class where everything is annotated and another where nothing is annotated.
    val metadataVisitor = { kmVisitor: KotlinMetadataVisitor -> ReferencedKotlinMetadataVisitor(kmVisitor) }

    include("Initialization", testHasAnnotation("Initializing", programClassPool.getClass("AnnotatedClass"), true, metadataVisitor))
    include("Initialization", testHasAnnotation("Initializing", programClassPool.getClass("NotAnnotatedClass"), false, metadataVisitor))

    // Then run the same tests through the `ReWritingMetadataVisitor`, to check that flags are written correctly
    val reWritingMetadataVisitor = { kmVisitor: KotlinMetadataVisitor -> ReWritingMetadataVisitor(kmVisitor) }
    include("ReWriting", testHasAnnotation("ReWriting", programClassPool.getClass("AnnotatedClass"), true, reWritingMetadataVisitor))
    include("ReWriting", testHasAnnotation("ReWriting", programClassPool.getClass("NotAnnotatedClass"), false, reWritingMetadataVisitor))
})

internal fun testHasAnnotation(prefix: String, clazz: Clazz, expected: Boolean, visitorWrapper: (KotlinMetadataVisitor) -> ClassVisitor) = funSpec {
    val className = clazz.name
    val kotlinClassVisitor = spyk<KotlinMetadataVisitor>()
    val kotlinFunctionVisitor = spyk<KotlinFunctionVisitor>()
    val kotlinConstructorVisitor = spyk<KotlinConstructorVisitor>()
    val kotlinPropertyVisitor = spyk<KotlinPropertyVisitor>()
    val kotlinValueParamVisitor = spyk<KotlinValueParameterVisitor>()
    val kotlinTypeParamVisitor = spyk<KotlinTypeParameterVisitor>()
    val kotlinTypeAliasVisitor = spyk<KotlinTypeAliasVisitor>()

    clazz.accept(
        visitorWrapper(
            MultiKotlinMetadataVisitor(
                kotlinClassVisitor,
                AllConstructorVisitor(kotlinConstructorVisitor),
                AllPropertyVisitor(kotlinPropertyVisitor),
                AllFunctionVisitor(
                    kotlinFunctionVisitor,
                    AllValueParameterVisitor(kotlinValueParamVisitor),
                    AllTypeParameterVisitor(kotlinTypeParamVisitor)
                ),
                AllTypeAliasVisitor(kotlinTypeAliasVisitor)
            )
        )
    )

    test("$prefix: $className's hasAnnotations flag should be $expected") {
        verify {
            kotlinClassVisitor.visitKotlinClassMetadata(
                clazz,
                withArg { it.flags.common.hasAnnotations shouldBe expected }
            )
        }
    }

    test("$prefix: $className's constructor hasAnnotations flag should be $expected") {
        verify(exactly = 1) {
            kotlinConstructorVisitor.visitConstructor(
                clazz,
                ofType(KotlinClassKindMetadata::class),
                withArg {
                    it.flags.common.hasAnnotations shouldBe expected
                }
            )
        }
    }

    test("$prefix: the hasAnnotations flag of the function in $className should be $expected") {
        verify(exactly = 1) {
            kotlinFunctionVisitor.visitAnyFunction(
                clazz,
                ofType(KotlinClassKindMetadata::class),
                withArg {
                    it.flags.common.hasAnnotations shouldBe expected
                }
            )
        }
    }

    test("$prefix: the hasAnnotations flags of the property in $className should be $expected") {
        verify(exactly = 1) {
            kotlinPropertyVisitor.visitAnyProperty(
                clazz,
                ofType(KotlinClassKindMetadata::class),
                withArg {
                    it.flags.common.hasAnnotations shouldBe expected
                    it.getterFlags.common.hasAnnotations shouldBe expected
                    it.setterFlags.common.hasAnnotations shouldBe expected
                }
            )
        }
    }

    test("$prefix: the hasAnnotations flag of the type param in $className should be $expected") {
        verify(exactly = 1) {
            kotlinTypeParamVisitor.visitAnyTypeParameter(
                clazz,
                withArg {
                    it.flags.common.hasAnnotations shouldBe expected
                }
            )
        }
    }

    test("$prefix: the hasAnnotation flag of the value param in $className should be $expected") {
        verify(exactly = 1) {
            kotlinValueParamVisitor.visitAnyValueParameter(
                clazz,
                withArg {
                    it.flags.common.hasAnnotations shouldBe expected
                }
            )
        }
    }

    test("$prefix: the hasAnnotations flag of the type alias in $className should be $expected") {
        verify(exactly = 1) {
            kotlinTypeAliasVisitor.visitTypeAlias(
                clazz,
                ofType(KotlinDeclarationContainerMetadata::class),
                withArg { it.flags.common.hasAnnotations shouldBe expected }
            )
        }
    }
}
