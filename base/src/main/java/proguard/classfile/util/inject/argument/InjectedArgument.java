package proguard.classfile.util.inject.argument;

import proguard.classfile.editor.InstructionSequenceBuilder;
import proguard.classfile.util.ClassUtil;

/**
 * This interface defines an API for modeling arguments to be passed to the method invocation
 * instructions that are injected by {@link proguard.classfile.util.inject.CodeInjector}.
 *
 * @author Kymeng Tang
 */
public interface InjectedArgument {
  // A getter that returns a boxed value of the argument.
  Object getValue();

  // A getter indicating the internal JVM type that describes the argument.
  String getInternalType();

  /**
   * Pushes an argument to the stack, adjusting the internal type in case it is an array.
   *
   * @param code InstructionSequenceBuilder to add the pushing instructions.
   */
  default void pushToStack(InstructionSequenceBuilder code) {
    Object value = getValue();
    if (value.getClass().isArray()) {
      // Remove the Array part from the internal type as it's not needed further.
      code.pushPrimitiveOrStringArray(
          ClassUtil.internalTypeFromArrayType(getInternalType()), (Object[]) value);
    } else {
      code.pushPrimitiveOrString(value, getInternalType());
    }
  }
}
