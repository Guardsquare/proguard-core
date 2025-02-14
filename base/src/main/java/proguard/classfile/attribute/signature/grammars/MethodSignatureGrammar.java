package proguard.classfile.attribute.signature.grammars;

import static proguard.classfile.attribute.signature.grammars.CommonTerminals.VOID_DESCRIPTOR;
import static proguard.classfile.attribute.signature.parsing.Combinators.chain;
import static proguard.classfile.attribute.signature.parsing.Combinators.oneOf;
import static proguard.classfile.attribute.signature.parsing.Combinators.optional;
import static proguard.classfile.attribute.signature.parsing.Combinators.repeat;
import static proguard.classfile.attribute.signature.parsing.Parsers.fixedChar;

import java.util.Collections;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import proguard.classfile.attribute.signature.ast.signature.MethodSignatureNode;
import proguard.classfile.attribute.signature.ast.signature.ResultNode;
import proguard.classfile.attribute.signature.ast.signature.ThrowsSignatureNode;
import proguard.classfile.attribute.signature.ast.signature.TypeSignatureNode;
import proguard.classfile.attribute.signature.parsing.Parser;

/**
 * Parser for method signature grammar, defined to be closely matching the grammar definition for
 * ease of maintenance.
 *
 * @see <a
 *     href="https://docs.oracle.com/javase/specs/jvms/se23/html/jvms-4.html#jvms-4.7.9.1">signature
 *     grammar</a>
 */
public final class MethodSignatureGrammar {
  static final Parser<List<TypeSignatureNode>> METHOD_SIGNATURE_ARGS =
      chain(
          fixedChar('('),
          repeat(TypeSignatureGrammar.JAVA_TYPE_SIGNATURE),
          fixedChar(')'),
          (nothing, signatures, nothing2) -> signatures);

  static final Parser<ThrowsSignatureNode> THROWS_SIGNATURE =
      oneOf(
          chain(
              fixedChar('^'),
              TypeSignatureGrammar.CLASS_TYPE_SIGNATURE,
              (nothing, signature) -> new ThrowsSignatureNode(signature)),
          chain(
              fixedChar('^'),
              TypeSignatureGrammar.TYPE_VARIABLE_SIGNATURE,
              (nothing, signature) -> new ThrowsSignatureNode(signature)));

  static final Parser<ResultNode> RESULT =
      oneOf(
          TypeSignatureGrammar.JAVA_TYPE_SIGNATURE.map(ResultNode::new),
          VOID_DESCRIPTOR.map(ResultNode::new));

  static final Parser<MethodSignatureNode> METHOD_SIGNATURE =
      chain(
          optional(ClassSignatureGrammar.TYPE_PARAMETERS),
          METHOD_SIGNATURE_ARGS,
          RESULT,
          repeat(THROWS_SIGNATURE),
          (typeParams, args, ret, exceptions) ->
              new MethodSignatureNode(
                  typeParams.orElseGet(Collections::emptyList), args, ret, exceptions));

  private MethodSignatureGrammar() {}

  /**
   * Parse the given method signature and return its parse tree.
   *
   * @param input The string to parse.
   * @return A parsed out result, or null if not valid method signature.
   */
  public static @Nullable MethodSignatureNode parse(@NotNull String input) {
    return METHOD_SIGNATURE.parse(input);
  }
}
