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

package proguard.classfile.kotlin.visitor.filter

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.mockk.spyk
import io.mockk.verify
import proguard.classfile.kotlin.KotlinClassKindMetadata
import proguard.classfile.kotlin.KotlinDeclarationContainerMetadata
import proguard.classfile.kotlin.KotlinFunctionMetadata
import proguard.classfile.kotlin.KotlinMetadata
import proguard.classfile.kotlin.KotlinPropertyMetadata
import proguard.classfile.kotlin.KotlinTypeAliasMetadata
import proguard.classfile.kotlin.visitor.AllTypeParameterVisitor
import proguard.classfile.kotlin.visitor.KotlinTypeParameterVisitor
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.KotlinSource
import java.util.function.Predicate

class KotlinTypeParameterFilterTest : FreeSpec({
    val (programClassPool, _) = ClassPoolBuilder.fromSource(
        KotlinSource(
            "Test.kt",
            """
                fun <FunctionTP> foo(): Int = 42
                typealias alias <AliasTP> = String
                val <PropertyTP> List<PropertyTP>.property: Unit
                    get() = Unit

                class Foo<ClassTP> {
                    fun <FunctionTP> foo(): Int = 42
                    @Suppress("TOPLEVEL_TYPEALIASES_ONLY")
                    typealias alias <AliasTP> = String
                    val <PropertyTP> List<PropertyTP>.property: Unit
                        get() = Unit
                }
            """
        )
    )

    "Given a file facade containing entities with type parameters" - {
        val clazz = programClassPool.getClass("TestKt")

        "Then using a filter for a property type parameter should visit only that type parameter" {
            val typeParameterVisitor = spyk<KotlinTypeParameterVisitor>()

            clazz.kotlinMetadataAccept(createVisitor("PropertyTP", typeParameterVisitor))

            verify(exactly = 1) {
                typeParameterVisitor.visitPropertyTypeParameter(
                    clazz,
                    ofType(KotlinDeclarationContainerMetadata::class),
                    ofType(KotlinPropertyMetadata::class),
                    withArg {
                        it.name shouldBe "PropertyTP"
                    }
                )
            }
        }

        "Then using a filter for a function type parameter should visit only that type parameter" {
            val typeParameterVisitor = spyk<KotlinTypeParameterVisitor>()

            clazz.kotlinMetadataAccept(createVisitor("FunctionTP", typeParameterVisitor))

            verify(exactly = 1) {
                typeParameterVisitor.visitFunctionTypeParameter(
                    clazz,
                    ofType(KotlinMetadata::class),
                    ofType(KotlinFunctionMetadata::class),
                    withArg {
                        it.name shouldBe "FunctionTP"
                    }
                )
            }
        }

        "Then using a filter for a typealias type parameter should visit only that type parameter" {
            val typeParameterVisitor = spyk<KotlinTypeParameterVisitor>()

            clazz.kotlinMetadataAccept(createVisitor("AliasTP", typeParameterVisitor))

            verify(exactly = 1) {
                typeParameterVisitor.visitAliasTypeParameter(
                    clazz,
                    ofType(KotlinDeclarationContainerMetadata::class),
                    ofType(KotlinTypeAliasMetadata::class),
                    withArg {
                        it.name shouldBe "AliasTP"
                    }
                )
            }
        }
    }

    "Given a class with type parameter containing entities with type parameters" - {
        val clazz = programClassPool.getClass("Foo")

        "Then using a filter for a class type parameter should visit only that type parameter" {
            val typeParameterVisitor = spyk<KotlinTypeParameterVisitor>()

            clazz.kotlinMetadataAccept(createVisitor("ClassTP", typeParameterVisitor))

            verify(exactly = 1) {
                typeParameterVisitor.visitClassTypeParameter(
                    clazz,
                    ofType(KotlinClassKindMetadata::class),
                    withArg {
                        it.name shouldBe "ClassTP"
                    }
                )
            }
        }

        "Then using a filter for a property type parameter should visit only that type parameter" {
            val typeParameterVisitor = spyk<KotlinTypeParameterVisitor>()

            clazz.kotlinMetadataAccept(createVisitor("PropertyTP", typeParameterVisitor))

            verify(exactly = 1) {
                typeParameterVisitor.visitPropertyTypeParameter(
                    clazz,
                    ofType(KotlinDeclarationContainerMetadata::class),
                    ofType(KotlinPropertyMetadata::class),
                    withArg {
                        it.name shouldBe "PropertyTP"
                    }
                )
            }
        }

        "Then using a filter for a function type parameter should visit only that type parameter" {
            val typeParameterVisitor = spyk<KotlinTypeParameterVisitor>()

            clazz.kotlinMetadataAccept(createVisitor("FunctionTP", typeParameterVisitor))

            verify(exactly = 1) {
                typeParameterVisitor.visitFunctionTypeParameter(
                    clazz,
                    ofType(KotlinMetadata::class),
                    ofType(KotlinFunctionMetadata::class),
                    withArg {
                        it.name shouldBe "FunctionTP"
                    }
                )
            }
        }

        "Then using a filter for a typealias type parameter should visit only that type parameter" {
            val typeParameterVisitor = spyk<KotlinTypeParameterVisitor>()

            clazz.kotlinMetadataAccept(createVisitor("AliasTP", typeParameterVisitor))

            verify(exactly = 1) {
                typeParameterVisitor.visitAliasTypeParameter(
                    clazz,
                    ofType(KotlinDeclarationContainerMetadata::class),
                    ofType(KotlinTypeAliasMetadata::class),
                    withArg {
                        it.name shouldBe "AliasTP"
                    }
                )
            }
        }
    }
})

private fun createVisitor(name: String, typeParameterVisitor: KotlinTypeParameterVisitor) =
    AllTypeParameterVisitor(
        KotlinTypeParameterFilter(
            Predicate { it.name == name },
            typeParameterVisitor
        )
    )
