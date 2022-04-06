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

/**
 * A {@link TaintSink} specifies a sink for the taint analysis. A sink can be sensitive to
 * the instance, the arguments, or the static fields. If a sink S is sensitive to X, then
 * if X is tainted, we conclude that the taint has reached S.
 *
 * @author Dmitry Ivanov
 */
public class TaintSink
{
    public final String       fqn;
    public final boolean      takesInstance;
    public final Set<Integer> takesArgs;
    public final Set<String>  takesGlobals;

    /**
     * Create a taint sink.
     *
     * @param fqn           the fully qualified name of a sink method
     * @param takesInstance whether the sink is sensitive to the calling instance
     * @param takesArgs     a set of sensitive arguments
     * @param takesGlobals  a set of sensitive global variables
     */
    public TaintSink(String fqn, boolean takesInstance, Set<Integer> takesArgs, Set<String> takesGlobals)
    {
        if (!takesInstance && takesArgs.isEmpty() && takesGlobals.isEmpty())
        {
            throw new RuntimeException(String.format("Tainted sink for method %s must have taint somewhere!", fqn));
        }
        this.fqn = fqn;
        this.takesInstance = takesInstance;
        this.takesArgs = takesArgs;
        this.takesGlobals = takesGlobals;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof TaintSink))
        {
            return false;
        }
        TaintSink taintSink = (TaintSink) o;
        return takesInstance == taintSink.takesInstance
               && Objects.equals(fqn, taintSink.fqn)
               && Objects.equals(takesArgs, taintSink.takesArgs)
               && Objects.equals(takesGlobals, taintSink.takesGlobals);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(fqn, takesInstance, takesArgs, takesGlobals);
    }

    @Override
    public String toString()
    {
        StringBuilder result = new StringBuilder("[TaintSink] ").append(fqn);
        if (takesInstance)
        {
            result.append(", takes instance");
        }
        if (!takesArgs.isEmpty())
        {
            result.append(", takes args (")
                  .append(takesArgs.stream()
                                   .map(Objects::toString)
                                   .sorted()
                                   .collect(Collectors.joining(", ")))
                  .append(")");
        }
        if (!takesGlobals.isEmpty())
        {
            result.append(", takes globals (")
                  .append(takesGlobals.stream()
                                      .sorted()
                                      .collect(Collectors.joining(", ")))
                  .append(")");
        }

        return result.toString();
    }
}
