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
import java.util.function.Predicate;
import proguard.classfile.Signature;

/**
 * A {@link TaintSink} specifies a sink for the taint analysis. A sink can be sensitive to
 * the instance, the arguments, or the static fields. If a sink S is sensitive to X, then
 * if X is tainted, we conclude that the taint has reached S.
 */
public abstract class TaintSink
{

    protected static final Predicate<TaintSource> IS_VALID_FOR_SOURCE_DEFAULT = x -> true;
    public final           Signature              signature;
    public final           Predicate<TaintSource> isValidForSource;

    /**
     * Create a taint sink.
     *
     * @param signature the signature of a sink method
     */
    public TaintSink(Signature signature)
    {
        this(signature, IS_VALID_FOR_SOURCE_DEFAULT);
    }

    /**
     * Create a taint sink.
     *
     * @param signature        the signature of a sink method
     * @param isValidForSource predicate on whether the sink is valid for a given source
     */
    public TaintSink(Signature signature, Predicate<TaintSource> isValidForSource)
    {
        this.signature = signature;
        this.isValidForSource = isValidForSource;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }
        if (!(other instanceof TaintSink))
        {
            return false;
        }
        TaintSink taintSink = (TaintSink) other;
        return Objects.equals(signature, taintSink.signature) && Objects.equals(isValidForSource, taintSink.isValidForSource);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(signature, isValidForSource);
    }

    @Override
    public abstract String toString();
}
