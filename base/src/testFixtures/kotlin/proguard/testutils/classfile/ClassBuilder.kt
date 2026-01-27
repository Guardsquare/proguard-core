package proguard.testutils.classfile

import proguard.classfile.AccessConstants
import proguard.classfile.ClassConstants
import proguard.classfile.ClassPool
import proguard.classfile.Clazz
import proguard.classfile.ProgramClass
import proguard.classfile.ProgramField
import proguard.classfile.ProgramMethod
import proguard.classfile.VersionConstants
import proguard.classfile.attribute.Attribute
import proguard.classfile.attribute.Attribute.CODE
import proguard.classfile.attribute.Attribute.LINE_NUMBER_TABLE
import proguard.classfile.attribute.CodeAttribute
import proguard.classfile.attribute.InnerClassesAttribute
import proguard.classfile.attribute.InnerClassesInfo
import proguard.classfile.attribute.LineNumberInfo
import proguard.classfile.attribute.LineNumberTableAttribute
import proguard.classfile.attribute.SourceFileAttribute
import proguard.classfile.editor.AttributesEditor
import proguard.classfile.editor.ClassEditor
import proguard.classfile.editor.CodeAttributeComposer
import proguard.classfile.editor.ConstantPoolEditor
import proguard.classfile.editor.InnerClassesAttributeEditor
import proguard.classfile.editor.InstructionSequenceBuilder
import proguard.classfile.instruction.Instruction
import proguard.classfile.util.ClassSubHierarchyInitializer
import proguard.classfile.util.ClassSuperHierarchyInitializer
import proguard.preverify.CodePreverifier

class ClassBuilder {
    companion object {

        /**
         * Creates a Java 7 program class without methods or attributes.
         */
        fun createClass(className: String, superClass: Clazz? = null): ProgramClass {
            val clazz = ProgramClass(
                VersionConstants.CLASS_VERSION_1_7,
                1,
                arrayOfNulls(1),
                AccessConstants.PUBLIC,
                0,
                0,
            )
            val constantPoolEditor = ConstantPoolEditor(clazz)
            clazz.u2thisClass = constantPoolEditor.addClassConstant(className, null)
            clazz.u2superClass = constantPoolEditor.addClassConstant(ClassConstants.NAME_JAVA_LANG_OBJECT, null)

            if (superClass != null) {
                addInheritance(clazz, superClass)
            }
            return clazz
        }

        fun addMethod(
            clazz: ProgramClass,
            accessFlags: Int,
            name: String,
            descriptor: String,
            instructions: Array<Instruction>? = null,
        ): ProgramMethod {
            val constantPoolEditor = ConstantPoolEditor(clazz)
            val method = ProgramMethod(
                accessFlags,
                constantPoolEditor.addUtf8Constant(name),
                constantPoolEditor.addUtf8Constant(descriptor),
                null,
            )
            if (instructions != null) {
                setCodeAttribute(clazz, method, instructions)
            }
            ClassEditor(clazz).addMethod(method)
            return method
        }

        fun addField(
            clazz: ProgramClass,
            accessFlags: Int,
            name: String,
            descriptor: String,
        ): ProgramField {
            val constantPoolEditor = ConstantPoolEditor(clazz)
            val field = ProgramField(
                accessFlags,
                constantPoolEditor.addUtf8Constant(name),
                constantPoolEditor.addUtf8Constant(descriptor),
                null,
            )
            ClassEditor(clazz).addField(field)
            return field
        }

        fun ConstantPoolEditor.buildInstructions(op: InstructionSequenceBuilder.() -> Unit): Array<Instruction> {
            val builder = InstructionSequenceBuilder(this)
            op(builder)
            return builder.instructions()
        }

        fun ProgramClass.buildInstructions(op: InstructionSequenceBuilder.() -> Unit): Array<Instruction> {
            return ConstantPoolEditor(this).buildInstructions(op)
        }

        private val codePreverifier = CodePreverifier(false)
        private val codeComposer = CodeAttributeComposer()

        fun setCodeAttribute(
            clazz: ProgramClass,
            method: ProgramMethod,
            instructions: Array<Instruction>,
        ): CodeAttribute {
            val constantPoolEditor = ConstantPoolEditor(clazz)
            val codeAttribute = CodeAttribute(constantPoolEditor.addUtf8Constant(CODE))
            codeComposer.reset()
            codeComposer.beginCodeFragment(instructions.size)
            for (index in instructions.indices) {
                codeComposer.appendInstruction(index, instructions[index])
            }
            codeComposer.endCodeFragment()
            codeComposer.visitCodeAttribute(clazz, method, codeAttribute)
            codePreverifier.visitCodeAttribute(clazz, method, codeAttribute)
            AttributesEditor(clazz, method, true).addAttribute(codeAttribute)
            return codeAttribute
        }

        fun setLineNumberTable(
            clazz: ProgramClass,
            method: ProgramMethod,
            codeAttribute: CodeAttribute,
            vararg lineNumbers: Pair<Int, Int>,
        ): LineNumberTableAttribute {
            return setLineNumberTable(clazz, method, codeAttribute, *lineNumbers.map { (startPc, lineNumber) -> LineNumberInfo(startPc, lineNumber) }.toTypedArray())
        }

        fun setLineNumberTable(
            clazz: ProgramClass,
            method: ProgramMethod,
            codeAttribute: CodeAttribute,
            vararg lineNumbers: LineNumberInfo,
        ): LineNumberTableAttribute {
            val constantPoolEditor = ConstantPoolEditor(clazz)
            val lineNumberTableAttribute = LineNumberTableAttribute(constantPoolEditor.addUtf8Constant(LINE_NUMBER_TABLE), lineNumbers.size, lineNumbers)
            AttributesEditor(clazz, method, codeAttribute, true).addAttribute(lineNumberTableAttribute)
            return lineNumberTableAttribute
        }

        fun addInnerClassesInfo(
            clazz: ProgramClass,
            innerClassesInfo: InnerClassesInfo,
        ) {
            val constantPoolEditor = ConstantPoolEditor(clazz)
            val innerClassesAttribute = InnerClassesAttribute(constantPoolEditor.addUtf8Constant(Attribute.INNER_CLASSES), 0, arrayOfNulls<InnerClassesInfo>(0))
            val innerClassesAttributeEditor = InnerClassesAttributeEditor(innerClassesAttribute)
            // The InnerClassesInfo needs to be part of the InnerClassAttribute before it can be added to a class.
            innerClassesAttributeEditor.addInnerClassesInfo(innerClassesInfo)
            AttributesEditor(clazz, false).addAttribute(innerClassesAttribute)
        }

        fun setSourceFile(clazz: ProgramClass, sourceFile: String): SourceFileAttribute {
            val constantPoolEditor = ConstantPoolEditor(clazz)
            val sourceFileAttribute = SourceFileAttribute(constantPoolEditor.addUtf8Constant(Attribute.SOURCE_FILE), constantPoolEditor.addUtf8Constant(sourceFile))
            AttributesEditor(clazz, true).addAttribute(sourceFileAttribute)
            return sourceFileAttribute
        }

        fun addInheritance(subClass: ProgramClass, superClass: Clazz) {
            val classPool = ClassPool()
            classPool.addClass(subClass)
            classPool.addClass(superClass)

            subClass.u2superClass = ConstantPoolEditor(subClass).addClassConstant(superClass)

            classPool.accept(ClassSubHierarchyInitializer())
            classPool.classesAccept(ClassSuperHierarchyInitializer(classPool, ClassPool()))
        }
    }
}
