package proguard.classfile.attribute.signature.parsing;

/** Utility class for common helpful parsers. */
public class Parsers {
  private Parsers() {}

  /**
   * Construct a parser which looks for a fixed string in the input.
   *
   * @param terminal The string to look for.
   * @return A parser which returns the string that it matched.
   */
  public static Parser<String> fixedString(String terminal) {
    return (context) -> {
      if (terminal.length() <= context.remainingLength() && context.startsWith(terminal)) {
        context.advance(terminal.length());
        return terminal;
      } else {
        return null;
      }
    };
  }

  /**
   * Construct a parser which looks for a fixed char in the input.
   *
   * @param terminal The char to look for.
   * @return A parser which returns the char that it matched.
   */
  public static Parser<Character> fixedChar(char terminal) {
    return (context) -> {
      if (context.remainingLength() > 0 && context.peekCharUnchecked(0) == terminal) {
        context.advance(1);
        return terminal;
      } else {
        return null;
      }
    };
  }
}
