package proguard.analysis.cpa.bam;

import proguard.analysis.cpa.defaults.LatticeAbstractState;
import proguard.analysis.cpa.defaults.ProgramLocationDependentReachedSet;
import proguard.analysis.cpa.defaults.SetAbstractState;
import proguard.analysis.cpa.interfaces.AbstractState;
import proguard.analysis.cpa.jvm.state.JvmAbstractState;

/**
 * If the usage of an {@link AbstractState} depends on the specific BAM cache entry it belongs to,
 * it should implement {@link BamLocationDependent} to link it to its source reached set.
 *
 * @param <ContentT> The content of the jvm states. For example, this can be a {@link
 *     SetAbstractState} of taints for taint analysis or a {@link
 *     proguard.analysis.cpa.jvm.domain.value.ValueAbstractState} for value analysis.
 */
public interface BamLocationDependent<ContentT extends LatticeAbstractState<ContentT>> {

  /** Returns the reached set the abstract state belongs to. */
  ProgramLocationDependentReachedSet<JvmAbstractState<ContentT>> getSourceReachedSet();

  /** Sets the reached set the abstract state belongs to. */
  void setSourceReachedSet(
      ProgramLocationDependentReachedSet<JvmAbstractState<ContentT>> sourceReachedSet);
}
