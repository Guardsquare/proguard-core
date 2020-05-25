/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2020 Guardsquare NV
 */

package proguard.classfile.kotlin.visitor;

import proguard.classfile.Clazz;
import proguard.classfile.kotlin.*;

import java.util.function.BiConsumer;

/**
 * This {@link KotlinTypeVisitor} delegates to multiple {@link KotlinTypeVisitor}s.
 *
 * @author James Hamilton
 */
public class MultiKotlinTypeParameterVisitor
implements   KotlinTypeParameterVisitor
{
    private final KotlinTypeParameterVisitor[] kotlinTypeParameterVisitors;

    public MultiKotlinTypeParameterVisitor(KotlinTypeParameterVisitor...kotlinTypeParameterVisitor)
    {
        this.kotlinTypeParameterVisitors = kotlinTypeParameterVisitor;
    }


    @Override
    public void visitAnyTypeParameter(Clazz clazz, KotlinTypeParameterMetadata kotlinTypeParameterMetadata) { }


    @Override
    public void visitClassTypeParameter(Clazz                       clazz,
                                        KotlinMetadata              kotlinMetadata,
                                        KotlinTypeParameterMetadata kotlinTypeParameterMetadata)
    {
        for (KotlinTypeParameterVisitor kotlinTypeParameterVisitor : this.kotlinTypeParameterVisitors)
        {
            kotlinTypeParameterVisitor.visitClassTypeParameter(clazz, kotlinMetadata, kotlinTypeParameterMetadata);
        }
    }


    @Override
    public void visitPropertyTypeParameter(Clazz                              clazz,
                                           KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
                                           KotlinPropertyMetadata             kotlinPropertyMetadata,
                                           KotlinTypeParameterMetadata        kotlinTypeParameterMetadata)
    {
        for (KotlinTypeParameterVisitor kotlinTypeParameterVisitor : this.kotlinTypeParameterVisitors)
        {
            kotlinTypeParameterVisitor.visitPropertyTypeParameter(clazz,
                                                                  kotlinDeclarationContainerMetadata,
                                                                  kotlinPropertyMetadata,
                                                                  kotlinTypeParameterMetadata);
        }
    }


    @Override
    public void visitFunctionTypeParameter(Clazz                       clazz,
                                           KotlinMetadata              kotlinMetadata,
                                           KotlinFunctionMetadata      kotlinFunctionMetadata,
                                           KotlinTypeParameterMetadata kotlinTypeParameterMetadata)
    {
        for (KotlinTypeParameterVisitor kotlinTypeParameterVisitor : this.kotlinTypeParameterVisitors)
        {
            kotlinTypeParameterVisitor.visitFunctionTypeParameter(clazz,
                                                                  kotlinMetadata,
                                                                  kotlinFunctionMetadata,
                                                                  kotlinTypeParameterMetadata);
        }
    }


    @Override
    public void visitAliasTypeParameter(Clazz                              clazz,
                                        KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
                                        KotlinTypeAliasMetadata            kotlinTypeAliasMetadata,
                                        KotlinTypeParameterMetadata        kotlinTypeParameterMetadata)
    {
        for (KotlinTypeParameterVisitor kotlinTypeParameterVisitor : this.kotlinTypeParameterVisitors)
        {
            kotlinTypeParameterVisitor.visitAliasTypeParameter(clazz,
                                                               kotlinDeclarationContainerMetadata,
                                                               kotlinTypeAliasMetadata,
                                                               kotlinTypeParameterMetadata);
        }
    }
}
