package proguard.testutils.classfile.extensions

import proguard.classfile.ClassMemberPair
import proguard.classfile.Clazz
import proguard.classfile.Member
import proguard.classfile.Method
import proguard.classfile.ProgramClass
import proguard.classfile.ProgramMember
import proguard.classfile.attribute.Attribute
import proguard.classfile.attribute.annotation.Annotation
import proguard.classfile.attribute.annotation.AnnotationsAttribute
import proguard.classfile.editor.AttributesEditor
import proguard.classfile.editor.ConstantPoolEditor
import proguard.classfile.editor.InstructionSequenceBuilder
import proguard.classfile.instruction.Instruction
import proguard.classfile.util.ClassUtil
import proguard.testutils.MethodInstructionCollector
import proguard.testutils.and
import proguard.testutils.classfile.visitor.addAnnotationsAttribute
import proguard.testutils.shouldMatch
import proguard.testutils.shouldNotMatch

infix fun ClassMemberPair.shouldMatch(builder: InstructionSequenceBuilder.() -> InstructionSequenceBuilder) =
    (clazz and member).shouldMatch(builder)

infix fun ClassMemberPair.shouldNotMatch(builder: InstructionSequenceBuilder.() -> InstructionSequenceBuilder) =
    (clazz and member).shouldNotMatch(builder)

infix operator fun Clazz.get(member: Member) = ClassMemberPair(this, member)

/**
Easier to use overload of annotateMemberWith, to use when the annotation is a simple class with no fields attached. You simply pass
the internal class name and this takes care of constructing the actual Annotation object, adding the necessary strings to the constant pool in the process.
IMPORTANT: This DOES NOT initialize the class references.
 **/
fun ClassMemberPair.annotateMemberWith(annotationClassName: String, runtimeVisible: Boolean = true) {
    if (this.clazz !is ProgramClass) { throw IllegalStateException("Not a program class: ${this.clazz}") }
    if (this.member !is ProgramMember) { throw IllegalStateException("Not a program member: ${this.member}") }
    val programClass = this.clazz as ProgramClass
    val editor = ConstantPoolEditor(programClass)
    val annotation = Annotation(
        editor.addUtf8Constant(
            ClassUtil.internalTypeFromClassName(annotationClassName),
        ),
        0,
        arrayOf(),
    )
    annotateMemberWith(annotation, runtimeVisible = runtimeVisible)
}

/**
 * Adds the list of annotations passed to the member of this ClassMemberPair.
 */
fun ClassMemberPair.annotateMemberWith(vararg annotations: Annotation, runtimeVisible: Boolean = true) {
    if (this.clazz !is ProgramClass) { throw IllegalStateException("Not a program class: ${this.clazz}") }
    if (this.member !is ProgramMember) { throw IllegalStateException("Not a program member: ${this.member}") }
    val programClass = this.clazz as ProgramClass
    val member = this.member as ProgramMember
    val toFind = if (runtimeVisible) Attribute.RUNTIME_VISIBLE_ANNOTATIONS else Attribute.RUNTIME_INVISIBLE_ANNOTATIONS
    val editor = ConstantPoolEditor(programClass)
    val attributesEditor = AttributesEditor(programClass, member, false)
    val annotationsAttribute =
        attributesEditor.findAttribute(toFind) as AnnotationsAttribute?
    attributesEditor.addAnnotationsAttribute(editor, annotationsAttribute, runtimeVisible, *annotations)
}

fun ClassMemberPair.instructions(): List<Instruction> {
    val method = member as? Method ?: return emptyList()
    return MethodInstructionCollector.Companion.getMethodInstructions(clazz, method)
}
