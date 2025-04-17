package proguard.classfile.editor.util

import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import proguard.classfile.ClassConstants
import proguard.classfile.Clazz
import proguard.classfile.Member
import proguard.classfile.Method
import proguard.classfile.ProgramClass
import proguard.classfile.ProgramMethod
import proguard.classfile.attribute.Attribute
import proguard.classfile.attribute.CodeAttribute
import proguard.classfile.attribute.ExceptionInfo
import proguard.classfile.attribute.visitor.AttributeVisitor
import proguard.classfile.attribute.visitor.ExceptionInfoVisitor
import proguard.classfile.instruction.ConstantInstruction
import proguard.classfile.instruction.Instruction
import proguard.classfile.instruction.visitor.AllInstructionVisitor
import proguard.classfile.instruction.visitor.InstructionVisitor
import proguard.classfile.util.inject.CodeInjector
import proguard.classfile.util.inject.argument.ConstantPrimitive
import proguard.classfile.util.inject.argument.ConstantString
import proguard.classfile.util.inject.argument.PrimitiveArrayConstantArgument
import proguard.classfile.util.inject.location.FirstBlock
import proguard.classfile.util.inject.location.SpecificOffset
import proguard.classfile.visitor.ClassPrinter
import proguard.classfile.visitor.MemberVisitor
import proguard.classfile.visitor.ReferencedMemberVisitor
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.JavaSource
import java.io.PrintWriter
import java.io.StringWriter

