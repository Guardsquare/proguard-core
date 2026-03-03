package proguard.classfile.attribute.signature.grammars;

import proguard.classfile.attribute.signature.ast.descriptor.BaseTypeNode;
import proguard.classfile.attribute.signature.ast.descriptor.VoidDescriptorNode;
import proguard.classfile.attribute.signature.parsing.Parser;

/** Utility class containing common terminals from the signature and descriptor grammar. */
final class CommonTerminals {
  private static final BaseTypeNode[] baseTypeNodes = new BaseTypeNode[256];

  static {
    for (BaseTypeNode node : BaseTypeNode.values()) {
      char c = node.toString().charAt(0);
      if (c < baseTypeNodes.length) {
        baseTypeNodes[c] = node;
      } // else case should never happen, baseTypeNodes are simple alphabetic characters
    }
  }

  static final Parser<BaseTypeNode> BASE_TYPE =
      (context) -> {
        if (context.remainingLength() == 0) {
          return null;
        }
        char c = context.peekCharUnchecked(0);

        if (c < baseTypeNodes.length && baseTypeNodes[c] != null) {
          context.advance(1);
          return baseTypeNodes[c];
        }

        return null;
      };

  static final Parser<String> CLASS_NAME =
      (context) -> {
        int semicolonIndex = context.indexOf(';');
        if (semicolonIndex == -1) {
          return null;
        } else {
          return context.chopFront(semicolonIndex);
        }
      };

  static final Parser<VoidDescriptorNode> VOID_DESCRIPTOR =
      (context) -> {
        if (context.remainingLength() != 0 && context.peekCharUnchecked(0) == 'V') {
          context.advance(1);
          return VoidDescriptorNode.INSTANCE;
        } else {
          return null;
        }
      };

  static final Parser<String> IDENTIFIER =
      (context) -> {
        int length;
        loop:
        for (length = 0; length < context.remainingLength(); length++) {
          switch (context.peekCharUnchecked(length)) {
            case '.':
            case ';':
            case '[':
            case '/':
            case '<':
            case '>':
            case ':':
              break loop;
            default:
              // just continue
          }
        }

        if (length == 0) {
          return null;
        } else {
          return context.chopFront(length);
        }
      };

  private CommonTerminals() {}
}
