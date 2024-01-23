package proguard.analysis.cpa.jvm.domain.taint;

import proguard.analysis.cpa.defaults.SetAbstractState;

/**
 * Class that can be passed to {@link
 * proguard.analysis.cpa.jvm.domain.taint.JvmTaintTransferRelation} to specify how a specific method
 * call should be treated.
 *
 * <p>For example this can be overridden to specify that a specific taint should not be propagated.
 */
public interface JvmTaintTransformer {

  /**
   * The transformer implementation can override this method to specify how to modify the return
   * value of a method call. The default implementation returns the original value.
   *
   * @param returnValue the original return state
   * @return the modified return state
   */
  default SetAbstractState<JvmTaintSource> transformReturn(
      SetAbstractState<JvmTaintSource> returnValue) {
    return returnValue;
  }
}
