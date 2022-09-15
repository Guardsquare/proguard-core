package proguard.classfile.util

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSingleElement
import io.kotest.matchers.collections.shouldHaveSize
import proguard.classfile.AccessConstants
import proguard.classfile.ClassConstants
import proguard.classfile.ClassPool
import proguard.classfile.Clazz
import proguard.classfile.ProgramClass
import proguard.classfile.VersionConstants
import proguard.classfile.editor.ConstantPoolEditor
import proguard.classfile.visitor.ClassCollector
import proguard.testutils.ClassPoolBuilder

class ClassSubHierarchyInitializerTest : StringSpec({
    "SubclassesAccept should work after running the ClassSubHierarchyInitializer" {
        val clazz1 = ClassBuilder.createClass("clazz1")
        val clazz2 = ClassBuilder.createClass("clazz2")

        clazz2.u2superClass = ConstantPoolEditor(clazz2).addClassConstant(clazz1)

        val classPool = ClassPoolBuilder.fromClasses(clazz1, clazz2)
        classPool.classesAccept(ClassSubHierarchyInitializer())

        val classList = mutableListOf<Clazz>()
        clazz1.subclassesAccept(ClassCollector(classList))
        classList shouldHaveSingleElement(clazz2)
    }

    "The ClassSubHierarchyInitializer should allow re-initialization of the sub-hierarchy in the optimized implementation" {
        val clazz1 = ClassBuilder.createClass("clazz1")
        val clazz2 = ClassBuilder.createClass("clazz2")

        clazz2.u2superClass = ConstantPoolEditor(clazz2).addClassConstant(clazz1)

        val classPool = ClassPoolBuilder.fromClasses(clazz1, clazz2)
        classPool.classesAccept(ClassSubHierarchyInitializer())

        classPool.removeClass(clazz2)
        val classList = mutableListOf<Clazz>()

        // Optimized implementation.
        classPool.accept(ClassSubHierarchyInitializer())
        clazz1.subclassesAccept(ClassCollector(classList))
        classList shouldHaveSize 0
    }
})

private class ClassBuilder {
    companion object {
        /**
         * Creates a Java 7 class without methods or attributes.
         */
        fun createClass(className: String, superClass: ProgramClass? = null): ProgramClass {
            val clazz = ProgramClass(
                VersionConstants.CLASS_VERSION_1_7,
                1, arrayOfNulls(1),
                AccessConstants.PUBLIC,
                0,
                0
            )
            val constantPoolEditor = ConstantPoolEditor(clazz)
            clazz.u2thisClass = constantPoolEditor.addClassConstant(className, null)
            clazz.u2superClass = constantPoolEditor.addClassConstant(ClassConstants.NAME_JAVA_LANG_OBJECT, null)

            if (superClass != null) {
                addInheritance(clazz, superClass)
            }
            return clazz
        }

        private fun addInheritance(subClass: ProgramClass, superClass: ProgramClass) {
            val classPool = ClassPool()
            classPool.addClass(subClass)
            classPool.addClass(superClass)

            subClass.u2superClass = ConstantPoolEditor(subClass).addClassConstant(superClass)

            classPool.accept(ClassSubHierarchyInitializer())
            classPool.classesAccept(ClassSuperHierarchyInitializer(classPool, ClassPool()))
        }
    }
}
