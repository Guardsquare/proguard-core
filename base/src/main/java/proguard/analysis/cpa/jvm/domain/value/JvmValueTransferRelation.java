package proguard.analysis.cpa.jvm.domain.value;

import static proguard.analysis.cpa.jvm.domain.value.ValueAbstractState.UNKNOWN;
import static proguard.classfile.TypeConstants.VOID;
import static proguard.classfile.util.ClassUtil.internalMethodReturnType;
import static proguard.classfile.util.ClassUtil.isInternalCategory2Type;

import java.util.Arrays;
import java.util.List;
import proguard.analysis.cpa.defaults.StackAbstractState;
import proguard.analysis.cpa.jvm.state.JvmAbstractState;
import proguard.analysis.cpa.jvm.transfer.JvmTransferRelation;
import proguard.analysis.datastructure.callgraph.Call;
import proguard.analysis.datastructure.callgraph.ConcreteCall;
import proguard.classfile.Clazz;
import proguard.classfile.Method;
import proguard.classfile.util.ClassUtil;
import proguard.classfile.visitor.ReturnClassExtractor;
import proguard.evaluation.ExecutingInvocationUnit;
import proguard.evaluation.value.IdentifiedReferenceValue;
import proguard.evaluation.value.TopValue;
import proguard.evaluation.value.Value;
import proguard.evaluation.value.ValueFactory;

/**
 * A {@link JvmTransferRelation} that tracks values.
 */
public class JvmValueTransferRelation extends JvmTransferRelation<ValueAbstractState>
{
    private final ValueFactory            valueFactory;
    public  final ExecutingInvocationUnit executingInvocationUnit;

    // Represents the dummy value that takes up the extra space when storing a long value or a
    // double value.
    private static final ValueAbstractState TOP_VALUE = new ValueAbstractState(new TopValue());

    public JvmValueTransferRelation(ValueFactory valueFactory, ExecutingInvocationUnit executingInvocationUnit)
    {
        this.valueFactory            = valueFactory;
        this.executingInvocationUnit = executingInvocationUnit;
    }

    public ValueFactory getValueFactory()
    {
        return this.valueFactory;
    }

    @Override
    public ValueAbstractState getAbstractDefault()
    {
        return UNKNOWN;
    }

    @Override
    public ValueAbstractState getAbstractByteConstant(byte b)
    {
        return new ValueAbstractState(valueFactory.createIntegerValue(b));
    }

    @Override
    public List<ValueAbstractState> getAbstractDoubleConstant(double d)
    {
        return Arrays.asList(
            TOP_VALUE,
            new ValueAbstractState(valueFactory.createDoubleValue(d))
        );

    }

    @Override
    public ValueAbstractState getAbstractFloatConstant(float f)
    {
        return new ValueAbstractState(valueFactory.createFloatValue(f));
    }

    @Override
    public ValueAbstractState getAbstractIntegerConstant(int i)
    {
        return new ValueAbstractState(valueFactory.createIntegerValue(i));
    }


    @Override
    public List<ValueAbstractState> getAbstractLongConstant(long l)
    {
        return Arrays.asList(
            TOP_VALUE,
            new ValueAbstractState(valueFactory.createLongValue(l))
        );
    }

    @Override
    public ValueAbstractState getAbstractNull()
    {
        return new ValueAbstractState(valueFactory.createReferenceValueNull());
    }

    @Override
    public ValueAbstractState getAbstractShortConstant(short s)
    {
        return new ValueAbstractState(valueFactory.createIntegerValue(s));
    }

    @Override
    public ValueAbstractState getAbstractReferenceValue(String className)
    {
        return getAbstractReferenceValue(className, null, true, true);
    }

    @Override
    public ValueAbstractState getAbstractReferenceValue(String className, Clazz referencedClazz, boolean mayBeExtension, boolean mayBeNull)
    {
        return new ValueAbstractState(valueFactory.createReferenceValue(className, referencedClazz, mayBeExtension, mayBeNull));
    }

    @Override
    public ValueAbstractState getAbstractReferenceValue(String className, Clazz referencedClazz, boolean mayBeExtension, boolean mayBeNull, Object value)
    {
        return new ValueAbstractState(valueFactory.createReferenceValue(className, referencedClazz, mayBeExtension, mayBeNull, value));
    }


