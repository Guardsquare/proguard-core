package proguard.analysis.cpa.jvm.domain.memory;

import java.util.Objects;
import proguard.analysis.cpa.bam.BamLocationDependent;
import proguard.analysis.cpa.defaults.ProgramLocationDependentReachedSet;
import proguard.analysis.cpa.interfaces.AbstractState;
import proguard.analysis.cpa.interfaces.ProgramLocationDependent;
import proguard.analysis.cpa.jvm.cfa.edges.JvmCfaEdge;
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode;
import proguard.analysis.cpa.jvm.witness.JvmMemoryLocation;
import proguard.classfile.MethodSignature;

/**
 * This class wraps a {@link JvmMemoryLocation} adding information on its program location and source reached set.
 *
 * @param <AbstractStateT> The type of the abstract states in the BAM cache.
 *
 * @author Carlo Alberto Pozzoli
 */
public class BamLocationDependentJvmMemoryLocation<AbstractStateT extends AbstractState & ProgramLocationDependent<JvmCfaNode, JvmCfaEdge, MethodSignature>>
    implements ProgramLocationDependent<JvmCfaNode, JvmCfaEdge, MethodSignature>,
               BamLocationDependent<JvmCfaNode, JvmCfaEdge, AbstractStateT, MethodSignature>
{

    private final JvmMemoryLocation                                                                           memoryLocation;
    private       JvmCfaNode                                                                                  programLocation;
    private       ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, AbstractStateT, MethodSignature> sourceReachedSet;

    public BamLocationDependentJvmMemoryLocation(JvmMemoryLocation memoryLocation)
    {
        this(memoryLocation, null, null);
    }

    public BamLocationDependentJvmMemoryLocation(JvmMemoryLocation memoryLocation, JvmCfaNode programLocation,
        ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, AbstractStateT, MethodSignature> sourceReachedSet)
    {
        this.memoryLocation = memoryLocation;
        this.programLocation = programLocation;
        this.sourceReachedSet = sourceReachedSet;
    }

    public JvmMemoryLocation getMemoryLocation()
    {
        return memoryLocation;
    }

    public BamLocationDependentJvmMemoryLocation<AbstractStateT> copy()
    {
        return new BamLocationDependentJvmMemoryLocation<>(memoryLocation, programLocation, sourceReachedSet);
    }

    // Implementations for ProgramLocationDependent

    @Override
    public JvmCfaNode getProgramLocation()
    {
        return programLocation;
    }

    @Override
    public void setProgramLocation(JvmCfaNode programLocation)
    {
        this.programLocation = programLocation;
    }

    // Implementations for BamLocationDependent

    @Override
    public ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, AbstractStateT, MethodSignature> getSourceReachedSet()
    {
        return sourceReachedSet;
    }

    @Override
    public void setSourceReachedSet(ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, AbstractStateT, MethodSignature> sourceReachedSet)
    {
        this.sourceReachedSet = sourceReachedSet;
    }

    // Implementations for Object

    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof BamLocationDependentJvmMemoryLocation))
        {
            return false;
        }

        BamLocationDependentJvmMemoryLocation other = (BamLocationDependentJvmMemoryLocation) obj;
        return sourceReachedSet == other.sourceReachedSet && programLocation == other.programLocation && Objects.equals(memoryLocation, other.memoryLocation);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(memoryLocation, programLocation, sourceReachedSet);
    }

    @Override
    public String toString()
    {
        return memoryLocation.toString() + (programLocation == null
                                            ? ""
                                            : "@" + programLocation.getSignature().getFqn() + ":" + programLocation.getOffset());
    }
}
