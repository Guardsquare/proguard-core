package proguard.classfile.util.inject.argument;

import proguard.classfile.util.ClassUtil;

public class PrimitiveArrayConstantArgument<T extends Number> implements InjectedArgument {
  private final T[] numericConstantArray;

  public PrimitiveArrayConstantArgument(T[] arrayConstant) {
    this.numericConstantArray = arrayConstant;
  }

  @Override
  public Object getValue() {
    return numericConstantArray;
  }

  @Override
  public String getInternalType() {
    switch (numericConstantArray.getClass().getName()) {
      case "[Ljava.lang.Boolean;":
        return "[Z";
      case "[Ljava.lang.Byte;":
        return "[B";
      case "[Ljava.lang.Character;":
        return "[C";
      case "[Ljava.lang.Short;":
        return "[S";
      case "[Ljava.lang.Integer;":
        return "[I";
      case "[Ljava.lang.Long;":
        return "[J";
      case "[Ljava.lang.Float;":
        return "[F";
      case "[Ljava.lang.Double;":
        return "[D";
      default:
        throw new RuntimeException("Unexpected type");
    }
  }

  @Override
  public String toString() {
    return numericConstantArray.toString() + ":" + ClassUtil.externalType(getInternalType());
  }
}
