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

package proguard.analysis.cpa.jvm.operators;

import static proguard.exception.ErrorId.ANALYSIS_JVM_DEFAULT_EXPAND_OPERATOR_EXIT_NODE_EXPECTED;
import static proguard.exception.ErrorId.ANALYSIS_JVM_DEFAULT_EXPAND_OPERATOR_MISSING_EXPECTED_CATCH_NODE_EXPECTED;
import static proguard.exception.ErrorId.ANALYSIS_JVM_DEFAULT_EXPAND_OPERATOR_RETURN_INSTRUCTION_EXPECTED;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import proguard.analysis.cpa.bam.ExpandOperator;
import proguard.analysis.cpa.defaults.LatticeAbstractState;
import proguard.analysis.cpa.defaults.MapAbstractState;
import proguard.analysis.cpa.defaults.SetAbstractState;
import proguard.analysis.cpa.interfaces.CallEdge;
import proguard.analysis.cpa.jvm.cfa.JvmCfa;
import proguard.analysis.cpa.jvm.cfa.edges.JvmCfaEdge;
import proguard.analysis.cpa.jvm.cfa.edges.JvmInstructionCfaEdge;
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode;
import proguard.analysis.cpa.jvm.state.JvmAbstractState;
import proguard.analysis.cpa.jvm.state.JvmFrameAbstractState;
import proguard.analysis.cpa.jvm.state.heap.JvmHeapAbstractState;
import proguard.analysis.cpa.jvm.util.InstructionClassifier;
import proguard.analysis.datastructure.callgraph.Call;
import proguard.classfile.Clazz;
import proguard.classfile.Method;
import proguard.classfile.MethodSignature;
import proguard.classfile.attribute.Attribute;
import proguard.classfile.attribute.CodeAttribute;
import proguard.classfile.attribute.ExceptionInfo;
import proguard.classfile.attribute.visitor.AllAttributeVisitor;
import proguard.classfile.attribute.visitor.AttributeVisitor;
import proguard.classfile.instruction.Instruction;
import proguard.exception.ProguardCoreException;

/**
 * This {@link ExpandOperator} simulates the JVM behavior on a method exit.
 *
 * <p>In case of exit with a return instruction it takes the heap and the frame from the exit state
 * of the called function and the local variables of the caller. Then pops the arguments of the call
 * from the stack, pushes the return value, and creates an abstract state at the target of the
 * intra-procedural call edge.
 *
 * <p>In case of exit with an exception besides performing the same reconstruction for local
 * variables, heap, and static fields, it discards the operand stack of the caller and pushes the
 * exception. The abstract successor location is either the first applicable catch node of the
 * caller, if exists, or the exception exit node of the caller.
 *
 * @param <ContentT> The content of the jvm states. For example, this can be a {@link
 *     SetAbstractState} of taints for taint analysis or a {@link
 *     proguard.analysis.cpa.jvm.domain.value.ValueAbstractState} for value analysis.
 */
