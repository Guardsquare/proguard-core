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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import proguard.classfile.util.ClassUtil;
import proguard.analysis.cpa.domain.taint.TaintSink;
import proguard.analysis.cpa.jvm.witness.JvmMemoryLocation;
import proguard.analysis.cpa.jvm.witness.JvmStackLocation;
import proguard.analysis.cpa.jvm.witness.JvmStaticFieldLocation;

/**
 * The {@link JvmTaintSink} adds an interface for extracting sensitive JVM memory locations.
 *
 * @author Dmitry Ivanov
 */
public class JvmTaintSink
    extends TaintSink
{

    /**
     * Create a taint sink.
     *
     * @param fqn           the fully qualified name of a sink method
     * @param takesInstance whether the sink is sensitive to the calling instance
     * @param takesArgs     a set of sensitive arguments
     * @param takesGlobals  a set of sensitive global variables
     */
    public JvmTaintSink(String fqn, boolean takesInstance, Set<Integer> takesArgs, Set<String> takesGlobals)
    {
        super(fqn, takesInstance, takesArgs, takesGlobals);
    }

    /**
     * Returns memory locations which trigger this taint sink.
     */
    public Set<JvmMemoryLocation> getMemoryLocations()
    {
        Set<JvmMemoryLocation> result = new HashSet<>();
        String descriptor = fqn.substring(fqn.indexOf("("));
        int parameterSize = ClassUtil.internalMethodParameterSize(descriptor);
        if (takesInstance)
        {
            result.add(new JvmStackLocation(parameterSize));
        }
        takesArgs.forEach(i -> result.add(new JvmStackLocation(parameterSize - ClassUtil.internalMethodVariableIndex(descriptor, true, i))));
        takesGlobals.forEach(fqn -> result.add(new JvmStaticFieldLocation(fqn)));
        return result;
    }

    public static Map<String, Set<JvmMemoryLocation>> convertSinksToMemoryLocations(Collection<? extends JvmTaintSink> taintSinks)
    {
        Map<String, Set<JvmMemoryLocation>> result = new HashMap<>();
        for (JvmTaintSink taintSink : taintSinks)
        {
            Set<JvmMemoryLocation> memoryLocations = taintSink.getMemoryLocations();
            result.merge(taintSink.fqn, memoryLocations, (fqn, ls) ->
            {
                ls.addAll(memoryLocations);
                return ls;
            });
        }
        return result;
    }
}
