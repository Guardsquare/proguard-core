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
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.spyk
import io.mockk.verify
import io.mockk.verifyOrder
import proguard.classfile.attribute.Attribute
import proguard.classfile.attribute.annotation.RuntimeInvisibleTypeAnnotationsAttribute
import proguard.classfile.attribute.visitor.AllAttributeVisitor
import proguard.classfile.attribute.visitor.AllRecordComponentInfoVisitor
import proguard.classfile.attribute.visitor.AttributeVisitor
import proguard.classfile.attribute.visitor.RecordComponentInfoVisitor
import proguard.classfile.util.MemberFinder
import proguard.classfile.visitor.AllMemberVisitor
import proguard.classfile.visitor.ConstructorMethodFilter
import proguard.classfile.visitor.MethodFilter
import testutils.ClassPoolBuilder
import testutils.JavaSource
import testutils.RequiresJavaVersion

@RequiresJavaVersion(15)
class JavaRecordTest : FreeSpec({

    "Given a Java record class" - {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            JavaSource(
                "Test.java",
                """
                public record Test (String firstname, String surname) { }
                """.trimIndent()
            ),
            javacArguments = if (testutils.currentJavaVersion == 15)
                listOf("--enable-preview", "--release", "15") else emptyList()
        )

        val recordClazz = programClassPool.getClass("Test")

        "Then the record class should not be null" {
            recordClazz shouldNotBe null
        }

        "Then the record component information should be correct" {
            val recordVisitor = spyk<RecordComponentInfoVisitor>()
            programClassPool.classesAccept(
                AllAttributeVisitor(
                    AllRecordComponentInfoVisitor(recordVisitor)
                )
            )

            verifyOrder {
                recordVisitor.visitRecordComponentInfo(
                    recordClazz,
                    withArg {
                        it.getName(recordClazz) shouldBe "firstname"
                        it.getDescriptor(recordClazz) shouldBe "Ljava/lang/String;"
                        it.referencedField shouldBe MemberFinder().findField(recordClazz, "firstname", "Ljava/lang/String;")
                    }
                )

                recordVisitor.visitRecordComponentInfo(
                    recordClazz,
                    withArg {
                        it.getName(recordClazz) shouldBe "surname"
                        it.getDescriptor(recordClazz) shouldBe "Ljava/lang/String;"
                        it.referencedField shouldBe MemberFinder().findField(recordClazz, "surname", "Ljava/lang/String;")
                    }
                )
            }
        }
    }

    "Given a record with a type annotation in its constructor" - {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            JavaSource("Test.java", "public record Test(@Annotation String str) {}"),
            JavaSource(
                "Annotation.java",
                """
                import java.lang.annotation.*;
                @Retention(value=RetentionPolicy.CLASS)
                @Target(value=ElementType.TYPE_USE)
                public @interface Annotation {}
                """.trimIndent()
            ),
            javacArguments = if (testutils.currentJavaVersion == 15)
                listOf("--enable-preview", "--release", "15") else emptyList()
        )
        val visitor = spyk<AttributeVisitor>(object : AttributeVisitor {
            override fun visitAnyAttribute(clazz: Clazz, attribute: Attribute) {}
        })

        programClassPool.classesAccept(
            "Test",
            AllMemberVisitor(
                MethodFilter(
                    ConstructorMethodFilter(
                        AllAttributeVisitor(false, visitor)
                    )
                )
            )
        )

        "Then the type annotation attribute should be visited" {
            verify(exactly = 1) {
                visitor.visitAnyTypeAnnotationsAttribute(
                    programClassPool.getClass("Test"),
                    ofType(RuntimeInvisibleTypeAnnotationsAttribute::class)
                )
            }
        }
    }
})
