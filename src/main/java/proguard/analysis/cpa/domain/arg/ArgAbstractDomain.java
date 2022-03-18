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

import java.util.Arrays;
import proguard.analysis.cpa.interfaces.AbstractDomain;
import proguard.analysis.cpa.interfaces.AbstractState;

/**
 * This {@link AbstractDomain} delegates its semi-lattice
 * interfaces to the inner abstract state and creates ARG nodes
 * upon discovering new abstract states.
 *
 * @author Dmitry Ivanov
 */
public class ArgAbstractDomain
    implements AbstractDomain
{

    protected final AbstractDomain          wrappedAbstractDomain;
    protected final ArgAbstractStateFactory argAbstractStateFactory;

    /**
     * Create a wrapper abstract domain for a specific abstract state factory.
     *
     * @param wrappedAbstractDomain   a wrapped abstract domain
     * @param argAbstractStateFactory an abstract state factory
     */
    public ArgAbstractDomain(AbstractDomain wrappedAbstractDomain, ArgAbstractStateFactory argAbstractStateFactory)
    {
        this.wrappedAbstractDomain = wrappedAbstractDomain;
        this.argAbstractStateFactory = argAbstractStateFactory;
    }

    // implementations for AbstractDomain

    @Override
    public AbstractState join(AbstractState abstractState1, AbstractState abstractState2)
    {
        if (!(abstractState1 instanceof ArgAbstractState && abstractState2 instanceof ArgAbstractState))
        {
            throw new IllegalArgumentException(getClass().getName() + " does not support " + abstractState1.getClass().getName() + " and " + abstractState2.getClass().getName());
        }
        ArgAbstractState argAbstractState1 = (ArgAbstractState) abstractState1;
        ArgAbstractState argAbstractState2 = (ArgAbstractState) abstractState2;
        AbstractState joinResult = wrappedAbstractDomain.join(argAbstractState1.getWrappedState(),
                                                              argAbstractState1.getWrappedState());
        return joinResult == argAbstractState1.getWrappedState()
               ? argAbstractState1
               : joinResult == argAbstractState2.getWrappedState()
                 ? argAbstractState2
                 : argAbstractStateFactory.createArgAbstractState(joinResult,
                                                                  Arrays.asList(argAbstractState1,
                                                                                argAbstractState2));
    }

    @Override
    public boolean isLessOrEqual(AbstractState abstractState1, AbstractState abstractState2)
    {
        if (!(abstractState1 instanceof ArgAbstractState && abstractState2 instanceof ArgAbstractState))
        {
            throw new IllegalArgumentException(getClass().getName() + " does not support " + abstractState1.getClass().getName() + " and " + abstractState2.getClass().getName());
        }
        return wrappedAbstractDomain.isLessOrEqual(((ArgAbstractState) abstractState1).getWrappedState(),
                                                   ((ArgAbstractState) abstractState2).getWrappedState());
    }
}
