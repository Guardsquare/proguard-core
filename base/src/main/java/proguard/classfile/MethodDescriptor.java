/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2023 Guardsquare NV
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

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.stream.Collectors;
import proguard.classfile.util.ClassUtil;
import proguard.classfile.util.InternalTypeEnumeration;

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
    private static final Map<String, WeakReference<String>> typeCache = new WeakHashMap<>();

    @Deprecated
    public final String       returnType;
    @Deprecated
    public final List<String> argumentTypes;

    // Cached hashCode, since computing the hashCode for the list of arguments everytime
    // can be expensive.
    private final int hash;

    public MethodDescriptor(String descriptor)
    {
        if (descriptor == null)
        {
            returnType    = null;
            argumentTypes = null;
            hash          = 0;
        }
        else
        {
            returnType = intern(ClassUtil.internalMethodReturnType(descriptor));
            int count  = ClassUtil.internalMethodParameterCount(descriptor);
            if (count == 0)
            {
                this.argumentTypes = Collections.emptyList();
            }
            else
            {
                String[] argumentTypes = new String[count];
                InternalTypeEnumeration typeEnum = new InternalTypeEnumeration(descriptor);
                for (int i = 0; i < count; i++)
                {
                    argumentTypes[i] = intern(typeEnum.nextType());
                }
                this.argumentTypes = Arrays.asList(argumentTypes);
            }

            hash = Objects.hash(returnType, argumentTypes);
        }
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

    public String getReturnType()
    {
        return returnType;
    }

    public List<String> getArgumentTypes()
    {
        return argumentTypes;
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
        if (wildcard   == null) return true;
        if (descriptor == null) return false;

        return (wildcard.returnType == null    || Objects.equals(descriptor.returnType, wildcard.returnType)) &&
               (wildcard.argumentTypes == null || Objects.equals(descriptor.argumentTypes, wildcard.argumentTypes));
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
        if (wildcard   == null) return true;
        if (descriptor == null) return false;

        return checkReturnType(descriptor.returnType, wildcard.returnType) &&
               checkArguments(wildcard.argumentTypes, descriptor.argumentTypes);
    }

    private static boolean checkReturnType(String returnType, String wildcardReturnType)
    {
        if (wildcardReturnType == null) return true;
        if (returnType         == null) return false;

        return Objects.equals(replaceDollar(returnType), replaceDollar(wildcardReturnType));
    }

    private static boolean checkArguments(List<String> wildcardArgumentTypes, List<String> argumentTypes)
    {
        if (wildcardArgumentTypes == null) return true;
        if (argumentTypes         == null) return false;

        if (wildcardArgumentTypes.size() != argumentTypes.size()) return false;

        return Objects.equals(
            argumentTypes.stream().map(MethodDescriptor::replaceDollar).collect(Collectors.toList()),
            wildcardArgumentTypes.stream().map(MethodDescriptor::replaceDollar).collect(Collectors.toList())
        );
    }

    private static String replaceDollar(String s)
    {
        return ClassUtil.isInternalClassType(s) ? s.replace('$', '/') : s;
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

    private synchronized static String intern(String item)
    {
        WeakReference<String> ref = typeCache.get(item);
        if (ref != null)
        {
            String oldItem = ref.get();
            if (oldItem != null)
            {
                return oldItem;
            }
        }
        typeCache.put(item, new WeakReference<>(item));
        return item;
    }
}
