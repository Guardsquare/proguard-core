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

import java.util.Arrays;
import java.util.HashSet;
import proguard.analysis.cpa.defaults.LatticeAbstractState;

/**
 * The {@link TaintAbstractState} is a set of {@link TaintSource}s in the subset order.
 *
 * @author Dmitry Ivanov
 */
public class TaintAbstractState
    extends HashSet<TaintSource>
    implements LatticeAbstractState<TaintAbstractState>
{
    public static final TaintAbstractState bottom = new TaintAbstractState();

    /**
     * Create an abstract state containing selected taint sources.
     *
     * @param taintSources a sequence of taint sources
     */
    public TaintAbstractState(TaintSource ...taintSources)
    {
        addAll(Arrays.asList(taintSources));
    }

    // implementations for LatticeAbstractState

    @Override
    public TaintAbstractState join(TaintAbstractState abstractState)
    {
        TaintAbstractState result = abstractState.copy();
        result.addAll(this);
        return result.isLessOrEqual(this)
               ? this
               : result.isLessOrEqual(abstractState)
                 ? abstractState
                 : result;
    }

    @Override
    public boolean isLessOrEqual(TaintAbstractState abstractState)
    {
        return abstractState.containsAll(this);
    }

    // implementations for AbstractState

    @Override
    public TaintAbstractState copy()
    {
        return (TaintAbstractState) super.clone();
    }
}
