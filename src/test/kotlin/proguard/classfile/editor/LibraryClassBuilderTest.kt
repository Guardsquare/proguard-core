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

package proguard.classfile.editor

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldExist
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import proguard.classfile.AccessConstants.PUBLIC
import proguard.classfile.ClassConstants
import proguard.classfile.Clazz
import proguard.classfile.LibraryClass
import proguard.classfile.LibraryField
import proguard.classfile.LibraryMethod
import proguard.classfile.kotlin.KotlinClassKindMetadata
import proguard.classfile.visitor.MemberVisitor

class LibraryClassBuilderTest : FreeSpec({
    "Given access flags, a class name and super class name" - {
        val accessFlags = PUBLIC
        val className = "MyClass"
        val superClassName = "MySuperClass"
        "When initializing a new LibraryClassBuilder" - {
            val builder = LibraryClassBuilder(accessFlags, className, superClassName)
            "Then the newly built LibraryClass has the given arguments as its properties" {
                builder.libraryClass.accessFlags shouldBe accessFlags
                builder.libraryClass.thisClassName shouldBe className
                builder.libraryClass.superClassName shouldBe superClassName
            }
        }
    }

    "Given access flags, a class name, a super class name, interface names, interface classes, sub class count, " +
        "sub classes, fields, methods, and kotlin meta data" - {
        val accessFlags = PUBLIC
        val className = "MyClass"
        val superClassName = "MySuperClass"
        val interfaceNames = emptyArray<String>()
        val interfaceClasses = emptyArray<Clazz>()
        val subClassCount = 0
        val subClasses = emptyArray<Clazz>()
        val fields = emptyArray<LibraryField>()
        val methods = emptyArray<LibraryMethod>()
        val metaData = KotlinClassKindMetadata(IntArray(0), 0, "", "")
        "When initializing a new LibraryClassBuilder" - {
            val builder = LibraryClassBuilder(
                accessFlags,
                className,
                superClassName,
                interfaceNames,
                interfaceClasses,
                subClassCount,
                subClasses,
                fields,
                methods,
                metaData
            )
            "Then the newly built LibraryClass has the given arguments as its properties" {
                builder.libraryClass.accessFlags shouldBe accessFlags
                builder.libraryClass.thisClassName shouldBe className
                builder.libraryClass.superClassName shouldBe superClassName
                builder.libraryClass.interfaceNames shouldBe interfaceNames
                builder.libraryClass.interfaceClasses shouldBe interfaceClasses
                builder.libraryClass.subClassCount shouldBe subClassCount
                builder.libraryClass.subClasses shouldBe subClasses
                builder.libraryClass.fields shouldBe fields
                builder.libraryClass.methods shouldBe methods
                builder.libraryClass.kotlinMetadata shouldBe metaData
            }
        }
    }

    "Given a LibraryClassBuilder, initialized from a given LibraryClass" - {
        val libraryClass = LibraryClass(
            PUBLIC,
            "MyLibraryClass",
            ClassConstants.NAME_JAVA_LANG_OBJECT
        )
        val builder = LibraryClassBuilder(libraryClass)

        "When getting the built LibraryClass" - {
            val builtLibraryClass = builder.libraryClass
            "Then the returned LibraryClass references the originally provided LibraryClass" {
                builtLibraryClass shouldBe libraryClass
            }
        }

        "When getting the ConstantPoolEditor of this LibraryClass(Builder)" - {
            "Then the getter should throw an UnsupportedOperationException" {
                shouldThrow<UnsupportedOperationException> {
                    builder.constantPoolEditor
                }
            }
        }

        "When adding an interface by class" - {
            val myInterface = mockk<Clazz>()
            every { myInterface.name } returns "MyInterface"
            builder.addInterface(myInterface)
            "Then the interfaceNames of the LibraryClass should contain the name of the added interface" {
                libraryClass.interfaceNames shouldContain "MyInterface"
            }
            "Then the interfaceClasses of the LibraryClass should contain the interface" {
                libraryClass.interfaceClasses shouldContain myInterface
            }
        }

        "When adding an interface by name and class" - {
            val myInterfaceName = "myInterfaceName"
            val myInterface = mockk<Clazz>()
            every { myInterface.name } returns "MyCustomInterfaceName"
            builder.addInterface(myInterfaceName, myInterface)
            "Then the interfaceNames of the LibraryClass should contain the name of the added interface" {
                libraryClass.interfaceNames shouldContain myInterfaceName
                libraryClass.interfaceNames shouldNotContain "MyCustomInterfaceName"
            }
            "Then the interfaceClasses of the LibraryClass should contain the interface" {
                libraryClass.interfaceClasses shouldContain myInterface
            }
            "Then the interfaceClasses of the LibraryClass should contain the interface at " +
                "the same index as the interface name is in the interfaceNames" {
                    val indexOfInterfaceName = libraryClass.interfaceNames.indexOf(myInterfaceName)
                    libraryClass.interfaceClasses[indexOfInterfaceName] shouldBe myInterface
                }
        }

        "When adding an interface by name" - {
            val interfaceName = "MyInterfaceByName"
            builder.addInterface(interfaceName)
            "Then the interfaceNames of the LibraryClass should contain the added interface" {
                libraryClass.interfaceNames shouldContain interfaceName
            }
            "Then the interfaceClasses of the LibraryClass should contain null at the same index as the " +
                "interfaceName is in the interfaceNames of the LibraryClass" {
                    val indexOfInterface = libraryClass.interfaceNames.indexOf(interfaceName)
                    libraryClass.interfaceClasses[indexOfInterface] shouldBe null
                }
        }

        "When adding a method by signature" - {
            builder.addMethod(PUBLIC, "myMethod", "()V")
            "Then the methods of the LibraryClass should contain a method with this signature" {
                libraryClass.methods shouldExist {
                    it.u2accessFlags == PUBLIC && it.name == "myMethod" &&
                        it.descriptor == "()V"
                }
            }
        }

        "When adding a method by signature and with a MemberVisitor" - {
            val memberVisitor = mockk<MemberVisitor>()
            every { memberVisitor.visitLibraryMethod(any(), any()) } returns Unit
            builder.addMethod(PUBLIC, "myMethod", "()V", memberVisitor)
            "Then the methods of the LibraryClass should contain a method with this signature" {
                libraryClass.methods shouldExist {
                    it.u2accessFlags == PUBLIC && it.name == "myMethod" &&
                        it.descriptor == "()V"
                }
            }
            "Then the MemberVisitor must have visited a LibraryMethod with this signature exactly once" {
                verify(exactly = 1) {
                    memberVisitor.visitLibraryMethod(
                        libraryClass,
                        withArg {
                            it.u2accessFlags shouldBe PUBLIC
                            it.name shouldBe "myMethod"
                            it.descriptor shouldBe "()V"
                        }
                    )
                }
            }
        }

        "When adding a method by signature and returning it" - {
            val method = builder.addAndReturnMethod(PUBLIC, "myMethod", "()V")
            "Then the methods of the LibraryClass should contain the returned method" {
                libraryClass.methods shouldContain method
            }
            "Then the returned method should have the given signature" {
                method.u2accessFlags shouldBe PUBLIC
                method.name shouldBe "myMethod"
                method.descriptor shouldBe "()V"
            }
        }

        "When adding a field by signature" - {
            builder.addField(PUBLIC, "myField", "(Z)Ljava/lang/Boolean;")
            "Then the fields of the LibraryClass should contain a field with this signature" {
                libraryClass.fields shouldExist {
                    it.u2accessFlags == PUBLIC && it.name == "myField" &&
                        it.descriptor == "(Z)Ljava/lang/Boolean;"
                }
            }
        }

        "When adding a field by signature and with a MemberVisitor" - {
            val memberVisitor = mockk<MemberVisitor>()
            every { memberVisitor.visitLibraryField(any(), any()) } returns Unit
            builder.addField(PUBLIC, "myField", "(Z)Ljava/lang/Boolean;", memberVisitor)
            "Then the methods of the LibraryClass should contain a field with this signature" {
                libraryClass.fields shouldExist {
                    it.u2accessFlags == PUBLIC && it.name == "myField" &&
                        it.descriptor == "(Z)Ljava/lang/Boolean;"
                }
            }
            "Then the MemberVisitor must have visited a LibraryField with this signature exactly once" {
                verify(exactly = 1) {
                    memberVisitor.visitLibraryField(
                        libraryClass,
                        withArg {
                            it.u2accessFlags shouldBe PUBLIC
                            it.name shouldBe "myField"
                            it.descriptor shouldBe "(Z)Ljava/lang/Boolean;"
                        }
                    )
                }
            }
        }

        "When adding a field by signature and returning it" - {
            val field = builder.addAndReturnField(PUBLIC, "myField", "(Z)Ljava/lang/Boolean;")
            "Then the fields of the LibraryClass should contain the returned field" {
                libraryClass.fields shouldContain field
            }
            "Then the returned field should have the given signature" {
                field.u2accessFlags shouldBe PUBLIC
                field.name shouldBe "myField"
                field.descriptor shouldBe "(Z)Ljava/lang/Boolean;"
            }
        }
    }
})
