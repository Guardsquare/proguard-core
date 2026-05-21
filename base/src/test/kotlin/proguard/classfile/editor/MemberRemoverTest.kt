package proguard.classfile.editor

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.inspectors.shouldForAll
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import proguard.classfile.AccessConstants.PUBLIC
import proguard.classfile.AccessConstants.STATIC
import proguard.classfile.ClassConstants.NAME_JAVA_LANG_OBJECT
import proguard.classfile.Clazz
import proguard.classfile.Field
import proguard.classfile.LibraryClass
import proguard.classfile.LibraryField
import proguard.classfile.LibraryMethod
import proguard.classfile.Member
import proguard.classfile.Method
import proguard.classfile.ProgramClass
import proguard.classfile.ProgramField
import proguard.classfile.visitor.AllMemberVisitor
import proguard.classfile.visitor.MemberCounter
import proguard.classfile.visitor.MemberToClassVisitor
import proguard.classfile.visitor.MemberVisitor
import proguard.classfile.visitor.MethodCollector
import proguard.classfile.visitor.MethodFilter
import proguard.classfile.visitor.MultiClassVisitor
import proguard.classfile.visitor.MultiMemberVisitor
import proguard.exception.ErrorId
import proguard.exception.ProguardCoreException
import proguard.testutils.classfile.extensions.addStaticField
import proguard.testutils.classfile.extensions.addStaticMethod
import proguard.testutils.classfile.extensions.buildClass

const val FIELD_DESCRIPTOR = "I"
const val METHOD_DESCRIPTOR = "()V"

const val PROGRAM_CLASS_1_FIELD_1_NAME = "programClass1Field1"
const val PROGRAM_CLASS_1_FIELD_2_NAME = "programClass1Field2"
const val PROGRAM_CLASS_2_FIELD_1_NAME = "programClass2Field1"
const val PROGRAM_CLASS_2_FIELD_2_NAME = "programClass2Field2"
const val LIBRARY_CLASS_FIELD_1_NAME = "libraryClassField1"
const val LIBRARY_CLASS_FIELD_2_NAME = "libraryClassField2"

const val PROGRAM_CLASS_1_METHOD_1_NAME = "programClass1Method1"
const val PROGRAM_CLASS_1_METHOD_2_NAME = "programClass1Method2"
const val PROGRAM_CLASS_2_METHOD_1_NAME = "programClass2Method1"
const val PROGRAM_CLASS_2_METHOD_2_NAME = "programClass2Method2"
const val LIBRARY_CLASS_METHOD_1_NAME = "libraryClassMethod1"
const val LIBRARY_CLASS_METHOD_2_NAME = "libraryClassMethod2"