class CodeInjectorTest : BehaviorSpec({
    isolationMode = IsolationMode.InstancePerLeaf

    Given("a class targeted for injection and a class holding the injection content") {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            JavaSource(
                "InjectionTarget.java",
                """
                public class InjectionTarget {
                    public InjectionTarget() {}
                    public InjectionTarget(int dummy) {}
                    public InjectionTarget(int dummyInt, float dummyFloat) {
                        this(dummyInt);
                    }
                    public static void main(String... args) {}
                    public int instanceMethod() {
                        return 0;
                    }
                    private static final Object LOCK = new Object();
                    private static boolean isEnabled() { return true; }
                    public void synchro() {
                        try{
                            synchronized (LOCK) {
                            boolean b = isEnabled();
                            }
                        } catch (Throwable e) {
                        e.printStackTrace();
                        }
                    }
                }
                """.trimIndent(),
            ),
            JavaSource(
                "InjectContent.java",
                """
                public class InjectContent { 
                    public static void log() {
                        System.out.println("log called");
                    }
                    public static void logPrimitive(int intData, double doubleData) {
                        System.out.printf("log param: %d:int, %f:double%n", intData, doubleData);
                    } 
                    public static void logString(String stringData) {
                        System.out.printf("log param: %s:String%n", stringData);
                    } 
                    public static void logMixed(String stringData, int intData, double doubleData) {
                        System.out.printf("log param: %s:String, %d:intData, %f:doubleData%n", stringData, intData, doubleData);
                    } 
                    public static void logMixedWithArray(int intId, String stringId , int[] inArrayData, String stringData) {
                        System.out.printf("log param:  %d:int, %s:String, %d:intData[], %f:stringData%n", intId, stringId, inArrayData, stringData);
                    }
                }
                """.trimIndent(),
            ),
        )
        val injectTargetClass = programClassPool.getClass("InjectionTarget") as ProgramClass
        val injectContentClass = programClassPool.getClass("InjectContent")

        When("Injecting InjectContent.log() into static method InjectTarget.main()") {
            val targetMethod = injectTargetClass.findMethod("main", "([Ljava/lang/String;)V") as ProgramMethod
            CodeInjector()
                .injectInvokeStatic(injectContentClass, injectContentClass.findMethod("log", "()V"))
                .into(injectTargetClass, targetMethod)
                .at(FirstBlock())
                .commit()

            Then("InjectTarget.main() should start with InjectContent.log()") {
                val renderedMethod = StringWriter()
                targetMethod.accept(injectTargetClass, ClassPrinter(PrintWriter(renderedMethod)))

                renderedMethod.toString() shouldContain Regex(
                    """
                    \s*\[0] invokestatic #\d+ = Methodref\(InjectContent\.log\(\)V\)
                    """.trimIndent(),
                )
            }
        }

        When("Injecting InjectContent.logPrimitive(...) into static method InjectTarget.main()") {
            val targetMethod = injectTargetClass.findMethod("main", "([Ljava/lang/String;)V") as ProgramMethod
            CodeInjector()
                .injectInvokeStatic(
                    injectContentClass,
                    injectContentClass.findMethod("logPrimitive", "(ID)V"),
                    ConstantPrimitive(99),
                    ConstantPrimitive(42.24),
                )
                .into(injectTargetClass, targetMethod)
                .at(FirstBlock())
                .commit()

            Then("The first instruction of InjectTarget.main() is InjectContent.logPrimitive(99, 42.24)") {
                val renderedMethod = StringWriter()
                targetMethod.accept(injectTargetClass, ClassPrinter(PrintWriter(renderedMethod)))

                renderedMethod.toString() shouldContain Regex(
                    """
                    \s*\[0] bipush 99
                    \s*\[\d+] ldc2_w #\d+ = Double\(42.24\)
                    \s*\[\d+] invokestatic #\d+ = Methodref\(InjectContent\.logPrimitive\(ID\)V\)
                    """.trimIndent(),
                )
            }
        }

        When("Injecting InjectContent.logString(...) into static method InjectTarget.main()") {
            val targetMethod = injectTargetClass.findMethod("main", "([Ljava/lang/String;)V") as ProgramMethod
            CodeInjector()
                .injectInvokeStatic(
                    injectContentClass,
                    injectContentClass.findMethod("logString", "(Ljava/lang/String;)V"),
                    ConstantString("foo"),
                )
                .into(injectTargetClass, targetMethod)
                .at(FirstBlock())
                .commit()

            Then("The first instruction is InjectContent.logString(\"foo\")") {
                val renderedMethod = StringWriter()
                targetMethod.accept(injectTargetClass, ClassPrinter(PrintWriter(renderedMethod)))

                renderedMethod.toString() shouldContain Regex(
                    """
                    \s*\[0] ldc #\d+ = String\("foo"\)
                    \s*\[\d+] invokestatic #\d+ = Methodref\(InjectContent\.logString\(Ljava/lang/String;\)V\)
                    """.trimIndent(),
                )
            }
        }

        When("Injecting InjectContent.logMixed(...) into static method InjectTarget.main()") {
            val targetMethod = injectTargetClass.findMethod("main", "([Ljava/lang/String;)V") as ProgramMethod
            CodeInjector()
                .injectInvokeStatic(
                    injectContentClass,
                    injectContentClass.findMethod("logMixed", "(Ljava/lang/String;ID)V"),
                    ConstantString("bar"),
                    ConstantPrimitive(1),
                    ConstantPrimitive(12.49),
                )
                .into(injectTargetClass, targetMethod)
                .at(FirstBlock())
                .commit()

            Then("The first instruction is InjectContent.logMixed(\"bar\", 1, 12.49)") {
                val renderedMethod = StringWriter()
                targetMethod.accept(injectTargetClass, ClassPrinter(PrintWriter(renderedMethod)))

                renderedMethod.toString() shouldContain Regex(
                    """
                    \s*\[0] ldc #\d+ = String\("bar"\)
                    \s*\[\d+] iconst_1
                    \s*\[\d+] ldc2_w #\d+ = Double\(12.49\)
                    \s*\[\d+] invokestatic #\d+ = Methodref\(InjectContent\.logMixed\(Ljava/lang/String;ID\)V\)
                    """.trimIndent(),
                )
            }
        }

        When("Injecting InjectContent.logMixed(...) into instance method InjectTarget.instanceMethod()") {
            val instanceMethod = injectTargetClass.findMethod("instanceMethod", "()I") as ProgramMethod
            CodeInjector()
                .injectInvokeStatic(
                    injectContentClass,
                    injectContentClass.findMethod("logMixed", "(Ljava/lang/String;ID)V"),
                    ConstantString("bar"),
                    ConstantPrimitive(1),
                    ConstantPrimitive(12.49),
                )
                .into(injectTargetClass, injectTargetClass.findMethod("instanceMethod", "()I") as ProgramMethod)
                .at(FirstBlock())
                .commit()

            Then("The first instruction is InjectContent.logMixed(\"bar\", 1, 12.49)") {
                val renderedMethod = StringWriter()
                instanceMethod.accept(injectTargetClass, ClassPrinter(PrintWriter(renderedMethod)))

                renderedMethod.toString() shouldContain Regex(
                    """
                    \s*\[0] ldc #\d+ = String\("bar"\)
                    \s*\[\d+] iconst_1
                    \s*\[\d+] ldc2_w #\d+ = Double\(12.49\)
                    \s*\[\d+] invokestatic #\d+ = Methodref\(InjectContent\.logMixed\(Ljava/lang/String;ID\)V\)
                    """.trimIndent(),
                )
            }
        }

        When("Injecting InjectContent.logMixed(...) into InjectTarget's default constructor") {
            val defaultConstructor = injectTargetClass.findMethod(ClassConstants.METHOD_NAME_INIT, ClassConstants.METHOD_TYPE_INIT) as ProgramMethod
            CodeInjector()
                .injectInvokeStatic(
                    injectContentClass,
                    injectContentClass.findMethod("logMixed", "(Ljava/lang/String;ID)V"),
                    ConstantString("bar"),
                    ConstantPrimitive(1),
                    ConstantPrimitive(12.49),
                )
                .into(injectTargetClass, defaultConstructor)
                .at(FirstBlock())
                .commit()

            Then("InjectContent.logMixed(\"bar\", 1, 12.49) is injected after super class' constructor call") {
                val renderedMethod = StringWriter()
                defaultConstructor.accept(injectTargetClass, ClassPrinter(PrintWriter(renderedMethod)))

                renderedMethod.toString() shouldContain Regex(
                    """
                    \s*\[0] aload_0 v0
                    \s*\[1] invokespecial #1 = Methodref\(java/lang/Object.<init>\(\)V\)
                    \s*\[\d+] ldc #\d+ = String\("bar"\)
                    \s*\[\d+] iconst_1
                    \s*\[\d+] ldc2_w #\d+ = Double\(12.49\)
                    \s*\[\d+] invokestatic #\d+ = Methodref\(InjectContent\.logMixed\(Ljava/lang/String;ID\)V\)
                    """.trimIndent(),
                )
            }
        }

        When("Injecting InjectContent.logMixed(...) into InjectTarget's delegated constructor") {
            val delegatedConstructor = injectTargetClass.findMethod(ClassConstants.METHOD_NAME_INIT, "(IF)V") as ProgramMethod
            CodeInjector()
                .injectInvokeStatic(
                    injectContentClass,
                    injectContentClass.findMethod("logMixed", "(Ljava/lang/String;ID)V"),
                    ConstantString("bar"),
                    ConstantPrimitive(1),
                    ConstantPrimitive(12.49),
                )
                .into(injectTargetClass, delegatedConstructor)
                .at(FirstBlock())
                .commit()

            Then("InjectContent.logMixed(\"bar\", 1, 12.49) is injected after constructor call") {
                val renderedMethod = StringWriter()
                delegatedConstructor.accept(injectTargetClass, ClassPrinter(PrintWriter(renderedMethod)))

                renderedMethod.toString() shouldContain Regex(
                    """
                    \s*\[0] aload_0 v0
                    \s*\[1] iload_1 v1
                    \s*\[2] invokespecial #\d+ = Methodref\(InjectionTarget.<init>\(I\)V\)
                    \s*\[\d+] ldc #\d+ = String\("bar"\)
                    \s*\[\d+] iconst_1
                    \s*\[\d+] ldc2_w #\d+ = Double\(12.49\)
                    \s*\[\d+] invokestatic #\d+ = Methodref\(InjectContent\.logMixed\(Ljava/lang/String;ID\)V\)
                    """.trimIndent(),
                )
            }
        }

        When("Injecting InjectContent.logMixedWithArray(...) into static method InjectTarget.main()") {
            val targetMethod = injectTargetClass.findMethod("main", "([Ljava/lang/String;)V") as ProgramMethod
            CodeInjector()
                .injectInvokeStatic(
                    injectContentClass,
                    injectContentClass.findMethod("logMixedWithArray", "(ILjava/lang/String;[ILjava/lang/String;)V"),
                    ConstantPrimitive(1),
                    ConstantString("foo"),
                    PrimitiveArrayConstantArgument(arrayOf(1, 2)),
                    ConstantString("bar"),
                )
                .into(injectTargetClass, targetMethod)
                .at(FirstBlock())
                .commit()

            Then("The first instruction is InjectContent.logMixedWithArray(1,\"foo\",\\[1,2\\],\"bar\")") {
                val renderedMethod = StringWriter()
                targetMethod.accept(injectTargetClass, ClassPrinter(PrintWriter(renderedMethod)))
                // Saw different byte code patterns to create and then pass array. Hence, not using the exact pattern to match array loading.
                // It just checks that a newarray instruction is present in between `ldc foo` and `ldc bar`
                renderedMethod.toString() shouldContain Regex(
                    """
                    \s*\[0] iconst_1
                    \s*\[\d+] ldc #\d+ = String\("foo"\)
                    ((.|\n)*)
                    \s*\[\d+] newarray ((.|\n)*)
                    ((.|\n)*)
                    \s*\[\d+] ldc #\d+ = String\("bar"\)
                    \s*\[\d+] invokestatic #\d+ = Methodref\(InjectContent\.logMixedWithArray\(ILjava/lang/String;\[ILjava/lang/String;\)V\)
                    """.trimIndent(),
                )
            }
        }
        When("Injecting InjectContent.log() into instance method synchro, which has a nested synchronized block in a try catch block, and the first instruction of the synchronized block is a static invocation") {
            val targetMethod = injectTargetClass.findMethod("synchro", "()V") as ProgramMethod
            val codeInjector = CodeInjector()
                .injectInvokeStatic(injectContentClass, injectContentClass.findMethod("log", "()V"))
                .into(injectTargetClass, targetMethod)

            targetMethod.attributesAccept(
                injectTargetClass,
                AllInstructionVisitor(object : InstructionVisitor {
                    override fun visitAnyInstruction(
                        clazz: Clazz,
                        method: Method,
                        codeAttribute: CodeAttribute,
                        offset: Int,
                        instruction: Instruction,
                    ) {
                    }

                    override fun visitConstantInstruction(
                        clazz: Clazz,
                        method: Method,
                        codeAttribute: CodeAttribute,
                        offset: Int,
                        constantInstruction: ConstantInstruction,
                    ) {
                        if (constantInstruction.opcode == Instruction.OP_INVOKESTATIC) {
                            clazz.constantPoolEntryAccept(
                                constantInstruction.constantIndex,
                                ReferencedMemberVisitor(object : MemberVisitor {
                                    override fun visitAnyMember(
                                        clazz: Clazz,
                                        member: Member,
                                    ) {}

                                    override fun visitProgramMethod(
                                        programClass: ProgramClass,
                                        programMethod: ProgramMethod,
                                    ) {
                                        if (programClass.name.equals(injectTargetClass.name) && programMethod.getName(programClass).equals("isEnabled")) {
                                            codeInjector.at(SpecificOffset(offset, true))
                                        }
                                    }
                                }),
                            )
                        }
                    }
                }),
            )
            codeInjector.commit()
            val renderedMethod = StringWriter()
            targetMethod.accept(injectTargetClass, ClassPrinter(PrintWriter(renderedMethod)))
            Then("InjectContent.log() should be injected after the monitorenter and before the isEnabled invocation") {

                renderedMethod.toString() shouldContain Regex(
                    """
                    \s*\[\d+] monitorenter
                    \s*\[\d+] invokestatic #\d+ = Methodref\(InjectContent\.log\(\)V\)
                    \s*\[\d+] invokestatic #\d+ = Methodref\(InjectionTarget\.isEnabled\(\)Z\)
                    """.trimIndent(),
                )
            }
            Then("The exception handler should include InjectContent.log() in its range") {
                data class ExceptionRange(val exceptionStart: Int, val exceptionEnd: Int)

                val exceptionRangesFinder = object : AttributeVisitor, ExceptionInfoVisitor {
                    val exceptionRanges = mutableListOf<ExceptionRange>()
                    override fun visitAnyAttribute(
                        clazz: Clazz,
                        attribute: Attribute,
                    ) {
                    }

                    override fun visitCodeAttribute(
                        clazz: Clazz,
                        method: Method,
                        codeAttribute: CodeAttribute,
                    ) {
                        codeAttribute.exceptionsAccept(clazz, method, this)
                    }

                    override fun visitExceptionInfo(
                        clazz: Clazz,
                        method: Method,
                        codeAttribute: CodeAttribute,
                        exceptionInfo: ExceptionInfo,
                    ) {
                        exceptionRanges.add(ExceptionRange(exceptionInfo.u2startPC, exceptionInfo.u2endPC))
                    }
                }
                targetMethod.attributesAccept(injectTargetClass, exceptionRangesFinder)
                val rangeChecker = object : InstructionVisitor {
                    var inRange = false
                    override fun visitAnyInstruction(
                        clazz: Clazz,
                        method: Method,
                        codeAttribute: CodeAttribute,
                        offset: Int,
                        instruction: Instruction,
                    ) {}

                    override fun visitConstantInstruction(
                        clazz: Clazz,
                        method: Method,
                        codeAttribute: CodeAttribute,
                        offset: Int,
                        constantInstruction: ConstantInstruction,
                    ) {
                        if (constantInstruction.opcode == Instruction.OP_INVOKESTATIC) {
                            clazz.constantPoolEntryAccept(
                                constantInstruction.constantIndex,
                                ReferencedMemberVisitor(object : MemberVisitor {
                                    override fun visitAnyMember(
                                        clazz: Clazz,
                                        member: Member,
                                    ) {
                                    }

                                    override fun visitProgramMethod(
                                        programClass: ProgramClass,
                                        programMethod: ProgramMethod,
                                    ) {
                                        if (programClass.name.equals(injectContentClass.name) && programMethod.getName(programClass).equals("log")) {
                                            val expectedRange = exceptionRangesFinder.exceptionRanges.first()
                                            inRange = offset in expectedRange.exceptionStart..expectedRange.exceptionEnd
                                        }
                                    }
                                }),
                            )
                        }
                    }
                }
                targetMethod.attributesAccept(injectTargetClass, AllInstructionVisitor(rangeChecker))
                rangeChecker.inRange shouldBe true
            }
        }
    }
})
