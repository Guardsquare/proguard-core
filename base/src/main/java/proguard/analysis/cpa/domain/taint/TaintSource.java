/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2022 Guardsquare NV
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

package proguard.analysis.cpa.domain.taint;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import proguard.classfile.Signature;

/**
 * A {@link TaintSource} specifies a method which can taint any (subset) of the following: the instance, the return value, the argument objects, or static fields.
 *
 * @author Dmitry Ivanov
 */
public class TaintSource
{

    public final Signature    signature;
    public final boolean      taintsThis;
    public final boolean      taintsReturn;
    public final Set<Integer> taintsArgs;
    public final Set<String>  taintsGlobals;

    /**
     * Create a taint source.
     *
     * @param signature     the signature a source method
     * @param taintsThis    whether the source taints the calling instance
     * @param taintsReturn  whether the source taints its return
     * @param taintsArgs    a set of tainted arguments
     * @param taintsGlobals a set of tainted global variables
     */
    public TaintSource(Signature signature, boolean taintsThis, boolean taintsReturn, Set<Integer> taintsArgs, Set<String> taintsGlobals)
    {
        this.signature = signature;
        this.taintsThis = taintsThis;
        this.taintsReturn = taintsReturn;
        this.taintsArgs = taintsArgs;
        this.taintsGlobals = taintsGlobals;
    }

    // implementations for Object

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (!(obj instanceof TaintSource))
        {
            return false;
        }
        TaintSource other = (TaintSource) obj;
        return Objects.equals(signature, other.signature)
               && taintsThis == other.taintsThis
               && taintsReturn == other.taintsReturn
               && Objects.equals(taintsArgs, other.taintsArgs)
               && Objects.equals(taintsGlobals, other.taintsGlobals);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(signature, taintsThis, taintsReturn, taintsArgs, taintsGlobals);
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder("[TaintSource] ").append(signature);
        if (taintsThis)
        {
            builder.append(", taints this");
        }
        if (taintsReturn)
        {
            builder.append(", taints return");
        }
        if (!taintsArgs.isEmpty())
        {
            builder.append(", taints args (")
                   .append(taintsArgs.stream()
                                     .map(Object::toString)
                                     .sorted()
                                     .collect(Collectors.joining(", ")))
                   .append(")");
        }
        if (!taintsGlobals.isEmpty())
        {
            builder.append(", taints globals (")
                   .append(taintsGlobals.stream()
                                        .sorted()
                                        .collect(Collectors.joining(", ")))
                   .append(")");
        }

        return builder.toString();
    }
}
