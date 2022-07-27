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

package proguard.analysis.cpa.bam;

import proguard.analysis.cpa.interfaces.AbstractState;
import proguard.analysis.cpa.interfaces.ReachedSet;
import proguard.analysis.cpa.interfaces.Waitlist;

/**
 * A block abstraction is a summary of the analysis of a procedure call, represented by the set of reached abstract states and a waitlist of states that still need to be analyzed. The BAM CPA can save
 * these abstractions in a cache and retrieve them when the same procedure is called with the same entry {@link AbstractState}.
 *
 * @author Carlo Alberto Pozzoli
 */
public class BlockAbstraction
{

    private final ReachedSet reachedSet;
    private final Waitlist   waitlist;

    /**
     * Create a new block abstraction.
     *
     * @param reachedSet a collection of discovered states
     * @param waitlist   a collection of states of the block that need to be analyzed
     */
    public BlockAbstraction(ReachedSet reachedSet, Waitlist waitlist)
    {
        this.reachedSet = reachedSet;
        this.waitlist = waitlist;
    }

    /**
     * Returns the {@link ReachedSet} of the block abstraction.
     */
    public ReachedSet getReachedSet()
    {
        return reachedSet;
    }

    /**
     * Returns the {@link Waitlist} of the block abstraction.
     */
    public Waitlist getWaitlist()
    {
        return waitlist;
    }
}
