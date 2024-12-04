package proguard.evaluation.value.object.model;

import java.util.Arrays;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import proguard.evaluation.MethodResult;
import proguard.evaluation.ValueCalculator;
import proguard.evaluation.executor.MethodExecutionInfo;
import proguard.evaluation.value.Value;
import proguard.util.ArrayUtil;

/** A {@link Model} to track array values. */
public class ArrayModel implements Model {

  private final Value[] values;
  private final String type;

  public ArrayModel(Value[] values, String type) {

    this.values = values;
    this.type = type;
  }

  public Value[] getValues() {
    return Arrays.copyOf(values, values.length);
  }

  @NotNull
  @Override
  public String getType() {
    return type;
  }

  @Override
  public MethodResult init(
      MethodExecutionInfo methodExecutionInfo, ValueCalculator valueCalculator) {
    throw new UnsupportedOperationException(
        "Constructors invocation is not supported in ArrayModel");
  }

  @Override
  public MethodResult invoke(
      MethodExecutionInfo methodExecutionInfo, ValueCalculator valueCalculator) {
    throw new UnsupportedOperationException(
        "Instance method invocation is not supported in ArrayModel");
  }

  @Override
  public MethodResult invokeStatic(
      MethodExecutionInfo methodExecutionInfo, ValueCalculator valueCalculator) {
    throw new UnsupportedOperationException(
        "Static method invocation is not supported in ArrayModel");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ArrayModel)) {
      return false;
    }
    ArrayModel that = (ArrayModel) o;
    return Objects.equals(type, that.type) && ArrayUtil.equalOrNull(this.values, that.values);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(type);
    result = 31 * result + Arrays.hashCode(values);
    return result;
  }

  @Override
  public String toString() {
    if (values == null) {
      return " null";
    }

    StringBuilder builder = new StringBuilder(super.toString());

    builder.append("ArrayModel{");
    for (int index = 0; index < values.length; index++) {
      builder.append(values[index]);
      builder.append(index < values.length - 1 ? ',' : '}');
    }

    return builder.toString();
  }
}
