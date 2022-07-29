import org.intellij.lang.annotations.Language

class SmaliSource(val filename: String, @Language("smali") val contents: String)
