package proguard.evaluation.value.object;

import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import proguard.evaluation.value.object.model.Model;

/**
 * A {@link AnalyzedObject} wrapping a {@link Model}. This can be used to track a model of a value
 * during an analysis instead of the exact value.
 */
class ModeledObject implements AnalyzedObject {

  private final Model value;

  ModeledObject(Model value) {
    this.value = value;
  }

  @NotNull
  @Override
  public Model getModeledValue() {
    return value;
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
    return false;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ModeledObject)) {
      return false;
    }
    ModeledObject that = (ModeledObject) o;
    return Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  @Override
  public String toString() {
    return String.format("ModeledObject(%s)", value);
  }
}
