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

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Represents the signature of a class without any member information.
 *
 * @author Samuel Hopstock
 */
public class ClassSignature
    extends Signature
{

    private static final transient Map<Clazz, ClassSignature> signatureCache = new IdentityHashMap<>();

    public ClassSignature(String className)
    {
        super(className);
    }

    public ClassSignature(Clazz clazz)
    {
        this(clazz.getName());
    }

    @Override
    protected String calculateFqn()
    {
        return className;
    }

    @Override
    protected String calculatePrettyFqn()
    {
        return externalClassName(className);
    }

    /**
     * Remove all currently cached {@link ClassSignature} objects from the cache,
     * allowing them to be removed by the garbage collector.
     */
    public static void clearCache()
    {
        signatureCache.clear();
    }

    /**
     * Get the singleton {@link ClassSignature} object for this specific {@link Clazz}.
     * If it is not yet available in the cache, it will be newly instantiated.
     *
     * @param clazz The class containing the target field
     * @return The cached or newly generated {@link ClassSignature} object
     */
    public static ClassSignature computeIfAbsent(Clazz clazz)
    {
        return signatureCache.computeIfAbsent(clazz, c -> new ClassSignature(c.getName()));
    }
}
