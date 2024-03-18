package proguard.evaluation.value.object;

import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** A {@link AnalyzedObject} for a value known to be null with additional type information. */
class TypedNullObject implements AnalyzedObject {

  private final String type;

  TypedNullObject(String type) {
    this.type = type;
  }

  @Nullable
  @Override
  public Object getPreciseValue() {
    return null;
  }

  @NotNull
  @Override
  public String getType() {
    return type;
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
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof TypedNullObject)) {
      return false;
    }
    TypedNullObject that = (TypedNullObject) o;
    return Objects.equals(type, that.type);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type);
  }

  @Override
  public String toString() {
    return String.format("TypedNullObject(%s)", type);
  }
}
