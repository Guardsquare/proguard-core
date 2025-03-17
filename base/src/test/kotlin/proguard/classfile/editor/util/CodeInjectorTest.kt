package proguard.classfile.editor.util

import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.string.shouldContain
import proguard.classfile.ClassConstants
import proguard.classfile.ProgramClass
import proguard.classfile.ProgramMethod
import proguard.classfile.util.inject.CodeInjector
import proguard.classfile.util.inject.argument.ConstantPrimitive
import proguard.classfile.util.inject.argument.ConstantString
import proguard.classfile.util.inject.location.FirstBlock
import proguard.classfile.visitor.ClassPrinter
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
    }
})
