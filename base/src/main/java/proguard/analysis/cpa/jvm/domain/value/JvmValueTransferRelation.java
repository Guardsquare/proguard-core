package proguard.analysis.cpa.jvm.domain.value;

import proguard.analysis.cpa.jvm.transfer.JvmTransferRelation;
import proguard.classfile.Clazz;
import proguard.classfile.instruction.Instruction;
import proguard.evaluation.ExecutingInvocationUnit;
import proguard.evaluation.value.TopValue;
import proguard.evaluation.value.ValueFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
        return JvmValueAbstractState.top;
    }

    @Override
    public JvmValueAbstractState getAbstractByteConstant(byte b)
    {
        return new JvmValueAbstractState(valueFactory.createIntegerValue(b));
    }

    @Override
    public List<JvmValueAbstractState> getAbstractDoubleConstant(double d)
    {
        List<JvmValueAbstractState> result = new ArrayList<>(2);
        result.add(new JvmValueAbstractState(valueFactory.createDoubleValue(d)));
        result.add(new JvmValueAbstractState(TOP_VALUE));
        return result;
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
        List<JvmValueAbstractState> result = new ArrayList<>(2);
        result.add(new JvmValueAbstractState(valueFactory.createLongValue(l)));
        result.add(new JvmValueAbstractState(TOP_VALUE));
        return result;
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

    @Override
    protected List<JvmValueAbstractState> applyArithmeticInstruction(Instruction instruction, List<JvmValueAbstractState> operands, int resultCount)
    {
        return Collections.nCopies(resultCount, getAbstractDefault());
    }
}
