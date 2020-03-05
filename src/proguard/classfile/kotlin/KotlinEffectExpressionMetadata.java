/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2019 GuardSquare NV
 */
package proguard.classfile.kotlin;

import proguard.classfile.Clazz;
import proguard.classfile.kotlin.flags.KotlinEffectExpressionFlags;
import proguard.classfile.kotlin.visitor.*;
import proguard.util.*;
import java.util.List;


public class KotlinEffectExpressionMetadata
extends      SimpleProcessable
implements   Processable
{
    public int parameterIndex = -1;

    public boolean hasConstantValue = false;
    public Object constantValue; // May be intentionally null;

    public KotlinTypeMetadata typeOfIs;

    public List<KotlinEffectExpressionMetadata> andRightHandSides;

    public List<KotlinEffectExpressionMetadata> orRightHandSides;

    public KotlinEffectExpressionFlags flags;


    public void andRightHandSideAccept(Clazz                   clazz,
                                       KotlinEffectMetadata    kotlinEffectMetadata,
                                       KotlinEffectExprVisitor kotlinEffectExprVisitor)
    {
        for (KotlinEffectExpressionMetadata rhs : andRightHandSides)
        {
            kotlinEffectExprVisitor.visitAndRHSExpression(clazz, kotlinEffectMetadata, this, rhs);
        }
    }


    public void orRightHandSideAccept(Clazz                   clazz,
                                      KotlinEffectMetadata    kotlinEffectMetadata,
                                      KotlinEffectExprVisitor kotlinEffectExprVisitor)
    {
        for (KotlinEffectExpressionMetadata rhs : andRightHandSides)
        {
            kotlinEffectExprVisitor.visitOrRHSExpression(clazz, kotlinEffectMetadata, this, rhs);
        }
    }


    public void typeOfIsAccept(Clazz clazz, KotlinTypeVisitor kotlinTypeVisitor)
    {
        if (typeOfIs != null)
        {
            kotlinTypeVisitor.visitTypeOfIsExpression(clazz, this, typeOfIs);
        }
    }


    public void setMetadataFlags(int flags)
    {
        this.flags = new KotlinEffectExpressionFlags(flags);
    }


    // Implementations for Object.
    @Override
    public String toString()
    {
        return "Kotlin contract effect";
    }

}
