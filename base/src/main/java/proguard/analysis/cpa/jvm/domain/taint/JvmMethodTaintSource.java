package proguard.analysis.cpa.jvm.domain.taint;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import proguard.analysis.cpa.domain.taint.TaintSource;
import proguard.analysis.datastructure.callgraph.Call;
import proguard.classfile.Signature;

/**
 * A {@link JvmMethodTaintSource} specifies a method which can taint any (subset) of the following:
 * the instance, the return value, the argument objects, or static fields. The {@link #callMatcher}
 * decides whether the call (already filtered by its {@link Signature}) should trigger this source.
 */
public class JvmMethodTaintSource implements TaintSource {

  private final Signature signature;
  private final boolean taintThis;
  private final boolean taintReturn;
  private final Set<Integer> taintArgs;
  private final Set<String> taintGlobals;
  public final Optional<Predicate<Call>> callMatcher;

  /**
   * Create a taint source.
   *
   * @param signature the signature a source method
   * @param callMatcher whether the call matches this taint source
   * @param taintThis whether the source taints the calling instance
   * @param taintReturn whether the source taints its return
   * @param taintArgs a set of tainted arguments
   * @param taintGlobals a set of tainted global variables
   */
  public JvmMethodTaintSource(
      Signature signature,
      Predicate<Call> callMatcher,
      boolean taintThis,
      boolean taintReturn,
      Set<Integer> taintArgs,
      Set<String> taintGlobals) {
    this(signature, Optional.of(callMatcher), taintThis, taintReturn, taintArgs, taintGlobals);
  }

  /**
   * Create a taint source.
   *
   * @param signature the signature a source method
   * @param taintThis whether the source taints the calling instance
   * @param taintReturn whether the source taints its return
   * @param taintArgs a set of tainted arguments
   * @param taintGlobals a set of tainted global variables
   */
  public JvmMethodTaintSource(
      Signature signature,
      boolean taintThis,
      boolean taintReturn,
      Set<Integer> taintArgs,
      Set<String> taintGlobals) {
    this(signature, Optional.empty(), taintThis, taintReturn, taintArgs, taintGlobals);
  }

  /**
   * Create a taint source.
   *
   * @param signature the signature a source method
   * @param callMatcher an optional predicate on whether the call matches this taint source
   * @param taintThis whether the source taints the calling instance
   * @param taintReturn whether the source taints its return
   * @param taintArgs a set of tainted arguments
   * @param taintGlobals a set of tainted global variables
   */
  public JvmMethodTaintSource(
      Signature signature,
      Optional<Predicate<Call>> callMatcher,
      boolean taintThis,
      boolean taintReturn,
      Set<Integer> taintArgs,
      Set<String> taintGlobals) {
    this.signature = signature;
    this.callMatcher = callMatcher;
    this.taintThis = taintThis;
    this.taintReturn = taintReturn;
    this.taintArgs = taintArgs;
    this.taintGlobals = taintGlobals;
  }

  // implementations for TaintSource

  @Override
  public Signature getSignature() {
    return signature;
  }

  public boolean getTaintsThis() {
    return taintThis;
  }

  public boolean getTaintsReturn() {
    return taintReturn;
  }

  public Set<Integer> getTaintArgs() {
    return taintArgs;
  }

  public Set<String> getTaintGlobals() {
    return taintGlobals;
  }

  // implementations for Object

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof JvmMethodTaintSource)) {
      return false;
    }
    JvmMethodTaintSource other = (JvmMethodTaintSource) obj;
    return Objects.equals(signature, other.signature)
        && Objects.equals(callMatcher, other.callMatcher)
        && taintThis == other.taintThis
        && taintReturn == other.taintReturn
        && Objects.equals(taintArgs, other.taintArgs)
        && Objects.equals(taintGlobals, other.taintGlobals);
  }

  @Override
  public int hashCode() {
    return Objects.hash(signature, callMatcher, taintThis, taintReturn, taintArgs, taintGlobals);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder("[TaintSource] ").append(signature);
    if (taintThis) {
      builder.append(", taints this");
    }
    if (taintReturn) {
      builder.append(", taints return");
    }
    if (!taintArgs.isEmpty()) {
      builder
          .append(", taints args (")
          .append(
              taintArgs.stream().map(Object::toString).sorted().collect(Collectors.joining(", ")))
          .append(")");
    }
    if (!taintGlobals.isEmpty()) {
      builder
          .append(", taints globals (")
          .append(taintGlobals.stream().sorted().collect(Collectors.joining(", ")))
          .append(")");
    }

    return callMatcher
        .map(p -> new StringBuilder(builder).append(" filtered by ").append(p).toString())
        .orElse(builder.toString());
  }
}
