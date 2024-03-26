package proguard.evaluation.value.object;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;
import proguard.classfile.ClassConstants;
import proguard.classfile.Clazz;
import proguard.classfile.MethodSignature;
import proguard.evaluation.value.Value;

/** A {@link Model} to track specific Clazz constants. */
public class ClassModel implements Model {

  private final Clazz clazz;

  public ClassModel(Clazz clazz) {
    this.clazz = clazz;
  }

  @NotNull
  @Override
  public String getType() {
    return ClassConstants.TYPE_JAVA_LANG_CLASS;
  }

  @Override
  public Optional<Value> init(
      MethodSignature signature, List<Value> parameters, Function<Object, Value> valueCalculator) {
    throw new UnsupportedOperationException(
        "Constructors invocation is not supported in ClassModel");
  }

  @Override
  public Optional<Value> invoke(
      MethodSignature signature, List<Value> parameters, Function<Object, Value> valueCalculator) {
    throw new UnsupportedOperationException(
        "Instance method invocation is not supported in ClassModel");
  }

  @Override
  public Optional<Value> invokeStatic(
      MethodSignature signature, List<Value> parameters, Function<Object, Value> valueCalculator) {
    throw new UnsupportedOperationException(
        "Static method invocation is not supported in ClassModel");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ClassModel that = (ClassModel) o;
    return Objects.equals(clazz, that.clazz);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(clazz);
  }

  @Override
  public String toString() {
    return clazz.getName();
  }

  public Clazz getClazz() {
    return clazz;
  }
}
