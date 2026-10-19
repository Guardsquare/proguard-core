package proguard.analysis.cpa.jvm.domain.taint;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import proguard.analysis.cpa.defaults.ListAbstractState;
import proguard.analysis.cpa.defaults.SetAbstractState;
import proguard.analysis.cpa.defaults.StackAbstractState;
import proguard.analysis.cpa.domain.taint.TaintSource;
import proguard.analysis.cpa.interfaces.Precision;
import proguard.analysis.cpa.jvm.state.JvmAbstractState;
import proguard.analysis.cpa.jvm.transfer.JvmTransferRelation;
import proguard.analysis.cpa.jvm.witness.JvmLocalVariableLocation;
import proguard.analysis.cpa.jvm.witness.JvmMemoryLocation;
import proguard.analysis.cpa.jvm.witness.JvmStackLocation;
import proguard.analysis.datastructure.callgraph.Call;
import proguard.classfile.Clazz;
import proguard.classfile.Method;
import proguard.classfile.MethodSignature;
import proguard.classfile.Signature;
import proguard.classfile.attribute.CodeAttribute;
import proguard.classfile.instruction.ConstantInstruction;
import proguard.classfile.instruction.Instruction;
import proguard.classfile.instruction.SimpleInstruction;
import proguard.classfile.util.ClassUtil;

/** The {@link JvmTaintTransferRelation} is parametrized by a set of {@link TaintSource} methods. */
public class JvmTaintTransferRelation extends JvmTransferRelation<SetAbstractState<TaintSource>> {

  private final Logger log = LogManager.getLogger(JvmTaintTransferRelation.class);

  private final Map<Signature, Set<TaintSource>> taintSources;
  private final Map<MethodSignature, JvmTaintTransformer> taintTransformers;

  /**
   * Maps calls to locations which should become tainted after the call is invoked. For example, it
   * maps 'init' calls to the locations in local variables array or on stack, to which the value was
   * put after duplicating the constructed instance.
   */
  private final Map<Call, Set<JvmMemoryLocation>> extraTaintPropagationLocations;

  /**
   * Create a taint transfer relation.
   *
   * @param taintSources a mapping from fully qualified names to taint sources
   */
  public JvmTaintTransferRelation(Map<Signature, Set<TaintSource>> taintSources) {
    this(taintSources, Collections.emptyMap(), Collections.emptyMap());
  }

  public JvmTaintTransferRelation(
      Map<Signature, Set<TaintSource>> taintSources,
      Map<MethodSignature, JvmTaintTransformer> taintTransformers,
      Map<Call, Set<JvmMemoryLocation>> extraTaintPropagationLocations) {
    this.taintSources = taintSources;
    this.taintTransformers = taintTransformers;
    this.extraTaintPropagationLocations = extraTaintPropagationLocations;
  }

  // implementations for JvmTransferRelation

  @Override
  public void invokeMethod(
      JvmAbstractState<SetAbstractState<TaintSource>> state,
      Call call,
      List<SetAbstractState<TaintSource>> operands) {

    MethodSignature target = call.getTarget();
    List<JvmMethodTaintSource> detectedSources =
        taintSources.getOrDefault(target, Collections.emptySet()).stream()
            .filter(JvmMethodTaintSource.class::isInstance)
            .map(JvmMethodTaintSource.class::cast)
            .filter(s -> s.callMatcher.map(m -> m.test(call)).orElse(true))
            .collect(Collectors.toList());
    int pushCount =
        ClassUtil.internalTypeSize(
            target.descriptor.returnType == null ? "?" : target.descriptor.returnType);
    SetAbstractState<TaintSource> answerContent =
        operands.stream().reduce(getAbstractDefault(), SetAbstractState::join);

    // taint the return
    List<JvmMethodTaintSource> detectedReturnSources =
        detectedSources.stream().filter(s -> s.getTaintsReturn()).collect(Collectors.toList());
    if (!detectedReturnSources.isEmpty() && !answerContent.containsAll(detectedReturnSources)) {
      answerContent = answerContent.copy();
      answerContent.addAll(detectedReturnSources);
    }

    propagateExtraTaints(call, state, answerContent);

    // pad to the return type size and put the abstract state on the top of the stack
    for (int i = 1; i < pushCount; i++) {
      state.push(getAbstractDefault());
    }
    if (pushCount > 0) {
      if (taintTransformers.containsKey(target)) {
        answerContent = taintTransformers.get(target).transformReturn(answerContent.copy());
      }
      state.push(answerContent);
    }

    // taint static fields
    if (detectedSources.isEmpty()) {
      return;
    }
    Map<String, SetAbstractState<TaintSource>> fqnToValue = new HashMap<>();
    detectedSources.stream()
        .filter(s -> !s.getTaintGlobals().isEmpty())
        .forEach(
            s -> {
              SetAbstractState<TaintSource> newValue = new SetAbstractState<>(s);
              s.getTaintGlobals()
                  .forEach(fqn -> fqnToValue.merge(fqn, newValue, SetAbstractState::join));
            });
    fqnToValue.forEach((fqn, value) -> state.setStatic(fqn, value, getAbstractDefault()));
  }

  @Override
  public SetAbstractState<TaintSource> getAbstractDefault() {
    return SetAbstractState.bottom();
  }

  @Override
  protected JvmAbstractState<SetAbstractState<TaintSource>> getAbstractSuccessorForInstruction(
      JvmAbstractState<SetAbstractState<TaintSource>> abstractState,
      Instruction instruction,
      Clazz clazz,
      Precision precision) {
    instruction.accept(clazz, null, null, 0, new InstructionAbstractInterpreter(abstractState));
    return abstractState;
  }

