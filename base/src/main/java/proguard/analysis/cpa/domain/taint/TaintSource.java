package proguard.analysis.cpa.domain.taint;

import proguard.classfile.Signature;

/** A {@link TaintSource} is a label carried by the taint abstract state. */
public interface TaintSource {

  /** Returns the signature of the program location this taint source refers to. */
  Signature getSignature();
}
