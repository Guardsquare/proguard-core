package proguard.testutils

import proguard.classfile.Clazz
import proguard.classfile.Method
import proguard.classfile.ProgramMethod
import proguard.classfile.attribute.Attribute
import proguard.classfile.attribute.CodeAttribute
import proguard.classfile.attribute.visitor.AttributeVisitor

class CodeAttributeFinder {
    companion object {
        fun findCodeAttribute(method: ProgramMethod): CodeAttribute? {
            var foundAttribute: CodeAttribute? = null
            val codeAttributeFinder = object : AttributeVisitor {
                override fun visitCodeAttribute(clazz: Clazz?, method: Method?, codeAttribute: CodeAttribute?) {
                    foundAttribute = codeAttribute
                }

                override fun visitAnyAttribute(clazz: Clazz?, attribute: Attribute?) {}
            }

            method.attributesAccept(null, codeAttributeFinder)
            return foundAttribute
        }
    }
}
