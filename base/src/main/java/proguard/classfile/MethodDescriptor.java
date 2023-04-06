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
import java.util.Optional;
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

    // Cached hashCode, since computing the hashCode for the list of arguments everytime
    // can be expensive.
    private final int hash;

    public MethodDescriptor(String descriptor)
    {
        int hashCode;
        if (descriptor == null)
        {
            returnType    = null;
            argumentTypes = null;
            hashCode      = 0;
        }
        else
        {
            returnType    = ClassUtil.internalMethodReturnType(descriptor);
            argumentTypes = new ArrayList<>();
            hashCode      = returnType.hashCode();
            for (int i = 0; i < ClassUtil.internalMethodParameterCount(descriptor); i++)
            {
                String e = ClassUtil.internalMethodParameterType(descriptor, i);
                argumentTypes.add(e);
                hashCode = 31 * hashCode + (e == null ? 0 : e.hashCode());
            }
        }
        this.hash = hashCode;
    }

    public MethodDescriptor(String returnType, List<String> argumentTypes)
    {
        this.returnType    = returnType;
        this.argumentTypes = argumentTypes;
        this.hash          = Objects.hash(returnType, argumentTypes);
    }

    /**
     * Check if this descriptor is missing information.
     */
    public boolean isIncomplete()
    {
        return returnType == null || argumentTypes == null;
    }

    /**
     * Analogous to {@link MethodSignature.matchesIgnoreNull(MethodSignature, MethodSignature)}.
     *
     * @param descriptor The {@link MethodDescriptor} to be compared
     * @param wildcard   The {@link MethodDescriptor} pattern to be matched against
     * @return true if the two objects match
     */
    public static boolean matchesIgnoreNull(MethodDescriptor descriptor, MethodDescriptor wildcard)
    {
        return wildcard == null
               || Optional.ofNullable(descriptor).map(d -> (wildcard.returnType == null
                                                            || Objects.equals(d.returnType, wildcard.returnType))
                                                           && (wildcard.argumentTypes == null
                                                               || Objects.equals(d.argumentTypes, wildcard.argumentTypes)))
                          .orElse(false);
    }

    /**
     * Analogous to {@link MethodSignature.matchesIgnoreNullAndDollar(MethodSignature, MethodSignature)}.
     *
     * @param descriptor The {@link MethodDescriptor} to be compared
     * @param wildcard   The {@link MethodDescriptor} pattern to be matched against
     * @return true if the two objects match
     */
    public static boolean matchesIgnoreNullAndDollar(MethodDescriptor descriptor, MethodDescriptor wildcard)
    {
        return wildcard == null
               || Optional.ofNullable(descriptor).map(d -> (wildcard.returnType == null
                                                            || Objects.equals(Optional.ofNullable(d.returnType)
                                                                                      .map(s -> s.replace('$', '/'))
                                                                                      .orElse(null),
                                                                              wildcard.returnType.replace('$', '/')))
                                                           && (wildcard.argumentTypes == null
                                                               || Objects.equals(Optional.ofNullable(d.argumentTypes)
                                                                                         .map(l -> l.stream()
                                                                                                    .map(t -> t.replace('$', '/'))
                                                                                                    .collect(Collectors.toList()))
                                                                                         .orElse(null),
                                                                                 wildcard.argumentTypes.stream()
                                                                                                       .map(t -> t.replace('$', '/'))
                                                                                                       .collect(Collectors.toList()))))
                          .orElse(false);
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
        return hash;
    }
}
