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

package proguard.classfile.kotlin

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.mockk.spyk
import io.mockk.verify
import proguard.classfile.kotlin.KotlinEffectInvocationKind.AT_LEAST_ONCE
import proguard.classfile.kotlin.KotlinEffectInvocationKind.AT_MOST_ONCE
import proguard.classfile.kotlin.KotlinEffectInvocationKind.EXACTLY_ONCE
import proguard.classfile.kotlin.KotlinEffectType.CALLS
import proguard.classfile.kotlin.KotlinEffectType.RETURNS_CONSTANT
import proguard.classfile.kotlin.visitor.AllFunctionVisitor
import proguard.classfile.kotlin.visitor.KotlinEffectVisitor
import proguard.classfile.kotlin.visitor.ReferencedKotlinMetadataVisitor
import proguard.classfile.kotlin.visitor.filter.KotlinFunctionFilter
import testutils.ClassPoolBuilder
import testutils.KotlinSource

class KotlinMetadataEffectTest : FreeSpec({
    val (programClassPool, _) = ClassPoolBuilder.fromSource(
        KotlinSource(
            "Test.kt",
            """
            import kotlin.contracts.ExperimentalContracts
            import kotlin.contracts.InvocationKind
            import kotlin.contracts.contract

            @ExperimentalContracts
            inline fun <R> callsInPlaceAtLeastOnce(block: () -> R): R {
                contract {
                    callsInPlace(block, InvocationKind.AT_LEAST_ONCE)
                }
                return block()
            }

            @ExperimentalContracts
            inline fun <R> callsInPlaceAtMostOnce(block: () -> R): R {
                contract {
                    callsInPlace(block, InvocationKind.AT_MOST_ONCE)
                }
                return block()
            }

            @ExperimentalContracts
            inline fun <R> callsInPlaceExactlyOnce(block: () -> R): R {
                contract {
                    callsInPlace(block, InvocationKind.EXACTLY_ONCE)
                }
                return block()
            }

            @ExperimentalContracts
            fun callsInPlace() {
                callsInPlaceAtLeastOnce {}
                callsInPlaceExactlyOnce {}
                callsInPlaceAtMostOnce  {}
            }
                                    
            @ExperimentalContracts
            fun printLengthOfString() {
                val aString: String? = "Test"
                if (aString.isNotNull()) {
                    println("The length of " + aString + " is " + aString.length)
                }
            }

            @ExperimentalContracts
            fun String?.isNotNull(): Boolean {
                contract {
                    returns(true) implies (this@isNotNull != null)
                    // returnsNotNull() implies (this@isNotNull != null)
                }
                return this != null
            }
            """.trimIndent()
        )
    )

    fun visitFunctionEffectMetadata(funcName: String, effectVisitor: KotlinEffectVisitor) {
        programClassPool.classesAccept(
            ReferencedKotlinMetadataVisitor(
                AllFunctionVisitor(
                    KotlinFunctionFilter(
                        { it.name == funcName },
                        { clazz, metadata, func ->
                            func.contractsAccept(clazz, metadata) { _, _, _, contract -> contract.effectsAccept(clazz, metadata, func, effectVisitor) }
                        }
                    )
                )
            )
        )
    }

    "Given functions with contracts" - {

        "Then the KotlinEffectMetadata for CALLS AT_LEAST_ONCE should be correct" {
            val effectVisitor = spyk<KotlinEffectVisitor>()
            visitFunctionEffectMetadata("callsInPlaceAtLeastOnce", effectVisitor)
            verify(exactly = 1) {
                effectVisitor.visitEffect(
                    programClassPool.getClass("TestKt"),
                    ofType(KotlinMetadata::class),
                    ofType(KotlinFunctionMetadata::class),
                    ofType(KotlinContractMetadata::class),
                    withArg {
                        it.effectType shouldBe CALLS
                        it.invocationKind shouldBe AT_LEAST_ONCE
                    }
                )
            }
        }

        "Then the KotlinEffectMetadata for CALLS AT_MOST_ONCE should be correct" {
            val effectVisitor = spyk<KotlinEffectVisitor>()
            visitFunctionEffectMetadata("callsInPlaceAtMostOnce", effectVisitor)
            verify {
                effectVisitor.visitEffect(
                    programClassPool.getClass("TestKt"),
                    ofType(KotlinMetadata::class),
                    ofType(KotlinFunctionMetadata::class),
                    ofType(KotlinContractMetadata::class),
                    withArg {
                        it.effectType shouldBe CALLS
                        it.invocationKind shouldBe AT_MOST_ONCE
                    }
                )
            }
        }

        "Then the KotlinEffectMetadata for CALLS EXACTLY_ONCE should be correct" {
            val effectVisitor = spyk<KotlinEffectVisitor>()
            visitFunctionEffectMetadata("callsInPlaceExactlyOnce", effectVisitor)
            verify {
                effectVisitor.visitEffect(
                    programClassPool.getClass("TestKt"),
                    ofType(KotlinMetadata::class),
                    ofType(KotlinFunctionMetadata::class),
                    ofType(KotlinContractMetadata::class),
                    withArg {
                        it.effectType shouldBe CALLS
                        it.invocationKind shouldBe EXACTLY_ONCE
                    }
                )
            }
        }

        "Then the KotlinEffectMetadata for RETURNS_CONSTANT should be correct" {
            val effectVisitor = spyk<KotlinEffectVisitor>()
            visitFunctionEffectMetadata("isNotNull", effectVisitor)
            verify {
                effectVisitor.visitEffect(
                    programClassPool.getClass("TestKt"),
                    ofType(KotlinMetadata::class),
                    ofType(KotlinFunctionMetadata::class),
                    ofType(KotlinContractMetadata::class),
                    withArg {
                        it.effectType shouldBe RETURNS_CONSTANT
                        it.invocationKind shouldBe null
                    }
                )
            }
        }

        // TODO - Add a test for KotlinEffectMetadata RETURNS_NOT_NULL
    }
})