public class DefaultExpandOperator<ContentT extends LatticeAbstractState<ContentT>>
    implements ExpandOperator<ContentT> {

  private final JvmCfa cfa;
  private final boolean expandHeap;

  /**
   * Create the default expand operator for the JVM.
   *
   * @param cfa the control flow automaton of the analyzed program
   */
  public DefaultExpandOperator(JvmCfa cfa) {
    this(cfa, true);
  }

  /**
   * Create the default expand operator for the JVM.
   *
   * @param cfa the control flow automaton of the analyzed program
   * @param expandHeap whether expansion of the heap is performed
   */
  public DefaultExpandOperator(JvmCfa cfa, boolean expandHeap) {
    this.cfa = cfa;
    this.expandHeap = expandHeap;
  }

  // Implementations for ExpandOperator

  @Override
  public JvmAbstractState<ContentT> expand(
      JvmAbstractState<ContentT> expandedInitialState,
      JvmAbstractState<ContentT> reducedExitState,
      JvmCfaNode blockEntryNode,
      Call call) {
    JvmCfaNode exitNode = reducedExitState.getProgramLocation();

    // expand return exit location
    if (exitNode.isReturnExitNode()) {

      // the next node is the target of the intra-procedural method call edge
      JvmCfaNode nextNode =
          cfa
              .getFunctionNode((MethodSignature) call.caller.signature, call.caller.offset)
              .getLeavingEdges()
              .stream()
              .filter(edge -> !(edge instanceof CallEdge))
              .findFirst()
              .get()
              .getTarget();

      JvmAbstractState<ContentT> returnState =
          createJvmAbstractState(
              nextNode,
              expandedInitialState.getFrame().copy(),
              reducedExitState.getHeap().copy(),
              reducedExitState.getStaticFields().copy());

      // pop the arguments of the invoke instruction from the initial state stack
      int elementsToPop = call.getJvmArgumentSize();
      for (int i = 0; i < elementsToPop; i++) {
        returnState.pop();
      }

      // push the return value on the caller stack
      JvmCfaEdge returnEdge = exitNode.getEnteringEdges().get(0);
      if (!InstructionClassifier.isReturn(
          ((JvmInstructionCfaEdge) returnEdge).getInstruction().opcode)) {
        throw new ProguardCoreException.Builder(
                "The entering edges into the return node should be return instructions",
                ANALYSIS_JVM_DEFAULT_EXPAND_OPERATOR_RETURN_INSTRUCTION_EXPECTED)
            .build();
      }

      Instruction returnInstruction = ((JvmInstructionCfaEdge) returnEdge).getInstruction();

      returnState.pushAll(calculateReturnValues(reducedExitState, returnInstruction, call));

      if (expandHeap) {
        expandHeap(returnState.getHeap(), expandedInitialState.getHeap());
      }

      return returnState;
    }

    // expand exception exit location
    if (exitNode.isExceptionExitNode()) {
      CallerExceptionHandlerFinder finder = new CallerExceptionHandlerFinder(call, cfa);
      call.caller.member.accept(call.caller.clazz, new AllAttributeVisitor(finder));

      JvmHeapAbstractState<ContentT> heap = reducedExitState.getHeap();
      if (expandHeap) {
        expandHeap(heap, expandedInitialState.getHeap());
      }

      return createJvmAbstractState(
          finder.nextNode,
          new JvmFrameAbstractState<>(
              expandedInitialState.getFrame().getLocalVariables(),
              reducedExitState.getFrame().getOperandStack()),
          heap,
          reducedExitState.getStaticFields());
    }

    throw new ProguardCoreException.Builder(
            "The node of %s at offset %d is not an exit node",
            ANALYSIS_JVM_DEFAULT_EXPAND_OPERATOR_EXIT_NODE_EXPECTED)
        .errorParameters(exitNode.getSignature(), exitNode.getOffset())
        .build();
  }

  /** Calculates the returned state. Can be overridden to handle special behavior. */
  protected List<ContentT> calculateReturnValues(
      JvmAbstractState<ContentT> reducedExitState, Instruction returnInstruction, Call call) {
    List<ContentT> returnValues = new ArrayList<>();

    for (int i = 0; i < returnInstruction.stackPopCount(null); i++) {
      ContentT returnByte = reducedExitState.peek(i);
      returnValues.add(0, returnByte);
    }

    return returnValues;
  }

  protected void expandHeap(
      JvmHeapAbstractState<ContentT> heap, JvmHeapAbstractState<ContentT> callerHeap) {
    heap.expand(callerHeap);
  }

  private static class CallerExceptionHandlerFinder implements AttributeVisitor {

    private final Call call;
    private final JvmCfa cfa;
    public JvmCfaNode nextNode;

    public CallerExceptionHandlerFinder(Call call, JvmCfa cfa) {
      this.call = call;
      this.cfa = cfa;
    }

    public void visitAnyAttribute(Clazz clazz, Attribute attribute) {}

    public void visitCodeAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute) {

      Optional<ExceptionInfo> firstCatch = Optional.empty();

      if (codeAttribute.exceptionTable != null) {
        // get the first applicable handler of the caller
        firstCatch =
            Arrays.stream(codeAttribute.exceptionTable)
                .filter(e -> e.isApplicable(call.caller.offset))
                .findFirst();
      }

      // get the handling node (either the first applicable catch node or the exception exit of the
      // caller)
      if (firstCatch.isPresent()) {
        JvmCfaNode firstCatchNode =
            cfa.getFunctionCatchNode(
                (MethodSignature) call.caller.signature, firstCatch.get().u2handlerPC);
        if (firstCatchNode == null) {
          throw new ProguardCoreException.Builder(
                  "Missing expected catch node in CFA for method %s",
                  ANALYSIS_JVM_DEFAULT_EXPAND_OPERATOR_MISSING_EXPECTED_CATCH_NODE_EXPECTED)
              .errorParameters(call.caller.signature)
              .build();
        }

        nextNode = firstCatchNode;
      } else {
        nextNode = cfa.getFunctionExceptionExitNode((MethodSignature) call.caller.signature, clazz);
      }
    }
  }

  protected JvmAbstractState<ContentT> createJvmAbstractState(
      JvmCfaNode programLocation,
      JvmFrameAbstractState<ContentT> frame,
      JvmHeapAbstractState<ContentT> heap,
      MapAbstractState<String, ContentT> staticFields) {
    return new JvmAbstractState<>(programLocation, frame, heap, staticFields);
  }
}
