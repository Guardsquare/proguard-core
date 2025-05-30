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

package proguard.analysis.cpa.interfaces;

import java.util.List;
import proguard.analysis.cpa.jvm.cfa.edges.JvmCfaEdge;
import proguard.analysis.cpa.jvm.state.JvmAbstractState;

/**
 * An interface for {@link TransferRelation}s that depend on the {@link
 * proguard.analysis.cpa.defaults.Cfa} location for which the successor can be defined for the
 * entering edges of the current location.
 *
 * @param <ContentT>> The content of the jvm states produced by the transfer relation. For example,
 *     this can be a {@link proguard.analysis.cpa.defaults.SetAbstractState} of taints for taint
 *     analysis or a {@link proguard.analysis.cpa.jvm.domain.value.ValueAbstractState} for value
 *     analysis.
 */
public interface ProgramLocationDependentBackwardTransferRelation<
        ContentT extends AbstractState<ContentT>>
    extends ProgramLocationDependentTransferRelation<ContentT> {
  @Override
  default List<JvmCfaEdge> getEdges(JvmAbstractState<ContentT> state) {
    return state.getProgramLocation().getEnteringEdges();
  }
}
