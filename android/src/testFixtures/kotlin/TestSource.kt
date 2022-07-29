import com.tschuchort.compiletesting.SourceFile
import org.intellij.lang.annotations.Language
import testutils.TestSource

class SmaliSource(val filename: String, @Language("smali") val contents: String) : TestSource() {
    override fun asSourceFile() = SourceFile.new(filename, contents)
}
