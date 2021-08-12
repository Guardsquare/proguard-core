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

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import proguard.classfile.AccessConstants
import proguard.classfile.ClassConstants
import proguard.classfile.Clazz
import proguard.classfile.LibraryClass
import proguard.classfile.LibraryField
import proguard.classfile.LibraryMethod

class LibraryClassEditorTest : FreeSpec({

    "Given a LibraryClassEditor, initialized from a given LibraryClass" - {
        val libraryClass = LibraryClass(
            AccessConstants.PUBLIC,
            "MyLibraryClass",
            ClassConstants.NAME_JAVA_LANG_OBJECT
        )
        val editor = LibraryClassEditor(libraryClass)

        "When adding an interface by name and class" - {
            val myInterfaceName = "myInterfaceName"
            val myInterface = mockk<Clazz>()
            every { myInterface.name } returns myInterfaceName
            editor.addInterface(myInterfaceName, myInterface)
            "Then the interfaceNames of the LibraryClass should contain the name of the added interface" {
                libraryClass.interfaceNames shouldContain myInterfaceName
            }
            "Then the interfaceClasses of the LibraryClass should contain the interface" {
                libraryClass.interfaceClasses shouldContain myInterface
            }
            "Then the interfaceClasses of the LibraryClass should contain the interface at " +
                "the same index as the interface name is in the interfaceNames" {
                    val indexOfInterfaceName = libraryClass.interfaceNames.indexOf(myInterfaceName)
                    libraryClass.interfaceClasses[indexOfInterfaceName] shouldBe myInterface
                }

            "When removing the interface by name" - {
                editor.removeInterface(myInterfaceName)
                "Then the LibraryClass should no longer contain the interface name" {
                    libraryClass.interfaceNames shouldNotContain myInterfaceName
                }
                "Then the LibraryClass should no longer contain the interface class" {
                    libraryClass.interfaceClasses shouldNotContain myInterface
                }
            }
        }

        "When adding a method" - {
            val method = LibraryMethod()
            editor.addMethod(method)
            "Then the methods of the LibraryClass should contain this method" {
                libraryClass.methods shouldContain method
            }
            "When removing the method" - {
                editor.removeMethod(method)
                "Then the methods of the LibraryClass should not contain this method" {
                    libraryClass.methods shouldNotContain method
                }
            }
        }

        "When adding a field" - {
            val field = LibraryField()
            editor.addField(field)
            "Then the fields of the LibraryClass should contain this field" {
                libraryClass.fields shouldContain field
            }
            "fields removing the field" - {
                editor.removeField(field)
                "Then the fields of the LibraryClass should not contain this field" {
                    libraryClass.fields shouldNotContain field
                }
            }
        }
    }
})
