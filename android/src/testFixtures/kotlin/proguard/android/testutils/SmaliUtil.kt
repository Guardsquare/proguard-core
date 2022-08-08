package proguard.android.testutils

import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.stream.Collectors

fun getAllSmaliResources(): List<File> {
    val res = object {}::class.java.getResource("/smalifile")

    return Files.walk(Paths.get(res.toURI()))
        .filter(Files::isRegularFile)
        .map { it.toFile() }
        .collect(Collectors.toList())
}

fun getSmaliResource(name: String): File {
    val res = object {}::class.java.getResource("/smalifile/$name")
    return Paths.get(res.toURI()).toFile()
}
