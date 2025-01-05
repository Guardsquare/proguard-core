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

package proguard.analysis.cpa.algorithms;

import java.util.LinkedHashSet;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import proguard.analysis.cpa.defaults.PrecisionAdjustmentResult;
import proguard.analysis.cpa.interfaces.AbstractState;
import proguard.analysis.cpa.interfaces.ConfigurableProgramAnalysis;
import proguard.analysis.cpa.interfaces.Precision;
import proguard.analysis.cpa.interfaces.ReachedSet;
import proguard.analysis.cpa.interfaces.Waitlist;

/**
 * This is the <a
 * href="https://www.sosy-lab.org/research/pub/2018-JAR.A_Unifying_View_on_SMT-Based_Software_Verification.pdf">CPA+</a>
 * algorithm. The algorithm computes the set of reached states based on the initial content of the
 * waitlist.
 *
 * @param <StateT> The type of the analyzed states.
 */
public class CpaAlgorithm<StateT extends AbstractState> {

  private static final Logger log = LogManager.getLogger(CpaAlgorithm.class);
  private final ConfigurableProgramAnalysis<StateT> cpa;

  /**
   * Create an algorithm to run the specified CPA.
   *
   * @param cpa a CPA instance wrapping the transfer relation, the merge, and the stop operator, and
   *     the precision adjustment
   */
  public CpaAlgorithm(ConfigurableProgramAnalysis<StateT> cpa) {
    this.cpa = cpa;
  }

  /**
   * Launches the algorithm updating the {@code reachedSet} and the {@code waitlist}. A proper
   * selection of parameters allows resuming the algorithm from a saved state.
   */
  public void run(ReachedSet<StateT> reachedSet, Waitlist<StateT> waitlist) {
    while (!waitlist.isEmpty()) {
      StateT currentState = waitlist.pop();
      try {
        if (cpa.getAbortOperator().abort(currentState)) {
          return;
        }
        Precision currentPrecision = currentState.getPrecision();
        PrecisionAdjustmentResult<StateT> precisionAdjustmentResult =
            cpa.getPrecisionAdjustment()
                .prec(currentState, currentPrecision, reachedSet.getReached(currentState));
        currentState = precisionAdjustmentResult.getAbstractState();
        currentPrecision = currentState.getPrecision();

        for (StateT successorState :
            cpa.getTransferRelation().generateAbstractSuccessors(currentState, currentPrecision)) {
          Set<StateT> gen =
              new LinkedHashSet<>(); // abstract states to be added to the waitlist and reached set
          Set<StateT> kill =
              new LinkedHashSet<>(); // abstract states to be removed from the waitlist and reached
          // set
          for (StateT reachedState :
              reachedSet.getReached(
                  successorState)) // iterate only over the reached sets which may be merged with
          // the successor state
          {
            StateT mergedState =
                cpa.getMergeOperator()
                    .merge(successorState, reachedState, successorState.getPrecision());
            if (!mergedState.equals(reachedState)) {
              gen.add(mergedState);
              kill.add(reachedState);
            }
          }
          reachedSet.addAll(gen);
          reachedSet.removeAll(kill);
          waitlist.addAll(gen);
          waitlist.removeAll(kill);
          if (!cpa.getStopOperator()
              .stop(
                  successorState,
                  reachedSet.getReached(successorState),
                  successorState.getPrecision())) {
            waitlist.add(successorState);
            reachedSet.add(successorState);
          }
        }
      } catch (Exception exception) {
        log.error("CPA run stopped for the following error: ", exception);
        waitlist.clear();
      }
    }
  }
}
