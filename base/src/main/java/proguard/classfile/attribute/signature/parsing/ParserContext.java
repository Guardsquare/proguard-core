package proguard.classfile.attribute.signature.parsing;

import java.io.EOFException;
import java.util.ArrayDeque;

/**
 * An object for storing the data of a currently running parsing operation. Wraps the input string
 * and also stores the current offset that is being parsed.
 */
public final class ParserContext {
  private final String input;
  private int startIndex;
  private final ArrayDeque<Integer> snapshots = new ArrayDeque<>();

  /**
   * Initialize a new parser context from the given input string. Used for starting a new parser
   * pass.
   *
   * @param input The input string to parse.
   */
  public ParserContext(String input) {
    this.input = input;
    this.startIndex = 0;
  }

  /**
   * Take a snapshot of the current state of the parser. Useful for handling recursion, where
   * children could fail parsing.
   *
   * <p>Always MUST be paired with one of {@link #revert()} or {@link #commit()}.
   */
  public void snapshot() {
    snapshots.push(startIndex);
  }

  /** Drop the last snapshot, use the currently valid parser state. */
  public void commit() {
    snapshots.pop();
  }

  /** Reverts to the last valid snapshot. */
  public void revert() {
    startIndex = snapshots.pop();
  }

  /**
   * Look-ahead at the input string.
   *
   * @param offset The number of characters that should be skipped. E.g. 0 means look at the
   *     currently parsed character.
   * @return The character at the proper offset in the input.
   * @throws EOFException When the read attempts to read anything past the end of the input string.
   */
  public char peekChar(int offset) throws EOFException {
    if (offset + startIndex == input.length()) {
      throw new EOFException();
    }

    return input.charAt(startIndex + offset);
  }

  /**
   * Same as {@link #peekChar(int)}, but doesn't check for the end of the string. Useful in loops
   * bounded by {@link #remainingLength()} to avoid the need for a try-catch block.
   *
   * @param offset The offset of the character to look at.
   * @return The character at the given position.
   */
  public char peekCharUnchecked(int offset) {
    return input.charAt(startIndex + offset);
  }

  /**
   * Consume the given number of characters.
   *
   * @param amount The number of characters to consider parsed.
   */
  public void advance(int amount) {
    startIndex += amount;
  }

  /**
   * @return The remaining length of the input string.
   */
  public int remainingLength() {
    return input.length() - startIndex;
  }

  /**
   * Consume {@literal length} characters from the input string, return them in a newly constructed
   * string.
   *
   * @param length The number of characters to consume from the beginning. (The method just calls
   *     {@link #advance(int)} internally with this number.)
   * @return A string of length {@literal length} that appears at the current position in the input.
   */
  public String chopFront(int length) {
    String retVal = input.substring(startIndex, startIndex + length);
    this.advance(retVal.length());
    return retVal;
  }

  /**
   * Tests whether the current state starts with the given string.
   *
   * @param prefix The prefix we are checking for.
   * @return True if the current input at the current offset starts with the given string.
   */
  public boolean startsWith(String prefix) {
    return input.startsWith(prefix, startIndex);
  }

  /**
   * Returns first index of a given character in the remaining part of the input string.
   *
   * @param c The character to search for.
   * @return The offset of the character from the current position, or -1 if not found.
   */
  public int indexOf(char c) {
    int result = input.indexOf(c, startIndex);
    if (result == -1) {
      return -1;
    } else {
      return result - startIndex;
    }
  }
}
