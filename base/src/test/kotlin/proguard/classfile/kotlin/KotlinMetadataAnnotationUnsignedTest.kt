/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2020 Guardsquare NV
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

import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.FreeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import io.mockk.spyk
import io.mockk.verify
import proguard.classfile.kotlin.visitor.AllKotlinAnnotationArgumentVisitor
import proguard.classfile.kotlin.visitor.AllKotlinAnnotationVisitor
import proguard.classfile.kotlin.visitor.AllTypeAliasVisitor
import proguard.classfile.kotlin.visitor.KotlinAnnotationArgumentVisitor
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.KotlinSource
import proguard.testutils.ReWritingMetadataVisitor

@OptIn(ExperimentalKotest::class)
class KotlinMetadataAnnotationUnsignedTest : FreeSpec({
    withData(
        UnsignedTestValues(
            name = "Unsigned zero should be converted to signed 0",
            uByte = "0u" expect 0,
            uShort = "0u" expect 0,
            uInt = "0u" expect 0,
            uLong = "0u" expect 0L
        ),
        UnsignedTestValues(
            name = "Unsigned MAX_VALUE should be converted to signed -1",
            uByte = "UByte.MAX_VALUE" expect -1,
            uShort = "UShort.MAX_VALUE" expect -1,
            uInt = "UInt.MAX_VALUE" expect -1,
            uLong = "ULong.MAX_VALUE" expect -1L
        ),
        UnsignedTestValues(
            name = "Unsigned MIN_VALUE should be converted to signed 0",
            uByte = "UByte.MIN_VALUE" expect 0,
            uShort = "UShort.MIN_VALUE" expect 0,
            uInt = "UInt.MIN_VALUE" expect 0,
            uLong = "ULong.MIN_VALUE" expect 0L
        ),
        UnsignedTestValues(
            name = "Unsigned (MAX_VALUE - 1) should be converted to signed -2",
            uByte = "${UByte.MAX_VALUE - 1u}u" expect -2,
            uShort = "${UShort.MAX_VALUE - 1u}u" expect -2,
            uInt = "${UInt.MAX_VALUE - 1u}u" expect -2,
            uLong = "${ULong.MAX_VALUE - 1u}u" expect -2,
        ),
        UnsignedTestValues(
            name = "Unsigned (MIN_VALUE + 1) should be converted to signed 1",
            uByte = "${UByte.MIN_VALUE + 1u}u" expect 1,
            uShort = "${UShort.MIN_VALUE + 1u}u" expect 1,
            uInt = "${UInt.MIN_VALUE + 1u}u" expect 1,
            uLong = "${ULong.MIN_VALUE + 1u}u" expect 1,
        ),
    ) { (_, uByte, uShort, uInt, uLong) ->
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            KotlinSource(
                "TestUnsigned.kt",
                """
                @Target(AnnotationTarget.TYPEALIAS)
                annotation class MyTypeAliasAnnotationWithUnsigned(
                    val uByte: UByte,
                    val uShort: UShort,
                    val uInt: UInt,
                    val uLong: ULong,
                )

                @MyTypeAliasAnnotationWithUnsigned(
                    uByte = ${uByte.first},
                    uShort = ${uShort.first},
                    uInt = ${uInt.first},
                    uLong = ${uLong.first}
                )
                typealias myAliasWithUnsigned = String
                """.trimIndent()
            ),
            kotlincArguments = listOf("-Xuse-experimental=kotlin.ExperimentalUnsignedTypes")
        )

        val annotationArgVisitor = spyk<KotlinAnnotationArgumentVisitor>()
        val clazz = programClassPool.getClass("TestUnsignedKt")

        programClassPool.classesAccept(
            clazz.name,
            ReWritingMetadataVisitor(
                AllTypeAliasVisitor(
                    AllKotlinAnnotationVisitor(
                        AllKotlinAnnotationArgumentVisitor(annotationArgVisitor)
                    )
                )
            )
        )

        verify(exactly = 1) {
            annotationArgVisitor.visitUByteArgument(
                clazz,
                ofType<KotlinAnnotatable>(),
                ofType<KotlinAnnotation>(),
                withArg { it.name shouldBe "uByte" },
                KotlinAnnotationArgument.UByteValue(uByte.second)
            )

            annotationArgVisitor.visitUShortArgument(
                clazz,
                ofType<KotlinAnnotatable>(),
                ofType<KotlinAnnotation>(),
                withArg { it.name shouldBe "uShort" },
                KotlinAnnotationArgument.UShortValue(uShort.second)
            )

            annotationArgVisitor.visitUIntArgument(
                clazz,
                ofType<KotlinAnnotatable>(),
                ofType<KotlinAnnotation>(),
                withArg { it.name shouldBe "uInt" },
                KotlinAnnotationArgument.UIntValue(uInt.second)
            )

            annotationArgVisitor.visitULongArgument(
                clazz,
                ofType<KotlinAnnotatable>(),
                ofType<KotlinAnnotation>(),
                withArg { it.name shouldBe "uLong" },
                KotlinAnnotationArgument.ULongValue(uLong.second)
            )
        }
    }
})
private data class UnsignedTestValues(
    val name: String,
    val uByte: Pair<String, Byte>,
    val uShort: Pair<String, Short>,
    val uInt: Pair<String, Int>,
    val uLong: Pair<String, Long>
) {
    override fun toString(): String = name
}

private inline infix fun <reified T : Number> String.expect(value: T) = Pair(this, value)
