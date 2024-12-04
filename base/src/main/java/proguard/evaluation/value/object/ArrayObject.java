package proguard.evaluation.value.object;

import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import proguard.evaluation.value.object.model.ArrayModel;
import proguard.evaluation.value.object.model.Model;

/** A {@link AnalyzedObject} which models arrays with a {@link ArrayModel}. */
class ArrayObject implements AnalyzedObject {

  private final ArrayModel value;

  ArrayObject(@NotNull ArrayModel value) {
    this.value = value;
  }

  @NotNull
  @Override
  public Model getModeledValue() {
    return value;
  }

  @NotNull
  @Override
  public Object[] getPreciseValue() {
    // TODO this should return the actual array if all the array's values are known and precise
    throw new UnsupportedOperationException("This is not a precise object");
  }

  @NotNull
  @Override
  public String getType() {
    return value.getType();
  }

  @Override
  public boolean isNull() {
    return false;
  }

  @Override
  public boolean isModeled() {
    return true;
  }

  @Override
  public boolean isPrecise() {
    // TODO this should return true if all the array's values are known and precise
    return false;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ArrayObject)) {
      return false;
    }
    ArrayObject that = (ArrayObject) o;
    return Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  @Override
  public String toString() {
    return String.format("ArrayObject(%s)", value);
  }
}
