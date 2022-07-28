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

import java.util.LinkedHashSet;
import proguard.analysis.cpa.interfaces.AbstractState;
import proguard.analysis.cpa.interfaces.Waitlist;

/**
 * This {@link Waitlist} pops the last element,
 * i.e., performs a depth first traversal over the {@link Cfa}.
 *
 * @author Dmitry Ivanov
 */
public class DepthFirstWaitlist extends AbstractWaitlist<LinkedHashSet<AbstractState>>
{

    public DepthFirstWaitlist()
    {
        super(new LinkedHashSet<>());
    }

    // implementations for AbstractWaitlist

    @Override
    public AbstractState pop()
    {
        AbstractState result = waitlist.stream().skip(waitlist.size() - 1).findFirst().get();
        waitlist.remove(result);
        return result;
    }
}
