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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import proguard.classfile.Signature;
import proguard.analysis.cpa.interfaces.AbstractState;
import proguard.analysis.cpa.interfaces.Precision;

/**
 * A simple implementation of {@link BamCache} where the cache is implemented as a {@link HashMap}.
 *
 * @author Carlo Alberto Pozzoli
 */
public class BamCacheImpl<SignatureT extends Signature>
    implements BamCache<SignatureT>
{

    private final Map<HashKey, BlockAbstraction> cache = new HashMap<>();

    // Implementations for BamCache

    @Override
    public void put(AbstractState stateKey, Precision precisionKey, SignatureT blockKey, BlockAbstraction blockAbstraction)
    {
        cache.put(getHashKey(stateKey, precisionKey, blockKey), blockAbstraction);
    }

    @Override
    public BlockAbstraction get(AbstractState stateKey, Precision precisionKey, SignatureT blockKey)
    {
        return cache.get(getHashKey(stateKey, precisionKey, blockKey));
    }

    /**
     * Returns the keys of the cache.
     */
    public Set<HashKey> getKeySet()
    {
        return cache.keySet();
    }

    /**
     * Returns the list of cache entries for a specified method.
     *
     * @param signature the signature of a method
     */
    public List<BlockAbstraction> getBySignature(SignatureT signature)
    {
        List<HashKey>          methodKeys = cache.keySet().stream().filter(k -> k.blockKey.equals(signature)).collect(Collectors.toList());
        List<BlockAbstraction> res        = new ArrayList<>();
        methodKeys.forEach(k -> res.add(cache.get(k)));
        return res;
    }

    @Override
    public Collection<BlockAbstraction> values()
    {
        return cache.values();
    }

    private HashKey getHashKey(AbstractState stateKey, Precision precisionKey, SignatureT blockKey)
    {
        return new HashKey(stateKey, precisionKey, blockKey);
    }

    /**
     * The key of the cache is created from the three parameters that define a block abstraction. The equals and hashCode methods are overridden to guarantee the correct behavior of the hash map.
     */
    private static class HashKey
    {

        private final AbstractState stateKey;
        private final Precision     precisionKey;
        private final Signature     blockKey;

        /**
         * Create a cache key from its components.
         *
         * @param stateKey     the entry abstract state of a method
         * @param precisionKey a precision
         * @param blockKey     the signature of a method
         */
        public HashKey(AbstractState stateKey, Precision precisionKey, Signature blockKey)
        {
            this.stateKey = stateKey;
            this.precisionKey = precisionKey;
            this.blockKey = blockKey;
        }

        /**
         * Returns the signature of the method that composes the key.
         */
        public Signature getBlockKey()
        {
            return blockKey;
        }

        /**
         * Returns the entry state of the block that composes the key.
         */
        public AbstractState getStateKey()
        {
            return stateKey;
        }

        /**
         * Returns the precision that composes the key.
         */
        public Precision getPrecisionKey()
        {
            return precisionKey;
        }

        // Implementations for Object

        @Override
        public boolean equals(Object o)
        {
            if (o == this)
            {
                return true;
            }
            if (!(o instanceof HashKey))
            {
                return false;
            }
            HashKey other = (HashKey) o;
            return Objects.equals(stateKey, other.stateKey)
                   && Objects.equals(precisionKey, other.precisionKey)
                   && Objects.equals(blockKey, other.blockKey);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(stateKey, precisionKey, blockKey);
        }
    }
}
