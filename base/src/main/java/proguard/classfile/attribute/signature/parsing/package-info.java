/**
 * This package contains a small library for writing recursive descent parsers through the use of
 * parser combinators. It is somewhat inspired by <a
 * href="https://github.com/jon-hanson/parsecj">ParsecJ</a>, but simplified to be good enough for
 * our needs.
 *
 * <h1>Debugging</h1>
 *
 * <p>Due to the declarative nature of the parser, it's quite hard to use a debugger with it. The
 * recommended debugging method therefore is the usage of unit tests. Once you find out that
 * something should be parseable, and it is not, write a test for it. Then proceed to minimise the
 * test. Remove parts of the input string that don't make the expectations fail. In the end, you
 * will end up with relatively clear indication about what is wrong, which grammar rule is defined
 * incorrectly.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Parser_combinator">Parser combinators</a>
 */
package proguard.classfile.attribute.signature.parsing;
