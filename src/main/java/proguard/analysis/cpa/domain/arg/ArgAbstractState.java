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

package proguard.analysis.cpa.domain.arg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import proguard.analysis.cpa.defaults.AbstractSingleWrapperState;
import proguard.analysis.cpa.interfaces.AbstractState;

/**
 * This {@link AbstractSingleWrapperState} represents a node of
 * the abstract reachability graph (ARG). It stores the {@link ArgAbstractState}
 * parents and delegates all the {@link AbstractState} interfaces
 * to the wrapped {@link AbstractState}.
 *
 * @author Dmitry Ivanov
 */
public class ArgAbstractState
    extends AbstractSingleWrapperState
{

    protected final List<ArgAbstractState> parents  = new ArrayList<>(0);

    /**
     * Create an ARG node wrapping the given {@code wrappedAbstractState}
     * and pointing at the {@code parents}.
     *
     * @param wrappedAbstractState an abstract state to be wrapped
     * @param parents              its ARG parents
     */
    public ArgAbstractState(AbstractState wrappedAbstractState, List<? extends ArgAbstractState> parents)
    {
        super(wrappedAbstractState);
        parents.forEach(this::addParent);
    }

    /**
     * Create a parentless {@link ArgAbstractState} wrapping the given {@code wrappedAbstractState}.
     *
     * @param wrappedAbstractState an abstract state to be wrapped
     */
    public ArgAbstractState(AbstractState wrappedAbstractState)
    {
        this(wrappedAbstractState, Collections.emptyList());
    }

    // implementations for AbstractState

    @Override
    public ArgAbstractState copy()
    {
        return new ArgAbstractState(getWrappedState(), new ArrayList<>(parents));
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        return obj instanceof ArgAbstractState && super.equals(obj);
    }

    /**
     * Returns a list of parent ARG nodes.
     */
    public List<? extends ArgAbstractState> getParents()
    {
        return parents;
    }

    /**
     * Adds a parent ARG node.
     */
    public void addParent(ArgAbstractState abstractState)
    {
        if (parents.stream().noneMatch(s -> s == abstractState))
        {
            parents.add(abstractState);
        }
    }
}
