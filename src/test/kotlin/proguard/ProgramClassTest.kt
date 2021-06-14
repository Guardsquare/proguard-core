/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2020 Guardsquare NV
 */

package proguard

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import proguard.classfile.Clazz
import proguard.classfile.ProgramClass
import proguard.classfile.visitor.ClassCollector

class ProgramClassTest : StringSpec({
    "Initially a class should have no subclasses" {
        val programClass = ProgramClass()
        val classList = mutableListOf<Clazz>()
        programClass.subclassesAccept(ClassCollector(classList))
        classList.shouldBeEmpty()
    }

    "Adding a subclass should add it to the array of subclasses" {
        val programClass = ProgramClass()
        val programClass2 = ProgramClass()
        val classList = mutableListOf<Clazz>()

        programClass.addSubClass(programClass2)
        programClass.subclassesAccept(ClassCollector(classList))
        classList.shouldContainExactly(programClass2)
    }

    "Adding and removing the same subclass should result in no subclasses" {
        val programClass = ProgramClass()
        val programClass2 = ProgramClass()
        val classList = mutableListOf<Clazz>()

        programClass.addSubClass(programClass2)
        programClass.removeSubClass(programClass2)
        programClass.subclassesAccept(ClassCollector(classList))
        classList.shouldBeEmpty()
    }
})
