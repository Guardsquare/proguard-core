package proguard.classfile.attribute.signature.ast;

import static proguard.exception.ErrorId.SIGNATURE_AST_INVALID_STRUCTURE;

import proguard.exception.ProguardCoreException;

public final class ASTStructureException extends ProguardCoreException {
  public ASTStructureException(String message) {
    super(SIGNATURE_AST_INVALID_STRUCTURE, message);
  }
}
