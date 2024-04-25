package proguard.classfile.util

import io.kotest.assertions.throwables.shouldThrowMessage
import io.kotest.core.spec.style.BehaviorSpec
import proguard.classfile.AccessConstants
import proguard.classfile.ClassPool
import proguard.classfile.Clazz
import proguard.classfile.JavaConstants
import proguard.classfile.editor.LibraryClassBuilder
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.JavaSource

class ClassSuperHierarchyInitializerTest : BehaviorSpec({
    Given("A missing reference visitor") {
        When("A library class extends a program class") {
            val (programClassPool, _) = ClassPoolBuilder.fromSource(
                JavaSource(
                    "A.java",
                    """
                    public class A {                    
                    }   
                    """.trimIndent(),
                ),
            )

            val libraryClassPool = ClassPool()
            val builder = LibraryClassBuilder(AccessConstants.PUBLIC, "B", "A")
            libraryClassPool.addClass(builder.libraryClass)

            val missingReferenceCrasher = object : InvalidClassReferenceVisitor {
                override fun visitMissingClass(referencingClazz: Clazz, reference: String) {
                    if (!reference.startsWith(ClassUtil.internalClassName(JavaConstants.PACKAGE_JAVA_LANG))) {
                        throw RuntimeException("Missing reference: $reference")
                    }
                }

                override fun visitProgramDependency(referencingClazz: Clazz, dependency: Clazz) {
                    throw RuntimeException("Library class depending on program class: ${dependency.getName()}")
                }
            }

            Then("visitProgramDependency should be invoked") {
                val hierarchyInitializer = ClassSuperHierarchyInitializer(programClassPool, libraryClassPool, missingReferenceCrasher)

                shouldThrowMessage("Library class depending on program class: A") {
                    // Test library class extending program class.
                    libraryClassPool.classesAccept(hierarchyInitializer)
                }
            }
        }
    }
})