  protected class InstructionAbstractInterpreter
      extends JvmTransferRelation<SetAbstractState<TaintSource>>.InstructionAbstractInterpreter {

    public InstructionAbstractInterpreter(
        JvmAbstractState<SetAbstractState<TaintSource>> abstractState) {
      super(abstractState);
    }

    @Override
    public void visitSimpleInstruction(
        Clazz clazz,
        Method method,
        CodeAttribute codeAttribute,
        int offset,
        SimpleInstruction simpleInstruction) {
      switch (simpleInstruction.opcode) {
        case Instruction.OP_IALOAD:
        case Instruction.OP_FALOAD:
        case Instruction.OP_AALOAD:
        case Instruction.OP_BALOAD:
        case Instruction.OP_CALOAD:
        case Instruction.OP_SALOAD:
          {
            SetAbstractState<TaintSource> index = abstractState.pop();
            abstractState.push(
                abstractState.getArrayElementOrDefault(
                    new JvmStackLocation(simpleInstruction.stackPopCount(clazz) - 1),
                    index,
                    abstractState.pop()));
            break;
          }
        case Instruction.OP_LALOAD:
        case Instruction.OP_DALOAD:
          {
            SetAbstractState<TaintSource> index = abstractState.pop();
            abstractState.push(getAbstractDefault());
            abstractState.push(
                abstractState.getArrayElementOrDefault(
                    new JvmStackLocation(simpleInstruction.stackPopCount(clazz) - 1),
                    index,
                    abstractState.pop()));
            break;
          }
        case Instruction.OP_IASTORE:
        case Instruction.OP_FASTORE:
        case Instruction.OP_AASTORE:
        case Instruction.OP_BASTORE:
        case Instruction.OP_CASTORE:
        case Instruction.OP_SASTORE:
          {
            SetAbstractState<TaintSource> value = abstractState.pop();
            SetAbstractState<TaintSource> index = abstractState.pop();
            abstractState.pop();
            abstractState.setArrayElement(
                new JvmStackLocation(simpleInstruction.stackPopCount(clazz) - 1), index, value);
            break;
          }
        case Instruction.OP_LASTORE:
        case Instruction.OP_DASTORE:
          {
            SetAbstractState<TaintSource> value = abstractState.pop();
            abstractState.pop();
            SetAbstractState<TaintSource> index = abstractState.pop();
            abstractState.pop();
            abstractState.setArrayElement(
                new JvmStackLocation(simpleInstruction.stackPopCount(clazz) - 1), index, value);
            break;
          }
        default:
          super.visitSimpleInstruction(clazz, method, codeAttribute, offset, simpleInstruction);
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
        case Instruction.OP_GETFIELD:
          {
            constantLookupVisitor.isStatic = false;
            clazz.constantPoolEntryAccept(constantInstruction.constantIndex, constantLookupVisitor);
            SetAbstractState<TaintSource> result =
                abstractState.getFieldOrDefault(
                    new JvmStackLocation(constantInstruction.stackPopCount(clazz) - 1),
                    constantLookupVisitor.result,
                    abstractState.pop());
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
            SetAbstractState<TaintSource> value = abstractState.pop();
            if (constantLookupVisitor.resultSize > 1) {
              abstractState.pop();
            }
            abstractState.pop();
            abstractState.setField(
                new JvmStackLocation(constantInstruction.stackPopCount(clazz) - 1),
                constantLookupVisitor.result,
                value);
            break;
          }
        case Instruction.OP_GETSTATIC:
          {
            constantLookupVisitor.isStatic = true;
            clazz.constantPoolEntryAccept(constantInstruction.constantIndex, constantLookupVisitor);
            SetAbstractState<TaintSource> result =
                abstractState.getStaticOrDefault(
                    constantLookupVisitor.result, getAbstractDefault());
            Set<TaintSource> fieldSources =
                taintSources.get(constantLookupVisitor.resultFieldSignature);
            if (fieldSources != null && !fieldSources.isEmpty()) {
              result = result.copy();
              result.addAll(fieldSources);
            }
            if (constantLookupVisitor.resultSize > 1) {
              abstractState.push(getAbstractDefault());
            }
            abstractState.push(result);
            break;
          }
        default:
          super.visitConstantInstruction(clazz, method, codeAttribute, offset, constantInstruction);
      }
    }
  }

  /** If relevant, taints locations which should become tainted after the call is invoked. */
  private void propagateExtraTaints(
      Call call,
      JvmAbstractState<SetAbstractState<TaintSource>> state,
      SetAbstractState<TaintSource> answerContent) {
    if (extraTaintPropagationLocations.containsKey(call)) {
      for (JvmMemoryLocation location : extraTaintPropagationLocations.get(call)) {
        if (location instanceof JvmLocalVariableLocation) {
          int index = ((JvmLocalVariableLocation) location).index;
          ListAbstractState<SetAbstractState<TaintSource>> localVariables =
              state.getFrame().getLocalVariables();
          SetAbstractState<TaintSource> newState =
              localVariables.getOrDefault(index, SetAbstractState.bottom()).join(answerContent);
          localVariables.set(index, newState, SetAbstractState.bottom());
        } else if (location instanceof JvmStackLocation) {
          StackAbstractState<SetAbstractState<TaintSource>> stack =
              state.getFrame().getOperandStack();
          // NB JvmStackLocation counts from the top of the stack
          int index = stack.size() - 1 - ((JvmStackLocation) location).index;
          if (index < 0) {
            log.warn(
                "Stack index < 0 in for call {}, is an extra taint propagator for the call misconfigured?",
                state.getProgramLocation().getSignature());
            return;
          }
          stack.set(index, stack.get(index).join(answerContent));
        }
      }
    }
  }
}
