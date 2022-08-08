package proguard.android.testutils

import org.intellij.lang.annotations.Language

class SmaliSource(val filename: String, @Language("smali") val contents: String, val apiLevel: Int = 15)
