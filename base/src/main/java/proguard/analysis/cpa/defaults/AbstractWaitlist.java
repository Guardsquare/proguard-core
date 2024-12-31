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
import java.util.LinkedHashSet;
import proguard.analysis.cpa.interfaces.AbstractState;
import proguard.analysis.cpa.interfaces.Waitlist;

/**
 * This is a base class for {@link Waitlist}s parametrized by the carrier {@code CollectionT}. It
 * delegates all the {@link Waitlist} interfaces to its carrier collection.
 *
 * @param <StateT> The states contained in the waitlist.
 * @param <CollectionT> The collection backing the waitlist.
 */
public abstract class AbstractWaitlist<StateT extends AbstractState> implements Waitlist<StateT> {
  protected final Collection<StateT> waitlist = new LinkedHashSet<>();

  // implementations for Waitlist

  @Override
  public void add(StateT abstractState) {
    waitlist.add(abstractState);
  }

  @Override
  public void addAll(Collection<? extends StateT> abstractStates) {
    waitlist.addAll(abstractStates);
  }

  @Override
  public void clear() {
    waitlist.clear();
  }

  @Override
  public boolean contains(StateT abstractState) {
    return waitlist.contains(abstractState);
  }

  @Override
  public boolean isEmpty() {
    return waitlist.isEmpty();
  }

  @Override
  public boolean remove(StateT abstractState) {
    return waitlist.remove(abstractState);
  }

  @Override
  public void removeAll(Collection<? extends StateT> abstractStates) {
    waitlist.removeAll(abstractStates);
  }

  @Override
  public int size() {
    return waitlist.size();
  }
}
