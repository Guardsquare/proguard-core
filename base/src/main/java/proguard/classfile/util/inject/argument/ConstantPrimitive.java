package proguard.classfile.util.inject.argument;

import proguard.classfile.util.ClassUtil;

/**
 * A model representing a constant value of primitive typed argument to be passed to the method
 * invocation instructions that are injected by {@link proguard.classfile.util.inject.CodeInjector}.
 *
 * @author Kymeng Tang
 */
public class ConstantPrimitive<T extends Number> implements InjectedArgument {
  private final T numericConstant;

  public ConstantPrimitive(T constant) {
    this.numericConstant = constant;
  }

  @Override
  public Object getValue() {
    return numericConstant;
  }

  @Override
  public String getInternalType() {
    switch (numericConstant.getClass().getName()) {
      case "java.lang.Boolean":
        return "Z";
      case "java.lang.Byte":
        return "B";
      case "java.lang.Character":
        return "C";
      case "java.lang.Short":
        return "S";
      case "java.lang.Integer":
        return "I";
      case "java.lang.Long":
        return "J";
      case "java.lang.Float":
        return "F";
      case "java.lang.Double":
        return "D";
      default:
        throw new RuntimeException("Unexpected type");
    }
  }

  @Override
  public String toString() {
    return numericConstant.toString() + ":" + ClassUtil.externalType(getInternalType());
  }
}
