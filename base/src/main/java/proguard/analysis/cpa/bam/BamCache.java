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
import java.util.Set;
import proguard.analysis.cpa.defaults.Cfa;
import proguard.classfile.Signature;
import proguard.analysis.cpa.interfaces.AbstractState;
import proguard.analysis.cpa.interfaces.Precision;

/**
 * Generic interface for the BAM cache, where the blocks represent a function identified with a {@code SignatureT}. Along with the {@link Cfa} the signature
 * can be used to retrieve the block (i.e. the respective CFA subgraph) as described in the BAM paper.
 *
 * <p>A block abstraction is uniquely identified by a triple of an entry {@link AbstractState} (that may be call-context dependent, e.g. if the calling parameters or global variables are
 * different), the corresponding {@link Precision}, and the {@link Signature} of the function the block belongs to.
 *
 * @author Carlo Alberto Pozzoli
 */
public interface BamCache<SignatureT extends Signature>
{

    /**
     * Adds the block abstraction identified by the provided keys to the cache.
     */
    void put(AbstractState stateKey, Precision precisionKey, SignatureT blockKey, BlockAbstraction blockAbstraction);

    /**
     * Gets the block abstraction identified by the provided keys from the cache.
     *
     * @return The requested block abstraction, null in case of cache-miss.
     */
    BlockAbstraction get(AbstractState stateKey, Precision precisionKey, SignatureT blockKey);

    /**
     * Returns a collection of all the cache entries for a specified method, empty in case there are not such entries.
     */
    Collection<BlockAbstraction> get(SignatureT blockKey);

    /**
     * Returns a collection of all the cache entries for a specified method with a certain precision, empty in case there are not such entries.
     */
    Collection<BlockAbstraction> get(Precision precision, SignatureT blockKey);

    /**
     * Returns block abstractions stored in the cache.
     */
    Collection<BlockAbstraction> values();

    /**
     * Returns the size of the cache.
     */
    int size();

    /**
     * Returns a set of all the methods that have an entry in the cache.
     */
    Set<SignatureT> getAllMethods();
}
