package proguard.classfile.attribute.signature.grammars;

import static proguard.classfile.attribute.signature.grammars.FieldDescriptorGrammar.FIELD_TYPE;
import static proguard.classfile.attribute.signature.parsing.Combinators.chain;
import static proguard.classfile.attribute.signature.parsing.Combinators.oneOf;
import static proguard.classfile.attribute.signature.parsing.Combinators.repeat;
import static proguard.classfile.attribute.signature.parsing.Parsers.fixedChar;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import proguard.classfile.attribute.signature.ast.descriptor.FieldTypeNode;
import proguard.classfile.attribute.signature.ast.descriptor.MethodDescriptorNode;
import proguard.classfile.attribute.signature.ast.descriptor.ReturnDescriptorNode;
import proguard.classfile.attribute.signature.parsing.Parser;

/**
 * Implements parser for the method descriptor grammar defined in JVM spec.
 *
 * @see <a href="https://docs.oracle.com/javase/specs/jvms/se23/html/jvms-4.html#jvms-4.3.3">method
 *     descriptor grammar</a>
 */
public final class MethodDescriptorGrammar {
  static final Parser<ReturnDescriptorNode> RETURN_DESCRIPTOR =
      oneOf(
          FIELD_TYPE.map(ReturnDescriptorNode::new),
          CommonTerminals.VOID_DESCRIPTOR.map(ReturnDescriptorNode::new));

  static final Parser<FieldTypeNode> PARAMETER_DESCRIPTOR = FIELD_TYPE;
  static final Parser<MethodDescriptorNode> METHOD_DESCRIPTOR =
      chain(
          fixedChar('('),
          repeat(PARAMETER_DESCRIPTOR),
          fixedChar(')'),
          RETURN_DESCRIPTOR,
          (s, args, s2, ret) -> new MethodDescriptorNode(args, ret));

  private MethodDescriptorGrammar() {}

  /**
   * Parse the given method descriptor and return its parse tree.
   *
   * @param input The string to parse.
   * @return A parsed out result, or null if not valid method descriptor.
   */
  public static @Nullable MethodDescriptorNode parse(@NotNull String input) {
    return METHOD_DESCRIPTOR.parse(input);
  }
}
