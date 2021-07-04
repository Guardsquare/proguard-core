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
import proguard.classfile.kotlin.KotlinDeclarationContainerMetadata
import proguard.classfile.kotlin.visitor.AllPropertyVisitor
import proguard.classfile.kotlin.visitor.KotlinMetadataVisitor
import proguard.classfile.kotlin.visitor.KotlinPropertyVisitor
import proguard.classfile.kotlin.visitor.ReferencedKotlinMetadataVisitor
import proguard.classfile.kotlin.visitor.filter.KotlinPropertyFilter
import testutils.ClassPoolBuilder
import testutils.KotlinSource
import testutils.ReWritingMetadataVisitor
import java.util.function.Predicate

class KotlinPropertyFlagsTest : FreeSpec({
    val (programClassPool, _) = ClassPoolBuilder.fromSource(
        KotlinSource(
            "Test.kt",
            """
            import kotlin.reflect.KProperty

            val valProperty: String = "valProperty"
            var varProperty: String = "varProperty"
            const val constProperty: String = "constProperty"
            val delegateProperty: String by lazy { "foo" }

            interface Foo {
                companion object {
                     @JvmField
                     val companionProperty:String = "string"
                }
            }

            class Bar {
                lateinit var lateInitProperty: String
            }
            """
        )
    )

    include(
        "Given a val property",
        testPropertyFlags(programClassPool.getClass("TestKt"), "valProperty") {

            withClue("isDeclared") { it.isDeclared shouldBe true }
            withClue("isFakeOverride") { it.isFakeOverride shouldBe false }
            withClue("isDelegation") { it.isDelegation shouldBe false }
            withClue("isSynthesized") { it.isSynthesized shouldBe false }
            withClue("isVar") { it.isVar shouldBe false }
            withClue("hasGetter") { it.hasGetter shouldBe true }
            withClue("hasSetter") { it.hasSetter shouldBe false }
            withClue("isConst") { it.isConst shouldBe false }
            withClue("isLateinit") { it.isLateinit shouldBe false }
            withClue("hasConstant") { it.hasConstant shouldBe true }
            withClue("isExternal") { it.isExternal shouldBe false }
            withClue("isDelegated") { it.isDelegated shouldBe false }
            withClue("isExpect") { it.isExpect shouldBe false }
            withClue("isMovedFromInterfaceCompanion") { it.isMovedFromInterfaceCompanion shouldBe false }

            withClue("hasAnnotations") { it.common.hasAnnotations shouldBe false }
        }
    )

    include(
        "Given a variable property",
        testPropertyFlags(programClassPool.getClass("TestKt"), "varProperty") {

            withClue("isDeclared") { it.isDeclared shouldBe true }
            withClue("isFakeOverride") { it.isFakeOverride shouldBe false }
            withClue("isDelegation") { it.isDelegation shouldBe false }
            withClue("isSynthesized") { it.isSynthesized shouldBe false }
            withClue("isVar") { it.isVar shouldBe true }
            withClue("hasGetter") { it.hasGetter shouldBe true }
            withClue("hasSetter") { it.hasSetter shouldBe true }
            withClue("isConst") { it.isConst shouldBe false }
            withClue("isLateinit") { it.isLateinit shouldBe false }
            withClue("hasConstant") { it.hasConstant shouldBe false }
            withClue("isExternal") { it.isExternal shouldBe false }
            withClue("isDelegated") { it.isDelegated shouldBe false }
            withClue("isExpect") { it.isExpect shouldBe false }
            withClue("isMovedFromInterfaceCompanion") { it.isMovedFromInterfaceCompanion shouldBe false }

            withClue("hasAnnotations") { it.common.hasAnnotations shouldBe false }
        }
    )

    include(
        "Given a constant property",
        testPropertyFlags(programClassPool.getClass("TestKt"), "constProperty") {

            withClue("isDeclared") { it.isDeclared shouldBe true }
            withClue("isFakeOverride") { it.isFakeOverride shouldBe false }
            withClue("isDelegation") { it.isDelegation shouldBe false }
            withClue("isSynthesized") { it.isSynthesized shouldBe false }
            withClue("isVar") { it.isVar shouldBe false }
            withClue("hasGetter") { it.hasGetter shouldBe true }
            withClue("hasSetter") { it.hasSetter shouldBe false }
            withClue("isConst") { it.isConst shouldBe true }
            withClue("isLateinit") { it.isLateinit shouldBe false }
            withClue("hasConstant") { it.hasConstant shouldBe true }
            withClue("isExternal") { it.isExternal shouldBe false }
            withClue("isDelegated") { it.isDelegated shouldBe false }
            withClue("isExpect") { it.isExpect shouldBe false }
            withClue("isMovedFromInterfaceCompanion") { it.isMovedFromInterfaceCompanion shouldBe false }

            withClue("hasAnnotations") { it.common.hasAnnotations shouldBe false }
        }
    )

    include(
        "Given a property annotated with @JvmField in an interface companion object",
        testPropertyFlags(programClassPool.getClass("Foo\$Companion"), "companionProperty") {

            withClue("isDeclared") { it.isDeclared shouldBe true }
            withClue("isFakeOverride") { it.isFakeOverride shouldBe false }
            withClue("isDelegation") { it.isDelegation shouldBe false }
            withClue("isSynthesized") { it.isSynthesized shouldBe false }
            withClue("isVar") { it.isVar shouldBe false }
            withClue("hasGetter") { it.hasGetter shouldBe true }
            withClue("hasSetter") { it.hasSetter shouldBe false }
            withClue("isConst") { it.isConst shouldBe false }
            withClue("isLateinit") { it.isLateinit shouldBe false }
            withClue("hasConstant") { it.hasConstant shouldBe true }
            withClue("isExternal") { it.isExternal shouldBe false }
            withClue("isDelegated") { it.isDelegated shouldBe false }
            withClue("isExpect") { it.isExpect shouldBe false }
            withClue("isMovedFromInterfaceCompanion") { it.isMovedFromInterfaceCompanion shouldBe true }

            withClue("hasAnnotations") { it.common.hasAnnotations shouldBe true }
        }
    )

    include(
        "Given a late init var property",
        testPropertyFlags(programClassPool.getClass("Bar"), "lateInitProperty") {

            withClue("isDeclared") { it.isDeclared shouldBe true }
            withClue("isFakeOverride") { it.isFakeOverride shouldBe false }
            withClue("isDelegation") { it.isDelegation shouldBe false }
            withClue("isSynthesized") { it.isSynthesized shouldBe false }
            withClue("isVar") { it.isVar shouldBe true }
            withClue("hasGetter") { it.hasGetter shouldBe true }
            withClue("hasSetter") { it.hasSetter shouldBe true }
            withClue("isConst") { it.isConst shouldBe false }
            withClue("isLateinit") { it.isLateinit shouldBe true }
            withClue("hasConstant") { it.hasConstant shouldBe false }
            withClue("isExternal") { it.isExternal shouldBe false }
            withClue("isDelegated") { it.isDelegated shouldBe false }
            withClue("isExpect") { it.isExpect shouldBe false }
            withClue("isMovedFromInterfaceCompanion") { it.isMovedFromInterfaceCompanion shouldBe false }

            withClue("hasAnnotations") { it.common.hasAnnotations shouldBe false }
        }
    )

    include(
        "Given a delegate property",
        testPropertyFlags(programClassPool.getClass("TestKt"), "delegateProperty") {

            withClue("isDeclared") { it.isDeclared shouldBe true }
            withClue("isFakeOverride") { it.isFakeOverride shouldBe false }
            withClue("isDelegation") { it.isDelegation shouldBe false }
            withClue("isSynthesized") { it.isSynthesized shouldBe false }
            withClue("isVar") { it.isVar shouldBe false }
            withClue("hasGetter") { it.hasGetter shouldBe true }
            withClue("hasSetter") { it.hasSetter shouldBe false }
            withClue("isConst") { it.isConst shouldBe false }
            withClue("isLateinit") { it.isLateinit shouldBe false }
            withClue("hasConstant") { it.hasConstant shouldBe false }
            withClue("isExternal") { it.isExternal shouldBe false }
            withClue("isDelegated") { it.isDelegated shouldBe true }
            withClue("isExpect") { it.isExpect shouldBe false }
            withClue("isMovedFromInterfaceCompanion") { it.isMovedFromInterfaceCompanion shouldBe false }

            withClue("hasAnnotations") { it.common.hasAnnotations shouldBe false }
        }
    )

    // TODO(T5480): more tests to cover isFakeOverride, isDelegation, isSynthesized, isExternal, isExpect
})

