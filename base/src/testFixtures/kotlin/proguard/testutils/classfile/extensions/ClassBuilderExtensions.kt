package proguard.testutils.classfile.extensions

import org.jetbrains.kotlin.descriptors.runtime.structure.primitiveByWrapper
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeFirstWord
import proguard.classfile.AccessConstants
import proguard.classfile.ClassConstants
import proguard.classfile.ProgramClass
import proguard.classfile.ProgramField
import proguard.classfile.ProgramMethod
import proguard.classfile.VersionConstants
import proguard.classfile.editor.ClassBuilder
import proguard.classfile.editor.CompactCodeAttributeComposer
import proguard.classfile.util.ClassUtil
import java.util.*

fun buildDataClass(
    name: String = makeClassName(),
    superName: String = ClassConstants.NAME_JAVA_LANG_OBJECT,
    accessFlags: Int = AccessConstants.PUBLIC,
    version: Int = VersionConstants.CLASS_VERSION_1_7,
    vararg fields: Pair<String, String>,
): ProgramClass {
    return buildClass(name, superName, accessFlags, version) {
        fields.forEach {
            addField(AccessConstants.PUBLIC, it.first, it.second)
            addMethod(if (it.first == "ok") "get${it.first.capitalizeFirstWord()}" else "get${it.first.capitalize()}", "()${it.second}") {
                aload(0)
                getfield(name, it.first, it.second)
                return_(it.second)
            }
            addMethod(if (it.first == "ok") "set${it.first.capitalizeFirstWord()}" else "set${it.first.capitalize()}", "(${it.second})V") {
                aload(0)
                load(1, it.second)
                putfield(name, it.first, it.second)
                return_()
            }
        }
        addMethod("<init>", "(${fields.joinToString(separator = "") { it.second }})V") {
            aload(0)
            invokespecial("java/lang/Object", "<init>", "()V")
            for (i in 0 until fields.size) {
                aload(0)
                load(i + 1, fields[i].second)
                putfield(name, fields[i].first, fields[i].second)
            }
            return_()
        }
    }.first
}

fun buildClass(
    name: String = makeClassName(),
    superName: String = ClassConstants.NAME_JAVA_LANG_OBJECT,
    accessFlags: Int = AccessConstants.PUBLIC,
    version: Int = VersionConstants.CLASS_VERSION_1_7,
) = buildClass(name, superName, accessFlags, version) {}.first

fun <T> buildClass(
    name: String = makeClassName(),
    superName: String = ClassConstants.NAME_JAVA_LANG_OBJECT,
    accessFlags: Int = AccessConstants.PUBLIC,
    version: Int = VersionConstants.CLASS_VERSION_1_7,
    op: ClassBuilder.() -> T,
): Pair<ProgramClass, T> {
    val builder = ClassBuilder(version, accessFlags, name, superName)
    return builder.programClass to op(builder)
}

fun ClassBuilder.addStaticMethod(
    name: String,
    descriptor: String = "()V",
    accessFlags: Int = AccessConstants.PUBLIC or AccessConstants.STATIC,
    op: (CompactCodeAttributeComposer.() -> Unit)? = null,
) = addMethod(name, descriptor, accessFlags, op)

fun ClassBuilder.addMethod(
    name: String,
    descriptor: String = "()V",
    accessFlags: Int = AccessConstants.PUBLIC,
    op: (CompactCodeAttributeComposer.() -> Unit)? = null,
): ProgramMethod {
    if (op == null) {
        return addAndReturnMethod(accessFlags, name, descriptor)
    }
    return addAndReturnMethod(accessFlags, name, descriptor, 200, op)
}

fun ClassBuilder.addField(
    name: String,
    descriptor: String,
    accessFlags: Int = AccessConstants.PUBLIC,
): ProgramField {
    return addAndReturnField(accessFlags, name, descriptor)
}

inline fun <reified T> ClassBuilder.addField(name: String, accessFlags: Int = AccessConstants.PUBLIC): ProgramField {
    return addField(name, externalReifiedType<T>(), accessFlags)
}

fun ClassBuilder.addStaticField(
    name: String,
    descriptor: String,
    accessFlags: Int = AccessConstants.PUBLIC or AccessConstants.STATIC,
) = addField(name, descriptor, accessFlags)

inline fun <reified T> ClassBuilder.addStaticField(
    name: String,
    accessFlags: Int = AccessConstants.PUBLIC or AccessConstants.STATIC,
) = addField<T>(name, accessFlags)

inline fun <reified T> externalReifiedType(): String =
    ClassUtil.internalType(T::class.java.let { it.primitiveByWrapper ?: it }.name)

private fun makeClassName(): String {
    val seed = Throwable().stackTraceToString().hashCode().toLong()
    val random = Random(seed)
    val uuid = UUID(random.nextLong(), random.nextLong())
    return "TestClass$uuid"
}
