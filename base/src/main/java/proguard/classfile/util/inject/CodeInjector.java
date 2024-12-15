package proguard.classfile.util.inject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import proguard.classfile.AccessConstants;
import proguard.classfile.ClassConstants;
import proguard.classfile.Clazz;
import proguard.classfile.Method;
import proguard.classfile.ProgramClass;
import proguard.classfile.ProgramMethod;
import proguard.classfile.attribute.Attribute;
import proguard.classfile.attribute.CodeAttribute;
import proguard.classfile.attribute.visitor.AllAttributeVisitor;
import proguard.classfile.attribute.visitor.AttributeNameFilter;
import proguard.classfile.attribute.visitor.AttributeVisitor;
import proguard.classfile.editor.CodeAttributeEditor;
import proguard.classfile.editor.InstructionSequenceBuilder;
import proguard.classfile.instruction.Instruction;
import proguard.classfile.util.ClassUtil;
import proguard.classfile.util.InternalTypeEnumeration;
import proguard.classfile.util.inject.argument.InjectedArgument;
import proguard.classfile.util.inject.location.InjectStrategy;

/**
 * This utility class allow for injecting a method invocation instruction optionally with arguments
 * modeled by instances of classes implementing {@link InjectedArgument} interface to the specified
 * target method at an offset determined by the implementation of the {@link InjectStrategy}
 * interface.
 *
 * <p>Example usage: new CodeInjector() .injectInvokeStatic(logUtilClass, logDebugMethod, new
 * ConstantPrimitive<Integer>(1), new ConstantString("Hello world")) .into(MainProgramClass,
 * mainMethod) .at(new FirstBlock()) .commit();
 *
 * @author Kymeng Tang
 */
public class CodeInjector {
  private List<ClassMethodPair> targets;
  private ClassMethodPair content;
  private InjectStrategy injectStrategy;
  private List<InjectedArgument> arguments = new ArrayList<>();

  /**
   * Specify the static method to be invoked.
   *
   * @param clazz The class in which the static method belongs to.
   * @param method The method to be invoked.
   */
  public CodeInjector injectInvokeStatic(Clazz clazz, Method method) {
    assert content == null
        : "The injection content: `"
            + renderInjectionContent(content.clazz, content.method, arguments)
            + "` "
            + "has already been specified.";

    assert (method.getAccessFlags() & AccessConstants.STATIC) != 0
            && !method.getName(clazz).equals(ClassConstants.METHOD_NAME_CLINIT)
        : "The method to be injected must be a (non-class initializer) static method.";

    content = new ClassMethodPair(clazz, method);
    return this;
  }

  /**
   * Specify the static method to be invoked.
   *
   * @param clazz The class in which the static method belongs to.
   * @param method The method to be invoked.
   * @param arguments a list of arguments to be passed to the method to be invoked.
   */
  public CodeInjector injectInvokeStatic(
      Clazz clazz, Method method, InjectedArgument... arguments) {
    injectInvokeStatic(clazz, method);

    InternalTypeEnumeration parametersIterator =
        new InternalTypeEnumeration(method.getDescriptor(clazz));
    Iterator<InjectedArgument> argumentsIterator = Arrays.stream(arguments).iterator();

    while (parametersIterator.hasNext() || argumentsIterator.hasNext()) {
      String expectedType = parametersIterator.next();
      InjectedArgument provided = argumentsIterator.next();

      assert expectedType.equals(provided.getInternalType())
          : String.format(
              "Provided argument: `%s` doesn't match the expected parameter type: %s for method: ",
              argumentsIterator,
              expectedType,
              renderMethodSignature(content.clazz, content.method));
    }
    this.arguments = Arrays.asList(arguments);

    return this;
  }

  /**
   * Specify the method where a static method invocation shall be injected into.
   *
   * @param programClass The program class that has the method where a static method invocation
   *     shall be injected into.
   * @param programMethod the method where a static method invocation shall be injected into.
   */
  public CodeInjector into(ProgramClass programClass, ProgramMethod programMethod) {
    assert targets == null : "The injection target has already been specified.";

    targets = Arrays.asList(new ClassMethodPair(programClass, programMethod));
    return this;
  }

