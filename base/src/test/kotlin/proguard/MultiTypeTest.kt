/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2020 Guardsquare NV
 */

package proguard

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import proguard.evaluation.BasicInvocationUnit
import proguard.evaluation.PartialEvaluator
import proguard.evaluation.exception.IncompleteClassHierarchyException
import proguard.evaluation.value.MultiTypedReferenceValue
import proguard.evaluation.value.MultiTypedReferenceValueFactory
import proguard.evaluation.value.ParticularIntegerValue
import proguard.evaluation.value.TypedReferenceValue
import proguard.evaluation.value.UnknownReferenceValue
import proguard.evaluation.value.Value
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.JavaSource
import proguard.testutils.PartialEvaluatorUtil

class MultiTypeTest : FreeSpec({

    val valueFactory = MultiTypedReferenceValueFactory()
    val invocationUnit = BasicInvocationUnit(valueFactory)
    val partialEvaluator = PartialEvaluator(valueFactory, invocationUnit, true)

    val codeSuper = JavaSource(
        "Super.java",
        """
        public class Super {
            public void call() {}
        }
        """
    )
    val codeRemovedSuper = JavaSource(
        "RemovedSuper.java",
        """
        public class RemovedSuper {
            public void call() {}
        }
        """
    )
    val codeA = JavaSource(
        "A.java",
        """
        public class A extends Super {
            @Override
            public void call() {}
        }
        """
    )
    val codeB = JavaSource(
        "B.java",
        """
        public class B extends Super {
            @Override
            public void call() {}
        }
        """
    )
    val codeC = JavaSource(
        "C.java",
        """
        public class C extends RemovedSuper {
            @Override
            public void call() {}
        }
        """
    )
    val codeTarget = JavaSource(
        "Target.java",
        """
        public class Target {
            public void ternary(boolean flag) {
                Super s = flag ? new A() : new B();
                // s is A or B
            }

            public void noSuperClass(boolean flag) {
                Object o = flag ? new A() : new C();
                // Common superclass is Object, as superclass of C is removed from the class pool
            }

            public void ifElse(boolean flag) {
                Super s;
                if (flag) {
                    s = new A();
                } else {
                    s = new B();
                }
                // s is A or B
            }

            public void switchStmt(String flag) {
                Super s;
                switch(flag) {
                    case "Super":
                        s = new Super();
                        break;
                    case "A":
                        s = new A();
                        break;
                    case "B":
                        s = new B();
                        break;
                    default:
                        s = null;
                }
                // s is Super, A, B or null
            }

            public void exact() {
                Super s = new Super();
                // s is always Super
            }

            public void array() {
                A[] array = new A[10];
                A a = array[3];
            }
        }
        """
    )
    val (classPool, _) = ClassPoolBuilder.fromSource(codeSuper, codeRemovedSuper, codeA, codeB, codeC, codeTarget, javacArguments = listOf("-g", "-source", "1.8", "-target", "1.8"), initialize = false)
    classPool.removeClass("RemovedSuper")
    ClassPoolBuilder.initialize(classPool, false)

    "Code examples" - {
        "Exact type" {
            val (instructions, variableTable) = PartialEvaluatorUtil.evaluate(
                "Target",
                "exact",
                "()V",
                classPool,
                partialEvaluator
            )
            val (methodEnd, _) = instructions.last()

            val s = partialEvaluator.getVariablesBefore(methodEnd)
                .getValue(variableTable["s"]!!) as MultiTypedReferenceValue
            s.generalizedType.type shouldBe "LSuper;"
            s.potentialTypes.map { it.type } shouldBe listOf("LSuper;")
        }
        "Ternary operator" {
            val (instructions, variableTable) = PartialEvaluatorUtil.evaluate(
                "Target",
                "ternary",
                "(Z)V",
                classPool,
                partialEvaluator
            )
            val (methodEnd, _) = instructions.last()

            val s = partialEvaluator.getVariablesBefore(methodEnd)
                .getValue(variableTable["s"]!!) as MultiTypedReferenceValue
            s.generalizedType.type shouldBe "LSuper;"
            s.isNull shouldBe Value.NEVER
            s.potentialTypes.map { it.type }.toSet() shouldBe setOf("LA;", "LB;")
        }
        "No super class with exception" {
            // Don't allow incomplete Class Hierarchies
            val allowIncompleteClassHierarchy = TypedReferenceValue::class.java.getDeclaredField("ALLOW_INCOMPLETE_CLASS_HIERARCHY")
            allowIncompleteClassHierarchy.isAccessible = true
            allowIncompleteClassHierarchy.setBoolean(TypedReferenceValue(null, null, true, true), false)

            val exception = shouldThrow<IncompleteClassHierarchyException> {
                PartialEvaluatorUtil.evaluate(
                    "Target",
                    "noSuperClass",
                    "(Z)V",
                    classPool,
                    partialEvaluator
                )
            }

            exception.message shouldContain "1 unknown classes: RemovedSuper"
        }
        "No super class" {
            // Allow incomplete Class Hierarchies
            val allowIncompleteClassHierarchy = TypedReferenceValue::class.java.getDeclaredField("ALLOW_INCOMPLETE_CLASS_HIERARCHY")
            allowIncompleteClassHierarchy.isAccessible = true
            allowIncompleteClassHierarchy.setBoolean(TypedReferenceValue(null, null, true, true), true)

            val (instructions, variableTable) = PartialEvaluatorUtil.evaluate(
                "Target",
                "noSuperClass",
                "(Z)V",
                classPool,
                partialEvaluator
            )
            val (methodEnd, _) = instructions.last()
            val s = partialEvaluator.getVariablesBefore(methodEnd)
                .getValue(variableTable["o"]!!) as MultiTypedReferenceValue
            s.generalizedType.type shouldBe "Ljava/lang/Object;"
            s.potentialTypes.map { it.type }.toSet() shouldBe setOf("LC;", "LA;")

            allowIncompleteClassHierarchy.setBoolean(TypedReferenceValue(null, null, true, true), false)
        }
        "If else" {
            val (instructions, variableTable) = PartialEvaluatorUtil.evaluate(
                "Target",
                "ifElse",
                "(Z)V",
                classPool,
                partialEvaluator
            )
            val (methodEnd, _) = instructions.last()

            val s = partialEvaluator.getVariablesBefore(methodEnd)
                .getValue(variableTable["s"]!!) as MultiTypedReferenceValue
            s.generalizedType.type shouldBe "LSuper;"
            s.isNull shouldBe Value.NEVER
            s.potentialTypes.map { it.type }.toSet() shouldBe setOf("LA;", "LB;")
        }
        "Switch" {
            val (instructions, variableTable) = PartialEvaluatorUtil.evaluate(
                "Target",
                "switchStmt",
                "(Ljava/lang/String;)V",
                classPool,
                partialEvaluator
            )
            val (methodEnd, _) = instructions.last()

            val s = partialEvaluator.getVariablesBefore(methodEnd)
                .getValue(variableTable["s"]!!) as MultiTypedReferenceValue
            s.generalizedType.type shouldBe "LSuper;"
            s.isNull shouldBe Value.MAYBE
            s.potentialTypes.map { it.type }.toSet() shouldBe setOf("LA;", "LB;", "LSuper;")
        }
        "Array handling" {
            val (instructions, variableTable) = PartialEvaluatorUtil.evaluate(
                "Target",
                "array",
                "()V",
                classPool,
                partialEvaluator
            )
            val (methodEnd, _) = instructions.last()

            val array = partialEvaluator.getVariablesBefore(methodEnd)
                .getValue(variableTable["array"]!!) as MultiTypedReferenceValue
            array.generalizedType.type shouldBe "[LA;"
            array.isNull shouldBe Value.NEVER
            array.potentialTypes.map { it.type }.toSet() shouldBe setOf("[LA;")

            val a = partialEvaluator.getVariablesBefore(methodEnd)
                .getValue(variableTable["a"]!!) as MultiTypedReferenceValue
            a.generalizedType.type shouldBe "LA;"
            a.isNull shouldBe Value.MAYBE
            a.potentialTypes.map { it.type }.toSet() shouldBe setOf("LA;")
        }
    }

    "Logic tests" - {
        val nul = TypedReferenceValue(null, null, false, true)
        val multiNull = MultiTypedReferenceValue(nul, false)

        val superClass = TypedReferenceValue(
            "LSuper;",
            classPool.getClass("Super"),
            false,
            false
        )
        val multiSuper = MultiTypedReferenceValue(superClass, false)

        val a = TypedReferenceValue(
            "LA;",
            classPool.getClass("A"),
            false,
            false
        )
        val multiA = MultiTypedReferenceValue(a, false)

        val b = TypedReferenceValue(
            "LB;",
            classPool.getClass("B"),
            false,
            false
        )
        val multiB = MultiTypedReferenceValue(b, false)

        val arrayA = TypedReferenceValue(
            "[LA;",
            classPool.getClass("A"),
            false,
            false
        )
        val multiArrayA = MultiTypedReferenceValue(arrayA, false)

        "Generalize" - {
            "(A, B) -> Super" {
                val generalized = multiA.generalize(multiB) as MultiTypedReferenceValue
                generalized.generalizedType shouldBe superClass
                generalized.potentialTypes shouldBe setOf(a, b)
                generalized.mayBeUnknown shouldBe false
            }
            "(X, Super) -> Super" {
                var generalized = multiA.generalize(multiSuper) as MultiTypedReferenceValue
                generalized.generalizedType shouldBe superClass
                generalized.potentialTypes shouldBe setOf(a, superClass)
                generalized.mayBeUnknown shouldBe false

                generalized = multiB.generalize(multiSuper) as MultiTypedReferenceValue
                generalized.generalizedType shouldBe superClass
                generalized.potentialTypes shouldBe setOf(b, superClass)
                generalized.mayBeUnknown shouldBe false
            }
            "(X, null) -> X" {
                val aMaybeNull = TypedReferenceValue(a.type, a.referencedClass, a.mayBeExtension(), true)
                val bMaybeNull = TypedReferenceValue(b.type, b.referencedClass, b.mayBeExtension(), true)
                val superMaybeNull = TypedReferenceValue(superClass.type, superClass.referencedClass, superClass.mayBeExtension(), true)

                var generalized = multiA.generalize(multiNull) as MultiTypedReferenceValue
                generalized.generalizedType shouldBe aMaybeNull
                generalized.potentialTypes shouldBe setOf(aMaybeNull)
                generalized.mayBeUnknown shouldBe false

                generalized = multiB.generalize(multiNull) as MultiTypedReferenceValue
                generalized.generalizedType shouldBe bMaybeNull
                generalized.potentialTypes shouldBe setOf(bMaybeNull)
                generalized.mayBeUnknown shouldBe false

                generalized = multiSuper.generalize(multiNull) as MultiTypedReferenceValue
                generalized.generalizedType shouldBe superMaybeNull
                generalized.potentialTypes shouldBe setOf(superMaybeNull)
                generalized.mayBeUnknown shouldBe false
            }
            "Identity" {
                var generalized = multiA.generalize(multiA) as MultiTypedReferenceValue
                generalized.generalizedType shouldBe a
                generalized.potentialTypes shouldBe setOf(a)
                generalized.mayBeUnknown shouldBe false

                generalized = multiB.generalize(multiB) as MultiTypedReferenceValue
                generalized.generalizedType shouldBe b
                generalized.potentialTypes shouldBe setOf(b)
                generalized.mayBeUnknown shouldBe false

                generalized = multiSuper.generalize(multiSuper) as MultiTypedReferenceValue
                generalized.generalizedType shouldBe superClass
                generalized.potentialTypes shouldBe setOf(superClass)
                generalized.mayBeUnknown shouldBe false

                generalized = multiNull.generalize(multiNull) as MultiTypedReferenceValue
                generalized.generalizedType shouldBe nul
                generalized.potentialTypes shouldBe setOf(nul)
                generalized.mayBeUnknown shouldBe false

                generalized = multiArrayA.generalize(multiArrayA) as MultiTypedReferenceValue
                generalized.generalizedType shouldBe arrayA
                generalized.potentialTypes shouldBe setOf(arrayA)
                generalized.mayBeUnknown shouldBe false
            }
            "Handle TypedReferenceValues transparently" {
                var generalized = multiA.generalize(a) as MultiTypedReferenceValue
                generalized.generalizedType shouldBe a
                generalized.potentialTypes shouldBe setOf(a)
                generalized.mayBeUnknown shouldBe false

                generalized = multiB.generalize(b) as MultiTypedReferenceValue
                generalized.generalizedType shouldBe b
                generalized.potentialTypes shouldBe setOf(b)
                generalized.mayBeUnknown shouldBe false

                generalized = multiSuper.generalize(superClass) as MultiTypedReferenceValue
                generalized.generalizedType shouldBe superClass
                generalized.potentialTypes shouldBe setOf(superClass)
                generalized.mayBeUnknown shouldBe false

                generalized = multiNull.generalize(nul) as MultiTypedReferenceValue
                generalized.generalizedType shouldBe nul
                generalized.potentialTypes shouldBe setOf(nul)
                generalized.mayBeUnknown shouldBe false

                generalized = multiArrayA.generalize(arrayA) as MultiTypedReferenceValue
                generalized.generalizedType shouldBe arrayA
                generalized.potentialTypes shouldBe setOf(arrayA)
                generalized.mayBeUnknown shouldBe false
            }
            "Handle UnknownReferenceValue" {
                var generalized =
                    multiA.generalize(UnknownReferenceValue()) as MultiTypedReferenceValue
                generalized.generalizedType shouldBe a
                generalized.potentialTypes shouldBe setOf(a)
                generalized.mayBeUnknown shouldBe true

                generalized = multiB.generalize(UnknownReferenceValue()) as MultiTypedReferenceValue
                generalized.generalizedType shouldBe b
                generalized.potentialTypes shouldBe setOf(b)
                generalized.mayBeUnknown shouldBe true

                generalized =
                    multiSuper.generalize(UnknownReferenceValue()) as MultiTypedReferenceValue
                generalized.generalizedType shouldBe superClass
                generalized.potentialTypes shouldBe setOf(superClass)
                generalized.mayBeUnknown shouldBe true
            }
            "Handle array dereference" {
                val dereferenced = multiArrayA.referenceArrayLoad(
                    ParticularIntegerValue(42),
                    valueFactory
                ) as MultiTypedReferenceValue

                dereferenced.type shouldBe "LA;"
                dereferenced.referencedClass shouldBe classPool.getClass("A")

                val referenced = valueFactory.createArrayReferenceValue(
                    "LA;",
                    classPool.getClass("A"),
                    ParticularIntegerValue(42)
                ) as MultiTypedReferenceValue

                referenced.type shouldBe "[LA;"
                referenced.referencedClass shouldBe classPool.getClass("A")
            }
        }
        "Null check" {
            multiNull.isNull shouldBe Value.ALWAYS

            multiA.isNull shouldBe Value.NEVER
            multiB.isNull shouldBe Value.NEVER
            multiSuper.isNull shouldBe Value.NEVER

            MultiTypedReferenceValue(setOf(a, b, nul), false).isNull shouldBe Value.MAYBE
            multiA.generalize(multiNull).isNull shouldBe Value.MAYBE
        }
        "Casts" - {
            "Upcasting preserves type" {
                multiA.cast(
                    superClass.type,
                    superClass.referencedClass,
                    valueFactory,
                    true
                ) shouldBe multiA
                multiB.cast(
                    superClass.type,
                    superClass.referencedClass,
                    valueFactory,
                    true
                ) shouldBe multiB
            }
            "Downcasting overrides type" {
                multiSuper.cast(
                    a.type,
                    a.referencedClass,
                    valueFactory,
                    true
                ) shouldBe multiA
                multiSuper.cast(
                    b.type,
                    b.referencedClass,
                    valueFactory,
                    true
                ) shouldBe multiB
            }
        }
    }
})
