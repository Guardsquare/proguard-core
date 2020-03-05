/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2019 GuardSquare NV
 */
package proguard.classfile.kotlin.visitor;

import proguard.classfile.Clazz;
import proguard.classfile.kotlin.*;

public interface KotlinEffectExprVisitor
{
    void visitAnyEffectExpression(Clazz                          clazz,
                                  KotlinEffectMetadata           kotlinEffectMetadata,
                                  KotlinEffectExpressionMetadata kotlinEffectExpressionMetadata);


    // Nested.

    default void visitAndRHSExpression(Clazz                          clazz,
                                       KotlinEffectMetadata           kotlinEffectMetadata,
                                       KotlinEffectExpressionMetadata lhs,
                                       KotlinEffectExpressionMetadata rhs)
    {
        visitAnyEffectExpression(clazz, kotlinEffectMetadata, rhs);
    }

    default void visitOrRHSExpression(Clazz                          clazz,
                                      KotlinEffectMetadata           kotlinEffectMetadata,
                                      KotlinEffectExpressionMetadata lhs,
                                      KotlinEffectExpressionMetadata rhs)
    {
        visitAnyEffectExpression(clazz, kotlinEffectMetadata, rhs);
    }

    // Effects.

    default void visitConstructorArgExpression(Clazz                          clazz,
                                               KotlinEffectMetadata           kotlinEffectMetadata,
                                               KotlinEffectExpressionMetadata kotlinEffectExpressionMetadata)
    {
        visitAnyEffectExpression(clazz, kotlinEffectMetadata, kotlinEffectExpressionMetadata);
    }

    default void visitConclusionExpression(Clazz                          clazz,
                                           KotlinEffectMetadata           kotlinEffectMetadata,
                                           KotlinEffectExpressionMetadata kotlinEffectExpressionMetadata)
    {
        visitAnyEffectExpression(clazz, kotlinEffectMetadata, kotlinEffectExpressionMetadata);
    }
}
