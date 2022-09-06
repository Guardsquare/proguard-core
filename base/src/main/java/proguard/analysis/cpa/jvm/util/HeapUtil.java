package proguard.analysis.cpa.jvm.util;

import proguard.analysis.cpa.defaults.SetAbstractState;
import proguard.analysis.cpa.jvm.domain.reference.Reference;
import proguard.analysis.cpa.jvm.domain.taint.JvmTaintTreeHeapFollowerAbstractState;
import proguard.analysis.cpa.jvm.witness.JvmStackLocation;
import proguard.classfile.util.ClassUtil;

/**
 * A class with utility methods for the {@link proguard.analysis.cpa.jvm.state.heap.tree.JvmTreeHeapAbstractState}.
 *
 * @author Dmitry Ivanov
 */
public class HeapUtil
{

    /**
     * Retrieves the argument reference from the principal state.
     */
    public static SetAbstractState<Reference> getArgumentReference(JvmTaintTreeHeapFollowerAbstractState expandedHeap,
                                                                   int parameterSize,
                                                                   String fqn,
                                                                   boolean isStatic,
                                                                   int index)
    {
        return expandedHeap.getReferenceAbstractState(new JvmStackLocation(parameterSize
                                                                           - ClassUtil.internalMethodVariableIndex(fqn,
                                                                                                                   isStatic,
                                                                                                                   index)
                                                                           - 1));
    }
}