    @Override
    public void invokeMethod(JvmAbstractState<ValueAbstractState> state, Call call, List<ValueAbstractState> operands)
    {
        if (call instanceof ConcreteCall)
        {
            Clazz  targetClass  = ((ConcreteCall) call).getTargetClass();
            Method targetMethod = ((ConcreteCall) call).getTargetMethod();

            if (executingInvocationUnit.isSupportedMethodCall(call.getTarget().getClassName(), call.getTarget().method))
            {
                // we can try to execute the method with reflection
                executeMethod(state,
                              operands,
                              targetClass,
                              targetMethod);
                return;
            }

            String returnType = call.getTarget().descriptor.returnType;
            String internalReturnClassName = ClassUtil.internalClassNameFromType(returnType);
            if (returnType != null
                && internalReturnClassName != null
                && executingInvocationUnit.isSupportedClass(internalReturnClassName))
            {
                // we can at most know the return type
                pushReturnTypedValue(state,
                                     operands,
                                     call,
                                     targetClass,
                                     targetMethod,
                                     returnType);
                return;
            }
        }

        super.invokeMethod(state, call, operands);
    }

    private void executeMethod(JvmAbstractState<ValueAbstractState> state,
                               List<ValueAbstractState>             operands,
                               Clazz                                targetClass,
                               Method                               targetMethod)
    {
        Value[] operandsArray = operands
            .stream()
            .map(ValueAbstractState::getValue)
            .toArray(Value[]::new);

        Value result = executingInvocationUnit.executeMethod(targetClass, targetMethod, operandsArray);
        String returnType = internalMethodReturnType(targetMethod.getDescriptor(targetClass));

        pushReturnValue(state, result, returnType);

        if (executingInvocationUnit.returnsOwnInstance(targetClass, targetMethod, operandsArray[0]))
        {
            updateStack(state, result, returnType);
            updateHeap(state, result);
        }
    }

    private void pushReturnTypedValue(JvmAbstractState<ValueAbstractState> state,
                                     List<ValueAbstractState>              operands,
                                     Call                                  call,
                                     Clazz                                 targetClass,
                                     Method                                targetMethod,
                                     String                                returnType)
    {
        ReturnClassExtractor returnClassExtractor = new ReturnClassExtractor();
        targetMethod.accept(targetClass, returnClassExtractor);
        if (returnClassExtractor.returnClass == null)
        {
            super.invokeMethod(state, call, operands);
        }
        else
        {
            Value result = valueFactory.createReferenceValue(ClassUtil.internalMethodReturnType(returnType),
                                                             returnClassExtractor.returnClass,
                                                             ClassUtil.isNullOrFinal(returnClassExtractor.returnClass),
                                                             true);
            pushReturnValue(state, result, returnType);
        }
    }

    private void pushReturnValue(JvmAbstractState<ValueAbstractState> state, Value result, String returnType)
    {
        if (!isVoidReturnType(returnType))
        {
            if (isInternalCategory2Type(returnType))
            {
                state.push(TOP_VALUE);
            }
            state.push(new ValueAbstractState(result));
        }
    }

    private void updateStack(JvmAbstractState<ValueAbstractState> state, Value result, String returnType)
    {
        if (!(result instanceof IdentifiedReferenceValue)) return;

        IdentifiedReferenceValue identifiedReferenceValue   = (IdentifiedReferenceValue) result;
        StackAbstractState<ValueAbstractState> operandStack = state.getFrame().getOperandStack();

        int start = isVoidReturnType(returnType) ?
                operandStack.size() - 1 :
                // If we just pushed something, no need to update it.
                operandStack.size() - 2;

        for (int i = start; i >= 0; i--)
        {
            ValueAbstractState stackEntry   = operandStack.get(i);
            Value              valueOnStack = stackEntry.getValue();
            if (valueOnStack instanceof IdentifiedReferenceValue &&
                ((IdentifiedReferenceValue) valueOnStack).id == identifiedReferenceValue.id)
            {
                stackEntry.setValue(identifiedReferenceValue);
            }
        }
    }

    private boolean isVoidReturnType(String returnType)
    {
        return returnType.equals(String.valueOf(VOID));
    }

    private void updateHeap(JvmAbstractState<ValueAbstractState> state, Value result)
    {
        if (!(result instanceof IdentifiedReferenceValue))
        {
            return;
        }

        IdentifiedReferenceValue identifiedReferenceValue = (IdentifiedReferenceValue) result;
        state.setField(identifiedReferenceValue.id, new ValueAbstractState(identifiedReferenceValue));
    }
}
