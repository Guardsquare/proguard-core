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

import org.jetbrains.annotations.NotNull;
import proguard.analysis.cpa.algorithms.CpaAlgorithm;
import proguard.analysis.cpa.defaults.LatticeAbstractState;
import proguard.analysis.cpa.interfaces.AbortOperator;
import proguard.analysis.cpa.interfaces.AbstractDomain;
import proguard.analysis.cpa.interfaces.ConfigurableProgramAnalysis;
import proguard.analysis.cpa.interfaces.MergeOperator;
import proguard.analysis.cpa.interfaces.PrecisionAdjustment;
import proguard.analysis.cpa.interfaces.StopOperator;
import proguard.analysis.cpa.interfaces.TransferRelation;
import proguard.analysis.cpa.jvm.cfa.JvmCfa;
import proguard.analysis.cpa.jvm.state.JvmAbstractState;
import proguard.classfile.MethodSignature;

/**
 * A {@link ConfigurableProgramAnalysis} for inter-procedural analysis using block abstraction
 * memoization as described in {@see https://dl.acm.org/doi/pdf/10.1145/3368089.3409718}, which is
 * defined by a domain-dependent {@link CpaWithBamOperators} that adds three operators: reduce,
 * expand, and rebuild. This allows an inter-procedural analysis running this CPA to be conducted by
 * the standard {@link CpaAlgorithm}.
 *
 * <p>A BAM CPA works on a domain-independent level and its abstract domain, merge operator, and
 * stop operator are defined by the domain-dependent wrapped CPA. The main feature of a BAM CPA is
 * its transfer relation (see {@link BamTransferRelation} for details) that is able to extend the
 * analysis of the wrapped CPA to the inter-procedural level.
 *
 * @param <ContentT>> The content of the jvm states produced by the transfer relation. For example,
 *     this can be a {@link proguard.analysis.cpa.defaults.SetAbstractState} of taints for taint
 *     analysis or a {@link proguard.analysis.cpa.jvm.domain.value.ValueAbstractState} for value
 *     analysis.
 */
public class BamCpa<ContentT extends LatticeAbstractState<ContentT>>
    implements ConfigurableProgramAnalysis<JvmAbstractState<ContentT>> {

  private final CpaWithBamOperators<ContentT> wrappedCpa;
  private final BamTransferRelation<ContentT> bamTransferRelation;

  /**
   * Create a BamCpa with default transfer relation.
   *
   * @param wrappedCpa a wrapped CPA with BAM operators
   * @param cfa a control flow automaton
   * @param mainFunction the signature of the main function of an analyzed program
   * @param cache a cache for the block abstractions
   */
  public BamCpa(
      CpaWithBamOperators<ContentT> wrappedCpa,
      JvmCfa cfa,
      MethodSignature mainFunction,
      BamCache<ContentT> cache) {
    this(wrappedCpa, cfa, mainFunction, cache, -1);
  }

  /**
   * Create a BamCpa with default transfer relation with a limited call depth. At the maximum call
   * depth further function calls are just analyzed intra-procedurally.
   *
   * @param wrappedCpa a wrapped cpa with BAM operators
   * @param cfa a control flow automaton
   * @param mainFunction the signature of a main function
   * @param cache a cache for the block abstractions
   * @param maxCallStackDepth maximum depth of the call stack analyzed inter-procedurally. 0 means
   *     intra-procedural analysis. < 0 means no maximum depth.
   */
  public BamCpa(
      CpaWithBamOperators<ContentT> wrappedCpa,
      JvmCfa cfa,
      MethodSignature mainFunction,
      BamCache<ContentT> cache,
      int maxCallStackDepth) {
    this.wrappedCpa = wrappedCpa;
    this.bamTransferRelation =
        new BamTransferRelation<>(this, cfa, mainFunction, cache, maxCallStackDepth);
  }

  // Implementations for ConfigurableProgramAnalysis

  /** Returns the abstract domain of the wrapped CPA. */
  @Override
  public @NotNull AbstractDomain<JvmAbstractState<ContentT>> getAbstractDomain() {
    return wrappedCpa.getAbstractDomain();
  }

  /** Returns the BAM transfer relation, more details in {@link BamTransferRelation}. */
  @Override
  public @NotNull BamTransferRelation<ContentT> getTransferRelation() {
    return bamTransferRelation;
  }

  /** Returns the merge operator of the wrapped CPA. */
  @Override
  public @NotNull MergeOperator<JvmAbstractState<ContentT>> getMergeOperator() {
    return wrappedCpa.getMergeOperator();
  }

  /** Returns the stop operator of the wrapped CPA. */
  @Override
  public @NotNull StopOperator<JvmAbstractState<ContentT>> getStopOperator() {
    return wrappedCpa.getStopOperator();
  }

  /** Returns the precision adjustment of the wrapped CPA. */
  @Override
  public @NotNull PrecisionAdjustment getPrecisionAdjustment() {
    return wrappedCpa.getPrecisionAdjustment();
  }

  @Override
  public @NotNull AbortOperator getAbortOperator() {
    return wrappedCpa.getAbortOperator();
  }

  /** Returns the reduce operator of the wrapped CPA. */
  public ReduceOperator<ContentT> getReduceOperator() {
    return wrappedCpa.getReduceOperator();
  }

  /** Returns the expand operator of the wrapped CPA. */
  public ExpandOperator<ContentT> getExpandOperator() {
    return wrappedCpa.getExpandOperator();
  }

  /** Returns the rebuild operator of the wrapped CPA. */
  public RebuildOperator getRebuildOperator() {
    return wrappedCpa.getRebuildOperator();
  }

  /** Returns the BAM cache used by the CPA. */
  public BamCache<ContentT> getCache() {
    return bamTransferRelation.getCache();
  }

  /** Returns the CFA used by the CPA. */
  public JvmCfa getCfa() {
    return bamTransferRelation.getCfa();
  }

  /**
   * Returns the transfer relation of the interprocedural CPA wrapped by the BamCpa.
   *
   * <p>This transfer relation is used to analyze all non-call instructions and call instructions
   * that cannot be analyzed inter-procedurally (e.g., because their code is not available or
   * because the maximum analysis depth has been reached).
   */
  public TransferRelation<JvmAbstractState<ContentT>> getIntraproceduralTransferRelation() {
    return wrappedCpa.getTransferRelation();
  }
}
