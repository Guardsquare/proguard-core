import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.stream.Collectors

fun getAllSmaliResources(): List<File> {
    val res = object {}::class.java.getResource("smali")

    return Files.walk(Paths.get(res.toURI()))
        .filter(Files::isRegularFile)
        .map { it.toFile() }
        .collect(Collectors.toList())
}

fun getSmaliResource(name: String): File {

    println("smali/$name")
    val res = object {}::class.java.getResource("smali/$name")
    println(res)
    return Paths.get(res.toURI()).toFile()
}
