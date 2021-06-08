/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2020 Guardsquare NV
 */

package proguard

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import proguard.classfile.ClassPool
import proguard.classfile.Clazz
import proguard.classfile.Method
import proguard.classfile.ProgramClass
import proguard.classfile.ProgramMethod
import proguard.classfile.attribute.Attribute
import proguard.classfile.attribute.CodeAttribute
import proguard.classfile.attribute.LocalVariableTableAttribute
import proguard.classfile.instruction.Instruction
import proguard.classfile.instruction.visitor.InstructionVisitor
import proguard.evaluation.BasicInvocationUnit
import proguard.evaluation.PartialEvaluator
import proguard.evaluation.value.MultiTypedReferenceValue
import proguard.evaluation.value.MultiTypedReferenceValueFactory
import proguard.evaluation.value.ParticularIntegerValue
import proguard.evaluation.value.TypedReferenceValue
import proguard.evaluation.value.UnknownReferenceValue
import proguard.evaluation.value.Value
import proguard.util.ClassPoolBuilder

class MultiTypeTest : FreeSpec({

    val valueFactory = MultiTypedReferenceValueFactory()
    val invocationUnit = BasicInvocationUnit(valueFactory)
    val partialEvaluator = PartialEvaluator(valueFactory, invocationUnit, true)

    val codeSuper = """
            public class Super {
                public void call() {}
            }
        """
    val codeA = """
            public class A extends Super {
                @Override
                public void call() {}
            }
        """
    val codeB = """
            public class B extends Super {
                @Override
                public void call() {}
            }
        """
    val codeTarget = """
            public class Target {
                public void ternary(boolean flag) {
                    Super s = flag ? new A() : new B();
                    // s is A or B
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
    val classPool = ClassPoolBuilder.fromStrings(listOf("-g"), codeSuper, codeA, codeB, codeTarget)

    "Code examples" - {
        "Exact type" {
            val (instructions, variableTable) = evaluate(
                "Target",
                "exact",
                "()V",
                classPool,
                partialEvaluator
            )
            val (methodEnd, _) = instructions.last()

            val s = partialEvaluator.getVariablesBefore(methodEnd)
                .getValue(variableTable["s"]!!) as MultiTypedReferenceValue
            s.generalizedType.type shouldBe "Super"
            s.potentialTypes.map { it.type } shouldBe listOf("Super")
        }
        "Ternary operator" {
            val (instructions, variableTable) = evaluate(
                "Target",
                "ternary",
                "(Z)V",
                classPool,
                partialEvaluator
            )
            val (methodEnd, _) = instructions.last()

            val s = partialEvaluator.getVariablesBefore(methodEnd)
                .getValue(variableTable["s"]!!) as MultiTypedReferenceValue
            s.generalizedType.type shouldBe "Super"
            s.isNull shouldBe Value.NEVER
            s.potentialTypes.map { it.type }.toSet() shouldBe setOf("A", "B")
        }
        "If else" {
            val (instructions, variableTable) = evaluate(
                "Target",
                "ifElse",
                "(Z)V",
                classPool,
                partialEvaluator
            )
            val (methodEnd, _) = instructions.last()

            val s = partialEvaluator.getVariablesBefore(methodEnd)
                .getValue(variableTable["s"]!!) as MultiTypedReferenceValue
            s.generalizedType.type shouldBe "Super"
            s.isNull shouldBe Value.NEVER
            s.potentialTypes.map { it.type }.toSet() shouldBe setOf("A", "B")
        }
        "Switch" {
            val (instructions, variableTable) = evaluate(
                "Target",
                "switchStmt",
                "(Ljava/lang/String;)V",
                classPool,
                partialEvaluator
            )
            val (methodEnd, _) = instructions.last()

            val s = partialEvaluator.getVariablesBefore(methodEnd)
                .getValue(variableTable["s"]!!) as MultiTypedReferenceValue
            s.generalizedType.type shouldBe "Super"
            s.isNull shouldBe Value.MAYBE
            s.potentialTypes.map { it.type }.toSet() shouldBe setOf("A", "B", "Super", null)
        }
        "Array handling" {
            val (instructions, variableTable) = evaluate(
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
            a.generalizedType.type shouldBe "A"
            a.isNull shouldBe Value.MAYBE
            a.potentialTypes.map { it.type }.toSet() shouldBe setOf("A")
        }
    }

    "Logic tests" - {
        val nul = TypedReferenceValue(null, null, false, true)
        val multiNull = MultiTypedReferenceValue(nul, false)

        val superClass = TypedReferenceValue(
            "Super",
            classPool.getClass("Super"),
            false,
            false
        )
        val multiSuper = MultiTypedReferenceValue(superClass, false)

        val a = TypedReferenceValue(
            "A",
            classPool.getClass("A"),
            false,
            false
        )
        val multiA = MultiTypedReferenceValue(a, false)

        val b = TypedReferenceValue(
            "B",
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
                var generalized = multiA.generalize(multiNull) as MultiTypedReferenceValue
                generalized.generalizedType shouldBe a
                generalized.potentialTypes shouldBe setOf(a, nul)
                generalized.mayBeUnknown shouldBe false

                generalized = multiB.generalize(multiNull) as MultiTypedReferenceValue
                generalized.generalizedType shouldBe b
                generalized.potentialTypes shouldBe setOf(b, nul)
                generalized.mayBeUnknown shouldBe false

                generalized = multiSuper.generalize(multiNull) as MultiTypedReferenceValue
                generalized.generalizedType shouldBe superClass
                generalized.potentialTypes shouldBe setOf(superClass, nul)
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

                dereferenced.type shouldBe "A"
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

fun evaluate(
    className: String,
    methodName: String,
    methodDescriptor: String,
    classPool: ClassPool,
    partialEvaluator: PartialEvaluator
): Pair<ArrayList<Pair<Int, Instruction>>, HashMap<String, Int>> {
    val clazz = classPool.getClass(className) as ProgramClass
    val method = clazz.findMethod(methodName, methodDescriptor) as ProgramMethod

    val codeAttribute =
        method.attributes.find { it.getAttributeName(clazz) == Attribute.CODE } as CodeAttribute
    val localVarTableAttribute =
        codeAttribute.attributes.find { it.getAttributeName(clazz) == Attribute.LOCAL_VARIABLE_TABLE } as LocalVariableTableAttribute?

    val instructions = ArrayList<Pair<Int, Instruction>>()
    codeAttribute.instructionsAccept(
        clazz, method,
        object : InstructionVisitor {
            override fun visitAnyInstruction(
                clazz: Clazz?,
                method: Method?,
                codeAttribute: CodeAttribute?,
                offset: Int,
                instruction: Instruction?
            ) {
                instruction?.let { instructions.add(Pair(offset, it)) }
            }
        }
    )

    val variableTable = HashMap<String, Int>()
    localVarTableAttribute?.localVariablesAccept(
        clazz,
        method,
        codeAttribute
    ) { _, _, _, localVar ->
        variableTable[localVar.getName(clazz)] = localVar.u2index
    }

    partialEvaluator.visitCodeAttribute(clazz, method, codeAttribute)

    return Pair(instructions, variableTable)
}
