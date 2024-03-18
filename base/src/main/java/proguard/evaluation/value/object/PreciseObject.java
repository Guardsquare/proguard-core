package proguard.evaluation.value.object;

import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import proguard.classfile.util.ClassUtil;

/**
 * A {@link AnalyzedObject} which value represents the actual tracked value, and its type has to be
 * considered the runtime type of the tracked value.
 */
class PreciseObject implements AnalyzedObject {

  @NotNull private final Object value;
  @NotNull private final String type;

  PreciseObject(@NotNull Object object) {
    this.value = object;
    this.type = ClassUtil.internalType(object.getClass().getTypeName());
  }

  @NotNull
  @Override
  public Object getPreciseValue() {
    return value;
  }

  @Override
  public @NotNull String getType() {
    return type;
  }

  @Override
  public boolean isNull() {
    return false;
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
    if (!(o instanceof PreciseObject)) {
      return false;
    }
    PreciseObject that = (PreciseObject) o;
    return Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  @Override
  public String toString() {
    return String.format("PreciseObject(%s)", value);
  }
}
