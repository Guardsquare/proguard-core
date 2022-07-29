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

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldExist
import io.mockk.spyk
import io.mockk.verify
import proguard.classfile.kotlin.KotlinTypeParameterMetadata
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.KotlinSource

class AllTypeParameterVisitorTest : FreeSpec({
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
            """.trimIndent()
        )
    )

    "Given a file facade containing entities with type parameters" - {
        val clazz = programClassPool.getClass("TestKt")

        "Then all type parameters should be visited" {
            val typeParameterVisitor = spyk<KotlinTypeParameterVisitor>()
            val slots = mutableListOf<KotlinTypeParameterMetadata>()

            clazz.kotlinMetadataAccept(AllTypeParameterVisitor(typeParameterVisitor))

            verify(exactly = 3) {
                typeParameterVisitor.visitAnyTypeParameter(clazz, capture(slots))
            }

            slots shouldExist { it.name == "FunctionTP" }
            slots shouldExist { it.name == "AliasTP" }
            slots shouldExist { it.name == "PropertyTP" }
        }
    }

    "Given a class with type parameter containing entities with type parameters" - {
        val clazz = programClassPool.getClass("Foo")

        "Then all type parameters should be visited" {
            val typeParameterVisitor = spyk<KotlinTypeParameterVisitor>()
            val slots = mutableListOf<KotlinTypeParameterMetadata>()

            clazz.kotlinMetadataAccept(AllTypeParameterVisitor(typeParameterVisitor))

            verify(exactly = 4) {
                typeParameterVisitor.visitAnyTypeParameter(clazz, capture(slots))
            }

            slots shouldExist { it.name == "ClassTP" }
            slots shouldExist { it.name == "FunctionTP" }
            slots shouldExist { it.name == "AliasTP" }
            slots shouldExist { it.name == "PropertyTP" }
        }
    }
})
