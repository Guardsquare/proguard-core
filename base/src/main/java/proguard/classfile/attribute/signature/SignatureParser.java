package proguard.classfile.attribute.signature;

import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import proguard.classfile.attribute.signature.ast.descriptor.FieldDescriptorNode;
import proguard.classfile.attribute.signature.ast.descriptor.MethodDescriptorNode;
import proguard.classfile.attribute.signature.ast.signature.ClassSignatureNode;
import proguard.classfile.attribute.signature.ast.signature.MethodSignatureNode;
import proguard.classfile.attribute.signature.ast.signature.TypeSignatureNode;
import proguard.classfile.attribute.signature.ast.visitor.ASTNode;
import proguard.classfile.attribute.signature.ast.visitor.ASTNodeVisitor;
import proguard.classfile.attribute.signature.grammars.ClassSignatureGrammar;
import proguard.classfile.attribute.signature.grammars.FieldDescriptorGrammar;
import proguard.classfile.attribute.signature.grammars.MethodDescriptorGrammar;
import proguard.classfile.attribute.signature.grammars.MethodSignatureGrammar;
import proguard.classfile.attribute.signature.grammars.TypeSignatureGrammar;
import proguard.exception.ErrorId;
import proguard.exception.ProguardCoreException;

/**
 * Basic accessor class to the signature and descriptor parsers. Doesn't do anything fancy, always
 * parses the input and returns the parsed AST.
 *
 * <p>{@see {@link CachingSignatureParser}}
 */
public class SignatureParser {
  public SignatureParser() {}

  public @Nullable MethodSignatureNode parseMethodSignature(@NotNull String input) {
    return MethodSignatureGrammar.parse(input);
  }

  public @Nullable MethodDescriptorNode parseMethodDescriptor(@NotNull String input) {
    return MethodDescriptorGrammar.parse(input);
  }

  public @Nullable FieldDescriptorNode parseFieldDescriptor(@NotNull String input) {
    return FieldDescriptorGrammar.parse(input);
  }

  public @Nullable TypeSignatureNode parseTypeSignature(@NotNull String input) {
    return TypeSignatureGrammar.parse(input);
  }

  public @Nullable ClassSignatureNode parseClassTypeSignature(@NotNull String input) {
    return ClassSignatureGrammar.parse(input);
  }

  public Optional<ASTNode> parseSignatureOrDescriptor(@NotNull String input) {
    return Optional.<ASTNode>ofNullable(parseMethodSignature(input))
        .map(Optional::of)
        .orElse(Optional.ofNullable(parseTypeSignature(input)))
        .map(Optional::of)
        .orElse(Optional.ofNullable(parseMethodDescriptor(input)))
        .map(Optional::of)
        .orElse(Optional.ofNullable(parseFieldDescriptor(input)))
        .map(Optional::of)
        .orElse(Optional.ofNullable(parseClassTypeSignature(input)));
  }

  public <R, P> R parseAndAccept(@NotNull String input, ASTNodeVisitor<R, P> astNodeVisitor, P p) {
    ASTNode node =
        parseSignatureOrDescriptor(input)
            .orElseThrow(
                () ->
                    new ProguardCoreException.Builder(
                            "String is not a valid signature or descriptor: %s",
                            ErrorId.SIGNATURE_AST_INVALID_STRUCTURE)
                        .errorParameters(input)
                        .build());
    return node.accept(astNodeVisitor, p);
  }
}
