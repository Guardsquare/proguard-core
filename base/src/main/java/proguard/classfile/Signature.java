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

import java.util.Objects;

import static proguard.classfile.util.ClassUtil.externalClassName;
import static proguard.classfile.util.ClassUtil.externalPackageName;

/**
 * A signature currently can be a Method- or a FieldSignature.
 * This class can be used to create the correct one from a ProguardCORE member object
 * (which can also be a method or a field).
 *
 * @author Dennis Titze, Samuel Hopstock
 */
public abstract class Signature
    implements Comparable<Signature>
{

    private static boolean cacheEnabled = false;

    // The fqn fields are filled lazily
    protected       String fqn;
    protected       String prettyFqn;
    protected final String className;

    // Cached hashCode, as Signatures are often used in sets/maps
    // and therefore hashCode() is called often.
    protected final int hashCode;

    /**
     * The {@link Clazz} that the {@link Signature#className}
     * references. May be <code>null</code> if there is no
     * reference available (e.g. class is missing from the class pool).
     */
    protected Clazz referencedClass;

    protected Signature(String internalClassName, int hashCode)
    {
        this.className = internalClassName;
        this.hashCode  = hashCode;
    }


    /**
     * Check if this signature is missing information.
     */
    public abstract boolean isIncomplete();

    /**
     * Convenience factory that takes any {@link Member} and generates the
     * appropriate signature, i.e. {@link MethodSignature}s for {@link Method}s
     * and {@link FieldSignature}s for {@link Field}s. If the member is null,
     * a {@link ClassSignature} is generated.
     *
     * <p>
     * Note that if {@link #cacheEnabled} is set to true,
     * the generation process is delegated to {@link #computeIfAbsent(Clazz, Member)}
     * to make use of caching features.
     * </p>
     *
     * @param clazz  The class containing the member
     * @param member The target member
     * @return The corresponding {@link Signature} object
     */
    public static Signature of(Clazz clazz, Member member)
    {
        if (cacheEnabled)
        {
            return computeIfAbsent(clazz, member);
        }

        if (member == null)
        {
            return new ClassSignature(clazz);
        }

        if (member instanceof Field)
        {
            return new FieldSignature(clazz, (Field) member);
        }

        return new MethodSignature(clazz, (Method) member);
    }

    /**
     * This factory uses the caching features provided
     * by {@link MethodSignature#computeIfAbsent(Clazz, Method)},
     * {@link FieldSignature#computeIfAbsent(Clazz, Field)}
     * and {@link ClassSignature#computeIfAbsent(Clazz)}.
     * Only one signature is created for each distinct {@link Method},
     * {@link Field} or {@link Clazz}. If the same signature is requested
     * several times, the previously created object will be returned.
     *
     * @param clazz  The class containing the member
     * @param member The target member
     * @return The corresponding {@link Signature} object
     */
    public static Signature computeIfAbsent(Clazz clazz, Member member)
    {
        if (member == null)
        {
            return ClassSignature.computeIfAbsent(clazz);
        }

        if (member instanceof Field)
        {
            return FieldSignature.computeIfAbsent(clazz, (Field) member);
        }

        return MethodSignature.computeIfAbsent(clazz, (Method) member);
    }

    /**
     * Clear the signature caches of all {@link Signature} subclasses.
     */
    public static void clearCache()
    {
        ClassSignature.clearCache();
        MethodSignature.clearCache();
        FieldSignature.clearCache();
    }

    /**
     * Enable or disable automatic caching in {@link #of(Clazz, Member)}.
     * Existing caches are purged once caching has been disabled.
     */
    public static void setCacheEnabled(boolean cacheEnabled)
    {
        Signature.cacheEnabled = cacheEnabled;
        if (!cacheEnabled)
        {
            clearCache();
        }
    }

    public String getFqn()
    {
        if (fqn == null)
        {
            fqn = calculateFqn();
        }
        return fqn;
    }

    public String getPrettyFqn()
    {
        if (prettyFqn == null)
        {
            prettyFqn = calculatePrettyFqn();
        }

        return prettyFqn;
    }

    // Method will be removed as the name does not indicate clearly 
    // that the external name will be returned.
    @Deprecated
    public String getPackageName()
    {
        return getExternalPackageName();
    }


    /**
     * @return the external package name (e.g., `java.lang` for `java.lang.Object`) 
     */
    public String getExternalPackageName()
    {
        return getClassName() == null ? "?" : externalPackageName(externalClassName(getClassName()));
    }

    public String getClassName()
    {
        return className;
    }

    protected abstract String calculateFqn();

    protected abstract String calculatePrettyFqn();

    /**
     * Returns the {@link Clazz} reference corresponding
     * to the class returned by {@link Signature#getClassName()}
     * or <code>null</code> if no reference is available (e.g. class is missing from the class pool).
     */
    public Clazz getReferencedClass()
    {
        return referencedClass;
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
        Signature signature = (Signature) o;
        return Objects.equals(className, signature.className);
    }

    @Override
    public int hashCode()
    {
        return this.hashCode;
    }

    @Override
    public int compareTo(Signature o)
    {
        if (o == null || getClass() != o.getClass())
        {
            return -1;
        }
        return getFqn().compareTo(o.getFqn());
    }

    @Override
    public String toString()
    {
        return getFqn();
    }
}
