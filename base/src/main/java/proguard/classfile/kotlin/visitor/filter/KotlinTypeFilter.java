/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2020 Guardsquare NV
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package proguard.classfile.kotlin.visitor.filter;

import proguard.classfile.Clazz;
import proguard.classfile.kotlin.*;
import proguard.classfile.kotlin.visitor.KotlinTypeVisitor;

import java.util.function.Predicate;

/**
 * This KotlinTypeVisitor delegates to another KotlinTypeVisitor if the
 * predicate succeeds.
 *
 * @author James Hamilton
 */
public class KotlinTypeFilter
implements   KotlinTypeVisitor
{
    private final KotlinTypeVisitor             kotlinTypeVisitor;
    private final Predicate<KotlinTypeMetadata> predicate;

    public KotlinTypeFilter(Predicate<KotlinTypeMetadata> predicate,
                            KotlinTypeVisitor             kotlinTypeVisitor)
    {
        this.predicate = predicate;
        this.kotlinTypeVisitor = kotlinTypeVisitor;
    }


    // Implements for KotlinTypeVisitor.

    @Override
    public void visitAnyType(Clazz clazz, KotlinTypeMetadata kotlinTypeMetadata) {}


    @Override
    public void visitTypeUpperBound(Clazz clazz, KotlinTypeMetadata boundedType, KotlinTypeMetadata upperBound)
    {
        if (this.predicate.test(upperBound))
        {
            this.kotlinTypeVisitor.visitTypeUpperBound(clazz, boundedType, upperBound);
        }
    }


    @Override
    public void visitAbbreviation(Clazz clazz,
                                  KotlinTypeMetadata kotlinTypeMetadata,
                                  KotlinTypeMetadata abbreviation)
    {
        if (this.predicate.test(abbreviation))
        {
            this.kotlinTypeVisitor.visitAbbreviation(clazz, kotlinTypeMetadata, abbreviation);
        }
    }


    @Override
    public void visitParameterUpperBound(Clazz clazz,
                                         KotlinTypeParameterMetadata boundedTypeParameter,
                                         KotlinTypeMetadata upperBound)
    {
        if (this.predicate.test(upperBound))
        {
            this.kotlinTypeVisitor.visitParameterUpperBound(clazz, boundedTypeParameter, upperBound);
        }
    }


    @Override
    public void visitTypeOfIsExpression(Clazz clazz,
                                        KotlinEffectExpressionMetadata kotlinEffectExprMetadata,
                                        KotlinTypeMetadata typeOfIs)
    {
        if (this.predicate.test(typeOfIs))
        {
            this.kotlinTypeVisitor.visitTypeOfIsExpression(clazz, kotlinEffectExprMetadata, typeOfIs);
        }
    }


    @Override
    public void visitTypeArgument(Clazz clazz,
                                  KotlinTypeMetadata kotlinTypeMetadata,
                                  KotlinTypeMetadata typeArgument)
    {
        if (this.predicate.test(typeArgument))
        {
            this.kotlinTypeVisitor.visitTypeArgument(clazz, kotlinTypeMetadata, typeArgument);
        }
    }


    @Override
    public void visitStarProjection(Clazz clazz, KotlinTypeMetadata typeWithStarArg)
    {
        if (this.predicate.test(typeWithStarArg))
        {
            this.kotlinTypeVisitor.visitStarProjection(clazz, typeWithStarArg);
        }
    }


    @Override
    public void visitOuterClass(Clazz clazz, KotlinTypeMetadata innerClass, KotlinTypeMetadata outerClass)
    {
        if (this.predicate.test(outerClass))
        {
            this.kotlinTypeVisitor.visitOuterClass(clazz, innerClass, outerClass);
        }
    }


    @Override
    public void visitSuperType(Clazz clazz,
                               KotlinClassKindMetadata kotlinMetadata,
                               KotlinTypeMetadata kotlinTypeMetadata)
    {
        if (this.predicate.test(kotlinTypeMetadata))
        {
            this.kotlinTypeVisitor.visitSuperType(clazz, kotlinMetadata, kotlinTypeMetadata);
        }
    }


    @Override
    public void visitConstructorValParamType(Clazz clazz,
                                             KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
                                             KotlinConstructorMetadata kotlinConstructorMetadata,
                                             KotlinValueParameterMetadata kotlinValueParameterMetadata,
                                             KotlinTypeMetadata kotlinTypeMetadata)
    {
        if (this.predicate.test(kotlinTypeMetadata))
        {
            this.kotlinTypeVisitor.visitConstructorValParamType(clazz,
                                                            kotlinDeclarationContainerMetadata,
                                                            kotlinConstructorMetadata,
                                                            kotlinValueParameterMetadata,
                                                            kotlinTypeMetadata);
        }
    }


    @Override
    public void visitConstructorValParamVarArgType(Clazz clazz,
                                                   KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
                                                   KotlinConstructorMetadata kotlinConstructorMetadata,
                                                   KotlinValueParameterMetadata kotlinValueParameterMetadata,
                                                   KotlinTypeMetadata kotlinTypeMetadata)
    {
        if (this.predicate.test(kotlinTypeMetadata))
        {
            this.kotlinTypeVisitor.visitConstructorValParamVarArgType(clazz,
                                                                  kotlinDeclarationContainerMetadata,
                                                                  kotlinConstructorMetadata,
                                                                  kotlinValueParameterMetadata,
                                                                  kotlinTypeMetadata);
        }
    }


    @Override
    public void visitPropertyType(Clazz clazz,
                                  KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
                                  KotlinPropertyMetadata kotlinPropertyMetadata,
                                  KotlinTypeMetadata kotlinTypeMetadata)
    {
        if (this.predicate.test(kotlinTypeMetadata))
        {
            this.kotlinTypeVisitor.visitPropertyType(clazz,
                                                 kotlinDeclarationContainerMetadata,
                                                 kotlinPropertyMetadata,
                                                 kotlinTypeMetadata);
        }
    }


    @Override
    public void visitPropertyReceiverType(Clazz clazz,
                                          KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
                                          KotlinPropertyMetadata kotlinPropertyMetadata,
                                          KotlinTypeMetadata kotlinTypeMetadata)
    {
        if (this.predicate.test(kotlinTypeMetadata))
        {
            this.kotlinTypeVisitor.visitPropertyReceiverType(clazz,
                                                         kotlinDeclarationContainerMetadata,
                                                         kotlinPropertyMetadata,
                                                         kotlinTypeMetadata);
        }
    }


    @Override
    public void visitPropertyValParamType(Clazz clazz,
                                          KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
                                          KotlinPropertyMetadata kotlinPropertyMetadata,
                                          KotlinValueParameterMetadata kotlinValueParameterMetadata,
                                          KotlinTypeMetadata kotlinTypeMetadata)
    {
        if (this.predicate.test(kotlinTypeMetadata))
        {
            this.kotlinTypeVisitor.visitPropertyValParamType(clazz,
                                                         kotlinDeclarationContainerMetadata,
                                                         kotlinPropertyMetadata,
                                                         kotlinValueParameterMetadata,
                                                         kotlinTypeMetadata);
        }
    }


    @Override
    public void visitPropertyValParamVarArgType(Clazz clazz,
                                                KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
                                                KotlinPropertyMetadata kotlinPropertyMetadata,
                                                KotlinValueParameterMetadata kotlinValueParameterMetadata,
                                                KotlinTypeMetadata kotlinTypeMetadata)
    {
        if (this.predicate.test(kotlinTypeMetadata))
        {
            this.kotlinTypeVisitor.visitPropertyValParamVarArgType(clazz,
                                                               kotlinDeclarationContainerMetadata,
                                                               kotlinPropertyMetadata,
                                                               kotlinValueParameterMetadata,
                                                               kotlinTypeMetadata);
        }
    }


    @Override
    public void visitFunctionReturnType(Clazz clazz,
                                        KotlinMetadata kotlinMetadata,
                                        KotlinFunctionMetadata kotlinFunctionMetadata,
                                        KotlinTypeMetadata kotlinTypeMetadata)
    {
        if (this.predicate.test(kotlinTypeMetadata))
        {
            this.kotlinTypeVisitor.visitFunctionReturnType(clazz,
                                                           kotlinMetadata,
                                                           kotlinFunctionMetadata,
                                                           kotlinTypeMetadata);
        }
    }


    @Override
    public void visitFunctionReceiverType(Clazz clazz,
                                          KotlinMetadata kotlinMetadata,
                                          KotlinFunctionMetadata kotlinFunctionMetadata,
                                          KotlinTypeMetadata kotlinTypeMetadata)
    {
        if (this.predicate.test(kotlinTypeMetadata))
        {
            this.kotlinTypeVisitor.visitFunctionReceiverType(clazz,
                                                         kotlinMetadata,
                                                         kotlinFunctionMetadata,
                                                         kotlinTypeMetadata);
        }
    }

    @Override
    public void visitFunctionContextReceiverType(Clazz                  clazz,
                                                 KotlinMetadata         kotlinMetadata,
                                                 KotlinFunctionMetadata kotlinFunctionMetadata,
                                                 KotlinTypeMetadata     kotlinTypeMetadata)
    {
        if (this.predicate.test(kotlinTypeMetadata))
        {
            this.kotlinTypeVisitor.visitFunctionContextReceiverType(
                clazz, kotlinMetadata, kotlinFunctionMetadata, kotlinTypeMetadata
            );
        }
    }

    @Override
    public void visitClassContextReceiverType(Clazz              clazz,
                                              KotlinMetadata     kotlinMetadata,
                                              KotlinTypeMetadata kotlinTypeMetadata)
    {
        if (this.predicate.test(kotlinTypeMetadata))
        {
            this.kotlinTypeVisitor.visitClassContextReceiverType(
                clazz, kotlinMetadata, kotlinTypeMetadata
            );
        }
    }


    @Override
    public void visitPropertyContextReceiverType(Clazz                  clazz,
                                                 KotlinMetadata         kotlinMetadata,
                                                 KotlinPropertyMetadata kotlinPropertyMetadata,
                                                 KotlinTypeMetadata     kotlinTypeMetadata)
    {
        if (this.predicate.test(kotlinTypeMetadata))
        {
            this.kotlinTypeVisitor.visitPropertyContextReceiverType(
                clazz, kotlinMetadata, kotlinPropertyMetadata, kotlinTypeMetadata
            );
        }
    }

    @Override
    public void visitFunctionValParamType(Clazz clazz,
                                          KotlinMetadata kotlinMetadata,
                                          KotlinFunctionMetadata kotlinFunctionMetadata,
                                          KotlinValueParameterMetadata kotlinValueParameterMetadata,
                                          KotlinTypeMetadata kotlinTypeMetadata)
    {
        if (this.predicate.test(kotlinTypeMetadata))
        {
            this.kotlinTypeVisitor.visitFunctionValParamType(clazz,
                                                         kotlinMetadata,
                                                         kotlinFunctionMetadata,
                                                         kotlinValueParameterMetadata,
                                                         kotlinTypeMetadata);
        }
    }


    @Override
    public void visitFunctionValParamVarArgType(Clazz clazz,
                                                KotlinMetadata kotlinMetadata,
                                                KotlinFunctionMetadata kotlinFunctionMetadata,
                                                KotlinValueParameterMetadata kotlinValueParameterMetadata,
                                                KotlinTypeMetadata kotlinTypeMetadata)
    {
        if (this.predicate.test(kotlinTypeMetadata))
        {
            this.kotlinTypeVisitor.visitFunctionValParamVarArgType(clazz,
                                                               kotlinMetadata,
                                                               kotlinFunctionMetadata,
                                                               kotlinValueParameterMetadata,
                                                               kotlinTypeMetadata);
        }
    }


    @Override
    public void visitAliasUnderlyingType(Clazz clazz,
                                         KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
                                         KotlinTypeAliasMetadata kotlinTypeAliasMetadata,
                                         KotlinTypeMetadata underlyingType)
    {
        if (this.predicate.test(underlyingType))
        {
            this.kotlinTypeVisitor.visitAliasUnderlyingType(clazz,
                                                        kotlinDeclarationContainerMetadata,
                                                        kotlinTypeAliasMetadata,
                                                        underlyingType);
        }
    }


    @Override
    public void visitAliasExpandedType(Clazz clazz,
                                       KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
                                       KotlinTypeAliasMetadata kotlinTypeAliasMetadata,
                                       KotlinTypeMetadata expandedType)
    {
        if (this.predicate.test(expandedType))
        {
            this.kotlinTypeVisitor.visitAliasExpandedType(clazz,
                                                      kotlinDeclarationContainerMetadata,
                                                      kotlinTypeAliasMetadata,
                                                      expandedType);
        }
    }
}
