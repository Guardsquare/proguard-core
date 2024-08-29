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

package proguard.analysis.cpa.jvm.domain.reference;

import static proguard.exception.ErrorId.ANALYSIS_JVM_REFERENCE_TRANSFER_RELATION_STATE_UNSUPPORTED;

import java.util.Collection;
import java.util.List;
import proguard.analysis.cpa.defaults.SetAbstractState;
import proguard.analysis.cpa.interfaces.AbstractState;
import proguard.analysis.cpa.interfaces.Precision;
import proguard.analysis.cpa.jvm.cfa.edges.JvmCfaEdge;
import proguard.analysis.cpa.jvm.state.JvmAbstractState;
import proguard.analysis.cpa.jvm.transfer.JvmTransferRelation;
import proguard.analysis.cpa.jvm.witness.JvmStackLocation;
import proguard.analysis.datastructure.callgraph.Call;
import proguard.classfile.instruction.Instruction;
import proguard.classfile.util.ClassUtil;
import proguard.exception.ProguardCoreException;

/**
 * This {@link JvmTransferRelation} propagates reference values, destroys references upon arithmetic
 * operations, and creates fresh references for return values of intraprocedurally analyzed calls.
 *
 * @author Dmitry Ivanov
 */
public class JvmReferenceTransferRelation extends JvmTransferRelation<SetAbstractState<Reference>> {

  // implementations for JvmTransferRelation

  @Override
  public JvmReferenceAbstractState generateEdgeAbstractSuccessor(
      AbstractState abstractState, JvmCfaEdge edge, Precision precision) {
    if (!(abstractState instanceof JvmReferenceAbstractState)) {
      throw new ProguardCoreException.Builder(
              "%s does not support %s", ANALYSIS_JVM_REFERENCE_TRANSFER_RELATION_STATE_UNSUPPORTED)
          .errorParameters(getClass().getName(), abstractState.getClass().getName())
          .build();
    }

    return (JvmReferenceAbstractState)
        super.generateEdgeAbstractSuccessor(abstractState, edge, precision);
  }

  @Override
  public SetAbstractState<Reference> getAbstractDefault() {
    return SetAbstractState.bottom;
  }

  @Override
  protected SetAbstractState<Reference> calculateArithmeticInstruction(
      Instruction instruction, List<SetAbstractState<Reference>> operands) {
    return getAbstractDefault();
  }

  @Override
  protected SetAbstractState<Reference> computeIncrement(
      SetAbstractState<Reference> state, int value) {
    // references cannot be incremented
    return getAbstractDefault();
  }

  @Override
  public void invokeMethod(
      JvmAbstractState<SetAbstractState<Reference>> state,
      Call call,
      List<SetAbstractState<Reference>> operands) {
    String returnType =
        call.getTarget().descriptor.returnType == null
            ? "?"
            : call.getTarget().descriptor.returnType;
    if (ClassUtil.isInternalArrayType(returnType) || ClassUtil.isInternalClassType(returnType)) {
      state.push(
          new SetAbstractState<>(
              new Reference(state.getProgramLocation(), new JvmStackLocation(0))));
      return;
    }
    int pushCount = ClassUtil.internalTypeSize(returnType);
    for (int i = 0; i < pushCount; i++) {
      state.push(getAbstractDefault());
    }
  }

  @Override
  public Collection<? extends AbstractState> generateEdgeAbstractSuccessors(
      AbstractState abstractState, JvmCfaEdge edge, Precision precision) {
    return wrapAbstractSuccessorInCollection(
        generateEdgeAbstractSuccessor(abstractState, edge, precision));
  }
}
