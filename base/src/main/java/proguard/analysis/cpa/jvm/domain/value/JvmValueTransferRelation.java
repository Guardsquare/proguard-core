package proguard.analysis.cpa.jvm.domain.value;

import proguard.analysis.cpa.defaults.MapAbstractState;
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode;
import proguard.analysis.cpa.jvm.state.JvmAbstractState;
import proguard.analysis.cpa.jvm.state.heap.JvmHeapAbstractState;
import proguard.analysis.cpa.jvm.state.heap.tree.JvmShallowHeapAbstractState;
import proguard.analysis.cpa.jvm.transfer.JvmTransferRelation;
import proguard.analysis.datastructure.callgraph.Call;
import proguard.analysis.datastructure.callgraph.ConcreteCall;
import proguard.classfile.Clazz;
import proguard.classfile.Method;
import proguard.evaluation.ExecutingInvocationUnit;
import proguard.evaluation.value.IdentifiedReferenceValue;
import proguard.evaluation.value.TopValue;
import proguard.evaluation.value.Value;
import proguard.evaluation.value.ValueFactory;

import java.util.Arrays;
import java.util.List;

import static proguard.analysis.cpa.jvm.domain.value.JvmValueAbstractState.UNKNOWN;
import static proguard.analysis.cpa.jvm.state.heap.JvmHeapAbstractState.FAKE_FIELD;
import static proguard.classfile.TypeConstants.VOID;
import static proguard.classfile.util.ClassUtil.internalMethodReturnType;
import static proguard.evaluation.value.ParticularReferenceValue.UNINITIALIZED;

/**
 * A {@link JvmTransferRelation} that tracks values.
 */
public class JvmValueTransferRelation extends JvmTransferRelation<JvmValueAbstractState>
{
    private final ValueFactory            valueFactory;
    public  final ExecutingInvocationUnit executingInvocationUnit;

    private static final TopValue TOP_VALUE = new TopValue();

    public JvmValueTransferRelation(ValueFactory valueFactory)
    {
        this.valueFactory            = valueFactory;
        this.executingInvocationUnit = new ExecutingInvocationUnit(valueFactory);
    }

    public ValueFactory getValueFactory()
    {
        return this.valueFactory;
    }

    @Override
    public JvmValueAbstractState getAbstractDefault()
    {
        return UNKNOWN;
    }

    @Override
    public JvmValueAbstractState getAbstractByteConstant(byte b)
    {
        return new JvmValueAbstractState(valueFactory.createIntegerValue(b));
    }

    @Override
    public List<JvmValueAbstractState> getAbstractDoubleConstant(double d)
    {
        return Arrays.asList(
            new JvmValueAbstractState(valueFactory.createDoubleValue(d)),
            new JvmValueAbstractState(TOP_VALUE)
        );

    }

    @Override
    public JvmValueAbstractState getAbstractFloatConstant(float f)
    {
        return new JvmValueAbstractState(valueFactory.createFloatValue(f));
    }

    @Override
    public JvmValueAbstractState getAbstractIntegerConstant(int i)
    {
        return new JvmValueAbstractState(valueFactory.createIntegerValue(i));
    }


    @Override
    public List<JvmValueAbstractState> getAbstractLongConstant(long l)
    {
        return Arrays.asList(
            new JvmValueAbstractState(valueFactory.createLongValue(l)),
            new JvmValueAbstractState(TOP_VALUE)
        );
    }

    @Override
    public JvmValueAbstractState getAbstractNull()
    {
        return new JvmValueAbstractState(valueFactory.createReferenceValueNull());
    }

    @Override
    public JvmValueAbstractState getAbstractShortConstant(short s)
    {
        return new JvmValueAbstractState(valueFactory.createIntegerValue(s));
    }

    @Override
    public JvmValueAbstractState getAbstractReferenceValue(String className)
    {
        return getAbstractReferenceValue(className, null, true, true);
    }

    @Override
    public JvmValueAbstractState getAbstractReferenceValue(String className, Clazz referencedClazz, boolean mayBeExtension, boolean mayBeNull)
    {
        return new JvmValueAbstractState(valueFactory.createReferenceValue(className, referencedClazz, mayBeExtension, mayBeNull));
    }

    @Override
    public JvmValueAbstractState getAbstractReferenceValue(String className, Clazz referencedClazz, boolean mayBeExtension, boolean mayBeNull, Object value)
    {
        return new JvmValueAbstractState(valueFactory.createReferenceValue(className, referencedClazz, mayBeExtension, mayBeNull, value));
    }


/*    @Override
    protected List<JvmValueAbstractState> applyArithmeticInstruction(Instruction instruction, List<JvmValueAbstractState> operands, int resultCount)
    {
        return Collections.nCopies(resultCount, getAbstractDefault());
    }*/

