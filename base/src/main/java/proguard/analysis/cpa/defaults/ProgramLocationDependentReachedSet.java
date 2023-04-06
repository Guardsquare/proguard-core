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

package proguard.analysis.cpa.defaults;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import proguard.classfile.Signature;
import proguard.analysis.cpa.interfaces.AbstractState;
import proguard.analysis.cpa.interfaces.CfaEdge;
import proguard.analysis.cpa.interfaces.CfaNode;
import proguard.analysis.cpa.interfaces.ProgramLocationDependent;
import proguard.analysis.cpa.interfaces.ReachedSet;

/**
 * This {@link ReachedSet} stores {@link ProgramLocationDependent} {@link AbstractState}s.
 * It assumes the analysis does merge the {@link AbstractState}s belonging to different {@link CfaNode}s and stores them in separate bins.
 *
 * @author Dmitry Ivanov
 */
public final class ProgramLocationDependentReachedSet<CfaNodeT extends CfaNode<CfaEdgeT, SignatureT>,
                                                      CfaEdgeT extends CfaEdge<CfaNodeT>,
                                                      AbstractStateT extends AbstractState & ProgramLocationDependent<CfaNodeT, CfaEdgeT, SignatureT>,
                                                      SignatureT extends Signature>
    implements ReachedSet
{

    private Map<CfaNodeT, Set<AbstractStateT>> locationToStates = new HashMap<>();

    // implementations for ReachedSet

    @Override
    public boolean add(AbstractState abstractState)
    {
        AbstractStateT state = (AbstractStateT) abstractState;
        return locationToStates.computeIfAbsent(state.getProgramLocation(), x -> new HashSet<>()).add(state);
    }

    @Override
    public boolean addAll(Collection<? extends AbstractState> abstractStates)
    {
        boolean result = false;

        for (AbstractState state: abstractStates)
        {
            result |= add(state);
        }

        return result;
    }

    @Override
    public boolean remove(AbstractState abstractState)
    {
        AbstractStateT state = (AbstractStateT) abstractState;
        CfaNodeT location = state.getProgramLocation();
        return locationToStates.containsKey(location) && locationToStates.get(location).remove(state);
    }

    @Override
    public boolean removeAll(Collection<?> abstractStates)
    {
        boolean result = false;

        for (Object state: abstractStates)
        {
            result |= remove((AbstractState) state);
        }

        return result;
    }

    @Override
    public Collection<AbstractStateT> asCollection()
    {
        int initialSize = locationToStates.values().size();
        return locationToStates.values().stream().reduce(new HashSet<>(initialSize), (x, y) ->
        {
            x.addAll(y);
            return x;
        });
    }

    @Override
    public Collection<? extends AbstractState> getReached(AbstractState abstractState)
    {
        return getReached(((AbstractStateT) abstractState).getProgramLocation());
    }

    /**
     * Returns a collection of abstract states belonging to the given {@code location}.
     */
    public Collection<? extends AbstractState> getReached(CfaNodeT location)
    {
        return locationToStates.getOrDefault(location, Collections.emptySet());
    }
}
