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

package proguard.analysis.cpa.jvm.domain.taint;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import proguard.analysis.cpa.domain.taint.TaintSource;
import proguard.analysis.datastructure.callgraph.Call;
import proguard.classfile.Signature;

/**
 * A {@link JvmTaintSource} specifies a method which can taint any (subset) of the following: the instance, the return value, the argument objects, or static fields.
 * The {@link #callMatcher} decides whether the call (already filtered by its {@link Signature}) should trigger this source.
 *
 * @author Dmitry Ivanov
 */
public class JvmTaintSource
    extends TaintSource
{

    public final Optional<Predicate<Call>> callMatcher;

    /**
     * Create a taint source.
     *
     * @param signature     the signature a source method
     * @param callMatcher   whether the call matches this taint source
     * @param taintsThis    whether the source taints the calling instance
     * @param taintsReturn  whether the source taints its return
     * @param taintsArgs    a set of tainted arguments
     * @param taintsGlobals a set of tainted global variables
     */
    public JvmTaintSource(Signature signature, Predicate<Call> callMatcher, boolean taintsThis, boolean taintsReturn, Set<Integer> taintsArgs, Set<String> taintsGlobals)
    {
        this(signature, Optional.of(callMatcher), taintsThis, taintsReturn, taintsArgs, taintsGlobals);
    }

    /**
     * Create a taint source.
     *
     * @param signature     the signature a source method
     * @param taintsThis    whether the source taints the calling instance
     * @param taintsReturn  whether the source taints its return
     * @param taintsArgs    a set of tainted arguments
     * @param taintsGlobals a set of tainted global variables
     */
    public JvmTaintSource(Signature signature, boolean taintsThis, boolean taintsReturn, Set<Integer> taintsArgs, Set<String> taintsGlobals)
    {
        this(signature, Optional.empty(), taintsThis, taintsReturn, taintsArgs, taintsGlobals);
    }

    /**
     * Create a taint source.
     *
     * @param signature     the signature a source method
     * @param callMatcher   an optional predicate on whether the call matches this taint source
     * @param taintsThis    whether the source taints the calling instance
     * @param taintsReturn  whether the source taints its return
     * @param taintsArgs    a set of tainted arguments
     * @param taintsGlobals a set of tainted global variables
     */
    public JvmTaintSource(Signature signature, Optional<Predicate<Call>> callMatcher, boolean taintsThis, boolean taintsReturn, Set<Integer> taintsArgs, Set<String> taintsGlobals)
    {
        super(signature, taintsThis, taintsReturn, taintsArgs, taintsGlobals);
        this.callMatcher = callMatcher;
    }

    // implementations for Object

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (!(obj instanceof JvmTaintSource))
        {
            return false;
        }
        JvmTaintSource other = (JvmTaintSource) obj;
        return Objects.equals(signature, other.signature)
               && Objects.equals(callMatcher, other.callMatcher)
               && taintsThis == other.taintsThis
               && taintsReturn == other.taintsReturn
               && Objects.equals(taintsArgs, other.taintsArgs)
               && Objects.equals(taintsGlobals, other.taintsGlobals);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(signature, callMatcher, taintsThis, taintsReturn, taintsArgs, taintsGlobals);
    }

    @Override
    public String toString()
    {
        return callMatcher.map(p -> new StringBuilder(super.toString()).append(" filtered by ").append(p).toString())
                          .orElse(super.toString());
    }
}
