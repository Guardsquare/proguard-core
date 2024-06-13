package proguard.analysis.cpa.jvm.domain.value;

import static proguard.analysis.cpa.jvm.domain.value.ValueAbstractState.UNKNOWN;
import static proguard.classfile.TypeConstants.VOID;
import static proguard.classfile.util.ClassUtil.internalMethodReturnType;
import static proguard.classfile.util.ClassUtil.isInternalCategory2Type;

import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import proguard.analysis.cpa.defaults.StackAbstractState;
import proguard.analysis.cpa.interfaces.AbstractState;
import proguard.analysis.cpa.interfaces.Precision;
import proguard.analysis.cpa.jvm.cfa.edges.JvmCfaEdge;
import proguard.analysis.cpa.jvm.state.JvmAbstractState;
import proguard.analysis.cpa.jvm.transfer.JvmTransferRelation;
import proguard.analysis.datastructure.CodeLocation;
import proguard.analysis.datastructure.callgraph.Call;
import proguard.analysis.datastructure.callgraph.ConcreteCall;
import proguard.classfile.Clazz;
import proguard.classfile.Method;
import proguard.classfile.util.ClassUtil;
import proguard.classfile.visitor.ReferencedClassesExtractor;
import proguard.evaluation.ExecutingInvocationUnit;
import proguard.evaluation.MethodResult;
import proguard.evaluation.value.IdentifiedReferenceValue;
import proguard.evaluation.value.TopValue;
import proguard.evaluation.value.TypedReferenceValue;
import proguard.evaluation.value.Value;
import proguard.evaluation.value.ValueFactory;
import proguard.evaluation.value.object.AnalyzedObjectFactory;

/** A {@link JvmTransferRelation} that tracks values. */
public class JvmValueTransferRelation extends JvmTransferRelation<ValueAbstractState> {
  private final ValueFactory valueFactory;
  public final ExecutingInvocationUnit executingInvocationUnit;

  // Represents the dummy value that takes up the extra space when storing a long value or a
  // double value.
  private static final ValueAbstractState TOP_VALUE = new ValueAbstractState(new TopValue());

  public JvmValueTransferRelation(
      ValueFactory valueFactory, ExecutingInvocationUnit executingInvocationUnit) {
    this.valueFactory = valueFactory;
    this.executingInvocationUnit = executingInvocationUnit;
  }

  public ValueFactory getValueFactory() {
    return this.valueFactory;
  }

  @Override
  public ValueAbstractState getAbstractDefault() {
    return UNKNOWN;
  }

  @Override
  public ValueAbstractState getAbstractByteConstant(byte b) {
    return new ValueAbstractState(valueFactory.createIntegerValue(b));
  }

  @Override
  public List<ValueAbstractState> getAbstractDoubleConstant(double d) {
    return Arrays.asList(TOP_VALUE, new ValueAbstractState(valueFactory.createDoubleValue(d)));
  }

  @Override
  public ValueAbstractState getAbstractFloatConstant(float f) {
    return new ValueAbstractState(valueFactory.createFloatValue(f));
  }

  @Override
  public ValueAbstractState getAbstractIntegerConstant(int i) {
    return new ValueAbstractState(valueFactory.createIntegerValue(i));
  }

  @Override
  public List<ValueAbstractState> getAbstractLongConstant(long l) {
    return Arrays.asList(TOP_VALUE, new ValueAbstractState(valueFactory.createLongValue(l)));
  }

  @Override
  public ValueAbstractState getAbstractNull() {
    return new ValueAbstractState(valueFactory.createReferenceValueNull());
  }

  @Override
  public ValueAbstractState getAbstractShortConstant(short s) {
    return new ValueAbstractState(valueFactory.createIntegerValue(s));
  }

  @Override
  public ValueAbstractState getAbstractReferenceValue(String className) {
    return getAbstractReferenceValue(className, null, true, true);
  }

