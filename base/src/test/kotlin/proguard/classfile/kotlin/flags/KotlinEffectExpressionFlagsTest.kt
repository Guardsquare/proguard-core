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

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.core.spec.style.funSpec
import io.kotest.matchers.shouldBe
import io.mockk.spyk
import io.mockk.verify
import proguard.classfile.Clazz
import proguard.classfile.kotlin.KotlinEffectMetadata
import proguard.classfile.kotlin.visitor.AllFunctionVisitor
import proguard.classfile.kotlin.visitor.KotlinEffectExprVisitor
import proguard.classfile.kotlin.visitor.KotlinMetadataVisitor
import proguard.classfile.kotlin.visitor.ReferencedKotlinMetadataVisitor
import proguard.classfile.kotlin.visitor.filter.KotlinFunctionFilter
import testutils.ClassPoolBuilder
import testutils.KotlinSource
import testutils.ReWritingMetadataVisitor

class KotlinEffectExpressionFlagsTest : FreeSpec({
    val (programClassPool, _) = ClassPoolBuilder.fromSource(
        KotlinSource(
            "Test.kt",
            """
            import kotlin.contracts.ExperimentalContracts
            import kotlin.contracts.InvocationKind
            import kotlin.contracts.contract

            @ExperimentalContracts
            fun String?.isNull(): Boolean {
                contract {
                    returns(true) implies (this@isNull == null)
                }
                return this == null
            }

            @ExperimentalContracts
            fun String?.isNotNull(): Boolean {
                contract {
                    returns(true) implies (this@isNotNull != null)
                }
                return this != null
            }

            @ExperimentalContracts
            fun Any?.isString(): Boolean {
                contract {
                    returns(true) implies (this@isString is String)
                }
                return this != null && this is String
            }

            @ExperimentalContracts
            fun Any?.isNotString(): Boolean {
                contract {
                    returns(true) implies (this@isNotString !is String)
                }
                return this !is String
            }
            """
        )
    )

    include(
        "Given a negated contract",
        testEffectExprFlags(programClassPool.getClass("TestKt"), "isNotNull") {
            withClue("isNegated") { it.isNegated shouldBe true }
            withClue("isNullCheckPredicate") { it.isNullCheckPredicate shouldBe true }
        }
    )

    include(
        "Given a null checking contract",
        testEffectExprFlags(programClassPool.getClass("TestKt"), "isNull") {
            withClue("isNegated") { it.isNegated shouldBe false }
            withClue("isNullCheckPredicate") { it.isNullCheckPredicate shouldBe true }
        }
    )

    include(
        "Given an instance checking contract",
        testEffectExprFlags(programClassPool.getClass("TestKt"), "isString") {
            withClue("isNegated") { it.isNegated shouldBe false }
            withClue("isNullCheckPredicate") { it.isNullCheckPredicate shouldBe false }
        }
    )

    include(
        "Given a negated instance checking contract",
        testEffectExprFlags(programClassPool.getClass("TestKt"), "isNotString") {
            withClue("isNegated") { it.isNegated shouldBe true }
            withClue("isNullCheckPredicate") { it.isNullCheckPredicate shouldBe false }
        }
    )
})

private fun createVisitor(funcName: String, effectExprVisitor: KotlinEffectExprVisitor): KotlinMetadataVisitor =
    AllFunctionVisitor(
        KotlinFunctionFilter(
            { it.name == funcName },
            { clazz, metadata, func ->
                func.contractsAccept(clazz, metadata) { _, _, _, contract ->
                    contract.effectsAccept(clazz, metadata, func) { _, _, _, _, effectMetadata ->
                        effectMetadata.conclusionOfConditionalEffectAccept(clazz, effectExprVisitor)
                    }
                }
            }
        )
    )

private fun testEffectExprFlags(clazz: Clazz, funcName: String, flags: (KotlinEffectExpressionFlags) -> Unit) = funSpec {

    test("Then $funcName flags should be initialized correctly") {
        val propertyVisitor = spyk<KotlinEffectExprVisitor>()
        clazz.accept(ReferencedKotlinMetadataVisitor(createVisitor(funcName, propertyVisitor)))

        verify {
            propertyVisitor.visitAnyEffectExpression(
                clazz,
                ofType(KotlinEffectMetadata::class),
                withArg { flags.invoke(it.flags) }
            )
        }
    }

    test("Then $funcName flags should be written and re-initialized correctly") {
        val propertyVisitor = spyk<KotlinEffectExprVisitor>()
        clazz.accept(ReWritingMetadataVisitor(createVisitor(funcName, propertyVisitor)))

        verify {
            propertyVisitor.visitAnyEffectExpression(
                clazz,
                ofType(KotlinEffectMetadata::class),
                withArg { flags.invoke(it.flags) }
            )
        }
    }
}
