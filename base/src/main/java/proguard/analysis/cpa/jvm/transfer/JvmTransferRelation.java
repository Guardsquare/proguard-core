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

package proguard.analysis.cpa.jvm.transfer;

import static proguard.classfile.ClassConstants.TYPE_JAVA_LANG_STRING;
import static proguard.classfile.util.ClassUtil.internalTypeFromClassName;
import static proguard.classfile.util.ClassUtil.isExtendable;
import static proguard.exception.ErrorId.ANALYSIS_JVM_TRANSFER_RELATION_CONSTANT_INSTRUCTION_VISITOR_OPCODE_UNSUPPORTED;
import static proguard.exception.ErrorId.ANALYSIS_JVM_TRANSFER_RELATION_INSTRUCTION_PUSH_COUNT_HIGHER_THAN_TWO;
import static proguard.exception.ErrorId.ANALYSIS_JVM_TRANSFER_RELATION_UNEXPECTED_UNKNOWN_SIGNATURE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import proguard.analysis.CallResolver;
import proguard.analysis.cpa.defaults.LatticeAbstractState;
import proguard.analysis.cpa.defaults.SetAbstractState;
import proguard.analysis.cpa.interfaces.Precision;
import proguard.analysis.cpa.interfaces.ProgramLocationDependentForwardTransferRelation;
import proguard.analysis.cpa.jvm.cfa.edges.JvmCallCfaEdge;
import proguard.analysis.cpa.jvm.cfa.edges.JvmCfaEdge;
import proguard.analysis.cpa.jvm.cfa.edges.JvmInstructionCfaEdge;
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode;
import proguard.analysis.cpa.jvm.state.JvmAbstractState;
import proguard.analysis.cpa.jvm.util.ConstantLookupVisitor;
import proguard.analysis.cpa.jvm.util.InstructionClassifier;
import proguard.analysis.datastructure.CodeLocation;
import proguard.analysis.datastructure.callgraph.Call;
import proguard.analysis.datastructure.callgraph.SymbolicCall;
import proguard.classfile.Clazz;
import proguard.classfile.Method;
import proguard.classfile.MethodSignature;
import proguard.classfile.ProgramClass;
import proguard.classfile.attribute.CodeAttribute;
import proguard.classfile.constant.ClassConstant;
import proguard.classfile.constant.Constant;
import proguard.classfile.constant.DoubleConstant;
import proguard.classfile.constant.FloatConstant;
import proguard.classfile.constant.IntegerConstant;
import proguard.classfile.constant.LongConstant;
import proguard.classfile.constant.StringConstant;
import proguard.classfile.constant.visitor.ConstantVisitor;
import proguard.classfile.instruction.BranchInstruction;
import proguard.classfile.instruction.ConstantInstruction;
import proguard.classfile.instruction.Instruction;
import proguard.classfile.instruction.SimpleInstruction;
import proguard.classfile.instruction.SwitchInstruction;
import proguard.classfile.instruction.VariableInstruction;
import proguard.classfile.instruction.visitor.InstructionVisitor;
import proguard.classfile.util.ClassUtil;
import proguard.evaluation.value.Value;
import proguard.exception.ProguardCoreException;

/**
 * The {@link JvmTransferRelation} computes the successors of an {@link JvmAbstractState} for a
 * given instruction. It stores category 2 computational types as tuples of the abstract state
 * containing the information about the value in the most significant bits and a default abstract
 * state in the least significant bits of the big-endian notation.
 *
 * @param <ContentT> The content of the jvm states. For example, this can be a {@link
 *     SetAbstractState} of taints for taint analysis or a {@link
 *     proguard.analysis.cpa.jvm.domain.value.ValueAbstractState} for value analysis.
 */
