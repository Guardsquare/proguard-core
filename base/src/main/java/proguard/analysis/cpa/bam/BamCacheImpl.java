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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import proguard.analysis.cpa.defaults.SetAbstractState;
import proguard.analysis.cpa.interfaces.AbstractState;
import proguard.analysis.cpa.interfaces.Precision;
import proguard.analysis.cpa.jvm.state.JvmAbstractState;
import proguard.classfile.MethodSignature;

/**
 * A simple implementation of {@link BamCache} where the cache is implemented as a {@link HashMap}.
 *
 * @param <ContentT> The content of the jvm states. For example, this can be a {@link
 *     SetAbstractState} of taints for taint analysis or a {@link
 *     proguard.analysis.cpa.jvm.domain.value.ValueAbstractState} for value analysis.
 */
public class BamCacheImpl<ContentT extends AbstractState<ContentT>> implements BamCache<ContentT> {

  private static final Logger log = LogManager.getLogger(BamCacheImpl.class);

  private final Map<MethodSignature, Map<HashKey<ContentT>, BlockAbstraction<ContentT>>> cache =
      new HashMap<>();
  private int size = 0;

  // Implementations for BamCache

  @Override
  public void put(
      JvmAbstractState<ContentT> stateKey,
      Precision precisionKey,
      MethodSignature blockKey,
      BlockAbstraction<ContentT> blockAbstraction) {
    if (cache
            .computeIfAbsent(blockKey, k -> new HashMap<>())
            .put(getHashKey(stateKey, precisionKey), blockAbstraction)
        == null) {
      size++;
      log.trace("BamCacheSize: {}", size);
    }
  }

  @Override
  public BlockAbstraction<ContentT> get(
      JvmAbstractState<ContentT> stateKey, Precision precisionKey, MethodSignature blockKey) {
    return cache
        .getOrDefault(blockKey, Collections.emptyMap())
        .get(getHashKey(stateKey, precisionKey));
  }

  @Override
  public Collection<BlockAbstraction<ContentT>> get(MethodSignature blockKey) {
    return cache.getOrDefault(blockKey, Collections.emptyMap()).values();
  }

  @Override
  public Collection<BlockAbstraction<ContentT>> get(Precision precision, MethodSignature blockKey) {
    return cache.getOrDefault(blockKey, Collections.emptyMap()).entrySet().stream()
        .filter(e -> e.getKey().precisionKey.equals(precision))
        .map(Entry::getValue)
        .collect(Collectors.toSet());
  }

  @Override
  public Collection<BlockAbstraction<ContentT>> values() {
    return cache.values().stream()
        .map(Map::values)
        .flatMap(Collection::stream)
        .collect(Collectors.toSet());
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public Set<MethodSignature> getAllMethods() {
    return Collections.unmodifiableSet(cache.keySet());
  }

  private HashKey<ContentT> getHashKey(
      JvmAbstractState<ContentT> stateKey, Precision precisionKey) {
    return new HashKey<>(stateKey, precisionKey);
  }

  /**
   * The key of the cache is created from the three parameters that define a block abstraction. The
   * equals and hashCode methods are overridden to guarantee the correct behavior of the hash map.
   */
  private static class HashKey<ContentT extends AbstractState<ContentT>> {

    private final JvmAbstractState<ContentT> stateKey;
    private final Precision precisionKey;

    /**
     * Create a cache key from its components.
     *
     * @param stateKey the entry abstract state of a method
     * @param precisionKey a precision
     */
    public HashKey(JvmAbstractState<ContentT> stateKey, Precision precisionKey) {
      this.stateKey = stateKey;
      this.precisionKey = precisionKey;
    }

    /** Returns the entry state of the block that composes the key. */
    public JvmAbstractState<ContentT> getStateKey() {
      return stateKey;
    }

    /** Returns the precision that composes the key. */
    public Precision getPrecisionKey() {
      return precisionKey;
    }

    // Implementations for Object

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (!(o instanceof HashKey)) {
        return false;
      }
      HashKey<?> other = (HashKey<?>) o;
      return Objects.equals(stateKey, other.stateKey)
          && Objects.equals(precisionKey, other.precisionKey);
    }

    @Override
    public int hashCode() {
      return Objects.hash(stateKey, precisionKey);
    }
  }
}
