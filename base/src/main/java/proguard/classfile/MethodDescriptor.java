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

package proguard.classfile;

import static proguard.classfile.util.ClassUtil.externalShortClassName;
import static proguard.classfile.util.ClassUtil.externalType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import proguard.classfile.util.ClassUtil;

/**
 * Represents the descriptor that is part of a {@link MethodSignature}.
 * A descriptor consists of parameter types and return type of a method,
 * e.g. "()V" for a void method with no parameters, or "(II)B" for a
 * method that takes two integers and returns a boolean. Read more about
 * this topic in
 * <a href="https://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.3.3">§4.3.3</a>
 * of the JVM specification.
 *
 * @author Samuel Hopstock
 */
public class MethodDescriptor
{

    public final String       returnType;
    public final List<String> argumentTypes;

    public MethodDescriptor(String descriptor)
    {
        if (descriptor == null)
        {
            returnType = null;
            argumentTypes = null;
        }
        else
        {
            returnType = ClassUtil.internalMethodReturnType(descriptor);
            argumentTypes = new ArrayList<>();
            for (int i = 0; i < ClassUtil.internalMethodParameterCount(descriptor); i++)
            {
                argumentTypes.add(ClassUtil.internalMethodParameterType(descriptor, i));
            }
        }
    }

    public MethodDescriptor(String returnType, List<String> argumentTypes)
    {
        this.returnType = returnType;
        this.argumentTypes = argumentTypes;
    }

    /**
     * Analogous to {@link MethodSignature#matchesIgnoreNull(MethodSignature)}.
     *
     * @param other The {@link MethodDescriptor} to compare with this one
     * @return true if the two objects match
     */
    public boolean matchesIgnoreNull(MethodDescriptor other)
    {
        return (returnType == null || other.returnType == null || Objects.equals(returnType, other.returnType))
               && (argumentTypes == null || other.argumentTypes == null || Objects.equals(argumentTypes, other.argumentTypes));
    }

    /**
     * Analogous to {@link MethodSignature#matchesIgnoreNullAndDollar(MethodSignature)}.
     *
     * @param other The {@link MethodDescriptor} to compare with this one
     * @return true if the two objects match
     */
    public boolean matchesIgnoreNullAndDollar(MethodDescriptor other)
    {
        return (returnType == null || other.returnType == null || Objects.equals(returnType.replace('$', '/'), other.returnType.replace('$', '/')))
               && (argumentTypes == null || other.argumentTypes == null || Objects
            .equals(argumentTypes.stream().map(t -> t.replace('$', '/')).collect(Collectors.toList()), other.argumentTypes.stream().map(t -> t.replace('$', '/')).collect(
                Collectors.toList())));
    }

    /**
     * Get the human readable representation of the return type.
     * E.g. "void" for "V" or "?" for an undefined return type.
     *
     * @return The human readable representation of {@link #returnType}
     */
    public String getPrettyReturnType()
    {
        if (returnType != null)
        {
            return externalShortClassName(externalType(returnType));
        }
        return "?";
    }

    /**
     * Get the human readable representation of the argument types.
     * E.g. "String,int" for "(Ljava/lang/String;I)".
     *
     * @return The human readable representation of {@link #argumentTypes}
     */
    public String getPrettyArgumentTypes()
    {
        if (argumentTypes != null)
        {
            return argumentTypes.stream()
                                .map(ClassUtil::externalType)
                                .map(ClassUtil::externalShortClassName)
                                .collect(Collectors.joining(","));
        }
        return "?";
    }

    @Override
    public String toString()
    {
        if (returnType != null)
        {
            if (argumentTypes != null)
            {
                return ClassUtil.internalMethodDescriptorFromInternalTypes(returnType, argumentTypes);
            }
            else
            {
                return "(?)" + returnType;
            }
        }
        else
        {
            if (argumentTypes != null)
            {
                return ClassUtil.internalMethodDescriptorFromInternalTypes("?", argumentTypes);
            }
            else
            {
                return "(?)?";
            }
        }
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }
        MethodDescriptor that = (MethodDescriptor) o;
        return Objects.equals(returnType, that.returnType) && Objects.equals(argumentTypes, that.argumentTypes);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(returnType, argumentTypes);
    }
}
