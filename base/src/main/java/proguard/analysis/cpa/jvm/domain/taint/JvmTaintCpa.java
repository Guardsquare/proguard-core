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

package proguard.analysis.cpa.jvm.domain.taint;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import proguard.analysis.cpa.defaults.DelegateAbstractDomain;
import proguard.analysis.cpa.defaults.MergeJoinOperator;
import proguard.analysis.cpa.defaults.SetAbstractState;
import proguard.analysis.cpa.defaults.SimpleCpa;
import proguard.analysis.cpa.defaults.StopJoinOperator;
import proguard.analysis.cpa.domain.taint.TaintSource;
import proguard.analysis.cpa.interfaces.AbstractDomain;
import proguard.analysis.cpa.jvm.state.JvmAbstractState;
import proguard.analysis.cpa.jvm.witness.JvmMemoryLocation;
import proguard.analysis.datastructure.callgraph.Call;
import proguard.classfile.MethodSignature;
import proguard.classfile.Signature;

/**
 * The {@link JvmTaintCpa} computes abstract states containing {@link JvmTaintSource}s which can
 * reach the given code location.
 */
public class JvmTaintCpa extends SimpleCpa<JvmAbstractState<SetAbstractState<JvmTaintSource>>> {

  /**
   * Create a taint CPA.
   *
   * @param sources a set of taint sources
   */
  public JvmTaintCpa(Set<? extends JvmTaintSource> sources) {
    this(
        createSourcesMap(sources),
        new DelegateAbstractDomain<>(),
        Collections.emptyMap(),
        Collections.emptyMap());
  }

  /**
   * Create a taint CPA.
   *
   * @param sources a set of taint sources
   * @param taintTransformers a mapping from method signature to a transformer object applied to the
   *     taint state when that method is invoked
   * @param extraTaintPropagationLocations a mapping from a specific method call to any jvm state
   *     location that is tainted as a result of the call
   */
  public JvmTaintCpa(
      Set<? extends JvmTaintSource> sources,
      Map<MethodSignature, JvmTaintTransformer> taintTransformers,
      Map<Call, Set<JvmMemoryLocation>> extraTaintPropagationLocations) {
    this(
        createSourcesMap(sources),
        new DelegateAbstractDomain<>(),
        taintTransformers,
        extraTaintPropagationLocations);
  }

  /**
   * Create a taint CPA.
   *
   * @param signaturesToSources a mapping from method signature to taint sources
   * @param taintTransformers a mapping from method signature to a transformer object applied to the
   *     taint state when that method is invoked
   * @param extraTaintPropagationLocations a mapping from a specific method call to any jvm state
   *     location that is tainted as a result of the call
   */
  public JvmTaintCpa(
      Map<Signature, Set<JvmTaintSource>> signaturesToSources,
      Map<MethodSignature, JvmTaintTransformer> taintTransformers,
      Map<Call, Set<JvmMemoryLocation>> extraTaintPropagationLocations) {
    this(
        signaturesToSources,
        new DelegateAbstractDomain<>(),
        taintTransformers,
        extraTaintPropagationLocations);
  }

  private JvmTaintCpa(
      Map<Signature, Set<JvmTaintSource>> sources,
      AbstractDomain<JvmAbstractState<SetAbstractState<JvmTaintSource>>> abstractDomain,
      Map<MethodSignature, JvmTaintTransformer> taintTransformers,
      Map<Call, Set<JvmMemoryLocation>> extraTaintPropagationLocations) {
    super(
        abstractDomain,
        new JvmTaintTransferRelation(sources, taintTransformers, extraTaintPropagationLocations),
        new MergeJoinOperator<>(abstractDomain),
        new StopJoinOperator<>(abstractDomain));
  }

  /**
   * Since the used data structure is a map that uses the fqn as key, which is a parameter of the
   * {@link TaintSource}s, this method constructs the map correctly starting from a set of sources.
   */
  public static Map<Signature, Set<JvmTaintSource>> createSourcesMap(
      Set<? extends JvmTaintSource> sources) {
    Map<Signature, Set<JvmTaintSource>> taintSourcesMap = new HashMap<>();
    sources.forEach(
        s -> taintSourcesMap.computeIfAbsent(s.signature, key -> new HashSet<>()).add(s));
    return taintSourcesMap;
  }
}
