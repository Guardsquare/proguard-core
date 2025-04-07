package proguard.classfile.util.inject;

import java.util.*;
import proguard.classfile.*;
import proguard.classfile.attribute.Attribute;
import proguard.classfile.attribute.CodeAttribute;
import proguard.classfile.attribute.visitor.AllAttributeVisitor;
import proguard.classfile.attribute.visitor.AttributeNameFilter;
import proguard.classfile.attribute.visitor.AttributeVisitor;
import proguard.classfile.editor.CodeAttributeEditor;
import proguard.classfile.editor.InstructionSequenceBuilder;
import proguard.classfile.instruction.Instruction;
import proguard.classfile.util.inject.argument.InjectedArgument;
import proguard.classfile.util.inject.location.InjectStrategy;

/**
 * This utility class allows for injecting multiple static method invocations in multiple target
 * methods. It allows the repeated use of the API methods defined in the {@link CodeInjector} class.
 */
public class AccumulatedCodeInjector extends CodeInjector {
  private final List<CodeInjector> injectors = new ArrayList<>();
  private CodeInjector currentInjector = new CodeInjector();

  @Override
  public CodeInjector injectInvokeStatic(Clazz clazz, Method method) {
    return injectInvokeStatic(clazz, method, new InjectedArgument[0]);
  }

  @Override
  public CodeInjector injectInvokeStatic(
      Clazz clazz, Method method, InjectedArgument... arguments) {
    currentInjector.injectInvokeStatic(clazz, method, arguments);
    commitCurrentInjector();
    return this;
  }

  @Override
  public CodeInjector into(ProgramClass programClass, ProgramMethod programMethod) {
    currentInjector.into(programClass, programMethod);
    commitCurrentInjector();
    return this;
  }

  @Override
  public CodeInjector at(InjectStrategy injectStrategy) {
    currentInjector.at(injectStrategy);
    commitCurrentInjector();
    return this;
  }

  @Override
  public void commit() {
    // Construct a map of target methods to the code injectors that should be applied.
    Map<ClassMethodPair, List<CodeInjector>> targetToInjectorsMap = new HashMap<>();
    injectors.forEach(
        injector ->
            injector
                .getTargets()
                .forEach(
                    target -> {
                      List<CodeInjector> currentInjectors =
                          targetToInjectorsMap.computeIfAbsent(target, k -> new ArrayList<>());
                      currentInjectors.add(injector);
                    }));

    CodeAttributeEditor editor = new CodeAttributeEditor();

    // Iterate over the target methods.
    targetToInjectorsMap.forEach(
        (target, injectors) -> {
          InstructionSequenceBuilder code =
              new InstructionSequenceBuilder((ProgramClass) target.clazz);

          // Map offsets to the list of instructions that should be applied, either before or after
          // the offset.
          Map<Integer, LinkedList<Instruction[]>> beforeInstructionInsertions = new HashMap<>();
          Map<Integer, LinkedList<Instruction[]>> afterInstructionInsertions = new HashMap<>();
          injectors.forEach(
              injector -> {
                // Push arguments.
                injector
                    .getArguments()
                    .forEach(
                        argument ->
                            code.pushPrimitiveOrString(
                                argument.getValue(), argument.getInternalType()));

                // Call static method.
                code.invokestatic(injector.getContent().clazz, injector.getContent().method);

                // Map instructions to offsets.
                InjectStrategy.InjectLocation[] injectLocations =
                    injector
                        .getInjectStrategy()
                        .getAllSuitableInjectionLocation(
                            (ProgramClass) target.clazz, (ProgramMethod) target.method);
                for (InjectStrategy.InjectLocation location : injectLocations) {
                  if (location.shouldInjectBefore()) {
                    beforeInstructionInsertions
                        .computeIfAbsent(location.getOffset(), (i_) -> new LinkedList<>())
                        .add(code.instructions());
                  } else {
                    afterInstructionInsertions
                        .computeIfAbsent(location.getOffset(), (i_) -> new LinkedList<>())
                        .add(code.instructions());
                  }
                }
              });

          // Reset code length.
          target.method.accept(
              target.clazz,
              new AllAttributeVisitor(
                  new AttributeNameFilter(
                      Attribute.CODE,
                      new AttributeVisitor() {
                        @Override
                        public void visitCodeAttribute(
                            Clazz clazz, Method method, CodeAttribute codeAttribute) {
                          editor.reset(codeAttribute.u4codeLength);
                        }
                      })));

          // Apply injections to the code attribute.
          for (Map.Entry<Integer, LinkedList<Instruction[]>> entry :
              beforeInstructionInsertions.entrySet()) {
            int offset = entry.getKey();
            Instruction[] instructions =
                entry.getValue().stream().flatMap(Arrays::stream).toArray(Instruction[]::new);
            editor.insertBeforeInstruction(offset, instructions);
          }

          for (Map.Entry<Integer, LinkedList<Instruction[]>> entry :
              afterInstructionInsertions.entrySet()) {
            int offset = entry.getKey();
            Instruction[] instructions =
                entry.getValue().stream().flatMap(Arrays::stream).toArray(Instruction[]::new);
            editor.insertAfterInstruction(offset, instructions);
          }

          target.method.accept(
              target.clazz,
              new AllAttributeVisitor(new AttributeNameFilter(Attribute.CODE, editor)));
        });
  }

  private void commitCurrentInjector() {
    if (currentInjector.readyToCommit()) {
      injectors.add(currentInjector);
      currentInjector = new CodeInjector();
    }
  }
}
