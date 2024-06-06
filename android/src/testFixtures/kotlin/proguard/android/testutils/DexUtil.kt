package proguard.android.testutils

import java.io.InputStream
import java.nio.file.Paths
import java.util.zip.ZipFile

fun getDexFromJar(name: String): InputStream {
    val root = object {}::class.java.getResource("/jar/$name")
    val jar = ZipFile(Paths.get(root!!.toURI()).toFile())
    return jar.getInputStream(jar.getEntry("classes.dex"))
}
