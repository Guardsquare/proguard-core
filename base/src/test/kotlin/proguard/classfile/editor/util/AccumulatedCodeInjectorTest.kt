package proguard.classfile.editor.util

import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.string.shouldContain
import proguard.classfile.ProgramClass
import proguard.classfile.ProgramMethod
import proguard.classfile.util.inject.AccumulatedCodeInjector
import proguard.classfile.util.inject.argument.ConstantPrimitive
import proguard.classfile.util.inject.argument.ConstantString
import proguard.classfile.util.inject.location.FirstBlock
import proguard.classfile.util.inject.location.LastBlocks
import proguard.classfile.util.inject.location.SpecificOffset
import proguard.classfile.visitor.ClassPrinter
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.JavaSource
import java.io.PrintWriter
import java.io.StringWriter

class AccumulatedCodeInjectorTest : BehaviorSpec({
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
            AccumulatedCodeInjector()
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

        When("Injecting all InjectContent log methods into static method InjectTarget.main()") {
            val targetMethod = injectTargetClass.findMethod("instanceMethod", "()I") as ProgramMethod
            AccumulatedCodeInjector()
                .injectInvokeStatic(injectContentClass, injectContentClass.findMethod("log", "()V"))
                .into(injectTargetClass, targetMethod)
                .at(SpecificOffset(0, true))
                .injectInvokeStatic(
                    injectContentClass,
                    injectContentClass.findMethod("logPrimitive", "(ID)V"),
                    ConstantPrimitive(99),
                    ConstantPrimitive(42.24),
                )
                .into(injectTargetClass, targetMethod)
                .at(FirstBlock())
                .injectInvokeStatic(
                    injectContentClass,
                    injectContentClass.findMethod("logString", "(Ljava/lang/String;)V"),
                    ConstantString("foo"),
                )
                .into(injectTargetClass, targetMethod)
                .at(LastBlocks())
                .injectInvokeStatic(
                    injectContentClass,
                    injectContentClass.findMethod("logMixed", "(Ljava/lang/String;ID)V"),
                    ConstantString("bar"),
                    ConstantPrimitive(1),
                    ConstantPrimitive(12.49),
                )
                .into(injectTargetClass, targetMethod)
                .at(SpecificOffset(1, false))
                .commit()

            Then("InjectTarget.main() should contain all logging instructions in the correct order") {
                val renderedMethod = StringWriter()
                targetMethod.accept(injectTargetClass, ClassPrinter(PrintWriter(renderedMethod)))

                renderedMethod.toString() shouldContain Regex(
                    """
                    \s*\[0] invokestatic #\d+ = Methodref\(InjectContent\.log\(\)V\)
                    \s*\[\d+] bipush 99
                    \s*\[\d+] ldc2_w #\d+ = Double\(42\.24\)
                    \s*\[\d+] invokestatic #\d+ = Methodref\(InjectContent\.logPrimitive\(ID\)V\)
                    \s*\[\d+] iconst_0
                    \s*\[\d+] ldc #\d+ = String\("foo"\)
                    \s*\[\d+] invokestatic #\d+ = Methodref\(InjectContent\.logString\(Ljava/lang/String;\)V\)
                    \s*\[\d+] ireturn
                    \s*\[\d+] ldc #\d+ = String\("bar"\)
                    \s*\[\d+] iconst_1
                    \s*\[\d+] ldc2_w #\d+ = Double\(12\.49\)
                    \s*\[\d+] invokestatic #\d+ = Methodref\(InjectContent\.logMixed\(Ljava/lang/String;ID\)V\)
                    """.trimIndent(),
                )
            }
        }
    }
})
