/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2021 Guardsquare NV
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

package proguard.backport;

import proguard.classfile.*;
import proguard.classfile.attribute.*;
import proguard.classfile.attribute.visitor.*;
import proguard.classfile.constant.*;
import proguard.classfile.constant.visitor.*;
import proguard.classfile.util.*;
import proguard.classfile.visitor.ClassVisitor;
import proguard.util.ArrayUtil;

import java.util.*;

/**
 * This ClassVisitor collects all lambda expressions that are defined in
 * a visited class. Such functionality is useful when you would like to
 * resolve the actual method that is called when invokedynamic wants
 * to execute a lambda expression.
 *
 * @author Thomas Neidhart
 */
public class LambdaExpressionCollector
implements   ClassVisitor,
             ConstantVisitor,
             AttributeVisitor,
             BootstrapMethodInfoVisitor
{
    private final Map<InvokeDynamicConstant, LambdaExpression> lambdaExpressions;

    private InvokeDynamicConstant referencedInvokeDynamicConstant;
    private int                   referencedBootstrapMethodIndex;
    private Clazz                 referencedInvokedClass;
    private Method                referencedInvokedMethod;

    private int                   lambdaExpressionIndex = 0;


    public LambdaExpressionCollector(Map<InvokeDynamicConstant, LambdaExpression> lambdaExpressions)
    {
        this.lambdaExpressions = lambdaExpressions;
    }


    // Implementations for ClassVisitor.

    @Override
    public void visitAnyClass(Clazz clazz) { }


    @Override
    public void visitProgramClass(ProgramClass programClass)
    {
        // Visit any InvokeDynamic constant.
        programClass.constantPoolEntriesAccept(
            new ConstantTagFilter(Constant.INVOKE_DYNAMIC,
                                  this));
    }


    // Implementations for ConstantVisitor.

    @Override
    public void visitAnyConstant(Clazz clazz, Constant constant) {}


    @Override
    public void visitInvokeDynamicConstant(Clazz clazz, InvokeDynamicConstant invokeDynamicConstant)
    {
        referencedInvokeDynamicConstant = invokeDynamicConstant;
        referencedBootstrapMethodIndex  = invokeDynamicConstant.getBootstrapMethodAttributeIndex();
        clazz.attributesAccept(this);
    }


    @Override
    public void visitAnyMethodrefConstant(Clazz clazz, AnyMethodrefConstant anyMethodrefConstant)
    {
        referencedInvokedClass  = anyMethodrefConstant.referencedClass;
        referencedInvokedMethod = (Method) anyMethodrefConstant.referencedMethod;
    }


    // Implementations for AttributeVisitor.

    @Override
    public void visitAnyAttribute(Clazz clazz, Attribute attribute) {}


    @Override
    public void visitBootstrapMethodsAttribute(Clazz                     clazz,
                                               BootstrapMethodsAttribute bootstrapMethodsAttribute)
    {
        bootstrapMethodsAttribute.bootstrapMethodEntryAccept(clazz, referencedBootstrapMethodIndex, this);
    }


    // Implementations for BootstrapMethodInfoVisitor.

    @Override
    public void visitBootstrapMethodInfo(Clazz clazz, BootstrapMethodInfo bootstrapMethodInfo)
    {
        ProgramClass programClass = (ProgramClass) clazz;

        MethodHandleConstant bootstrapMethodHandle =
            (MethodHandleConstant) programClass.getConstant(bootstrapMethodInfo.u2methodHandleIndex);

        if (isLambdaMetaFactory(bootstrapMethodHandle.getClassName(clazz)))
        {
            String factoryMethodDescriptor =
                referencedInvokeDynamicConstant.getType(clazz);

            String interfaceClassName =
                ClassUtil.internalClassNameFromClassType(ClassUtil.internalMethodReturnType(factoryMethodDescriptor));

            // Find the actual method that is being invoked.
            MethodHandleConstant invokedMethodHandle =
                (MethodHandleConstant) programClass.getConstant(bootstrapMethodInfo.u2methodArguments[1]);

            referencedInvokedClass  = null;
            referencedInvokedMethod = null;
            clazz.constantPoolEntryAccept(invokedMethodHandle.u2referenceIndex, this);

            // Collect all the useful information.
            LambdaExpression lambdaExpression =
                new LambdaExpression(programClass,
                                     referencedBootstrapMethodIndex,
                                     bootstrapMethodInfo,
                                     factoryMethodDescriptor,
                                     new String[] { interfaceClassName },
                                     new String[0],
                                     referencedInvokeDynamicConstant.getName(clazz),
                                     getMethodTypeConstant(programClass, bootstrapMethodInfo.u2methodArguments[0]).getType(clazz),
                                     invokedMethodHandle.getReferenceKind(),
                                     invokedMethodHandle.getClassName(clazz),
                                     invokedMethodHandle.getName(clazz),
                                     invokedMethodHandle.getType(clazz),
                                     referencedInvokedClass,
                                     referencedInvokedMethod,
                                     lambdaExpressionIndex++);

            if (isAlternateFactoryMethod(bootstrapMethodHandle.getName(clazz)))
            {
                int flags =
                    getIntegerConstant(programClass,
                                       bootstrapMethodInfo.u2methodArguments[3]);

                // For the alternate metafactory, the optional arguments start
                // at index 4.
                int argumentIndex = 4;

                if ((flags & BootstrapMethodInfo.FLAG_MARKERS) != 0)
                {
                    int markerInterfaceCount =
                        getIntegerConstant(programClass,
                                           bootstrapMethodInfo.u2methodArguments[argumentIndex++]);

                    for (int i = 0; i < markerInterfaceCount; i++)
                    {
                        String interfaceName =
                            programClass.getClassName(bootstrapMethodInfo.u2methodArguments[argumentIndex++]);

                        lambdaExpression.interfaces =
                            ArrayUtil.add(lambdaExpression.interfaces,
                                          lambdaExpression.interfaces.length,
                                          interfaceName);
                    }
                }

                if ((flags & BootstrapMethodInfo.FLAG_BRIDGES) != 0)
                {
                    int bridgeMethodCount =
                        getIntegerConstant(programClass,
                                           bootstrapMethodInfo.u2methodArguments[argumentIndex++]);

                    for (int i = 0; i < bridgeMethodCount; i++)
                    {
                        MethodTypeConstant methodTypeConstant =
                            getMethodTypeConstant(programClass,
                                                  bootstrapMethodInfo.u2methodArguments[argumentIndex++]);

                        lambdaExpression.bridgeMethodDescriptors =
                            ArrayUtil.add(lambdaExpression.bridgeMethodDescriptors,
                                          lambdaExpression.bridgeMethodDescriptors.length,
                                          methodTypeConstant.getType(programClass));
                    }
                }

                if ((flags & BootstrapMethodInfo.FLAG_SERIALIZABLE) != 0)
                {
                    lambdaExpression.interfaces =
                        ArrayUtil.add(lambdaExpression.interfaces,
                                      lambdaExpression.interfaces.length,
                                      ClassConstants.NAME_JAVA_IO_SERIALIZABLE);
                }
            }

            lambdaExpressions.put(referencedInvokeDynamicConstant, lambdaExpression);
        }
    }

    // Small utility methods
    private static final String NAME_JAVA_LANG_INVOKE_LAMBDA_METAFACTORY = "java/lang/invoke/LambdaMetafactory";

    private static final String LAMBDA_ALTERNATE_METAFACTORY_METHOD      = "altMetafactory";

    private static boolean isLambdaMetaFactory(String className)
    {
        return NAME_JAVA_LANG_INVOKE_LAMBDA_METAFACTORY.equals(className);
    }

    private static boolean isAlternateFactoryMethod(String methodName)
    {
        return LAMBDA_ALTERNATE_METAFACTORY_METHOD.equals(methodName);
    }

    private static int getIntegerConstant(ProgramClass programClass, int constantIndex)
    {
        return ((IntegerConstant) programClass.getConstant(constantIndex)).getValue();
    }


    private static MethodTypeConstant getMethodTypeConstant(ProgramClass programClass, int constantIndex)
    {
        return (MethodTypeConstant) programClass.getConstant(constantIndex);
    }

}
