package proguard.testutils.classfile.visitor

import proguard.classfile.attribute.Attribute
import proguard.classfile.attribute.CodeAttribute
import proguard.classfile.attribute.LineNumberTableAttribute
import proguard.classfile.attribute.annotation.Annotation
import proguard.classfile.attribute.annotation.AnnotationsAttribute
import proguard.classfile.attribute.annotation.RuntimeInvisibleAnnotationsAttribute
import proguard.classfile.attribute.annotation.RuntimeVisibleAnnotationsAttribute
import proguard.classfile.editor.AnnotationsAttributeEditor
import proguard.classfile.editor.AttributesEditor
import proguard.classfile.editor.ConstantPoolEditor

val CodeAttribute.lineNumberTableAttribute: LineNumberTableAttribute?
    get() = attributes?.asSequence()?.take(u2attributesCount)?.filterIsInstance<LineNumberTableAttribute>()
        ?.firstOrNull()

fun AttributesEditor.addAnnotationsAttribute(editor: ConstantPoolEditor, annotationsAttribute: AnnotationsAttribute?, runtimeVisible: Boolean, vararg annotations: Annotation) {
    var attribute = annotationsAttribute
    val toAdd = if (runtimeVisible) {
        RuntimeVisibleAnnotationsAttribute(
            editor.addUtf8Constant(Attribute.RUNTIME_VISIBLE_ANNOTATIONS),
            annotations.size,
            annotations,
        )
    } else {
        RuntimeInvisibleAnnotationsAttribute(
            editor.addUtf8Constant(Attribute.RUNTIME_INVISIBLE_ANNOTATIONS),
            annotations.size,
            annotations,
        )
    }
    if (attribute == null) {
        attribute = toAdd
        this.addAttribute(attribute)
    } else {
        val annotationsAttributeEditor =
            AnnotationsAttributeEditor(attribute)
        annotations.forEach { annotationsAttributeEditor.addAnnotation(it) }
    }
}
