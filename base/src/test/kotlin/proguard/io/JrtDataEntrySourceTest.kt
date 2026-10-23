package proguard.io

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkStatic
import proguard.classfile.ClassConstants.NAME_JAVA_LANG_OBJECT
import proguard.classfile.ClassPool
import proguard.classfile.visitor.ClassNameCollector
import proguard.classfile.visitor.ClassPoolFiller
import proguard.exception.ErrorId
import proguard.exception.ProguardCoreException
import java.io.IOException
import java.nio.file.FileSystems
import java.nio.file.Path

class JrtDataEntrySourceTest : FunSpec({
    context("JrtDataEntrySource tests") {

        val javaHome = System.getProperty("java.home")
        test("Reading all modules from the jrt") {
            val source = JrtDataEntrySource(Path.of(javaHome), null)
            val classNames = mutableSetOf<String>()
            val classReader = ClassFilter(ClassReader(true, true, true, true, null, ClassNameCollector(classNames)))
            source.pumpDataEntries(classReader)
            classNames.size shouldBeGreaterThan 0
            classNames shouldContain NAME_JAVA_LANG_OBJECT
            classNames shouldContain "java/net/http/HttpClient"
        }
        test("Reading the single java.base module") {
            val source = JrtDataEntrySource(Path.of(javaHome), "java.base")
            val classNames = mutableSetOf<String>()
            val classReader = ClassFilter(ClassReader(true, true, true, true, null, ClassNameCollector(classNames)))
            source.pumpDataEntries(classReader)
            classNames.size shouldBeGreaterThan 0
            classNames shouldContain NAME_JAVA_LANG_OBJECT
            classNames shouldNotContain "java/net/http/HttpClient"
        }
        test("Reading a non-existing module") {
            val source = JrtDataEntrySource(Path.of(javaHome), "java.foo")
            val classReader = ClassFilter(ClassReader(true, true, true, true, null, ClassPoolFiller(ClassPool())))
            val exc = shouldThrow<ProguardCoreException> { source.pumpDataEntries(classReader) }
            exc.componentErrorId shouldBe ErrorId.JRT_INVALID_MODULE
            exc.message shouldBe "JRT module 'java.foo' does not exist"
        }
        test("Invalid java home") {
            val exc = shouldThrow<ProguardCoreException> { JrtDataEntrySource(Path.of("foo"), "java.foo") }
            exc.componentErrorId shouldBe ErrorId.JRT_INVALID_JAVA_HOME
            exc.message shouldBe "foo does not look like a JAVA HOME: missing file foo/lib/jrt-fs.jar"
        }
        test("Invalid jrt file system") {
            mockkStatic(FileSystems::class) {
                val fakeExc = IOException("An error occurred")
                every { FileSystems.newFileSystem(any(), any(), any()) } throws fakeExc
                val exc = shouldThrow<ProguardCoreException> { JrtDataEntrySource(Path.of(javaHome), "java.foo") }
                exc.componentErrorId shouldBe ErrorId.JRT_FILE_SYSTEM_ERROR
                exc.cause shouldBe fakeExc
            }
        }
    }
})
