package proguard.classfile.attribute.signature.parsing;

import org.jetbrains.annotations.Nullable;

/**
 * Lazily initialized parser. Used to avoid problems with recursive definitions of grammar.
 *
 * @param <T> The return type of the parser.
 */
public final class LazyParser<T> implements Parser<T> {
  @Nullable Parser<T> delegate = null;

  /** Configure with the parser that all calls will be forwarded to. */
  public void setDelegate(Parser<T> delegate) {
    if (this.delegate != null) {
      throw new IllegalStateException("Duplicate initialization of LazyParser.");
    }
    this.delegate = delegate;
  }

  @Override
  public @Nullable T parse(ParserContext context) {
    if (this.delegate == null) {
      throw new IllegalStateException("Parser was not initialized before use.");
    }
    return this.delegate.parse(context);
  }
}
