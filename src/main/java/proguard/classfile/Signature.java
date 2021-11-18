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
import static proguard.classfile.util.ClassUtil.externalPackageName;

import java.util.Objects;

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
    protected final String packageName;
    protected final String className;

    protected Signature(String className)
    {
        this.className = className;
        this.packageName = className == null ? "?" : externalPackageName(externalClassName(className));
    }

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

    public String getPackageName()
    {
        return packageName;
    }

    public String getClassName()
    {
        return className;
    }

    protected abstract String calculateFqn();

    protected abstract String calculatePrettyFqn();

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
        return Objects.equals(packageName, signature.packageName) && Objects.equals(className, signature.className);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(packageName, className);
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