    @Override
    public void invokeMethod(JvmAbstractState<JvmValueAbstractState> state, Call call, List<JvmValueAbstractState> operands)
    {
        if (call instanceof ConcreteCall &&
            executingInvocationUnit.isSupportedMethodCall(call.getTarget().getClassName(), call.getTarget().method))
        {
            Clazz  targetClass  = ((ConcreteCall) call).getTargetClass();
            Method targetMethod = ((ConcreteCall) call).getTargetMethod();

            Value[] operandsArray = operands
                    .stream()
                    .map(it -> getValue(state, it))
                    .toArray(Value[]::new);

            Value result = executingInvocationUnit.executeMethod(targetClass, targetMethod, operandsArray);

            boolean isVoidReturnType = internalMethodReturnType(targetMethod.getDescriptor(targetClass)).equals(String.valueOf(VOID));

            if (!isVoidReturnType)
            {
                state.push(new JvmValueAbstractState(result));
            }

            setValueInHeap(state, result);
        }
        else
        {
            super.invokeMethod(state, call, operands);
        }
    }

    private static void setValueInHeap(JvmAbstractState<JvmValueAbstractState> state, Value result)
    {
        if (result instanceof IdentifiedReferenceValue)
        {
            IdentifiedReferenceValue identifiedReferenceValue = (IdentifiedReferenceValue) result;
            state.getHeap().setField(identifiedReferenceValue.id, FAKE_FIELD, new JvmValueAbstractState(identifiedReferenceValue));
        }
    }

    private static Value getValue(JvmAbstractState<JvmValueAbstractState> state, JvmValueAbstractState it)
    {
        if (it.getValue() instanceof IdentifiedReferenceValue)
        {
            int objectId = ((IdentifiedReferenceValue) it.getValue()).id;
            return state
                    .getHeap()
                    .getFieldOrDefault(objectId, FAKE_FIELD, new JvmValueAbstractState(it.getValue()))
                    .getValue();
       }
       else
       {
            return it.getValue();
       }
    }


    public static final class JvmValueHeapModel extends JvmShallowHeapAbstractState<Integer, JvmValueAbstractState>
    {

        private final ValueFactory valueFactory;

        /**
         * Create a shallow heap abstract state returning the specified value for all queries from an existing reference to abstract state map.
         *
         * @param referenceToObject the value to be returned by memory accesses
         * @param referenceClass    the class of the reference used for addressing
         * @param defaultValue      the value to be returned by memory accesses
         */
        public JvmValueHeapModel(ValueFactory valueFactory, MapAbstractState<Integer, JvmValueAbstractState> referenceToObject, Class<Integer> referenceClass, JvmValueAbstractState defaultValue)
        {
            super(referenceToObject, referenceClass, defaultValue);
            this.valueFactory = valueFactory;
        }

        @Override
        public JvmValueAbstractState newObject(Clazz clazz, JvmCfaNode creationCite)
        {
            IdentifiedReferenceValue value = (IdentifiedReferenceValue) valueFactory.createReferenceValue(clazz, UNINITIALIZED);
            JvmValueAbstractState jvmValueAbstractState = new JvmValueAbstractState(value);
            setField(value.id, FAKE_FIELD, jvmValueAbstractState);
            return jvmValueAbstractState;
        }

        @Override
        public JvmShallowHeapAbstractState<Integer, JvmValueAbstractState> join(JvmHeapAbstractState<JvmValueAbstractState> abstractState)
        {
            JvmShallowHeapAbstractState<Integer, JvmValueAbstractState> other = (JvmShallowHeapAbstractState<Integer, JvmValueAbstractState>) abstractState;
            MapAbstractState<Integer, JvmValueAbstractState> newReferenceToState = referenceToObject.join(other.referenceToObject);
            if (referenceToObject == newReferenceToState)
            {
                return this;
            }
            if (other.referenceToObject == newReferenceToState)
            {
                return other;
            }
            return new JvmValueHeapModel(valueFactory, newReferenceToState, referenceClass, defaultValue);
        }


        @Override
        public JvmShallowHeapAbstractState<Integer, JvmValueAbstractState> copy()
        {
            return new JvmValueHeapModel(valueFactory, referenceToObject.copy(), referenceClass, defaultValue);
        }
    }
}
