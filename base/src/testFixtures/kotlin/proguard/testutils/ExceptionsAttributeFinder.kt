package proguard.testutils

import proguard.classfile.Clazz
import proguard.classfile.Method
import proguard.classfile.ProgramMethod
import proguard.classfile.attribute.Attribute
import proguard.classfile.attribute.ExceptionsAttribute
import proguard.classfile.attribute.visitor.AttributeVisitor

class ExceptionsAttributeFinder {
    companion object {
        fun findExceptionTableAttribute(method: ProgramMethod): ExceptionsAttribute? {
            var foundAttribute: ExceptionsAttribute? = null
            val exceptionsAttributeFinder = object : AttributeVisitor {
                override fun visitExceptionsAttribute(clazz: Clazz?, method: Method?, exceptionsAttribute: ExceptionsAttribute?) {
                    foundAttribute = exceptionsAttribute
                }

                override fun visitAnyAttribute(clazz: Clazz?, attribute: Attribute?) {}
            }

            method.attributesAccept(null, exceptionsAttributeFinder)
            return foundAttribute
        }
    }
}