public abstract class JvmTransferRelation<ContentT extends LatticeAbstractState<ContentT>>
    implements ProgramLocationDependentForwardTransferRelation<ContentT> {

  // Implementations for ProgramLocationDependentTransferRelation

  @Override
  public Collection<JvmAbstractState<ContentT>> generateEdgeAbstractSuccessors(
      JvmAbstractState<ContentT> abstractState, JvmCfaEdge edge, Precision precision) {
    return wrapAbstractSuccessorInCollection(
        generateEdgeAbstractSuccessor(abstractState, edge, precision));
  }

  public JvmAbstractState<ContentT> generateEdgeAbstractSuccessor(
      JvmAbstractState<ContentT> abstractState, JvmCfaEdge edge, Precision precision) {
    JvmAbstractState<ContentT> successor = abstractState.copy();

    if (edge instanceof JvmCallCfaEdge) {
      // successor location is the intraprocedural successor node after method invocation
      successor.setProgramLocation(edge.getSource().getLeavingInvokeEdge().get().getTarget());
      processCall(successor, ((JvmCallCfaEdge) edge).getCall());
    } else {
      if (edge instanceof JvmInstructionCfaEdge) {
        Instruction instruction = ((JvmInstructionCfaEdge) edge).getInstruction();
        boolean isInterproceduralInvoke =
            InstructionClassifier.isInvoke(instruction.opcode)
                && !abstractState.getProgramLocation().getLeavingInterproceduralEdges().isEmpty();
        if (isInterproceduralInvoke) {
          // If we have at least one JvmCallCfaEdge we produce the successor(s) for the method
          // invocation
          // when those edges are analyzed, so we skip the corresponding JvmInstructionEdge.
          return null;
        }
        successor =
            getAbstractSuccessorForInstruction(
                successor, instruction, abstractState.getProgramLocation().getClazz(), precision);
      }
      successor.setProgramLocation(edge.getTarget());
    }

    return successor;
  }

  /** Returns the result of applying {@code instruction} to the {@code abstractState}. */
  protected JvmAbstractState<ContentT> getAbstractSuccessorForInstruction(
      JvmAbstractState<ContentT> abstractState,
      Instruction instruction,
      Clazz clazz,
      Precision precision) {
    instruction.accept(clazz, null, null, 0, new InstructionAbstractInterpreter(abstractState));
    return abstractState;
  }

  /**
   * Calculates the result of the instruction application. The default implementation computes join
   * over its arguments.
   */
  protected ContentT calculateArithmeticInstruction(
      Instruction instruction, List<ContentT> operands) {
    return operands.stream().reduce(getAbstractDefault(), ContentT::join);
  }

  /**
   * Returns the abstract state of the incremented input {@code state} by {@code value}. The default
   * implementation computes the join.
   */
  protected ContentT computeIncrement(ContentT state, int value) {
    return state.join(getAbstractIntegerConstant(value));
  }

  /** Returns an abstract representation of a byte constant {@code b}. */
  public ContentT getAbstractByteConstant(byte b) {
    return getAbstractDefault();
  }

  /**
   * Returns a default abstract state. In case of lattice abstract domains, it should be the bottom
   * element.
   */
  public abstract ContentT getAbstractDefault();

  /** Returns an abstract representation of a double constant {@code d}. */
  public List<ContentT> getAbstractDoubleConstant(double d) {
    return Arrays.asList(getAbstractDefault(), getAbstractDefault());
  }

  /** Returns an abstract representation of a float constant {@code f}. */
  public ContentT getAbstractFloatConstant(float f) {
    return getAbstractDefault();
  }

  /** Returns an abstract representation of an integer constant {@code i}. */
  public ContentT getAbstractIntegerConstant(int i) {
    return getAbstractDefault();
  }

  /** Returns an abstract representation of a long constant {@code l}. */
  public List<ContentT> getAbstractLongConstant(long l) {
    return Arrays.asList(getAbstractDefault(), getAbstractDefault());
  }

  /** Returns an abstract representation of a null reference. */
  public ContentT getAbstractNull() {
    return getAbstractDefault();
  }

  /** Returns an abstract representation of a short constant {@code s}. */
  public ContentT getAbstractShortConstant(short s) {
    return getAbstractDefault();
  }

  /** Returns an abstract representation of a reference value {@code object}. */
  public ContentT getAbstractReferenceValue(String className) {
    return getAbstractDefault();
  }

  /** Returns an abstract representation of a reference value {@code object}. */
  public ContentT getAbstractReferenceValue(
      String className, Clazz referencedClazz, boolean mayBeExtension, boolean mayBeNull) {
    return getAbstractDefault();
  }

  /** Returns an abstract representation of a reference value {@code object}. */
  public ContentT getAbstractReferenceValue(
      String className,
      Clazz referencedClazz,
      boolean mayBeExtension,
      boolean mayBeNull,
      Clazz creationClass,
      Method creationMethod,
      int creationOffset,
      Object value) {
    return getAbstractDefault();
  }

  /** Pops the arguments from the operand stack and passes them to {@code invokeMethod}. */
  protected void processCall(JvmAbstractState<ContentT> state, Call call) {
    Deque<ContentT> operands = new LinkedList<>();
    if (call.getTarget().descriptor.argumentTypes != null) {
      List<String> argumentTypes = call.getTarget().descriptor.argumentTypes;
      for (int i = argumentTypes.size() - 1; i >= 0; i--) {
        boolean isCategory2 = ClassUtil.isInternalCategory2Type(argumentTypes.get(i));
        if (isCategory2) {
          ContentT higherByte = state.pop();
          operands.offerFirst(state.pop());
          operands.offerFirst(higherByte);
        } else {
          operands.offerFirst(state.pop());
        }
      }
    }
    if (!call.isStatic()) {
      operands.offerFirst(state.pop());
    }
    invokeMethod(state, call, (List<ContentT>) operands);
  }

  /** The default implementation computes join over its arguments. */
  public void invokeMethod(JvmAbstractState<ContentT> state, Call call, List<ContentT> operands) {
    int pushCount =
        ClassUtil.internalTypeSize(
            call.getTarget().descriptor.returnType == null
                ? "?"
                : call.getTarget().descriptor.returnType);
    ContentT answerContent = operands.stream().reduce(getAbstractDefault(), ContentT::join);
    for (int i = 0; i < pushCount; i++) {
      state.push(answerContent);
    }
  }

  /** Returns an abstract state representing the result of the {@code instanceof} operation. */
  protected ContentT isInstanceOf(ContentT state, String type) {
    return getAbstractDefault();
  }

  /** Returns an abstract state representing the result of the {@code checkcast} operation. */
  protected ContentT handleCheckCast(ContentT state, String typeName) {
    // Default behavior: ignore the instruction, checkcast leaves the object reference where it is
    return state;
  }

  /**
   * This {@link InstructionVisitor} performs generic operations (e.g., loads, stores) parametrized
   * by the specific behavior of {@link JvmTransferRelation} for instruction applications, method
   * invocations, and constructing literals.
   */
  protected class InstructionAbstractInterpreter implements InstructionVisitor {

    protected final JvmAbstractState<ContentT> abstractState;
    protected final ConstantLookupVisitor constantLookupVisitor = new ConstantLookupVisitor();
    private final LdcConstantValueStatePusher constantValueStatePusher =
        new LdcConstantValueStatePusher();

    public InstructionAbstractInterpreter(JvmAbstractState<ContentT> abstractState) {
      this.abstractState = abstractState;
    }

    // implementations for InstructionVisitor

    @Override
    public void visitSimpleInstruction(
        Clazz clazz,
        Method method,
        CodeAttribute codeAttribute,
        int offset,
        SimpleInstruction simpleInstruction) {
      switch (simpleInstruction.opcode) {
        case Instruction.OP_ACONST_NULL:
          abstractState.push(getAbstractNull());
          break;
        case Instruction.OP_ICONST_M1:
        case Instruction.OP_ICONST_0:
        case Instruction.OP_ICONST_1:
        case Instruction.OP_ICONST_2:
        case Instruction.OP_ICONST_3:
        case Instruction.OP_ICONST_4:
        case Instruction.OP_ICONST_5:
          abstractState.push(getAbstractIntegerConstant(simpleInstruction.constant));
          break;
        case Instruction.OP_LCONST_0:
        case Instruction.OP_LCONST_1:
          abstractState.pushAll(getAbstractLongConstant(simpleInstruction.constant));
          break;
        case Instruction.OP_FCONST_0:
        case Instruction.OP_FCONST_1:
        case Instruction.OP_FCONST_2:
          abstractState.push(getAbstractFloatConstant(simpleInstruction.constant));
          break;
        case Instruction.OP_DCONST_0:
        case Instruction.OP_DCONST_1:
          abstractState.pushAll(getAbstractDoubleConstant(simpleInstruction.constant));
          break;
        case Instruction.OP_IALOAD:
        case Instruction.OP_FALOAD:
        case Instruction.OP_AALOAD:
        case Instruction.OP_BALOAD:
        case Instruction.OP_CALOAD:
        case Instruction.OP_SALOAD:
          {
            ContentT index = abstractState.pop();
            abstractState.push(
                abstractState.getArrayElementOrDefault(
                    abstractState.pop(), index, getAbstractDefault()));
            break;
          }
        case Instruction.OP_LALOAD:
        case Instruction.OP_DALOAD:
          {
            ContentT index = abstractState.pop();
            ContentT array = abstractState.pop();
            abstractState.push(getAbstractDefault());
            abstractState.push(
                abstractState.getArrayElementOrDefault(array, index, getAbstractDefault()));
            break;
          }
        case Instruction.OP_IASTORE:
        case Instruction.OP_FASTORE:
        case Instruction.OP_AASTORE:
        case Instruction.OP_BASTORE:
        case Instruction.OP_CASTORE:
        case Instruction.OP_SASTORE:
          {
            ContentT value = abstractState.pop();
            ContentT index = abstractState.pop();
            abstractState.setArrayElement(abstractState.pop(), index, value);
            break;
          }
        case Instruction.OP_LASTORE:
        case Instruction.OP_DASTORE:
          {
            ContentT value = abstractState.pop();
            abstractState.pop();
            ContentT index = abstractState.pop();
            abstractState.setArrayElement(abstractState.pop(), index, value);
            break;
          }
        case Instruction.OP_BIPUSH:
          abstractState.push(getAbstractByteConstant((byte) simpleInstruction.constant));
          break;
        case Instruction.OP_SIPUSH:
          abstractState.push(getAbstractShortConstant((short) simpleInstruction.constant));
          break;
        case Instruction.OP_POP:
        case Instruction.OP_MONITORENTER: // TODO synchronization is not yet modeled
        case Instruction.OP_MONITOREXIT:
          abstractState.pop();
          break;
        case Instruction.OP_POP2:
          abstractState.pop();
          abstractState.pop();
          break;
        case Instruction.OP_DUP:
          abstractState.push(abstractState.peek());
          break;
        case Instruction.OP_DUP_X1:
          {
            ContentT state1 = abstractState.pop();
            ContentT state2 = abstractState.pop();
            abstractState.push(state1);
            abstractState.push(state2);
            abstractState.push(state1);
            break;
          }
        case Instruction.OP_DUP_X2:
          {
            ContentT state1 = abstractState.pop();
            ContentT state2 = abstractState.pop();
            ContentT state3 = abstractState.pop();
            abstractState.push(state1);
            abstractState.push(state3);
            abstractState.push(state2);
            abstractState.push(state1);
            break;
          }
        case Instruction.OP_DUP2:
          {
            ContentT state1 = abstractState.peek();
            ContentT state2 = abstractState.peek(1);
            abstractState.push(state2);
            abstractState.push(state1);
            break;
          }
        case Instruction.OP_DUP2_X1:
          {
            ContentT state1 = abstractState.pop();
            ContentT state2 = abstractState.pop();
            ContentT state3 = abstractState.pop();
            abstractState.push(state2);
            abstractState.push(state1);
            abstractState.push(state3);
            abstractState.push(state2);
            abstractState.push(state1);
            break;
          }
        case Instruction.OP_DUP2_X2:
          {
            ContentT state1 = abstractState.pop();
            ContentT state2 = abstractState.pop();
            ContentT state3 = abstractState.pop();
            ContentT state4 = abstractState.pop();
            abstractState.push(state2);
            abstractState.push(state1);
            abstractState.push(state4);
            abstractState.push(state3);
            abstractState.push(state2);
            abstractState.push(state1);
            break;
          }
        case Instruction.OP_SWAP:
          {
            ContentT state1 = abstractState.pop();
            ContentT state2 = abstractState.pop();
            abstractState.push(state1);
            abstractState.push(state2);
            break;
          }
          // in case of return we don't touch the value on the stack to be able to provide it to the
          // caller
        case Instruction.OP_RETURN:
        case Instruction.OP_ARETURN:
        case Instruction.OP_DRETURN:
        case Instruction.OP_FRETURN:
        case Instruction.OP_LRETURN:
        case Instruction.OP_IRETURN:
          break;
        case Instruction.OP_ATHROW:
          ContentT exceptionState = abstractState.pop();
          abstractState.clearOperandStack();
          abstractState.push(exceptionState);
          break;
        case Instruction.OP_ARRAYLENGTH:
          abstractState.pop();
          abstractState.push(getAbstractDefault());
          break;
        default: // arithmetic instructions
          {
            List<ContentT> operands = new ArrayList<>(simpleInstruction.stackPopCount(clazz));
            // long shift instruction have to be considered separately because they have a category2
            // and a category1 parameter
            if (InstructionClassifier.isLongShift(simpleInstruction.opcode)) {
              operands.add(abstractState.pop());
              ContentT higherByte = abstractState.pop();
              operands.add(abstractState.pop());
              operands.add(higherByte);
            } else {
              for (int i = 0;
                  i
                      < simpleInstruction.stackPopCount(clazz)
                          / (simpleInstruction.isCategory2() ? 2 : 1);
                  i++) {
                if (simpleInstruction.isCategory2()) {
                  ContentT higherByte = abstractState.pop();
                  operands.add(abstractState.pop());
                  operands.add(higherByte);
                } else {
                  operands.add(abstractState.pop());
                }
              }
            }

            int resultCount = simpleInstruction.stackPushCount(clazz);
            if (resultCount > 2) {
              throw new ProguardCoreException.Builder(
                      "No instruction with push count higher than 2 should be handled here. Instruction: %s",
                      ANALYSIS_JVM_TRANSFER_RELATION_INSTRUCTION_PUSH_COUNT_HIGHER_THAN_TWO)
                  .errorParameters(simpleInstruction)
                  .build();
            }
            if (resultCount == 2) {
              abstractState.push(getAbstractDefault());
            }
            if (resultCount > 0) {
              Collections.reverse(operands);
              abstractState.push(calculateArithmeticInstruction(simpleInstruction, operands));
            }
          }
      }
    }

    @Override
    public void visitVariableInstruction(
        Clazz clazz,
        Method method,
        CodeAttribute codeAttribute,
        int offset,
        VariableInstruction variableInstruction) {
      if (variableInstruction.opcode == Instruction.OP_IINC) {
        abstractState.setVariable(
            variableInstruction.variableIndex,
            computeIncrement(
                abstractState.getVariableOrDefault(
                    variableInstruction.variableIndex, getAbstractDefault()),
                variableInstruction.constant),
            getAbstractDefault());
        return;
      }
      if (variableInstruction.isLoad()) {
        if (variableInstruction.isCategory2()) {
          abstractState.push(
              abstractState.getVariableOrDefault(
                  variableInstruction.variableIndex + 1, getAbstractDefault()));
        }
        abstractState.push(
            abstractState.getVariableOrDefault(
                variableInstruction.variableIndex, getAbstractDefault()));
        return;
      }
      // process store instruction
      abstractState.setVariable(
          variableInstruction.variableIndex, abstractState.pop(), getAbstractDefault());
      if (variableInstruction.isCategory2()) {
        abstractState.setVariable(
            variableInstruction.variableIndex + 1, abstractState.pop(), getAbstractDefault());
      }
    }

    @Override
    public void visitConstantInstruction(
        Clazz clazz,
        Method method,
        CodeAttribute codeAttribute,
        int offset,
        ConstantInstruction constantInstruction) {
      constantLookupVisitor.resetResult();
      switch (constantInstruction.opcode) {
        case Instruction.OP_LDC:
        case Instruction.OP_LDC_W:
        case Instruction.OP_LDC2_W:
          clazz.constantPoolEntryAccept(
              constantInstruction.constantIndex, constantValueStatePusher);
          break;
        case Instruction.OP_GETSTATIC:
          constantLookupVisitor.isStatic = true;
          clazz.constantPoolEntryAccept(constantInstruction.constantIndex, constantLookupVisitor);
          if (constantLookupVisitor.resultSize > 1) {
            abstractState.push(getAbstractDefault());
          }
          abstractState.push(
              abstractState.getStaticOrDefault(constantLookupVisitor.result, getAbstractDefault()));
          break;
        case Instruction.OP_PUTSTATIC:
          {
            constantLookupVisitor.isStatic = true;
            clazz.constantPoolEntryAccept(constantInstruction.constantIndex, constantLookupVisitor);
            ContentT value = abstractState.pop();
            if (constantLookupVisitor.resultSize > 1) {
              abstractState.pop();
            }
            abstractState.setStatic(constantLookupVisitor.result, value, getAbstractDefault());
            break;
          }
        case Instruction.OP_GETFIELD:
          {
            constantLookupVisitor.isStatic = false;
            clazz.constantPoolEntryAccept(constantInstruction.constantIndex, constantLookupVisitor);
            ContentT result =
                abstractState.getFieldOrDefault(
                    abstractState.pop(), constantLookupVisitor.result, getAbstractDefault());
            if (constantLookupVisitor.resultSize > 1) {
              abstractState.push(getAbstractDefault());
            }
            abstractState.push(result);
            break;
          }
        case Instruction.OP_PUTFIELD:
          {
            constantLookupVisitor.isStatic = false;
            clazz.constantPoolEntryAccept(constantInstruction.constantIndex, constantLookupVisitor);
            ContentT value = abstractState.pop();
            if (constantLookupVisitor.resultSize > 1) {
              abstractState.pop();
            }
            abstractState.setField(abstractState.pop(), constantLookupVisitor.result, value);
            break;
          }
        case Instruction.OP_INVOKESTATIC:
        case Instruction.OP_INVOKEDYNAMIC:
        case Instruction.OP_INVOKEVIRTUAL:
        case Instruction.OP_INVOKESPECIAL:
        case Instruction.OP_INVOKEINTERFACE:
          // this should be run just if call edges are missing for the CFA (incomplete call
          // analysis)
          // otherwise the information on the call edge should be used instead of performing a
          // partial call resolution here
          MethodSignature calleeSignature =
              CallResolver.quickResolve(constantInstruction, (ProgramClass) clazz);
          if (calleeSignature.equals(MethodSignature.UNKNOWN)) {
            throw new ProguardCoreException.Builder(
                    "Unexpected unknown signature",
                    ANALYSIS_JVM_TRANSFER_RELATION_UNEXPECTED_UNKNOWN_SIGNATURE)
                .build();
          }
          processCall(
              abstractState,
              new SymbolicCall(
                  new CodeLocation(clazz, method, offset),
                  calleeSignature,
                  Value.MAYBE,
                  constantInstruction,
                  false,
                  false));
          break;
        case Instruction.OP_INSTANCEOF:
          clazz.constantPoolEntryAccept(constantInstruction.constantIndex, constantLookupVisitor);
          abstractState.push(isInstanceOf(abstractState.pop(), constantLookupVisitor.result));
          break;
        case Instruction.OP_NEW:
          clazz.constantPoolEntryAccept(constantInstruction.constantIndex, constantLookupVisitor);
          abstractState.push(
              constantLookupVisitor.resultClazz != null
                  ? abstractState.newObject(constantLookupVisitor.resultClazz)
                  : abstractState.newObject(constantLookupVisitor.result));
          break;
        case Instruction.OP_NEWARRAY:
          abstractState.push(
              abstractState.newArray(
                  String.valueOf(
                      proguard.classfile.instruction.InstructionUtil.internalTypeFromArrayType(
                          (byte) constantInstruction.constant)),
                  Collections.singletonList(abstractState.pop())));
          break;
        case Instruction.OP_ANEWARRAY:
          clazz.constantPoolEntryAccept(constantInstruction.constantIndex, constantLookupVisitor);
          abstractState.push(
              abstractState.newArray(
                  constantLookupVisitor.result, Collections.singletonList(abstractState.pop())));
          break;
        case Instruction.OP_MULTIANEWARRAY:
          {
            List<ContentT> dimensions = new ArrayList<>(constantInstruction.constant);
            for (int i = 0; i < constantInstruction.stackPopCount(clazz); i++) {
              dimensions.add(abstractState.pop());
            }
            Collections.reverse(dimensions);
            clazz.constantPoolEntryAccept(constantInstruction.constantIndex, constantLookupVisitor);
            abstractState.push(abstractState.newArray(constantLookupVisitor.result, dimensions));
            break;
          }
        case Instruction.OP_CHECKCAST:
          clazz.constantPoolEntryAccept(constantInstruction.constantIndex, constantLookupVisitor);
          abstractState.push(handleCheckCast(abstractState.pop(), constantLookupVisitor.result));
          break;
        default: // should never happen
          throw new ProguardCoreException.Builder(
                  "The opcode %02X is not supported by the constant instruction visitor",
                  ANALYSIS_JVM_TRANSFER_RELATION_CONSTANT_INSTRUCTION_VISITOR_OPCODE_UNSUPPORTED)
              .errorParameters(constantInstruction.opcode)
              .build();
      }
    }

    @Override
    public void visitBranchInstruction(
        Clazz clazz,
        Method method,
        CodeAttribute codeAttribute,
        int offset,
        BranchInstruction branchInstruction) {
      for (int i = 0; i < branchInstruction.stackPopCount(clazz); i++) {
        abstractState.pop();
      }
      for (int i = 0; i < branchInstruction.stackPushCount(clazz); i++) {
        abstractState.push(getAbstractDefault());
      }
    }

    @Override
    public void visitAnySwitchInstruction(
        Clazz clazz,
        Method method,
        CodeAttribute codeAttribute,
        int offset,
        SwitchInstruction switchInstruction) {
      for (int i = 0; i < switchInstruction.stackPopCount(clazz); i++) {
        abstractState.pop();
      }
    }

    private class LdcConstantValueStatePusher implements ConstantVisitor {
      @Override
      public void visitAnyConstant(Clazz clazz, Constant constant) {
        abstractState.push(getAbstractDefault());
      }

      @Override
      public void visitLongConstant(Clazz clazz, LongConstant longConstant) {
        abstractState.pushAll(getAbstractLongConstant(longConstant.u8value));
      }

      @Override
      public void visitDoubleConstant(Clazz clazz, DoubleConstant doubleConstant) {
        abstractState.pushAll(getAbstractDoubleConstant(doubleConstant.f8value));
      }

      @Override
      public void visitIntegerConstant(Clazz clazz, IntegerConstant integerConstant) {
        abstractState.push(getAbstractIntegerConstant(integerConstant.u4value));
      }

      @Override
      public void visitFloatConstant(Clazz clazz, FloatConstant floatConstant) {
        abstractState.push(getAbstractFloatConstant(floatConstant.f4value));
      }

      @Override
      public void visitStringConstant(Clazz clazz, StringConstant stringConstant) {
        JvmCfaNode programLocation = abstractState.getProgramLocation();
        abstractState.push(
            getAbstractReferenceValue(
                TYPE_JAVA_LANG_STRING,
                stringConstant.javaLangStringClass,
                false,
                false,
                programLocation.getClazz(),
                programLocation.getSignature().getReferencedMethod(),
                programLocation.getOffset(),
                stringConstant.getString(clazz)));
      }

      @Override
      public void visitClassConstant(Clazz clazz, ClassConstant classConstant) {
        abstractState.push(
            getAbstractReferenceValue(
                internalTypeFromClassName(classConstant.getName(clazz)),
                classConstant.referencedClass,
                isExtendable(classConstant.referencedClass),
                true));
      }
    }
  }
}
