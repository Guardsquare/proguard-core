package proguard.testutils.classfile.visitor

import proguard.classfile.Clazz
import proguard.classfile.attribute.Attribute
import proguard.classfile.attribute.annotation.Annotation
import proguard.classfile.attribute.annotation.RuntimeInvisibleAnnotationsAttribute
import proguard.classfile.attribute.annotation.RuntimeVisibleAnnotationsAttribute
import proguard.classfile.attribute.annotation.visitor.AnnotationVisitor
import proguard.classfile.attribute.visitor.AttributeVisitor
import java.util.Arrays

class MarkedRuntimeVisibleAnnotationFilter(private val acceptedVisitor: AnnotationVisitor, private val rejectedVisitor: AnnotationVisitor, private val visibilityMarkerName: String) : AttributeVisitor, AnnotationVisitor {

    override fun visitAnyAttribute(clazz: Clazz, attribute: Attribute) { }

    override fun visitRuntimeVisibleAnnotationsAttribute(clazz: Clazz, runtimeVisibleAnnotationsAttribute: RuntimeVisibleAnnotationsAttribute) {
        runtimeVisibleAnnotationsAttribute.annotationsAccept(clazz, this)
    }

    override fun visitAnnotation(clazz: Clazz, annotation: Annotation) {
        if (!annotation.elementValues.isNullOrEmpty() &&
            Arrays.stream(annotation.elementValues).anyMatch { it.getMethodName(clazz) == visibilityMarkerName }
        ) {
            acceptedVisitor.visitAnnotation(clazz, annotation)
        } else {
            rejectedVisitor.visitAnnotation(clazz, annotation)
        }
    }
}

class MarkedRuntimeInvisibleAnnotationFilter(private val annotationCounterVisitor: AnnotationVisitor, private val rejectedAnnotationCounterVisitor: AnnotationVisitor, private val visibilityMarkerName: String) : AttributeVisitor, AnnotationVisitor {

    override fun visitAnyAttribute(clazz: Clazz, attribute: Attribute) { }

    override fun visitRuntimeInvisibleAnnotationsAttribute(clazz: Clazz, runtimeInvisibleAnnotationsAttribute: RuntimeInvisibleAnnotationsAttribute) {
        runtimeInvisibleAnnotationsAttribute.annotationsAccept(clazz, this)
    }

    override fun visitAnnotation(clazz: Clazz, annotation: Annotation) {
        if (!annotation.elementValues.isNullOrEmpty() &&
            Arrays.stream(annotation.elementValues).anyMatch { it.getMethodName(clazz) == visibilityMarkerName }
        ) {
            annotationCounterVisitor.visitAnnotation(clazz, annotation)
        } else {
            rejectedAnnotationCounterVisitor.visitAnnotation(clazz, annotation)
        }
    }
}
