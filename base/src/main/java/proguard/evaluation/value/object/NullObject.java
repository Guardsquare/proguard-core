package proguard.evaluation.value.object;

import org.jetbrains.annotations.Nullable;

/** Tracks a simple null reference with no additional information. */
class NullObject implements AnalyzedObject {

  static NullObject INSTANCE = new NullObject();

  private NullObject() {}

  @Nullable
  @Override
  public Object getPreciseValue() {
    return null;
  }

  @Nullable
  @Override
  public String getType() {
    return null;
  }

  @Override
  public boolean isNull() {
    return true;
  }

  @Override
  public boolean isModeled() {
    return false;
  }

  @Override
  public boolean isPrecise() {
    return true;
  }

  @Override
  public int hashCode() {
    return System.identityHashCode(this);
  }

  @Override
  public boolean equals(Object obj) {
    return INSTANCE == obj;
  }

  @Override
  public String toString() {
    return "NullObject";
  }
}
