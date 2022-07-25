/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2021 Guardsquare NV
 */

package proguard.classfile.kotlin.visitor;

import proguard.classfile.Clazz;
import proguard.classfile.attribute.annotation.*;
import proguard.classfile.attribute.annotation.visitor.*;
import proguard.classfile.attribute.visitor.*;
import proguard.classfile.constant.*;
import proguard.classfile.constant.visitor.ConstantVisitor;
import proguard.classfile.kotlin.*;
import proguard.classfile.visitor.ClassVisitor;

import java.util.function.Predicate;

import static proguard.classfile.attribute.Attribute.RUNTIME_VISIBLE_ANNOTATIONS;
import static proguard.classfile.kotlin.KotlinConstants.TYPE_KOTLIN_METADATA;
import static proguard.classfile.util.kotlin.KotlinMetadataInitializer.*;

/**
 * Tests a predicate on the classes' KotlinMetadataVersion and delegates to different ClassVisitors based on the result.
 */
public class KotlinMetadataVersionFilter
implements ClassVisitor,
           AnnotationVisitor,
           ElementValueVisitor
{
    private final Predicate<KotlinMetadataVersion> predicate;
    private final ClassVisitor accepted;
    private final ClassVisitor rejected;

    private final int[] mv = new int[3];


    /**
     * Tests a predicate on the classes' KotlinMetadataVersion and delegates to different ClassVisitors based on the result.
     * @param predicate The predicate to test the KotlinMetadata against
     * @param accepted  The ClassVisitor to delegate to iff predicate.test
     * @param rejected  The ClassVisitor to delegate to iff !predicate.test
     */
    public KotlinMetadataVersionFilter(Predicate<KotlinMetadataVersion> predicate, ClassVisitor accepted, ClassVisitor rejected)
    {
        this.predicate = predicate;
        this.accepted  = accepted;
        this.rejected  = rejected;
    }

    // Implementations for ClassVisitor.

    @Override
    public void visitAnyClass(Clazz clazz)
    {
        clazz.accept(
            new AllAttributeVisitor(
            new AttributeNameFilter(RUNTIME_VISIBLE_ANNOTATIONS,
            new AllAnnotationVisitor(
            new AnnotationTypeFilter(TYPE_KOTLIN_METADATA,
                                     this)))));
    }

    // Implementations for AnnotationVisitor.

    @Override
    public void visitAnnotation(Clazz clazz, Annotation annotation)
    {
        annotation.elementValuesAccept(clazz, this);

        if (predicate.test(new KotlinMetadataVersion(mv)))
        {
            clazz.accept(accepted);
        }
        else
        {
            clazz.accept(rejected);
        }
    }

    // Implementations for ElementValueVisitor.

    @Override
    public void visitAnyElementValue(Clazz clazz, Annotation annotation, ElementValue elementValue){}

    @Override
    public void visitArrayElementValue(Clazz clazz, Annotation annotation, ArrayElementValue arrayElementValue)
    {
        MetadataType arrayElementType = MetadataType.valueOf(arrayElementValue.getMethodName(clazz));
        // Collect the major, minor and patch elements of the metadataVersion in mv.
        arrayElementValue.elementValuesAccept(clazz,
                                              annotation,
                                              new MetadataVersionCollector(arrayElementType));
    }

    private class MetadataVersionCollector
        implements ElementValueVisitor,
                   ConstantVisitor
    {
        private int index = 0;

        private final MetadataType arrayElementType;

        public MetadataVersionCollector(MetadataType arrayElementType)
        {
            this.arrayElementType = arrayElementType;
        }

        // Implementations for ElementValueVisitor

        @Override
        public void visitAnyElementValue(Clazz clazz, Annotation annotation, ElementValue elementValue){}

        @Override
        public void visitConstantElementValue(Clazz clazz, Annotation annotation, ConstantElementValue constantElementValue)
        {
            clazz.constantPoolEntryAccept(constantElementValue.u2constantValueIndex, this);
        }

        // Implementations for ConstantVisitor

        @Override
        public void visitAnyConstant(Clazz clazz, Constant constant){}

        @Override
        public void visitIntegerConstant(Clazz clazz, IntegerConstant integerConstant)
        {
            if (this.arrayElementType == MetadataType.mv)
            {
                mv[index++] = integerConstant.getValue();
            }
        }
    }
}
