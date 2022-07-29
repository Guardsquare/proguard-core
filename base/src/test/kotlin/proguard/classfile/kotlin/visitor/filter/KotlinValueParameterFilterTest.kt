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
import proguard.classfile.kotlin.KotlinConstructorMetadata
import proguard.classfile.kotlin.KotlinFunctionMetadata
import proguard.classfile.kotlin.KotlinPropertyMetadata
import proguard.classfile.kotlin.visitor.AllValueParameterVisitor
import proguard.classfile.kotlin.visitor.KotlinValueParameterVisitor
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.KotlinSource
import java.util.function.Predicate

class KotlinValueParameterFilterTest : FreeSpec({
    val (programClassPool, _) = ClassPoolBuilder.fromSource(
        KotlinSource(
            "Test.kt",
            """
             @Suppress("UNUSED_PARAMETER")
             class Foo(consVP: String) {
                var property: String = "foo"
                    set(propVP) { }
                fun foo(funVP: String) { }
             }
            """
        )
    )

    "Given a class value parameters" - {
        val clazz = programClassPool.getClass("Foo")

        "Then using a filter for a constructor value parameter should visit only that type parameter" {
            val valueParameterVisitor = spyk<KotlinValueParameterVisitor>()

            clazz.kotlinMetadataAccept(createVisitor("consVP", valueParameterVisitor))

            verify(exactly = 1) {
                valueParameterVisitor.visitConstructorValParameter(
                    clazz,
                    ofType(KotlinClassKindMetadata::class),
                    ofType(KotlinConstructorMetadata::class),
                    withArg {
                        it.parameterName shouldBe "consVP"
                    }
                )
            }
        }

        "Then using a filter for a function value parameter should visit only that type parameter" {
            val valueParameterVisitor = spyk<KotlinValueParameterVisitor>()

            clazz.kotlinMetadataAccept(createVisitor("funVP", valueParameterVisitor))

            verify(exactly = 1) {
                valueParameterVisitor.visitFunctionValParameter(
                    clazz,
                    ofType(KotlinClassKindMetadata::class),
                    ofType(KotlinFunctionMetadata::class),
                    withArg {
                        it.parameterName shouldBe "funVP"
                    }
                )
            }
        }

        "Then using a filter for a property value parameter should visit only that type parameter" {
            val valueParameterVisitor = spyk<KotlinValueParameterVisitor>()

            clazz.kotlinMetadataAccept(createVisitor("propVP", valueParameterVisitor))

            verify(exactly = 1) {
                valueParameterVisitor.visitPropertyValParameter(
                    clazz,
                    ofType(KotlinClassKindMetadata::class),
                    ofType(KotlinPropertyMetadata::class),
                    withArg {
                        it.parameterName shouldBe "propVP"
                    }
                )
            }
        }
    }
})

private fun createVisitor(name: String, valueParameterVisitor: KotlinValueParameterVisitor) =
    AllValueParameterVisitor(
        KotlinValueParameterFilter(
            Predicate { it.parameterName == name },
            valueParameterVisitor
        )
    )
