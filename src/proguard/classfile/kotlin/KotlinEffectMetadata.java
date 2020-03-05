/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2019 GuardSquare NV
 */
package proguard.classfile.kotlin;

import kotlinx.metadata.*;
import proguard.classfile.Clazz;
import proguard.classfile.kotlin.visitor.*;
import proguard.util.*;

import java.util.List;


public class KotlinEffectMetadata
extends      SimpleProcessable
implements   Processable
{
    public KmEffectType effectType;

    public KmEffectInvocationKind invocationKind;

    public KotlinEffectExpressionMetadata conclusionOfConditionalEffect;

    public List<KotlinEffectExpressionMetadata> constructorArguments;


    public KotlinEffectMetadata(KmEffectType           effectType,
                                KmEffectInvocationKind invocationKind)
    {
        this.effectType = effectType;
        this.invocationKind = invocationKind;
    }


    public void accept(Clazz                  clazz,
                       KotlinMetadata         kotlinMetadata,
                       KotlinFunctionMetadata kotlinFunctionMetadata,
                       KotlinContractMetadata kotlinContractMetadata,
                       KotlinEffectVisitor    kotlinEffectVisitor)
    {
        kotlinEffectVisitor.visitEffect(clazz,
                                        kotlinMetadata,
                                        kotlinFunctionMetadata,
                                        kotlinContractMetadata,
                                        this);
    }

    public void constructorArgumentAccept(Clazz                   clazz,
                                          KotlinEffectExprVisitor kotlinEffectExprVisitor)
    {
        for (KotlinEffectExpressionMetadata constructorArgument : constructorArguments)
        {
            kotlinEffectExprVisitor.visitConstructorArgExpression(clazz, this, constructorArgument);
        }
    }

    public void conclusionOfConditionalEffectAccept(Clazz                   clazz,
                                                    KotlinEffectExprVisitor kotlinEffectExprVisitor)
    {
        if (conclusionOfConditionalEffect != null)
        {
            kotlinEffectExprVisitor.visitConclusionExpression(clazz, this, conclusionOfConditionalEffect);
        }
    }


    // Implementations for Object.
    @Override
    public String toString()
    {
        return "Kotlin contract effect";
    }
}
