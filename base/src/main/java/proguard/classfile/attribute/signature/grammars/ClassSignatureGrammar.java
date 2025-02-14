package proguard.classfile.attribute.signature.grammars;

import static proguard.classfile.attribute.signature.parsing.Combinators.chain;
import static proguard.classfile.attribute.signature.parsing.Combinators.optional;
import static proguard.classfile.attribute.signature.parsing.Combinators.repeat;
import static proguard.classfile.attribute.signature.parsing.Parsers.fixedChar;

import java.util.Collections;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import proguard.classfile.attribute.signature.ast.signature.ClassBoundNode;
import proguard.classfile.attribute.signature.ast.signature.ClassSignatureNode;
import proguard.classfile.attribute.signature.ast.signature.InterfaceBoundNode;
import proguard.classfile.attribute.signature.ast.signature.SuperclassSignatureNode;
import proguard.classfile.attribute.signature.ast.signature.SuperinterfaceSignatureNode;
import proguard.classfile.attribute.signature.ast.signature.TypeParameterNode;
import proguard.classfile.attribute.signature.parsing.Parser;

/**
 * Parser for class signature grammar, defined to be closely matching the grammar definition for
 * ease of maintenance.
 *
 * @see <a
 *     href="https://docs.oracle.com/javase/specs/jvms/se23/html/jvms-4.html#jvms-4.7.9.1">signature
 *     grammar</a>
 */
public class ClassSignatureGrammar {
  static final Parser<ClassBoundNode> CLASS_BOUND =
      chain(
          fixedChar(':'),
          optional(TypeSignatureGrammar.REFERENCE_TYPE_SIGNATURE),
          (nothing, ref) -> new ClassBoundNode(ref.orElse(null)));

  static final Parser<InterfaceBoundNode> INTERFACE_BOUND =
      chain(
          fixedChar(':'),
          TypeSignatureGrammar.REFERENCE_TYPE_SIGNATURE,
          (nothing, ref) -> new InterfaceBoundNode(ref));

  static final Parser<TypeParameterNode> TYPE_PARAMETER =
      chain(
          CommonTerminals.IDENTIFIER, CLASS_BOUND, repeat(INTERFACE_BOUND), TypeParameterNode::new);

  static final Parser<List<TypeParameterNode>> TYPE_PARAMETERS =
      chain(
          fixedChar('<'),
          TYPE_PARAMETER,
          repeat(TYPE_PARAMETER),
          fixedChar('>'),
          (nothing, firstParam, params, nothing2) -> {
            params.add(0, firstParam);
            return params;
          });

  static final Parser<SuperinterfaceSignatureNode> SUPERINTERFACE_SIGNATURE =
      TypeSignatureGrammar.CLASS_TYPE_SIGNATURE.map(SuperinterfaceSignatureNode::new);

  static final Parser<SuperclassSignatureNode> SUPERCLASS_SIGNATURE =
      TypeSignatureGrammar.CLASS_TYPE_SIGNATURE.map(SuperclassSignatureNode::new);

  static final Parser<ClassSignatureNode> CLASS_SIGNATURE =
      chain(
          optional(TYPE_PARAMETERS),
          SUPERCLASS_SIGNATURE,
          repeat(SUPERINTERFACE_SIGNATURE),
          (typeParams, superClass, superInterfaces) ->
              new ClassSignatureNode(
                  typeParams.orElseGet(Collections::emptyList), superClass, superInterfaces));

  private ClassSignatureGrammar() {}

  /**
   * Parse the given class signature and return its parse tree.
   *
   * @param input The string to parse.
   * @return A parsed out result, or null if not valid class signature.
   */
  public static @Nullable ClassSignatureNode parse(@NotNull String input) {
    return CLASS_SIGNATURE.parse(input);
  }
}
