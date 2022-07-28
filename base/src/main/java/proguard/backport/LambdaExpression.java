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
import proguard.classfile.attribute.BootstrapMethodInfo;
import proguard.classfile.constant.MethodHandleConstant;
import proguard.classfile.util.*;

/**
 * A small helper class that captures useful information
 * about a lambda expression as encountered in a class file.
 *
 * @author Thomas Neidhart
 */
public class LambdaExpression
{
    // The referenced class of the lambda expression.
    public ProgramClass referencedClass;

    // The referenced bootstrap method index.
    public int                 bootstrapMethodIndex;
    // The referenced bootstrap method info.
    public BootstrapMethodInfo bootstrapMethodInfo;

    // The lambda factory method type.
    public String factoryMethodDescriptor;

    // The implemented interfaces of the Lambda expression.
    public String[] interfaces;

    // The additional bridge method descriptors to be added.
    public String[] bridgeMethodDescriptors;

    // The name and descriptor of the implemented interface method.
    public String interfaceMethod;
    public String interfaceMethodDescriptor;

    // Information regarding the invoked method.
    public int    invokedReferenceKind;
    public String invokedClassName;
    public String invokedMethodName;
    public String invokedMethodDesc;

    public Clazz  referencedInvokedClass;
    public Method referencedInvokedMethod;

    // The created lambda class.
    public ProgramClass lambdaClass;

    //The index of the lambda expression, used for naming purposes.
    private final int   lambdaExpressionIndex;


    /**
     * Creates a new initialized LambdaExpression (except for the lambdaClass).
     */
    public LambdaExpression(ProgramClass        referencedClass,
                            int                 bootstrapMethodIndex,
                            BootstrapMethodInfo bootstrapMethodInfo,
                            String              factoryMethodDescriptor,
                            String[]            interfaces,
                            String[]            bridgeMethodDescriptors,
                            String              interfaceMethod,
                            String              interfaceMethodDescriptor,
                            int                 invokedReferenceKind,
                            String              invokedClassName,
                            String              invokedMethodName,
                            String              invokedMethodDesc,
                            Clazz               referencedInvokedClass,
                            Method              referencedInvokedMethod,
                            int                 lambdaExpressionIndex)
    {
        this.referencedClass           = referencedClass;
        this.bootstrapMethodIndex      = bootstrapMethodIndex;
        this.bootstrapMethodInfo       = bootstrapMethodInfo;
        this.factoryMethodDescriptor   = factoryMethodDescriptor;
        this.interfaces                = interfaces;
        this.bridgeMethodDescriptors   = bridgeMethodDescriptors;
        this.interfaceMethod           = interfaceMethod;
        this.interfaceMethodDescriptor = interfaceMethodDescriptor;
        this.invokedReferenceKind      = invokedReferenceKind;
        this.invokedClassName          = invokedClassName;
        this.invokedMethodName         = invokedMethodName;
        this.invokedMethodDesc         = invokedMethodDesc;
        this.referencedInvokedClass    = referencedInvokedClass;
        this.referencedInvokedMethod   = referencedInvokedMethod;
        this.lambdaExpressionIndex     = lambdaExpressionIndex;
    }


    /**
     * Returns the class name of the converted anonymous class.
     */
    public String getLambdaClassName()
    {
        return String.format("%s$$Lambda$%d",
                             referencedClass.getName(),
                             lambdaExpressionIndex);
    }


    public String getConstructorDescriptor()
    {
        if (isStateless())
        {
            return ClassConstants.METHOD_TYPE_INIT;
        }
        else
        {
            int endIndex = factoryMethodDescriptor.indexOf(TypeConstants.METHOD_ARGUMENTS_CLOSE);

            return factoryMethodDescriptor.substring(0, endIndex + 1) + TypeConstants.VOID;
        }
    }


    /**
     * Returns whether the lambda expression is serializable.
     */
    public boolean isSerializable()
    {
        for (String interfaceName : interfaces)
        {
            if (ClassConstants.NAME_JAVA_IO_SERIALIZABLE.equals(interfaceName))
            {
                return true;
            }
        }
        return false;
    }


    /**
     * Returns whether the lambda expression is actually a method reference.
     */
    public boolean isMethodReference()
    {
        return !isLambdaMethod(invokedMethodName);
    }


    /**
     * Returns whether the lambda expression is stateless.
     */
    public boolean isStateless()
    {
        // The lambda expression is stateless if the factory method does
        // not have arguments.
        return
            ClassUtil.internalMethodParameterCount(factoryMethodDescriptor) == 0;
    }


    /**
     * Returns whether the invoked method is a static interface method.
     */
    public boolean invokesStaticInterfaceMethod()
    {
        // We assume unknown classes are not interfaces.
        return invokedReferenceKind == MethodHandleConstant.REF_INVOKE_STATIC &&
               referencedInvokedClass != null                          &&
               (referencedInvokedClass.getAccessFlags() & AccessConstants.INTERFACE) != 0;
    }


    /**
     * Returns whether the invoked method is a non-static, private synthetic
     * method in an interface.
     */
    boolean referencesPrivateSyntheticInterfaceMethod()
    {
        // We assume unknown classes are not interfaces.
        return (referencedInvokedClass != null)                                                 &&
               (referencedInvokedClass .getAccessFlags() &  AccessConstants.INTERFACE)  != 0 &&
               (referencedInvokedMethod.getAccessFlags() & (AccessConstants.PRIVATE |
                                                            AccessConstants.SYNTHETIC)) != 0 ;
    }


    /**
     * Returns whether an accessor method is needed to access
     * the invoked method from the lambda class.
     */
    public boolean needsAccessorMethod()
    {
        // We assume unknown classes don't need an accessor method.
        return referencedInvokedClass != null &&
               new MemberFinder().findMethod(lambdaClass,
                                             referencedInvokedClass,
                                             invokedMethodName,
                                             invokedMethodDesc) == null;
    }


    /**
     * Returns whether the lambda expression is a method reference
     * to a private constructor.
     */
    public boolean referencesPrivateConstructor()
    {
        return invokedReferenceKind == MethodHandleConstant.REF_NEW_INVOKE_SPECIAL &&
               ClassConstants.METHOD_NAME_INIT.equals(invokedMethodName)   &&
               (referencedInvokedMethod.getAccessFlags() & AccessConstants.PRIVATE) != 0;
    }


    // Small Utility methods.

    private static final String LAMBDA_METHOD_PREFIX = "lambda$";

    private static boolean isLambdaMethod(String methodName)
    {
        return methodName.startsWith(LAMBDA_METHOD_PREFIX);
    }
}