private fun createVisitor(typeName: String, typeVisitor: KotlinPropertyVisitor): KotlinMetadataVisitor =
    AllPropertyVisitor(
        KotlinPropertyFilter(
            Predicate { it.name == typeName },
            typeVisitor
        )
    )

private fun testPropertyFlags(clazz: Clazz, propName: String, flags: (KotlinPropertyFlags) -> Unit) = funSpec {

    test("Then $propName flags should be initialized correctly") {
        val propertyVisitor = spyk<KotlinPropertyVisitor>()
        clazz.accept(ReferencedKotlinMetadataVisitor(createVisitor(propName, propertyVisitor)))

        verify {
            propertyVisitor.visitAnyProperty(
                clazz,
                ofType(KotlinDeclarationContainerMetadata::class),
                withArg { flags.invoke(it.flags) }
            )
        }
    }

    test("Then $propName flags should be written and re-initialized correctly") {
        val propertyVisitor = spyk<KotlinPropertyVisitor>()
        clazz.accept(ReWritingMetadataVisitor(createVisitor(propName, propertyVisitor)))

        verify {
            propertyVisitor.visitAnyProperty(
                clazz,
                ofType(KotlinDeclarationContainerMetadata::class),
                withArg { flags.invoke(it.flags) }
            )
        }
    }
}
