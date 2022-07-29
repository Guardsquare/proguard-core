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
import proguard.classfile.kotlin.visitor.AllValueParameterVisitor
import proguard.classfile.kotlin.visitor.KotlinMetadataVisitor
import proguard.classfile.kotlin.visitor.KotlinValueParameterVisitor
import proguard.classfile.kotlin.visitor.ReferencedKotlinMetadataVisitor
import proguard.classfile.kotlin.visitor.filter.KotlinValueParameterFilter
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.KotlinSource
import proguard.testutils.ReWritingMetadataVisitor
import java.util.function.Predicate

class KotlinValueParameterFlagsTest : FreeSpec({
    val (programClassPool, _) = ClassPoolBuilder.fromSource(
        KotlinSource(
            "Test.kt",
            """
             fun foo(valueParamWithoutDefault: String) = valueParamWithoutDefault
             fun foo1(valueParamWithDefault: String = "default") = valueParamWithDefault

             @Suppress("NOTHING_TO_INLINE")
             inline fun foo2(noinline noinlineValueParam: () -> Unit) = noinlineValueParam
             inline fun foo3(crossinline crossinlineValueParam: () -> Unit) = crossinlineValueParam()

             @Suppress("NOTHING_TO_INLINE")
             inline fun foo4(noinline noinlineVPWithDefault: () -> Unit = {}) = noinlineVPWithDefault
             inline fun foo5(crossinline crossinlineVPWithDefault: () -> Unit = {}) = crossinlineVPWithDefault()
            """
        )
    )

    include(
        "Given a value parameter without a default value",
        testValueParameterFlags(programClassPool.getClass("TestKt"), "valueParamWithoutDefault") {
            withClue("hasDefaultValue") { it.hasDefaultValue shouldBe false }
            withClue("isNoInline") { it.isNoInline shouldBe false }
            withClue("isCrossInline") { it.isCrossInline shouldBe false }

            withClue("hasAnnotations") { it.common.hasAnnotations shouldBe false }
        }
    )

    include(
        "Given a value parameter with a default value",
        testValueParameterFlags(programClassPool.getClass("TestKt"), "valueParamWithDefault") {
            withClue("hasDefaultValue") { it.hasDefaultValue shouldBe true }
            withClue("isNoInline") { it.isNoInline shouldBe false }
            withClue("isCrossInline") { it.isCrossInline shouldBe false }

            withClue("hasAnnotations") { it.common.hasAnnotations shouldBe false }
        }
    )

    include(
        "Given an noinline value parameter",
        testValueParameterFlags(programClassPool.getClass("TestKt"), "noinlineValueParam") {
            withClue("hasDefaultValue") { it.hasDefaultValue shouldBe false }
            withClue("isNoInline") { it.isNoInline shouldBe true }
            withClue("isCrossInline") { it.isCrossInline shouldBe false }

            withClue("hasAnnotations") { it.common.hasAnnotations shouldBe false }
        }
    )

    include(
        "Given an crossinline value parameter",
        testValueParameterFlags(programClassPool.getClass("TestKt"), "crossinlineValueParam") {
            withClue("hasDefaultValue") { it.hasDefaultValue shouldBe false }
            withClue("isNoInline") { it.isNoInline shouldBe false }
            withClue("isCrossInline") { it.isCrossInline shouldBe true }

            withClue("hasAnnotations") { it.common.hasAnnotations shouldBe false }
        }
    )

    include(
        "Given an noinline value parameter with default value",
        testValueParameterFlags(programClassPool.getClass("TestKt"), "noinlineVPWithDefault") {
            withClue("hasDefaultValue") { it.hasDefaultValue shouldBe true }
            withClue("isNoInline") { it.isNoInline shouldBe true }
            withClue("isCrossInline") { it.isCrossInline shouldBe false }

            withClue("hasAnnotations") { it.common.hasAnnotations shouldBe false }
        }
    )

    include(
        "Given an crossinline value parameter with default value",
        testValueParameterFlags(programClassPool.getClass("TestKt"), "crossinlineVPWithDefault") {
            withClue("hasDefaultValue") { it.hasDefaultValue shouldBe true }
            withClue("isNoInline") { it.isNoInline shouldBe false }
            withClue("isCrossInline") { it.isCrossInline shouldBe true }

            withClue("hasAnnotations") { it.common.hasAnnotations shouldBe false }
        }
    )
})

private fun createVisitor(valueParameterName: String, valueParameterVisitor: KotlinValueParameterVisitor): KotlinMetadataVisitor =
    AllValueParameterVisitor(
        KotlinValueParameterFilter(
            Predicate { it.parameterName == valueParameterName },
            valueParameterVisitor
        )
    )

private fun testValueParameterFlags(clazz: Clazz, propName: String, flags: (KotlinValueParameterFlags) -> Unit) = funSpec {

    test("Then $propName flags should be initialized correctly") {
        val propertyVisitor = spyk<KotlinValueParameterVisitor>()
        clazz.accept(ReferencedKotlinMetadataVisitor(createVisitor(propName, propertyVisitor)))

        verify {
            propertyVisitor.visitAnyValueParameter(
                clazz,
                withArg { flags.invoke(it.flags) }
            )
        }
    }

    test("Then $propName flags should be written and re-initialized correctly") {
        val propertyVisitor = spyk<KotlinValueParameterVisitor>()
        clazz.accept(ReWritingMetadataVisitor(createVisitor(propName, propertyVisitor)))

        verify {
            propertyVisitor.visitAnyValueParameter(
                clazz,
                withArg { flags.invoke(it.flags) }
            )
        }
    }
}
