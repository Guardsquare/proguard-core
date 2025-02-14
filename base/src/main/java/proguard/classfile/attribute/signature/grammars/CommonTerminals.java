package proguard.classfile.attribute.signature.grammars;

import java.io.EOFException;
import proguard.classfile.attribute.signature.ast.descriptor.BaseTypeNode;
import proguard.classfile.attribute.signature.ast.descriptor.VoidDescriptorNode;
import proguard.classfile.attribute.signature.parsing.Parser;

/** Utility class containing common terminals from the signature and descriptor grammar. */
final class CommonTerminals {
  static final Parser<BaseTypeNode> BASE_TYPE =
      (context) -> {
        try {
          BaseTypeNode result = BaseTypeNode.valueOf(String.valueOf(context.peekChar(0)));

          context.advance(1);
          return result;
        } catch (IllegalArgumentException | EOFException e) {
          // not possible to parse
          return null;
        }
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
        try {
          if (context.peekChar(0) == 'V') {
            context.advance(1);
            return VoidDescriptorNode.INSTANCE;
          } else {
            return null;
          }
        } catch (EOFException e) {
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
