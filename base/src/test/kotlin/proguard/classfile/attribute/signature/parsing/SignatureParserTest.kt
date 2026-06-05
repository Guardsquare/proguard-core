package proguard.classfile.attribute.signature.parsing

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import proguard.classfile.attribute.signature.SignatureParser
import proguard.classfile.attribute.signature.ast.visitor.ClassNameCollectingVisitor
import proguard.exception.ErrorId
import proguard.exception.ProguardCoreException

class SignatureParserTest : FunSpec({
    context("Signature parser tests") {

        val parser = SignatureParser()
        withData(
            "method descriptor" to "(IDLjava/lang/Thread;)Ljava/lang/Object;",
            "method signature" to "<I:Ljava/lang/Object;O:Ljava/lang/Object;>(Landroidx/activity/result/contract/ActivityResultContract<TI;TO;>;Landroidx/activity/result/ActivityResultCallback<TO;>;)Landroidx/activity/result/ActivityResultLauncher<TI;>;",
            "field descriptor" to "[[Ljava/lang/Object;",
            "type signature" to "Ljava/util/concurrent/CopyOnWriteArrayList<Landroidx/core/util/Consumer<Landroidx/core/app/MultiWindowModeChangedInfo;>;>;",

        ) { (_, input) ->
            test("Parsing a valid signature or descriptor returns a filled optional") {
                val node = parser.parseSignatureOrDescriptor(input)
                node.isEmpty shouldBe false
            }
        }
        withData(
            "method descriptor" to "(IDLjava/lang/Thread;)void",
            "method signature" to "<I:Ljava/lang/Object;O:Ljava/lang/Object;>(null)Landroidx/activity/result/ActivityResultLauncher<TI;>;",
            "field descriptor" to "[[Ljava/",
            "type signature" to "java/util/concurrent/CopyOnWriteArrayList",

        ) { (_, input) ->
            test("Parsing an invalid signature or descriptor returns the empty optional") {
                val node = parser.parseSignatureOrDescriptor(input)
                node.isEmpty shouldBe true
            }
        }
        test("Parsing a valid method descriptor and applying the ClassNameCollectingVistor returns the list of referenced classes") {
            val classNameCollector = ClassNameCollectingVisitor()
            val set = mutableSetOf<String>()
            val input = "(IDLjava/lang/Thread;Ljava/lang/String;)Ljava/lang/Object;"
            parser.parseAndAccept(input, classNameCollector, set)
            set.shouldContainOnly("java/lang/Object", "java/lang/String", "java/lang/Thread")
        }
        test("Parsing an invalid method descriptor and applying the ClassNameCollectingVistor throws a ProguardCoreException") {
            val classNameCollector = ClassNameCollectingVisitor()
            val set = mutableSetOf<String>()
            val input = "()null"
            val exception = shouldThrow<ProguardCoreException> {
                parser.parseAndAccept(input, classNameCollector, set)
            }
            exception.componentErrorId shouldBe ErrorId.SIGNATURE_AST_INVALID_STRUCTURE
            exception.message shouldContain "String is not a valid signature or descriptor: $input"
        }
    }
})