  /**
   * Specify the location in which the invoke instruction should be injected into.
   *
   * @param injectStrategy The implementation of InjectStrategy interface which determine the offset
   *     to inject the invoke instruction.
   * @return
   */
  public CodeInjector at(InjectStrategy injectStrategy) {
    assert this.injectStrategy == null
        : "The injection strategy: " + injectStrategy + " has already been specified.";

    this.injectStrategy = injectStrategy;
    return this;
  }

  /**
   * Apply the invoke instruction in accordance to the specifications provided via the
   * `.injectInvokeStatic(...)`, `.into(...)` and `at(...)` method.
   */
  public void commit() {
    assert content != null
        : "The injection content hasn't been provided; please use `.injectInvokeStatic(...)` "
            + "to indicate the method invocation to be injected.";

    assert targets != null
        : "The injection target hasn't been provided; please use `.into(...)` to indicate the method targeted for "
            + "injecting "
            + renderInjectionContent(content.clazz, content.method, arguments)
            + ".";

    assert injectStrategy != null
        : "The injection location hasn't been provided. please use `.at(...)` to indicate the place to inject "
            + renderInjectionContent(content.clazz, content.method, arguments)
            + " into the target method.";

    targets.forEach(
        target -> {
          CodeAttributeEditor editor = new CodeAttributeEditor();
          InstructionSequenceBuilder code =
              new InstructionSequenceBuilder((ProgramClass) target.clazz);

          arguments.forEach(
              argument ->
                  code.pushPrimitiveOrString(argument.getValue(), argument.getInternalType()));
          code.invokestatic(content.clazz, content.method);

          target.method.accept(
              target.clazz,
              new AllAttributeVisitor(
                  new AttributeNameFilter(
                      Attribute.CODE, new InstructionInjector(editor, code, injectStrategy))));
        });
  }

  // Internal utility methods
  private static String renderMethodSignature(Clazz clazz, Method method) {
    return ClassUtil.externalFullMethodDescription(
        clazz.getName(),
        method.getAccessFlags(),
        method.getName(clazz),
        method.getDescriptor(clazz));
  }

  private static String renderInjectionContent(
      Clazz clazz, Method method, List<InjectedArgument> arguments) {
    return new StringBuilder()
        .append(clazz.getName())
        .append(method.getName(clazz))
        .append("(")
        .append(arguments.stream().map(Object::toString).collect(Collectors.joining(",")))
        .append("):")
        .append(ClassUtil.externalMethodReturnType(method.getDescriptor(clazz)))
        .toString();
  }

  // Internal utility classes
  private static class InstructionInjector implements AttributeVisitor {
    private final CodeAttributeEditor editor;
    private final InstructionSequenceBuilder code;
    private final InjectStrategy injectStrategy;

    private InstructionInjector(
        CodeAttributeEditor editor,
        InstructionSequenceBuilder code,
        InjectStrategy injectStrategy) {
      this.editor = editor;
      this.code = code;
      this.injectStrategy = injectStrategy;
    }

    @Override
    public void visitCodeAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute) {
      editor.reset(codeAttribute.u4codeLength);
      InjectStrategy.InjectLocation[] injectLocations =
          injectStrategy.getAllSuitableInjectionLocation(
              (ProgramClass) clazz, (ProgramMethod) method);
      for (InjectStrategy.InjectLocation location : injectLocations) {
        final BiConsumer<Integer, Instruction[]> inserter =
            location.shouldInjectBefore()
                ? editor::insertBeforeOffset
                : editor::insertAfterInstruction;

        inserter.accept(location.getOffset(), code.instructions());
      }
      codeAttribute.accept(clazz, method, editor);
    }
  }

  private static class ClassMethodPair {
    public Clazz clazz;
    public Method method;

    public ClassMethodPair(Clazz clazz, Method method) {
      this.clazz = clazz;
      this.method = method;
    }
  }
}
