package proguard.analysis.cpa.jvm.domain.memory;

import java.util.Objects;
import java.util.Optional;
import proguard.analysis.cpa.bam.BamLocationDependent;
import proguard.analysis.cpa.defaults.ProgramLocationDependentReachedSet;
import proguard.analysis.cpa.defaults.SetAbstractState;
import proguard.analysis.cpa.interfaces.AbstractState;
import proguard.analysis.cpa.interfaces.ProgramLocationDependent;
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode;
import proguard.analysis.cpa.jvm.state.JvmAbstractState;
import proguard.analysis.cpa.jvm.witness.JvmMemoryLocation;

/**
 * This class wraps a {@link JvmMemoryLocation} adding information on its program location and
 * source reached set.
 *
 * @param <ContentT> The content of the jvm states for the traced analysis, contained in the BAM
 *     cache. For example, this can be a {@link SetAbstractState} of taints for taint analysis or a
 *     {@link proguard.analysis.cpa.jvm.domain.value.ValueAbstractState} for value analysis.
 */
public class BamLocationDependentJvmMemoryLocation<ContentT extends AbstractState<ContentT>>
    implements ProgramLocationDependent, BamLocationDependent<ContentT> {

  private final JvmMemoryLocation memoryLocation;
  private JvmCfaNode programLocation;
  private ProgramLocationDependentReachedSet<JvmAbstractState<ContentT>> sourceReachedSet;

  public BamLocationDependentJvmMemoryLocation(JvmMemoryLocation memoryLocation) {
    this(memoryLocation, null, null);
  }

  public BamLocationDependentJvmMemoryLocation(
      JvmMemoryLocation memoryLocation,
      JvmCfaNode programLocation,
      ProgramLocationDependentReachedSet<JvmAbstractState<ContentT>> sourceReachedSet) {
    this.memoryLocation = memoryLocation;
    this.programLocation = programLocation;
    this.sourceReachedSet = sourceReachedSet;
  }

  public JvmMemoryLocation getMemoryLocation() {
    return memoryLocation;
  }

  public BamLocationDependentJvmMemoryLocation<ContentT> copy() {
    return new BamLocationDependentJvmMemoryLocation<>(
        memoryLocation, programLocation, sourceReachedSet);
  }

  /**
   * Extract the value from the first state from the reached state corresponding to the program,
   * memory location, and BAM cache entry represented by this object.
   *
   * <p>Most analyses will have one state at most for each program location, and this method should
   * be mostly used for this kind of analyses since it will return the only valid state. It's also
   * possible that, for analyses where multiple states are possible, the returned value might be
   * non-deterministic depending on the underlying reached set implementation.
   *
   * @return an empty optional if there is no analysis state in the cache entry and program location
   *     represented by this object. Otherwise, the content for the memory location specified by
   *     this object of the first state (there is usually only one state anyway, but this is
   *     analysis-dependent) for that program location.
   */
  public ContentT extractFirstValue(ContentT defaultValue) {
    Optional<JvmAbstractState<ContentT>> firstReachedState =
        sourceReachedSet.getReached(programLocation).stream().findFirst();
    if (!firstReachedState.isPresent()) {
      return defaultValue;
    }

    return memoryLocation.extractValueOrDefault(firstReachedState.get(), defaultValue);
  }

  // Implementations for ProgramLocationDependent

  @Override
  public JvmCfaNode getProgramLocation() {
    return programLocation;
  }

  @Override
  public void setProgramLocation(JvmCfaNode programLocation) {
    this.programLocation = programLocation;
  }

  // Implementations for BamLocationDependent

  @Override
  public ProgramLocationDependentReachedSet<JvmAbstractState<ContentT>> getSourceReachedSet() {
    return sourceReachedSet;
  }

  @Override
  public void setSourceReachedSet(
      ProgramLocationDependentReachedSet<JvmAbstractState<ContentT>> sourceReachedSet) {
    this.sourceReachedSet = sourceReachedSet;
  }

  // Implementations for Object

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof BamLocationDependentJvmMemoryLocation)) {
      return false;
    }

    BamLocationDependentJvmMemoryLocation<?> other = (BamLocationDependentJvmMemoryLocation<?>) obj;
    return Objects.equals(sourceReachedSet, other.sourceReachedSet)
        && programLocation == other.programLocation
        && Objects.equals(memoryLocation, other.memoryLocation);
  }

  @Override
  public int hashCode() {
    return Objects.hash(memoryLocation, programLocation, sourceReachedSet);
  }

  @Override
  public String toString() {
    return memoryLocation.toString()
        + (programLocation == null
            ? ""
            : "@" + programLocation.getSignature().getFqn() + ":" + programLocation.getOffset());
  }
}
