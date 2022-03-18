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
import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;
import proguard.analysis.cpa.interfaces.AbstractState;
import proguard.analysis.cpa.interfaces.Waitlist;

/**
 * This is a base class for {@link Waitlist}s parametrized by the carrier {@code CollectionT}. It delegates all the {@link Waitlist} interfaces to its carrier collection.
 *
 * @author Dmitry Ivanov
 */
public abstract class AbstractWaitlist<CollectionT extends Collection<AbstractState>> implements Waitlist
{
    protected final CollectionT waitlist;

    /**
     * Create a waitlist from a carrier collection.
     *
     * @param waitList the carrier collection
     */
    protected AbstractWaitlist(CollectionT waitList)
    {
        this.waitlist = waitList;
    }

    // implementations for Waitlist

    @Override
    public void add(AbstractState abstractState)
    {
        waitlist.add(abstractState);
    }

    @Override
    public void addAll(Collection<? extends AbstractState> abstractStates)
    {
        waitlist.addAll(abstractStates);
    }

    @Override
    public void clear()
    {
        waitlist.clear();
    }

    @Override
    public boolean contains(AbstractState abstractState)
    {
        return waitlist.contains(abstractState);
    }

    @Override
    public boolean isEmpty()
    {
        return waitlist.isEmpty();
    }

    @Override
    public boolean remove(AbstractState abstractState)
    {
        return waitlist.remove(abstractState);
    }

    @Override
    public void removeAll(Collection<?> abstractStates)
    {
        waitlist.removeAll(abstractStates);
    }

    @Override
    public int size()
    {
        return waitlist.size();
    }

    // implementations for Iterable

    @NotNull
    @Override
    public Iterator<AbstractState> iterator()
    {
        return waitlist.iterator();
    }

    @Override
    public void forEach(Consumer<? super AbstractState> action)
    {
        waitlist.forEach(action);
    }

    @Override
    public Spliterator<AbstractState> spliterator()
    {
        return waitlist.spliterator();
    }
}
