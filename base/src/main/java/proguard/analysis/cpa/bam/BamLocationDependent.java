package proguard.analysis.cpa.bam;

import proguard.analysis.cpa.defaults.ProgramLocationDependentReachedSet;
import proguard.analysis.cpa.interfaces.AbstractState;
import proguard.analysis.cpa.interfaces.CfaEdge;
import proguard.analysis.cpa.interfaces.CfaNode;
import proguard.analysis.cpa.interfaces.ProgramLocationDependent;
import proguard.classfile.Signature;

/**
 * If the usage of an {@link AbstractState} depends on the specific BAM cache entry it belongs to, it should implement
 * {@link BamLocationDependent} to link it to its source reached set.
 *
 * @param <AbstractStateT> The type of the abstract states in the BAM cache.
 *
 * @author Carlo Alberto Pozzoli
 */
public interface BamLocationDependent<CfaNodeT extends CfaNode<CfaEdgeT, SignatureT>,
                                      CfaEdgeT extends CfaEdge<CfaNodeT>,
                                      AbstractStateT extends AbstractState & ProgramLocationDependent<CfaNodeT, CfaEdgeT, SignatureT>,
                                      SignatureT extends Signature>
{

    /**
     * Returns the reached set the abstract state belongs to.
     */
    ProgramLocationDependentReachedSet<CfaNodeT, CfaEdgeT, AbstractStateT, SignatureT> getSourceReachedSet();

    /**
     * Sets the reached set the abstract state belongs to.
     */
    void setSourceReachedSet(ProgramLocationDependentReachedSet<CfaNodeT, CfaEdgeT, AbstractStateT, SignatureT> sourceReachedSet);
}
