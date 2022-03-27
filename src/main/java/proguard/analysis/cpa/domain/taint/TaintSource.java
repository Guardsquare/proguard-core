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

/**
 * A {@link TaintSource} specifies a method which can taint any (subset) of the following: the instance, the return value, the argument objects, or static fields.
 *
 * @author Dmitry Ivanov
 */
public class TaintSource
{

    public final String       fqn;
    public final boolean      taintsThis;
    public final boolean      taintsReturn;
    public final Set<Integer> taintsArgs;
    public final Set<String>  taintsGlobals;

    /**
     * Create a taint source.
     *
     * @param fqn           the fully qualified name of a source method
     * @param taintsThis    whether the source taints the calling instance
     * @param taintsReturn  whether the source taints its return
     * @param taintsArgs    a set of tainted arguments
     * @param taintsGlobals a set of tainted global variables
     */
    public TaintSource(String fqn, boolean taintsThis, boolean taintsReturn, Set<Integer> taintsArgs, Set<String> taintsGlobals)
    {
        this.fqn = fqn;
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
        return fqn.equals(other.fqn)
               && taintsThis == other.taintsThis
               && taintsReturn == other.taintsReturn
               && taintsArgs.equals(other.taintsArgs)
               && taintsGlobals.equals(other.taintsGlobals);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(fqn, taintsThis, taintsReturn, taintsArgs, taintsGlobals);
    }
}
