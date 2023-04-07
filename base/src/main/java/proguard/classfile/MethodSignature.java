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

import static proguard.classfile.util.ClassUtil.externalClassName;
import static proguard.classfile.util.ClassUtil.externalShortClassName;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents a Method signature containing a class, method and a descriptor.
 *
 * <p>
 * In order to avoid a huge blow-up in memory consumption for analyses that
 * rely heavily on method signatures, e.g. the creation of a call graph,
 * this class provides the ability to cache signatures belonging to concrete
 * {@link Method} objects. By using the corresponding {@link #computeIfAbsent(Clazz, Method)}
 * method, the amount of strings that need to be retained during runtime is
 * reduced.
 * </p>
 *
 * @author Dennis Titze, Samuel Hopstock
 */
public class MethodSignature
    extends Signature
{

    public static final            MethodSignature              UNKNOWN        = new MethodSignature(null, null, (MethodDescriptor) null);
    private static final           Map<Method, MethodSignature> signatureCache = new IdentityHashMap<>();
    public final                   String                       method;
    public final                   MethodDescriptor             descriptor;
    private                        Method                       referencedMethod;

    public MethodSignature(String internalClassName, String method, MethodDescriptor descriptor)
    {
        super(internalClassName, Objects.hash(internalClassName, method, descriptor));
        this.method           = method;
        this.descriptor       = descriptor;
        this.referencedClass  = null;
        this.referencedMethod = null;
    }

    public MethodSignature(String internalClassName, String method, String descriptor)
    {
        this(internalClassName, method, new MethodDescriptor(descriptor));
    }

    public MethodSignature(Clazz clazz, Method method)
    {
        this(clazz.getName(), method.getName(clazz), method.getDescriptor(clazz));
        this.referencedClass  = clazz;
        this.referencedMethod = method;
    }

    /**
     * Returns the {@link Method} reference that corresponds to the method
     * represented by this {@link MethodSignature} or <code>null</code>
     * if no reference is available (e.g. the methhod or its class were missing).
     */
    public Method getReferencedMethod()
    {
        return this.referencedMethod;
    }

    /**
     * Get the singleton {@link MethodSignature} object for this specific {@link Method}.
     * If it is not yet available in the cache, it will be newly instantiated.
     *
     * @param clazz  The class containing the target method
     * @param method The method whose signature is to be generated
     * @return The cached or newly generated {@link MethodSignature} object
     */
    public static MethodSignature computeIfAbsent(Clazz clazz, Method method)
    {
        return signatureCache.computeIfAbsent(method, m -> new MethodSignature(clazz, method));
    }

    @Override
    public boolean isIncomplete()
    {
        return className == null || method == null || descriptor == null || descriptor.isIncomplete();
    }

    @Override
    protected String calculateFqn()
    {
        return String.format("L%s;%s%s", className == null ? "?" : className,
                             method == null ? "?" : method,
                             descriptor);
    }

    @Override
    protected String calculatePrettyFqn()
    {
        String result;
        String params = descriptor == null ? "?" : descriptor.getPrettyArgumentTypes();

        String shortClassName = className == null ? "?" : externalShortClassName(externalClassName(className));
        if (ClassConstants.METHOD_NAME_INIT.equals(method))
        {
            result = String.format("%s(%s)", shortClassName, params);
        }
        else if (ClassConstants.METHOD_NAME_CLINIT.equals(method))
        {
            result = String.format("static initializer (%s)", shortClassName);
        }
        else
        {
            String returnType = descriptor == null ? "?" : descriptor.getPrettyReturnType();
            String methodName = method == null ? "?" : method;
            result = String.format("%s %s.%s(%s)", returnType, shortClassName, methodName, params);
        }

        return result;
    }

    /**
     * Remove all currently cached {@link MethodSignature} objects from the cache,
     * allowing them to be removed by the garbage collector.
     */
    public static void clearCache()
    {
        signatureCache.clear();
    }

    /**
     * Fuzzy check if two {@link MethodSignature} objects are equal. If any
     * pattern field is null, its value in the matched object does not influence
     * the check result, providing a way to create a wildcard {@link MethodSignature}
     * that e.g. matches several methods of a class that have the same name
     * but a different {@link MethodDescriptor}.
     *
     * @param signature The {@link MethodSignature} to be compared
     * @param wildcard  The {@link MethodSignature} pattern to be matched against
     * @return true     if the two objects match
     */
    public static boolean matchesIgnoreNull(MethodSignature signature, MethodSignature wildcard)
    {
        if (wildcard  == null) return true;
        if (signature == null) return false;

        return (wildcard.className  == null || Objects.equals(signature.className, wildcard.className)) &&
               (wildcard.method     == null || Objects.equals(signature.method, wildcard.method)) &&
               MethodDescriptor.matchesIgnoreNull(signature.descriptor, wildcard.descriptor);
    }

    /**
     * Fuzzy check like {@link .matchesIgnoreNull(MethodSignature)} but allows
     * dollar signs in type strings. Usually the notation for inner classes
     * is {@code com/example/Foo$Bar}, but sometimes external systems call
     * this class {@code com/example/Foo/Bar}. These two names correspond to
     * the same class and thus should be treated as the same if they appear
     * in a {@link MethodSignature}. If it is to be expected that this situation
     * may occur, this method should be preferred over
     * {@link .matchesIgnoreNull(MethodSignature)} to avoid false negatives.
     *
     * @param signature The {@link MethodSignature} to be compared
     * @param wildcard  The {@link MethodSignature} pattern to be matched against
     * @return true if the two objects match
     */
    public static boolean matchesIgnoreNullAndDollar(MethodSignature signature, MethodSignature wildcard)
    {
        if (wildcard  == null) return true;
        if (signature == null) return false;

        return checkClassNameIgnoreNullAndDollar(wildcard.className, signature.className) &&
               checkMethodNameIgnoreNull(wildcard.method, signature.method) &&
               MethodDescriptor.matchesIgnoreNullAndDollar(signature.descriptor, wildcard.descriptor);
    }

    private static boolean checkClassNameIgnoreNullAndDollar(String className, String wildcardClassName)
    {
        if (wildcardClassName == null) return true;
        if (className         == null) return false;

        return Objects.equals(className.replace('$', '/'), wildcardClassName.replace('$', '/'));
    }

    private static boolean checkMethodNameIgnoreNull(String method, String wildcardMethod)
    {
        if (wildcardMethod == null) return true;
        if (method         == null) return false;

        return Objects.equals(method, wildcardMethod);
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
        if (!super.equals(o))
        {
            return false;
        }
        MethodSignature that = (MethodSignature) o;
        return Objects.equals(method, that.method) && Objects.equals(descriptor, that.descriptor);
    }
}