  @Override
  public ValueAbstractState getAbstractReferenceValue(
      String internalType, Clazz referencedClazz, boolean mayBeExtension, boolean mayBeNull) {
    return new ValueAbstractState(
        valueFactory.createReferenceValue(
            internalType, referencedClazz, mayBeExtension, mayBeNull));
  }

  @Override
  public ValueAbstractState getAbstractReferenceValue(
      String internalType,
      Clazz referencedClazz,
      boolean mayBeExtension,
      boolean mayBeNull,
      Clazz creationClass,
      Method creationMethod,
      int creationOffset,
      Object value) {
    return new ValueAbstractState(
        valueFactory.createReferenceValue(
            referencedClazz,
            mayBeExtension,
            mayBeNull,
            new CodeLocation(creationClass, creationMethod, creationOffset),
            AnalyzedObjectFactory.create(value, internalType, referencedClazz)));
  }

  @Override
  protected void processCall(JvmAbstractState<ValueAbstractState> state, Call call) {
    Deque<ValueAbstractState> operands = new LinkedList<>();
    if (call.getTarget().descriptor.argumentTypes != null) {
      List<String> argumentTypes = call.getTarget().descriptor.argumentTypes;
      for (int i = argumentTypes.size() - 1; i >= 0; i--) {
        boolean isCategory2 = ClassUtil.isInternalCategory2Type(argumentTypes.get(i));
        operands.offerFirst(state.pop());
        if (isCategory2) {
          // ExecutingInvocationUnit expects a single parameter for category 2 values
          state.pop();
        }
      }
    }
    if (!call.isStatic()) {
      operands.offerFirst(state.pop());
    }
    invokeMethod(state, call, (List<ValueAbstractState>) operands);
  }

  @Override
  public void invokeMethod(
      JvmAbstractState<ValueAbstractState> state, Call call, List<ValueAbstractState> operands) {
    if (call instanceof ConcreteCall) {
      if (executingInvocationUnit.canExecute(call.getTarget())) {
        // we can try to execute the method with reflection
        executeMethod((ConcreteCall) call, state, operands);
        return;
      }

      String returnType = call.getTarget().descriptor.returnType;
      String internalReturnClassName = ClassUtil.internalClassNameFromType(returnType);
      if (returnType != null
          && internalReturnClassName != null
          && executingInvocationUnit.supportsInstancesOf(internalReturnClassName)) {
        // we can at most know the return type
        pushReturnTypedValue(state, operands, (ConcreteCall) call, returnType);
        return;
      }
    }

    super.invokeMethod(state, call, operands);
  }

  @Override
  protected ValueAbstractState handleCheckCast(ValueAbstractState state, String internalName) {
    Value value = state.getValue();

    // JVM spec demands that the operand of checkcast is a reference value, and we can't make
    // a decision without type information. Note that this excludes arrays, as they are currently
    // generally not supported in value analysis.
    if (!(value instanceof TypedReferenceValue)) {
      return getAbstractDefault();
    }

    // By the JVM spec, if objectref is null, the stack is left unchanged
    TypedReferenceValue typedValue = (TypedReferenceValue) value;
    if (typedValue.isParticular() && typedValue.getValue().isNull()) {
      return state;
    }

    boolean castSuccess;
    Clazz referencedClass = typedValue.getReferencedClass();
    if (referencedClass == null) {
      // Fallback to type string comparison
      String typeName = ClassUtil.internalTypeFromClassName(internalName);
      castSuccess = Objects.equals(typeName, typedValue.getType());
    } else {
      castSuccess = referencedClass.extendsOrImplements(internalName);
    }

    return castSuccess ? state : getAbstractDefault();
  }

  // Private methods

