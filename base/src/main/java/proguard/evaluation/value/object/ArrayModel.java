package proguard.evaluation.value.object;

import java.util.Arrays;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import proguard.evaluation.value.Value;
import proguard.util.ArrayUtil;

public class ArrayModel implements Model {

  private final Value[] values;
  private final String type;

  ArrayModel(Value[] values, String type) {

    this.values = values;
    this.type = type;
  }

  @NotNull
  @Override
  public String getType() {
    return type;
  }

  public Value[] getValues() {
    return Arrays.copyOf(values, values.length);
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

    builder.append('{');
    for (int index = 0; index < values.length; index++) {
      builder.append(values[index]);
      builder.append(index < values.length - 1 ? ',' : '}');
    }

    return builder.toString();
  }
}