class MemberRemoverTest : BehaviorSpec({
    fun removeField(clazz: Clazz, fieldName: String) {
        val remover = MemberRemover()
        clazz.fieldAccept(
            fieldName,
            FIELD_DESCRIPTOR,
            MultiMemberVisitor(
                remover,
                MemberToClassVisitor(remover),
            ),
        )
    }

    fun removeMethod(clazz: Clazz, methodName: String) {
        val remover = MemberRemover()
        clazz.methodAccept(
            methodName,
            METHOD_DESCRIPTOR,
            MultiMemberVisitor(
                remover,
                MemberToClassVisitor(remover),
            ),
        )
    }

    fun collectAllMembers(clazz: Clazz): List<Member> {
        val methods = mutableListOf<Method>()
        val fields = mutableListOf<Field>()
        clazz.accept(
            AllMemberVisitor(
                MethodFilter(
                    MethodCollector(methods),
                    FieldCollector(fields),
                ),
            ),
        )

        return methods + fields
    }

    Context("Testing program class field removal") {
        Given("Setup") {
            val (programClass) = setUp()

            When("Removing a field") {
                removeField(programClass, PROGRAM_CLASS_1_FIELD_2_NAME)

                Then("The field is no longer present in the class") {
                    programClass.findField(PROGRAM_CLASS_1_FIELD_2_NAME, FIELD_DESCRIPTOR).shouldBeNull()
                }

                Then("All other members are still present") {
                    programClass.findField(PROGRAM_CLASS_1_FIELD_1_NAME, FIELD_DESCRIPTOR).shouldNotBeNull()
                    programClass.findMethod(PROGRAM_CLASS_1_METHOD_1_NAME, METHOD_DESCRIPTOR).shouldNotBeNull()
                    programClass.findMethod(PROGRAM_CLASS_1_METHOD_2_NAME, METHOD_DESCRIPTOR).shouldNotBeNull()
                }
            }
        }
    }

    Context("Testing program class method removal") {
        Given("Setup") {
            val (programClass) = setUp()

            When("Removing a method") {
                removeMethod(programClass, PROGRAM_CLASS_1_METHOD_2_NAME)

                Then("The method is no longer present in the class") {
                    programClass.findMethod(PROGRAM_CLASS_1_METHOD_2_NAME, METHOD_DESCRIPTOR).shouldBeNull()
                }

                Then("All other members are still present") {
                    programClass.findField(PROGRAM_CLASS_1_FIELD_1_NAME, FIELD_DESCRIPTOR).shouldNotBeNull()
                    programClass.findField(PROGRAM_CLASS_1_FIELD_2_NAME, FIELD_DESCRIPTOR).shouldNotBeNull()
                    programClass.findMethod(PROGRAM_CLASS_1_METHOD_1_NAME, METHOD_DESCRIPTOR).shouldNotBeNull()
                }
            }
        }
    }

    Context("Testing removal of all members from a program class") {
        Given("Setup") {
            val (programClass) = setUp()

            When("Removing all members") {
                val remover = MemberRemover()
                programClass.accept(
                    MultiClassVisitor(
                        AllMemberVisitor(remover),
                        remover,
                    ),
                )

                Then("The class has no members anymore") {
                    val counter = MemberCounter()
                    programClass.accept(AllMemberVisitor(counter))
                    counter.count shouldBe 0
                }
            }
        }
    }

    Context("Testing reuse of the same MemberRemover instance") {
        Given("Setup") {
            val (programClass1, programClass2) = setUp()

            When("Using the same MemberRemover to remove members from two different program classes") {
                val remover = MemberRemover()

                programClass1.fieldAccept(PROGRAM_CLASS_1_FIELD_2_NAME, FIELD_DESCRIPTOR, remover)
                programClass1.methodAccept(PROGRAM_CLASS_1_METHOD_2_NAME, METHOD_DESCRIPTOR, remover)
                programClass1.accept(remover)

                programClass2.fieldAccept(PROGRAM_CLASS_2_FIELD_2_NAME, FIELD_DESCRIPTOR, remover)
                programClass2.methodAccept(PROGRAM_CLASS_2_METHOD_2_NAME, METHOD_DESCRIPTOR, remover)
                programClass2.accept(remover)

                Then("The expected members were removed from programClass1") {
                    programClass1.findField(PROGRAM_CLASS_1_FIELD_2_NAME, FIELD_DESCRIPTOR).shouldBeNull()
                    programClass1.findMethod(PROGRAM_CLASS_1_METHOD_2_NAME, METHOD_DESCRIPTOR).shouldBeNull()
                }
                Then("The expected members were removed from programClass2") {
                    programClass2.findField(PROGRAM_CLASS_2_FIELD_2_NAME, FIELD_DESCRIPTOR).shouldBeNull()
                    programClass2.findMethod(PROGRAM_CLASS_2_METHOD_2_NAME, METHOD_DESCRIPTOR).shouldBeNull()
                }
                Then("The expected remaining members are still present in programClass1") {
                    programClass1.findField(PROGRAM_CLASS_1_FIELD_1_NAME, FIELD_DESCRIPTOR).shouldNotBeNull()
                    programClass1.findMethod(PROGRAM_CLASS_1_METHOD_1_NAME, METHOD_DESCRIPTOR).shouldNotBeNull()
                }
                Then("The expected remaining members are still present in programClass2") {
                    programClass2.findField(PROGRAM_CLASS_2_FIELD_1_NAME, FIELD_DESCRIPTOR).shouldNotBeNull()
                    programClass2.findMethod(PROGRAM_CLASS_2_METHOD_1_NAME, METHOD_DESCRIPTOR).shouldNotBeNull()
                }
            }
        }
    }

    Context("Verifying that MemberRemover is a no-op for library classes") {
        Given("Setup") {
            val (_, _, libraryClass) = setUp()
            val originalMembers = collectAllMembers(libraryClass)
            originalMembers.size shouldBe 4

            When("Visiting library members with MemberRemover") {
                removeField(libraryClass, LIBRARY_CLASS_FIELD_1_NAME)
                removeMethod(libraryClass, LIBRARY_CLASS_METHOD_1_NAME)

                val remainingMembers = collectAllMembers(libraryClass)

                Then("remainingMembers contains exactly originalMembers") {
                    remainingMembers shouldContainExactlyInAnyOrder originalMembers
                }
            }
        }
    }

    Context("Testing reset") {
        Given("Setup") {
            val (programClass1, programClass2) = setUp()
            val originalProgramClass1Members = collectAllMembers(programClass1)
            originalProgramClass1Members.size shouldBe 4

            When("Collecting members") {
                val remover = MemberRemover()
                programClass1.fieldAccept(PROGRAM_CLASS_1_FIELD_1_NAME, FIELD_DESCRIPTOR, remover)
                programClass1.methodAccept(PROGRAM_CLASS_1_METHOD_1_NAME, METHOD_DESCRIPTOR, remover)

                And("Resetting the MemberRemover") {
                    remover.reset()
                    val remainingProgramClass1Members = collectAllMembers(programClass1)

                    And("Removing members from another class") {
                        programClass2.fieldAccept(PROGRAM_CLASS_2_FIELD_1_NAME, FIELD_DESCRIPTOR, remover)
                        programClass2.methodAccept(PROGRAM_CLASS_2_METHOD_1_NAME, METHOD_DESCRIPTOR, remover)
                        programClass2.accept(remover)

                        Then("remainingProgramClass1Members contains exactly originalProgramClass1Members") {
                            remainingProgramClass1Members shouldContainExactlyInAnyOrder originalProgramClass1Members
                        }

                        Then("The expected members were removed from programClass2") {
                            programClass2.findField(PROGRAM_CLASS_2_FIELD_1_NAME, FIELD_DESCRIPTOR).shouldBeNull()
                            programClass2.findMethod(PROGRAM_CLASS_2_METHOD_1_NAME, FIELD_DESCRIPTOR).shouldBeNull()
                        }

                        Then("The expected remaining members are still present in programClass2") {
                            programClass2.findField(PROGRAM_CLASS_2_FIELD_2_NAME, FIELD_DESCRIPTOR).shouldNotBeNull()
                            programClass2.findMethod(PROGRAM_CLASS_2_METHOD_2_NAME, METHOD_DESCRIPTOR).shouldNotBeNull()
                        }
                    }
                }
            }
        }
    }

    fun assertThrowsExpectedException(op: () -> Unit) {
        val exception = shouldThrow<ProguardCoreException>(op)

        exception.message shouldBe "Cannot register members to remove for multiple classes at once. Commit the removal of members of the current class first before moving on to the next class."
        exception.componentErrorId shouldBe ErrorId.MEMBER_REMOVER_NOT_FINISHED_WITH_CURRENT_CLASS
    }

    Context("Verifying that visiting a class while still collecting members for another class results in an exception") {
        class ExpectedExceptionTestCase(
            val sequence: (remover: MemberRemover, setup: MemberRemoverTestSetup) -> Unit,
        ) {
            fun executeCase() {
                val remover = MemberRemover()
                sequence.invoke(remover, setUp())
            }
        }

        Given("Test cases") {
            val testCases = listOf(
                // Visiting a program class and then another program class.
                ExpectedExceptionTestCase { remover, (programClass1, programClass2) ->
                    programClass1.fieldAccept(PROGRAM_CLASS_1_FIELD_1_NAME, FIELD_DESCRIPTOR, remover)
                    programClass2.fieldAccept(PROGRAM_CLASS_2_FIELD_1_NAME, FIELD_DESCRIPTOR, remover)
                },
                ExpectedExceptionTestCase { remover, (programClass1, programClass2) ->
                    programClass1.methodAccept(PROGRAM_CLASS_1_METHOD_1_NAME, METHOD_DESCRIPTOR, remover)
                    programClass2.fieldAccept(PROGRAM_CLASS_2_FIELD_1_NAME, FIELD_DESCRIPTOR, remover)
                },
                ExpectedExceptionTestCase { remover, (programClass1, programClass2) ->
                    programClass1.fieldAccept(PROGRAM_CLASS_1_FIELD_1_NAME, FIELD_DESCRIPTOR, remover)
                    programClass2.methodAccept(PROGRAM_CLASS_2_METHOD_1_NAME, METHOD_DESCRIPTOR, remover)
                },
                ExpectedExceptionTestCase { remover, (programClass1, programClass2) ->
                    programClass1.methodAccept(PROGRAM_CLASS_1_METHOD_1_NAME, METHOD_DESCRIPTOR, remover)
                    programClass2.methodAccept(PROGRAM_CLASS_2_METHOD_1_NAME, METHOD_DESCRIPTOR, remover)
                },

                // programClass2.accept while member removals for programClass1 have not been committed yet.
                ExpectedExceptionTestCase { remover, (programClass1, programClass2) ->
                    programClass1.fieldAccept(PROGRAM_CLASS_1_FIELD_1_NAME, FIELD_DESCRIPTOR, remover)
                    programClass2.accept(remover)
                },
                ExpectedExceptionTestCase { remover, (programClass1, programClass2) ->
                    programClass1.methodAccept(PROGRAM_CLASS_1_METHOD_1_NAME, METHOD_DESCRIPTOR, remover)
                    programClass2.accept(remover)
                },
            )

            When("Executing the test cases") {
                Then("All should result in the expected exception") {
                    testCases.shouldForAll {
                        assertThrowsExpectedException { it.executeCase() }
                    }
                }
            }
        }
    }
})

