package proguard.classfile.attribute.signature;

import java.util.HashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import proguard.classfile.attribute.signature.ast.descriptor.FieldDescriptorNode;
import proguard.classfile.attribute.signature.ast.descriptor.MethodDescriptorNode;
import proguard.classfile.attribute.signature.ast.signature.ClassSignatureNode;
import proguard.classfile.attribute.signature.ast.signature.MethodSignatureNode;
import proguard.classfile.attribute.signature.ast.signature.TypeSignatureNode;

/**
 * {@link SignatureParser} which caches the parsed out ASTs.
 *
 * <p>WARNING: Do not mutate the stored ASTs, the changes would propagate across different runs of
 * the parser. {@see {@link SignatureParser}}
 */
public class CachingSignatureParser extends SignatureParser {
  private final HashMap<String, MethodSignatureNode> methodSignatureCache = new HashMap<>();
  private final HashMap<String, MethodDescriptorNode> methodDescriptorCache = new HashMap<>();
  private final HashMap<String, FieldDescriptorNode> fieldDescriptorCache = new HashMap<>();
  private final HashMap<String, TypeSignatureNode> typeSignatureCache = new HashMap<>();
  private final HashMap<String, ClassSignatureNode> classSignatureCache = new HashMap<>();

  @Override
  public @Nullable MethodSignatureNode parseMethodSignature(@NotNull String input) {
    return methodSignatureCache.computeIfAbsent(input, super::parseMethodSignature);
  }

  @Override
  public @Nullable MethodDescriptorNode parseMethodDescriptor(@NotNull String input) {
    return methodDescriptorCache.computeIfAbsent(input, super::parseMethodDescriptor);
  }

  @Override
  public @Nullable FieldDescriptorNode parseFieldDescriptor(@NotNull String input) {
    return fieldDescriptorCache.computeIfAbsent(input, super::parseFieldDescriptor);
  }

  @Override
  public @Nullable TypeSignatureNode parseTypeSignature(@NotNull String input) {
    return typeSignatureCache.computeIfAbsent(input, super::parseTypeSignature);
  }

  @Override
  public @Nullable ClassSignatureNode parseClassTypeSignature(@NotNull String input) {
    return classSignatureCache.computeIfAbsent(input, super::parseClassTypeSignature);
  }
}
