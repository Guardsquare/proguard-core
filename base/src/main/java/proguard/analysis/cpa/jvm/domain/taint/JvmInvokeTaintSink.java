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

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import proguard.analysis.cpa.interfaces.CallEdge;
import proguard.analysis.cpa.jvm.cfa.edges.JvmCallCfaEdge;
import proguard.analysis.cpa.jvm.cfa.edges.JvmCfaEdge;
import proguard.analysis.cpa.jvm.witness.JvmMemoryLocation;
import proguard.analysis.cpa.jvm.witness.JvmStackLocation;
import proguard.analysis.cpa.jvm.witness.JvmStaticFieldLocation;
import proguard.analysis.datastructure.callgraph.Call;
import proguard.classfile.Signature;
import proguard.classfile.util.ClassUtil;

/**
 * A {@link JvmTaintSink} on a method invocation.
 * This sinks can be sensitive to the instance, the arguments, or the static fields. If a sink S is sensitive to X, then
 * if X is tainted, we conclude that the taint has reached S.
 *
 * @author Dmitry Ivanov
 */
public class JvmInvokeTaintSink
    extends JvmTaintSink
{

    public final Optional<Predicate<Call>> callMatcher;

    public final boolean      takesInstance;
    public final Set<Integer> takesArgs;
    public final Set<String>  takesGlobals;

    /**
     * Create a taint sink.
     *
     * @param signature     the signature of a sink method
     * @param takesInstance whether the sink is sensitive to the calling instance
     * @param takesArgs     a set of sensitive arguments
     * @param takesGlobals  a set of sensitive global variables
     */
    public JvmInvokeTaintSink(Signature signature, boolean takesInstance, Set<Integer> takesArgs, Set<String> takesGlobals)
    {
        this(signature, Optional.empty(), takesInstance, takesArgs, takesGlobals);
    }

    /**
     * Create a taint sink.
     *
     * @param signature     the signature of a sink method
     * @param callMatcher   whether the call matches this taint sink
     * @param takesInstance whether the sink is sensitive to the calling instance
     * @param takesArgs     a set of sensitive arguments
     * @param takesGlobals  a set of sensitive global variables
     */
    public JvmInvokeTaintSink(Signature signature, Predicate<Call> callMatcher, boolean takesInstance, Set<Integer> takesArgs, Set<String> takesGlobals)
    {
        this(signature, Optional.of(callMatcher), takesInstance, takesArgs, takesGlobals);
    }

    /**
     * Create a taint sink.
     *
     * @param signature     the signature of a sink method
     * @param callMatcher   an optional predicate on whether the call matches this taint sink
     * @param takesInstance whether the sink is sensitive to the calling instance
     * @param takesArgs     a set of sensitive arguments
     * @param takesGlobals  a set of sensitive global variables
     */
    private JvmInvokeTaintSink(Signature signature, Optional<Predicate<Call>> callMatcher, boolean takesInstance, Set<Integer> takesArgs, Set<String> takesGlobals)
    {
        super(signature);

        if (!takesInstance && takesArgs.isEmpty() && takesGlobals.isEmpty())
        {
            throw new RuntimeException(String.format("Tainted sink for method %s must have taint somewhere!", signature));
        }

        this.callMatcher = callMatcher;
        this.takesInstance = takesInstance;
        this.takesArgs = takesArgs;
        this.takesGlobals = takesGlobals;
    }

    /**
     * Returns memory locations which trigger this taint sink.
     */
    @Override
    public Set<JvmMemoryLocation> getMemoryLocations()
    {
        Set<JvmMemoryLocation> result = new HashSet<>();
        String fqn = signature.getFqn();
        String descriptor = fqn.substring(fqn.indexOf("("));
        int parameterSize = ClassUtil.internalMethodParameterSize(descriptor);
        if (takesInstance)
        {
            result.add(new JvmStackLocation(parameterSize));
        }
        takesArgs.forEach(i -> result.add(new JvmStackLocation(parameterSize - ClassUtil.internalMethodVariableIndex(descriptor, true, i))));
        takesGlobals.forEach(n -> result.add(new JvmStaticFieldLocation(n)));
        return result;
    }

    /**
     * Returns true if the edge is a call to the sink method.
     */
    @Override
    public boolean matchCfaEdge(JvmCfaEdge edge)
    {
        if (!(edge instanceof JvmCallCfaEdge))
        {
            return false;
        }
        CallEdge callEdge = (CallEdge) edge;
        return signature.equals(callEdge.getCall().getTarget()) && callMatcher.map(p -> p.test(callEdge.getCall())).orElse(true);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof JvmInvokeTaintSink))
        {
            return false;
        }
        JvmInvokeTaintSink taintSink = (JvmInvokeTaintSink) o;
        return takesInstance == taintSink.takesInstance
               && Objects.equals(signature, taintSink.signature)
               && Objects.equals(takesArgs, taintSink.takesArgs)
               && Objects.equals(takesGlobals, taintSink.takesGlobals)
               && Objects.equals(callMatcher, taintSink.callMatcher);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(signature, takesInstance, takesArgs, takesGlobals, callMatcher);
    }

    @Override
    public String toString()
    {
        StringBuilder result = new StringBuilder("[JvmInvokeTaintSink] ").append(signature);
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
        callMatcher.ifPresent(p -> result.append(" filtered by ").append(p));

        return result.toString();
    }
}
