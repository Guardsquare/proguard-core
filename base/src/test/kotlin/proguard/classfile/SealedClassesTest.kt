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

package proguard.classfile

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldExistInOrder
import io.kotest.matchers.shouldNotBe
import io.mockk.spyk
import io.mockk.verify
import proguard.classfile.attribute.Attribute.PERMITTED_SUBCLASSES
import proguard.classfile.attribute.PermittedSubclassesAttribute
import proguard.classfile.attribute.visitor.AllAttributeVisitor
import proguard.classfile.attribute.visitor.AttributeNameFilter
import proguard.classfile.attribute.visitor.AttributeVisitor
import proguard.classfile.constant.ClassConstant
import proguard.classfile.constant.Constant
import proguard.classfile.constant.visitor.ConstantVisitor
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.JavaSource
import proguard.testutils.RequiresJavaVersion
import proguard.testutils.currentJavaVersion

@RequiresJavaVersion(15)
class SealedClassesTest : FreeSpec({

    "Given a Java record class" - {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            JavaSource(
                "Test.java",
                """
                public sealed class Test permits A, B, C { }
                final class A extends Test {}
                final class B extends Test {}
                final class C extends Test {}
                """.trimIndent()
            ),
            javacArguments = if (currentJavaVersion == 15 || currentJavaVersion == 16)
                listOf("--enable-preview", "--release", "$currentJavaVersion") else emptyList()
        )

        val clazz = programClassPool.getClass("Test")

        "Then the record class should not be null" {
            clazz shouldNotBe null
        }

        "Then the sealed classes should be listed" {
            val permittedClassVisitor = spyk(object : ConstantVisitor {
                override fun visitAnyConstant(clazz: Clazz, constant: Constant) {}
            })

            programClassPool.classesAccept(
                "Test",
                AllAttributeVisitor(
                    AttributeNameFilter(
                        PERMITTED_SUBCLASSES,
                        object : AttributeVisitor {
                            override fun visitPermittedSubclassesAttribute(clazz: Clazz, permittedSubclassesAttribute: PermittedSubclassesAttribute) {
                                permittedSubclassesAttribute.permittedSubclassConstantsAccept(clazz, permittedClassVisitor)
                            }
                        }
                    )
                )
            )

            val permittedSubclasses = mutableListOf<ClassConstant>()

            verify(exactly = 3) {
                permittedClassVisitor.visitClassConstant(
                    clazz,
                    capture(permittedSubclasses)
                )
            }

            permittedSubclasses.map { it.referencedClass }.shouldExistInOrder(
                { it == programClassPool.getClass("A") },
                { it == programClassPool.getClass("B") },
                { it == programClassPool.getClass("C") }
            )
        }
    }
})
