package proguard.classfile.util.inject.argument;

import proguard.classfile.editor.InstructionSequenceBuilder;

public class LocalVariable implements InjectedArgument {
  private final String internalType;
  private final int targetVariableIndex;

  public LocalVariable(int targetVariableIndex, String internalType) {
    this.targetVariableIndex = targetVariableIndex;
    this.internalType = internalType;
  }

  @Override
  public Object getValue() {
    return null;
  }

  @Override
  public String getInternalType() {
    return internalType;
  }

  public int getTargetVariableIndex() {
    return targetVariableIndex;
  }

  @Override
  public void pushToStack(InstructionSequenceBuilder code) {
    String type = getInternalType();
    if (type.startsWith("L") || type.startsWith("[")) {
      code.aload(targetVariableIndex);
    } else {
      switch (type) {
        case "I":
        case "S":
        case "C":
        case "B":
        case "Z":
          code.iload(targetVariableIndex);
          break;
        case "F":
          code.fload(targetVariableIndex);
          break;
        case "J":
          code.lload(targetVariableIndex);
          break;
        case "D":
          code.dload(targetVariableIndex);
          break;
      }
    }
  }
}
