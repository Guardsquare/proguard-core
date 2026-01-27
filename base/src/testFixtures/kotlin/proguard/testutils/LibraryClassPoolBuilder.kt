package proguard.testutils

import proguard.classfile.ClassPool
import proguard.classfile.LibraryClass
import proguard.classfile.LibraryField
import proguard.classfile.LibraryMethod
import proguard.classfile.ProgramClass
import proguard.classfile.ProgramField
import proguard.classfile.ProgramMethod

/**
 * Creates a new library class pool to avoid influencing other tests because the one returned
 * from `ClassPoolBuilder.fromSource(...)` is a property of the companion object (shared across tests).
 * The library classes should be initially compiled and stored in the program class pool so
 * that their subclasses can be linked at compile time.
 */
fun createLibraryClassPool(programClassPool: ClassPool, clazzNames: List<String>): ClassPool {
    val libraryClassPool = ClassPool()

    clazzNames.map {
        programClassPool.getClass(it)
    }.forEach {
        libraryClassPool.addClass(if (it is ProgramClass) it.asLibraryClass() else it)
        programClassPool.removeClass(it)
    }

    return libraryClassPool
}

private fun ProgramClass.asLibraryClass() = LibraryClass(
    u2accessFlags,
    name,
    superName,
    u2interfaces.map(this::getInterfaceName).toTypedArray(),
    u2interfaces.map(this::getInterface).toTypedArray(),
    subClassCount,
    subClasses,
    fields.map(this::asLibraryField).toTypedArray(),
    methods.map(this::asLibraryMethod).toTypedArray(),
    kotlinMetadata,
)

private fun ProgramClass.asLibraryField(field: ProgramField) =
    LibraryField(field.u2accessFlags, field.getName(this), field.getDescriptor(this))

private fun ProgramClass.asLibraryMethod(method: ProgramMethod) =
    LibraryMethod(method.u2accessFlags, method.getName(this), method.getDescriptor(this))
