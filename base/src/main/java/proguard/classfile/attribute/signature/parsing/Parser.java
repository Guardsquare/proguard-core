package proguard.classfile.attribute.signature.parsing;

import java.util.function.Function;
import org.jetbrains.annotations.Nullable;

/**
 * Main interface of the parser library, used to implement custom parsers.
 *
 * @param <T> The AST node this parser will be returning.
 */
public interface Parser<T> {
  /**
   * The core of the parsing logic. The context argument specifies where in the input string we are
   * currently parsing. The responsibility of this function is to do one of:
   *
   * <ul>
   *   <li>Consume a part of the input and return a non-null value.
   *   <li>Return null and keep the input context untouched. This can be achieved either by avoiding
   *       the calls to {@link ParserContext#advance(int)}, or through the use of snapshotting (see
   *       {@link ParserContext#snapshot()}).
   * </ul>
   *
   * @param context Context of the parser containing the parsed string and the position in that
   *     string.
   * @return Null-value if this parser did not match anything. Non-null if it matched.
   */
  @Nullable
  T parse(ParserContext context);

  /**
   * Helper function to parse whole strings.
   *
   * @param input A string to parse.
   * @return A parsed out value (likely AST) or null, if it does not match.
   */
  default T parse(String input) {
    ParserContext ctx = new ParserContext(input);
    T result = this.parse(ctx);
    if (ctx.remainingLength() != 0) {
      return null;
    } else {
      return result;
    }
  }

  /**
   * Helper for converting a result of the parser into another type. More or less equivalent to
   * {@link java.util.stream.Stream#map(Function)}.
   *
   * @param function Function that should be used to map the result of the given parser.
   * @return A new parser which returns the expected type.
   * @param <R> The return type of the new parser.
   */
  default <R> Parser<R> map(Function<T, R> function) {
    return (ctx) -> {
      T result = this.parse(ctx);
      if (result == null) {
        return null;
      } else {
        return function.apply(result);
      }
    };
  }
}
