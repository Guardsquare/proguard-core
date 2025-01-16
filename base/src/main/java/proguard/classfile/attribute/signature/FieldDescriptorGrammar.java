package proguard.classfile.attribute.signature;

import static proguard.classfile.attribute.signature.parsing.Combinators.chain;
import static proguard.classfile.attribute.signature.parsing.Combinators.oneOf;
import static proguard.classfile.attribute.signature.parsing.Parsers.fixedChar;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import proguard.classfile.attribute.signature.ast.descriptor.ArrayTypeNode;
import proguard.classfile.attribute.signature.ast.descriptor.ClassTypeNode;
import proguard.classfile.attribute.signature.ast.descriptor.FieldDescriptorNode;
import proguard.classfile.attribute.signature.ast.descriptor.FieldTypeNode;
import proguard.classfile.attribute.signature.parsing.LazyParser;
import proguard.classfile.attribute.signature.parsing.Parser;

/**
 * Implements a field descriptor parser based on the specification.
 *
 * @see <a href="https://docs.oracle.com/javase/specs/jvms/se23/html/jvms-4.html#jvms-4.3.2">field
 *     descriptor grammar</a>
 */
public final class FieldDescriptorGrammar {
  static LazyParser<FieldTypeNode> FIELD_TYPE = new LazyParser<>();

  static final Parser<FieldDescriptorNode> FIELD_DESCRIPTOR =
      FIELD_TYPE.map(FieldDescriptorNode::new);
  static final Parser<ClassTypeNode> CLASS_TYPE =
      chain(
          fixedChar('L'),
          CommonTerminals.CLASS_NAME,
          fixedChar(';'),
          (nothing1, name, nothing2) -> new ClassTypeNode(name));
  static final Parser<FieldTypeNode> COMPONENT_TYPE = FIELD_TYPE;

  static final Parser<ArrayTypeNode> ARRAY_TYPE =
      chain(fixedChar('['), COMPONENT_TYPE, (nothing, component) -> new ArrayTypeNode(component));

  static {
    FIELD_TYPE.setDelegate(
        oneOf(
            CommonTerminals.BASE_TYPE.map(FieldTypeNode::new),
            CLASS_TYPE.map(FieldTypeNode::new),
            ARRAY_TYPE.map(FieldTypeNode::new)));
  }

  private FieldDescriptorGrammar() {}

  /**
   * Parse the given field descriptor and return its parse tree.
   *
   * @param input The string to parse.
   * @return A parsed out result, or null if not valid field descriptor.
   */
  public static @Nullable FieldDescriptorNode parse(@NotNull String input) {
    return FIELD_DESCRIPTOR.parse(input);
  }
}