private fun setUp(): MemberRemoverTestSetup {
    val programClass1 = buildClass {
        addStaticField(PROGRAM_CLASS_1_FIELD_1_NAME, FIELD_DESCRIPTOR)
        addStaticField(PROGRAM_CLASS_1_FIELD_2_NAME, FIELD_DESCRIPTOR)
        addStaticMethod(PROGRAM_CLASS_1_METHOD_1_NAME, METHOD_DESCRIPTOR)
        addStaticMethod(PROGRAM_CLASS_1_METHOD_2_NAME, METHOD_DESCRIPTOR)
    }.first
    val programClass2 = buildClass {
        addStaticField(PROGRAM_CLASS_2_FIELD_1_NAME, FIELD_DESCRIPTOR)
        addStaticField(PROGRAM_CLASS_2_FIELD_2_NAME, FIELD_DESCRIPTOR)
        addStaticMethod(PROGRAM_CLASS_2_METHOD_1_NAME, METHOD_DESCRIPTOR)
        addStaticMethod(PROGRAM_CLASS_2_METHOD_2_NAME, METHOD_DESCRIPTOR)
    }.first

    val libraryClass = LibraryClass(
        PUBLIC,
        "LibraryClass1",
        NAME_JAVA_LANG_OBJECT,
    )
    val editor = LibraryClassEditor(libraryClass)

    val libraryClassField1 = LibraryField(
        PUBLIC or STATIC,
        LIBRARY_CLASS_FIELD_1_NAME,
        FIELD_DESCRIPTOR,
    )
    editor.addField(libraryClassField1)
    val libraryClassField2 = LibraryField(
        PUBLIC or STATIC,
        LIBRARY_CLASS_FIELD_2_NAME,
        FIELD_DESCRIPTOR,
    )
    editor.addField(libraryClassField2)

    val libraryClassMethod1 = LibraryMethod(
        PUBLIC or STATIC,
        LIBRARY_CLASS_METHOD_1_NAME,
        METHOD_DESCRIPTOR,
    )
    editor.addMethod(libraryClassMethod1)
    val libraryClassMethod2 = LibraryMethod(
        PUBLIC or STATIC,
        LIBRARY_CLASS_METHOD_2_NAME,
        METHOD_DESCRIPTOR,
    )
    editor.addMethod(libraryClassMethod2)

    return MemberRemoverTestSetup(
        programClass1,
        programClass2,
        libraryClass,
    )
}

data class MemberRemoverTestSetup(
    val programClass1: ProgramClass,
    val programClass2: ProgramClass,
    val libraryClass: LibraryClass,
)

// TODO Add FieldCollector API class.
class FieldCollector(val fields: MutableCollection<Field>) : MemberVisitor {
    override fun visitAnyMember(clazz: Clazz, member: Member) {}

    override fun visitProgramField(programClass: ProgramClass, programField: ProgramField) {
        fields.add(programField)
    }

    override fun visitLibraryField(libraryClass: LibraryClass, libraryField: LibraryField) {
        fields.add(libraryField)
    }
}
