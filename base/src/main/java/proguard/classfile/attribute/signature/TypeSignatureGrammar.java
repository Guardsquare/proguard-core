package proguard.classfile.attribute.signature;

import static proguard.classfile.attribute.signature.CommonTerminals.BASE_TYPE;
import static proguard.classfile.attribute.signature.parsing.Combinators.chain;
import static proguard.classfile.attribute.signature.parsing.Combinators.oneOf;
import static proguard.classfile.attribute.signature.parsing.Combinators.optional;
import static proguard.classfile.attribute.signature.parsing.Combinators.repeat;
import static proguard.classfile.attribute.signature.parsing.Parsers.fixedChar;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import proguard.classfile.attribute.signature.ast.signature.AnyTypeArgumentNode;
import proguard.classfile.attribute.signature.ast.signature.ArrayTypeSignatureNode;
import proguard.classfile.attribute.signature.ast.signature.BoundedTypeArgumentNode;
import proguard.classfile.attribute.signature.ast.signature.ClassTypeSignatureNode;
import proguard.classfile.attribute.signature.ast.signature.JavaTypeSignatureNode;
import proguard.classfile.attribute.signature.ast.signature.PackageSpecifierNode;
import proguard.classfile.attribute.signature.ast.signature.ReferenceTypeSignatureNode;
import proguard.classfile.attribute.signature.ast.signature.SimpleClassTypeSignatureNode;
import proguard.classfile.attribute.signature.ast.signature.TypeArgumentNode;
import proguard.classfile.attribute.signature.ast.signature.TypeVariableSignatureNode;
import proguard.classfile.attribute.signature.ast.signature.WildcardIndicatorNode;
import proguard.classfile.attribute.signature.parsing.LazyParser;
import proguard.classfile.attribute.signature.parsing.Parser;

/**
 * Parser for type signature grammar, defined to be closely matching the grammar definition for ease
 * of maintenance.
 *
 * @see <a
 *     href="https://docs.oracle.com/javase/specs/jvms/se23/html/jvms-4.html#jvms-4.7.9.1">signature
 *     grammar</a>
 */
public final class TypeSignatureGrammar {
  static final Parser<WildcardIndicatorNode> WILDCARD_INDICATOR =
      oneOf(fixedChar('+'), fixedChar('-'))
          .map(
              str -> {
                switch (str) {
                  case '+':
                    return WildcardIndicatorNode.PLUS;
                  case '-':
                    return WildcardIndicatorNode.MINUS;
                  default:
                    return null;
                }
              });

  static LazyParser<ReferenceTypeSignatureNode> REFERENCE_TYPE_SIGNATURE = new LazyParser<>();

  static final Parser<PackageSpecifierNode> PACKAGE_SPECIFIER =
      repeat(chain(CommonTerminals.IDENTIFIER, fixedChar('/'), (id, nothing) -> id))
          .map(PackageSpecifierNode::new);

  static final Parser<TypeArgumentNode> TYPE_ARGUMENT =
      oneOf(
          chain(
              optional(WILDCARD_INDICATOR),
              REFERENCE_TYPE_SIGNATURE,
              (wildcard, reference) ->
                  new TypeArgumentNode(
                      new BoundedTypeArgumentNode(wildcard.orElse(null), reference))),
          fixedChar('*').map(nothing -> new TypeArgumentNode(AnyTypeArgumentNode.INSTANCE)));

  static final Parser<List<TypeArgumentNode>> TYPE_ARGUMENTS =
      chain(
          fixedChar('<'),
          TYPE_ARGUMENT,
          repeat(TYPE_ARGUMENT),
          fixedChar('>'),
          (nothing, args1, otherArgs, nothing2) ->
              Stream.concat(Stream.of(args1), otherArgs.stream()).collect(Collectors.toList()));

  static final Parser<SimpleClassTypeSignatureNode> SIMPLE_CLASS_TYPE_SIGNATURE =
      chain(
          CommonTerminals.IDENTIFIER,
          optional(TYPE_ARGUMENTS),
          (id, args) ->
              new SimpleClassTypeSignatureNode(id, args.orElseGet(Collections::emptyList)));

  static final Parser<SimpleClassTypeSignatureNode> CLASS_TYPE_SIGNATURE_SUFFIX =
      chain(fixedChar('.'), SIMPLE_CLASS_TYPE_SIGNATURE, (nothing, signature) -> signature);

  static final Parser<TypeVariableSignatureNode> TYPE_VARIABLE_SIGNATURE =
      chain(
          fixedChar('T'),
          CommonTerminals.IDENTIFIER,
          fixedChar(';'),
          (nothing, identifier, nothing2) -> new TypeVariableSignatureNode(identifier));

  static final Parser<JavaTypeSignatureNode> JAVA_TYPE_SIGNATURE =
      oneOf(
          REFERENCE_TYPE_SIGNATURE.map(JavaTypeSignatureNode::new),
          BASE_TYPE.map(JavaTypeSignatureNode::new));
  static final Parser<ArrayTypeSignatureNode> ARRAY_TYPE_SIGNATURE =
      chain(
          fixedChar('['),
          JAVA_TYPE_SIGNATURE,
          (nothing, signature) -> new ArrayTypeSignatureNode(signature));

  static final Parser<ClassTypeSignatureNode> CLASS_TYPE_SIGNATURE =
      chain(
          fixedChar('L'),
          optional(PACKAGE_SPECIFIER),
          SIMPLE_CLASS_TYPE_SIGNATURE,
          repeat(CLASS_TYPE_SIGNATURE_SUFFIX),
          fixedChar(';'),
          (n1, pkg, name, suffix, n2) ->
              new ClassTypeSignatureNode(pkg.orElseGet(PackageSpecifierNode::new), name, suffix));

  static {
    REFERENCE_TYPE_SIGNATURE.setDelegate(
        oneOf(
            CLASS_TYPE_SIGNATURE.map(ReferenceTypeSignatureNode::new),
            TYPE_VARIABLE_SIGNATURE.map(ReferenceTypeSignatureNode::new),
            ARRAY_TYPE_SIGNATURE.map(ReferenceTypeSignatureNode::new)));
  }

  private TypeSignatureGrammar() {}

  /**
   * Parse the given type signature and return its parse tree.
   *
   * @param input The string to parse.
   * @return A parsed out result, or null if not valid type signature.
   */
  public static @Nullable JavaTypeSignatureNode parse(@NotNull String input) {
    return JAVA_TYPE_SIGNATURE.parse(input);
  }
}
