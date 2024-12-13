package proguard.normalize;

import proguard.classfile.ClassConstants;
import proguard.classfile.ClassPool;
import proguard.classfile.Clazz;
import proguard.classfile.Method;
import proguard.classfile.ProgramClass;
import proguard.classfile.attribute.CodeAttribute;
import proguard.classfile.attribute.visitor.AllAttributeVisitor;
import proguard.classfile.constant.Constant;
import proguard.classfile.constant.StringConstant;
import proguard.classfile.constant.visitor.ConstantVisitor;
import proguard.classfile.editor.CodeAttributeEditor;
import proguard.classfile.editor.ConstantPoolShrinker;
import proguard.classfile.editor.InstructionSequenceBuilder;
import proguard.classfile.editor.PeepholeEditor;
import proguard.classfile.instruction.ConstantInstruction;
import proguard.classfile.instruction.Instruction;
import proguard.classfile.instruction.visitor.InstructionVisitor;
import proguard.classfile.visitor.AllMethodVisitor;
import proguard.classfile.visitor.ClassVisitor;
import proguard.util.StringUtil;

/** This class visitor splits any strings longer than 65535 bytes into smaller strings. */
public class LargeStringSplitter implements ClassVisitor, InstructionVisitor, ConstantVisitor {
  private static final int MAX_STRING_SIZE = 0xffff;

  private final ClassPool programClassPool, libraryClassPool;

  private final CodeAttributeEditor codeAttributeEditor;
  private final ConstantPoolShrinker constantPoolShrinker;

  private int offset;
  private boolean classModified = false;

  public LargeStringSplitter(ClassPool programClassPool, ClassPool libraryClassPool) {
    this.programClassPool = programClassPool;
    this.libraryClassPool = libraryClassPool;
    this.codeAttributeEditor = new CodeAttributeEditor();
    this.constantPoolShrinker = new ConstantPoolShrinker();
  }

  // Implementations for ClassVisitor.

  @Override
  public void visitAnyClass(Clazz clazz) {}

  @Override
  public void visitProgramClass(ProgramClass programClass) {
    classModified = false;
    programClass.accept(
        new LargeStringClassConstantClassFilter(
            new AllMethodVisitor(
                new AllAttributeVisitor(new PeepholeEditor(codeAttributeEditor, this)))));
    if (classModified) {
      programClass.accept(constantPoolShrinker);
    }
  }

  // Implementations for InstructionVisitor.

  @Override
  public void visitAnyInstruction(
      Clazz clazz,
      Method method,
      CodeAttribute codeAttribute,
      int offset,
      Instruction instruction) {}

  @Override
  public void visitConstantInstruction(
      Clazz clazz,
      Method method,
      CodeAttribute codeAttribute,
      int offset,
      ConstantInstruction constantInstruction) {
    this.offset = offset;
    clazz.constantPoolEntryAccept(constantInstruction.constantIndex, this);
  }

  // Implementations for ConstantVisitor.

  @Override
  public void visitAnyConstant(Clazz clazz, Constant constant) {}

  @Override
  public void visitStringConstant(Clazz clazz, StringConstant stringConstant) {
    String fullString = stringConstant.getString(clazz);
    if (StringUtil.getModifiedUtf8Length(fullString) > MAX_STRING_SIZE) {
      InstructionSequenceBuilder ____ =
          new InstructionSequenceBuilder((ProgramClass) clazz, programClassPool, libraryClassPool);

      ____.new_(ClassConstants.NAME_JAVA_LANG_STRING_BUILDER)
          .dup()
          .invokespecial(
              ClassConstants.NAME_JAVA_LANG_STRING_BUILDER,
              ClassConstants.METHOD_NAME_INIT,
              ClassConstants.METHOD_TYPE_INIT);

      // Iterate through substrings, of at most 65535 bytes, until we reach the end of the string.
      int substringStart = 0;
      while (substringStart < fullString.length()) {
        int substringEnd = nextSubstringEnd(fullString, substringStart);

        ____.ldc(fullString.substring(substringStart, substringEnd))
            .invokevirtual(
                ClassConstants.NAME_JAVA_LANG_STRING_BUILDER,
                ClassConstants.METHOD_NAME_APPEND,
                ClassConstants.METHOD_TYPE_STRING_STRING_BUILDER);
        substringStart = substringEnd;
      }

      ____.invokevirtual(
          ClassConstants.NAME_JAVA_LANG_OBJECT,
          ClassConstants.METHOD_NAME_TOSTRING,
          ClassConstants.METHOD_TYPE_TOSTRING);
      codeAttributeEditor.replaceInstruction(offset, ____.__());
      classModified = true;
    }
  }

  /**
   * Returns the size of the largest substring of the given sequence that encodes to modified UTF-8
   * in 65535 bytes or fewer, starting at the given position.
   *
   * @see StringUtil#getModifiedUtf8Length(String)
   */
  private static int nextSubstringEnd(String fullString, int start) {
    // Highly optimized implementation inspired by Guava's Utf8.encodedLength:
    // https://github.com/google/guava/blob/8a24204a29275636b5bd30136ef289e9f6651bb1/guava/src/com/google/common/base/Utf8.java#L49-L77
    int end = start;
    int maxLength = Integer.min(fullString.length(), start + MAX_STRING_SIZE);

    // If we have pure ASCII at the start, skip past it.
    while (end < maxLength && fullString.charAt(end) < 0x80) {
      end++;
    }

    int utf8Length = end - start;

    while (end < maxLength) {
      char c = fullString.charAt(end);
      if (c < 0x800) {
        utf8Length += 1 + ((0x7f - c) >>> 31); // Branch free!
      } else {
        utf8Length += 3;
      }
      // If the new length is bigger than the maximum, exit before we extend.
      if (utf8Length > MAX_STRING_SIZE) {
        break;
      }
      end += 1;
    }
    return end;
  }

  /**
   * A {@link ClassVisitor} that delegates to the given delegate {@link ClassVisitor} if the class
   * contains a string constant pool entry with size greater the 65,535 bytes.
   */
  private static class LargeStringClassConstantClassFilter
      implements ClassVisitor, ConstantVisitor {

    private final ClassVisitor acceptedVisitor;
    private boolean found;

    private LargeStringClassConstantClassFilter(ClassVisitor acceptedVisitor) {
      this.acceptedVisitor = acceptedVisitor;
    }

    // Implementations for ClassVisitor.

    @Override
    public void visitAnyClass(Clazz clazz) {}

    @Override
    public void visitProgramClass(ProgramClass programClass) {
      found = false;
      programClass.constantPoolEntriesAccept(this);
    }

    // Implementations for ConstantVisitor

    @Override
    public void visitAnyConstant(Clazz clazz, Constant constant) {}

    @Override
    public void visitStringConstant(Clazz clazz, StringConstant stringConstant) {
      if (found) return; // We already found one, no need to check again.

      String fullString = stringConstant.getString(clazz);
      if (StringUtil.getModifiedUtf8Length(fullString) > MAX_STRING_SIZE) {
        clazz.accept(acceptedVisitor);
        found = true;
      }
    }
  }
}
