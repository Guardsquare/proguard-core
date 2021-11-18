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
import static proguard.classfile.util.ClassUtil.externalShortClassName;
import static proguard.classfile.util.ClassUtil.externalType;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a field signature consisting of class and member name.
 *
 * @author Dennis Titze, Samuel Hopstock
 */
public class FieldSignature
    extends Signature
{

    private static final transient Map<Field, FieldSignature> signatureCache = new IdentityHashMap<>();
    public final                   String                     memberName;
    public final                   String                     descriptor;

    public FieldSignature(String clazzName, String memberName, String descriptor)
    {
        super(clazzName);
        this.memberName = memberName;
        this.descriptor = descriptor;
    }

    public FieldSignature(Clazz clazz, Field field)
    {
        this(clazz.getName(), field.getName(clazz), field.getDescriptor(clazz));
    }

    @Override
    protected String calculateFqn()
    {
        return String.format("L%s;%s", className, memberName);
    }

    @Override
    protected String calculatePrettyFqn()
    {
        String type = descriptor == null ? null : externalShortClassName(externalType(descriptor));
        String shortClazzName = className == null ? "?" : externalShortClassName(externalClassName(className));
        return String.format("%s %s.%s", type, shortClazzName, memberName);
    }

    /**
     * Get the singleton {@link FieldSignature} object for this specific {@link Field}.
     * If it is not yet available in the cache, it will be newly instantiated.
     *
     * @param clazz The class containing the target field
     * @param field The field whose signature is to be generated
     * @return The cached or newly generated {@link FieldSignature} object
     */
    public static FieldSignature computeIfAbsent(Clazz clazz, Field field)
    {
        return signatureCache.computeIfAbsent(field, f -> new FieldSignature(clazz, field));
    }

    /**
     * Remove all currently cached {@link FieldSignature} objects from the cache,
     * allowing them to be removed by the garbage collector.
     */
    public static void clearCache()
    {
        signatureCache.clear();
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
        FieldSignature that = (FieldSignature) o;
        return Objects.equals(memberName, that.memberName) && Objects.equals(descriptor, that.descriptor);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(super.hashCode(), memberName, descriptor);
    }
}
