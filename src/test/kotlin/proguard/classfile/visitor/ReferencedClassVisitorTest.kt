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

package proguard.classfile.visitor

import io.kotest.core.spec.style.FreeSpec
import io.mockk.spyk
import io.mockk.verify
import proguard.classfile.LibraryClass
import proguard.classfile.ProgramClass
import testutils.ClassPoolBuilder
import testutils.KotlinSource
import proguard.classfile.kotlin.KotlinConstants.dummyClassPool as kotlinDummyClassPool

class ReferencedClassVisitorTest : FreeSpec({

    "Given Kotlin classes" - {
        val (programClassPool, libraryClassPool) = ClassPoolBuilder.fromSource(
            KotlinSource(
                "Test.kt",
                """
                import kotlin.reflect.KClass

                @Target(AnnotationTarget.TYPE)
                annotation class MyTypeAnnotation(
                    val kClass: KClass<*>,
                    val enum: MyEnum,
                    val array: Array<Foo2>,
                    val annotation: Foo
                )

                val x: @MyTypeAnnotation(
                    kClass = Bar::class,
                    enum = MyEnum.FOO,
                    array = arrayOf(Foo2("foo")),
                    annotation = Foo("foo")) String = "foo"

                // extra helpers

                enum class MyEnum { FOO, BAR }
                annotation class Foo(val string: String)
                annotation class Foo2(val string: String)
                data class Bar(val string: String)

                val p0: Byte = 1
                val p1: Short = 1
                val p2: Int = 1
                val p3: Long = 1
                val p4: Char = 'a'
                val p5: Float = 1f
                val p6: Double = 1.0
                val p7: Boolean = true
                """.trimIndent()
            )
        )

        val classVisitor = spyk<ClassVisitor>()
        programClassPool.classesAccept(ReferencedClassVisitor(true, classVisitor))

        "Then referenced Kotlin stdlib classes should be visited" {
            verify {
                with(libraryClassPool) {
                    classVisitor.visitLibraryClass(getClass("kotlin/Metadata") as LibraryClass)
                    classVisitor.visitLibraryClass(getClass("kotlin/reflect/KClass") as LibraryClass)
                }

                with(kotlinDummyClassPool) {
                    classVisitor.visitProgramClass(getClass("kotlin/Any") as ProgramClass)
                    classVisitor.visitProgramClass(getClass("kotlin/String") as ProgramClass)
                    classVisitor.visitProgramClass(getClass("kotlin/Byte") as ProgramClass)
                    classVisitor.visitProgramClass(getClass("kotlin/Short") as ProgramClass)
                    classVisitor.visitProgramClass(getClass("kotlin/Int") as ProgramClass)
                    classVisitor.visitProgramClass(getClass("kotlin/Long") as ProgramClass)
                    classVisitor.visitProgramClass(getClass("kotlin/Char") as ProgramClass)
                    classVisitor.visitProgramClass(getClass("kotlin/Float") as ProgramClass)
                    classVisitor.visitProgramClass(getClass("kotlin/Double") as ProgramClass)
                    classVisitor.visitProgramClass(getClass("kotlin/Boolean") as ProgramClass)
                    classVisitor.visitProgramClass(getClass("kotlin/Array") as ProgramClass)
                }
            }
        }

        "Then the file facade class should be visited" {
            verify {
                with(programClassPool) {
                    classVisitor.visitProgramClass(getClass("TestKt") as ProgramClass)
                }
            }
        }

        "Then classes referenced from Kotlin annotations should be visited" {
            verify {
                with(programClassPool) {
                    classVisitor.visitProgramClass(getClass("MyTypeAnnotation") as ProgramClass)
                    classVisitor.visitProgramClass(getClass("Foo") as ProgramClass)
                    classVisitor.visitProgramClass(getClass("Foo2") as ProgramClass)
                    classVisitor.visitProgramClass(getClass("MyEnum") as ProgramClass)
                }
            }
        }
    }
})