  private void executeMethod(
      ConcreteCall call,
      JvmAbstractState<ValueAbstractState> state,
      List<ValueAbstractState> operands) {
    Clazz targetClass = call.getTargetClass();
    Method targetMethod = call.getTargetMethod();

    Value[] operandsArray =
        operands.stream().map(ValueAbstractState::getValue).toArray(Value[]::new);

    if (operandsArray.length
        != ClassUtil.internalMethodParameterCount(
            call.getTarget().descriptor.toString(), call.isStatic())) {
      throw new IllegalStateException("Unexpected number of parameters");
    }

    MethodResult result = null;

    if (!call.isStatic() && UNKNOWN.getValue().equals(operandsArray[0])) {
      result = MethodResult.invalidResult();
    } else {
      result = executingInvocationUnit.executeMethod(call, operandsArray);
    }

    String returnType = internalMethodReturnType(targetMethod.getDescriptor(targetClass));
    if (!isVoidReturnType(returnType)) {
      Value returnValue =
          result.isReturnValuePresent() ? result.getReturnValue() : UNKNOWN.getValue();
      pushReturnValue(state, returnValue, returnType);
    }

    if (result.isInstanceUpdated()) {
      updateStack(state, result.getUpdatedInstance(), returnType);
      updateHeap(state, result.getUpdatedInstance());
    }
  }

  private void pushReturnTypedValue(
      JvmAbstractState<ValueAbstractState> state,
      List<ValueAbstractState> operands,
      ConcreteCall call,
      String returnType) {
    ReferencedClassesExtractor referencedClassesExtractor = new ReferencedClassesExtractor();
    call.targetMethodAccept(referencedClassesExtractor);
    if (referencedClassesExtractor.getReturnClass() == null) {
      super.invokeMethod(state, call, operands);
    } else {
      Value result =
          valueFactory.createReferenceValue(
              ClassUtil.internalMethodReturnType(returnType),
              referencedClassesExtractor.getReturnClass(),
              ClassUtil.isExtendable(referencedClassesExtractor.getReturnClass()),
              true);
      pushReturnValue(state, result, returnType);
    }
  }

  private void pushReturnValue(
      JvmAbstractState<ValueAbstractState> state, Value result, String returnType) {
    if (isInternalCategory2Type(returnType)) {
      state.push(TOP_VALUE);
    }
    state.push(new ValueAbstractState(result));
  }

  private void updateStack(
      JvmAbstractState<ValueAbstractState> state, Value result, String returnType) {
    if (!(result instanceof IdentifiedReferenceValue)) {
      return;
    }

    IdentifiedReferenceValue identifiedReferenceValue = (IdentifiedReferenceValue) result;
    StackAbstractState<ValueAbstractState> operandStack = state.getFrame().getOperandStack();

    int start =
        isVoidReturnType(returnType)
            ? operandStack.size() - 1
            :
            // If we just pushed something, no need to update it.
            operandStack.size() - 2;

    for (int i = start; i >= 0; i--) {
      ValueAbstractState stackEntry = operandStack.get(i);
      Value valueOnStack = stackEntry.getValue();
      if (valueOnStack instanceof IdentifiedReferenceValue
          && ((IdentifiedReferenceValue) valueOnStack).id.equals(identifiedReferenceValue.id)) {
        stackEntry.setValue(identifiedReferenceValue);
      }
    }
  }

  private boolean isVoidReturnType(String returnType) {
    return returnType.equals(String.valueOf(VOID));
  }

  private void updateHeap(JvmAbstractState<ValueAbstractState> state, Value result) {
    if (!(result instanceof IdentifiedReferenceValue)) {
      return;
    }

    IdentifiedReferenceValue identifiedReferenceValue = (IdentifiedReferenceValue) result;
    state.setField(identifiedReferenceValue.id, new ValueAbstractState(identifiedReferenceValue));
  }

  @Override
  public Collection<? extends AbstractState> generateEdgeAbstractSuccessors(
      AbstractState abstractState, JvmCfaEdge edge, Precision precision) {
    return wrapAbstractSuccessorInCollection(
        generateEdgeAbstractSuccessor(abstractState, edge, precision));
  }
}
