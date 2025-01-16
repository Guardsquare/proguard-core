package proguard.classfile.attribute.signature.parsing;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Wrapper class containing parser combinators. Tools to chain together multiple parsers into more
 * complex parsers.
 */
public final class Combinators {
  public interface BiCombinator<A, B, R> {
    R combine(A a, B c);
  }

  public interface TerCombinator<A, B, C, R> {
    R combine(A a, B b, C c);
  }

  public interface QuaterCombinator<A, B, C, D, R> {
    R combine(A a, B b, C c, D d);
  }

  public interface PentaCombinator<A, B, C, D, E, R> {
    R combine(A a, B b, C c, D d, E e);
  }

  /**
   * Take two parsers and return a new one, which will pass iff both parsers succeed when running
   * one after the other.
   *
   * @param aParser The parser that should run first.
   * @param bParser The second parser.
   * @param combinator A function to combine the result of both parsers into one object.
   * @return A new parser which succeeds only when both input parsers succeed.
   * @param <A> Return type of the first parser.
   * @param <B> Return type of the second parser.
   * @param <R> The new return type.
   */
  public static <A, B, R> Parser<R> chain(
      Parser<A> aParser, Parser<B> bParser, BiCombinator<A, B, R> combinator) {
    return (ctx) -> {
      ctx.snapshot();

      A a = aParser.parse(ctx);
      if (a == null) {
        ctx.revert();
        return null;
      }

      B b = bParser.parse(ctx);
      if (b == null) {
        ctx.revert();
        return null;
      }

      ctx.commit();
      return combinator.combine(a, b);
    };
  }

  /** Same as {@link #chain(Parser, Parser, BiCombinator)}, just with 3 parsers. */
  public static <A, B, C, R> Parser<R> chain(
      Parser<A> aParser,
      Parser<B> bParser,
      Parser<C> cParser,
      TerCombinator<A, B, C, R> combinator) {
    return (ctx) -> {
      ctx.snapshot();

      A a = aParser.parse(ctx);
      if (a == null) {
        ctx.revert();
        return null;
      }

      B b = bParser.parse(ctx);
      if (b == null) {
        ctx.revert();
        return null;
      }

      C c = cParser.parse(ctx);
      if (c == null) {
        ctx.revert();
        return null;
      }

      ctx.commit();
      return combinator.combine(a, b, c);
    };
  }

  /** Same as {@link #chain(Parser, Parser, BiCombinator)}, just with 4 parsers. */
  public static <A, B, C, D, R> Parser<R> chain(
      Parser<A> aParser,
      Parser<B> bParser,
      Parser<C> cParser,
      Parser<D> dParser,
      QuaterCombinator<A, B, C, D, R> combinator) {
    return (ctx) -> {
      ctx.snapshot();

      A a = aParser.parse(ctx);
      if (a == null) {
        ctx.revert();
        return null;
      }

      B b = bParser.parse(ctx);
      if (b == null) {
        ctx.revert();
        return null;
      }

      C c = cParser.parse(ctx);
      if (c == null) {
        ctx.revert();
        return null;
      }

      D d = dParser.parse(ctx);
      if (d == null) {
        ctx.revert();
        return null;
      }

      ctx.commit();
      return combinator.combine(a, b, c, d);
    };
  }

  /** Same as {@link #chain(Parser, Parser, BiCombinator)}, just with 5 parsers. */
  public static <A, B, C, D, E, R> Parser<R> chain(
      Parser<A> aParser,
      Parser<B> bParser,
      Parser<C> cParser,
      Parser<D> dParser,
      Parser<E> eParser,
      PentaCombinator<A, B, C, D, E, R> combinator) {
    return (ctx) -> {
      ctx.snapshot();

      A a = aParser.parse(ctx);
      if (a == null) {
        ctx.revert();
        return null;
      }

      B b = bParser.parse(ctx);
      if (b == null) {
        ctx.revert();
        return null;
      }

      C c = cParser.parse(ctx);
      if (c == null) {
        ctx.revert();
        return null;
      }

      D d = dParser.parse(ctx);
      if (d == null) {
        ctx.revert();
        return null;
      }

      E e = eParser.parse(ctx);
      if (e == null) {
        ctx.revert();
        return null;
      }

      ctx.commit();
      return combinator.combine(a, b, c, d, e);
    };
  }

  /**
   * Construct a new parser, that will try to run the given one repeatedly and return a list.
   *
   * <p>Note: This parser always succeeds, because if it won't match anything, it will still return
   * an empty list.
   *
   * @param parser The parser to repeatedly run.
   * @return The new parser.
   * @param <T> Return type of the input parser.
   */
  public static <T> Parser<List<T>> repeat(Parser<T> parser) {
    return (context) -> {
      List<T> result = new ArrayList<>();
      while (true) {
        T res = parser.parse(context);
        if (res == null) {
          break;
        } else {
          result.add(res);
        }
      }
      return result;
    };
  }

  /**
   * Return a parser which succeeds even in cases when it can't parse anything.
   *
   * @param parser A parser to wrap with an optional check.
   * @return A new parser that doesn't fail when no input is successfully parsed.
   * @param <T> The return type of the parser.
   */
  public static <T> Parser<Optional<T>> optional(Parser<T> parser) {
    return (context) -> Optional.ofNullable(parser.parse(context));
  }

  /**
   * Given a list of parsers of the same type, return a result of the first one that succeeds. Or
   * null if none succeed.
   *
   * @param parsers The input parsers to all try.
   * @return A new parser that tries all the given parsers.
   * @param <T> The return type of all the parsers involved.
   */
  @SafeVarargs
  public static <T> Parser<T> oneOf(Parser<? extends T>... parsers) {
    return (ctx) -> {
      for (Parser<? extends T> parser : parsers) {
        T r = parser.parse(ctx);
        if (r != null) {
          return r;
        }
      }
      return null;
    };
  }
}
