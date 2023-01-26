package proguard.analysis.cpa.jvm.domain.value;

import proguard.analysis.cpa.jvm.transfer.JvmTransferRelation;
import proguard.classfile.instruction.Instruction;
import proguard.evaluation.value.ReferenceValue;

import java.util.Collections;
import java.util.List;

/**
 * A {@link JvmTransferRelation} that tracks values.
 */
public class JvmValueTransferRelation extends JvmTransferRelation<JvmValueAbstractState>
{

    @Override
    public JvmValueAbstractState getAbstractDefault()
    {
        return JvmValueAbstractState.top;
    }

    @Override
    public JvmValueAbstractState getAbstractReferenceValue(ReferenceValue object)
    {
        return new JvmValueAbstractState(object);
    }

    @Override
    protected List<JvmValueAbstractState> applyArithmeticInstruction(Instruction instruction, List<JvmValueAbstractState> operands, int resultCount)
    {
        return Collections.nCopies(resultCount, getAbstractDefault());
    }
}
