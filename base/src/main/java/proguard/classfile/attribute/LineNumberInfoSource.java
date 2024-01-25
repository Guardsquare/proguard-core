package proguard.classfile.attribute;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import proguard.classfile.JavaTypeConstants;
import proguard.classfile.MethodSignature;
import proguard.classfile.util.ClassUtil;
import proguard.exception.ErrorId;
import proguard.exception.ProguardCoreException;

/**
 * This class represents the source info attached to a line number for methods that were inlined,
 * moved from a different class, etc.
 */
public class LineNumberInfoSource {
  // This pattern matches
  // - Two optional "transformed" line numbers, each followed by ":".
  // - A fully qualified method name (internal format, class and method separated with a dot),
  // split into the class, method name and descriptor groups.
  // - Two optional "original" line numbers, each preceded by ":".
  // Matching examples are "package/Foo.bar(ILjava/lang/String)V", "1:2:p/F.b()V:3:4" and
  // "p/F.b()V:5"
  private static final Pattern sourcePattern =
      Pattern.compile(
          "(?:(\\d+):)?(?:(\\d+):)?([^.]+)\\.([^\\(]+)(\\(.*\\)[^:]+)(?::(\\d+))?(?::(\\d+))?");

  public static final int NONE = -1;

  private final MethodSignature signature;
  private int transformedStart;
  private int transformedEnd;
  private int originalStart;
  private int originalEnd;

  /** Create a LineNumberInfoSource of the form "void com.example.Foo.bar()" */
  public LineNumberInfoSource(MethodSignature signature) {
    this.signature = signature;
    this.transformedStart = NONE;
    this.transformedEnd = NONE;
    this.originalStart = NONE;
    this.originalEnd = NONE;
  }

  /** Create a LineNumberInfoSource of the form "100:200:void com.example.Foo.bar()" */
  public LineNumberInfoSource(int transformedStart, int transformedEnd, MethodSignature signature) {
    this.signature = signature;
    this.transformedStart = transformedStart;
    this.transformedEnd = transformedEnd;
    this.originalStart = NONE;
    this.originalEnd = NONE;
  }

  /** Create a LineNumberInfoSource of the form "void com.example.Foo.bar():300:400" */
  public LineNumberInfoSource(MethodSignature signature, int originalStart, int originalEnd) {
    this.signature = signature;
    this.transformedStart = NONE;
    this.transformedEnd = NONE;
    this.originalStart = originalStart;
    this.originalEnd = originalEnd;
  }

  /** Create a LineNumberInfoSource of the form "100:200:void com.example.Foo.bar():300:400" */
  public LineNumberInfoSource(
      int transformedStart,
      int transformedEnd,
      MethodSignature signature,
      int originalStart,
      int originalEnd) {
    this.signature = signature;
    this.transformedStart = transformedStart;
    this.transformedEnd = transformedEnd;
    this.originalStart = originalStart;
    this.originalEnd = originalEnd;
  }

  public static LineNumberInfoSource fromString(String source) {
    Matcher matcher = sourcePattern.matcher(source);
    if (matcher.matches()) {
      int transformedStart = matcher.group(1) == null ? NONE : Integer.parseInt(matcher.group(1));
      int transformedEnd = matcher.group(2) == null ? NONE : Integer.parseInt(matcher.group(2));
      MethodSignature signature =
          new MethodSignature(matcher.group(3), matcher.group(4), matcher.group(5));
      int originalStart = matcher.group(6) == null ? NONE : Integer.parseInt(matcher.group(6));
      int originalEnd = matcher.group(7) == null ? NONE : Integer.parseInt(matcher.group(7));
      return new LineNumberInfoSource(
          transformedStart, transformedEnd, signature, originalStart, originalEnd);
    } else {
      throw new ProguardCoreException(
          ErrorId.INVALID_MAPPING_SOURCE, "Failed to parse invalid line number source: " + source);
    }
  }

  /**
   * Get a string representation that matches the format expected by {@link ExtendedLineNumberInfo}.
   */
  @Deprecated
  public String toInternalString() {
    StringBuilder sb = new StringBuilder();

    if (transformedStart != NONE) {
      sb.append(transformedStart).append(':');
    }
    if (transformedEnd != NONE) {
      sb.append(transformedEnd).append(':');
    }

    sb.append(signature.getClassName()).append(".");
    sb.append(signature.method).append(signature.descriptor.toString());

    if (originalStart != NONE) {
      sb.append(':').append(originalStart);
    }
    if (originalEnd != NONE) {
      sb.append(':').append(originalEnd);
    }

    return sb.toString();
  }

  /** Get a string representation for use in mapping files. */
  public String toExternalString(boolean includeClass) {
    StringBuilder sb = new StringBuilder();

    if (transformedStart != NONE) {
      sb.append(transformedStart).append(':');
    }
    if (transformedEnd != NONE) {
      sb.append(transformedEnd).append(':');
    }

    sb.append(ClassUtil.externalType(signature.descriptor.getReturnType())).append(" ");
    if (includeClass) {
      sb.append(ClassUtil.externalClassName(signature.getClassName())).append(".");
    }
    sb.append(signature.method)
        .append(JavaTypeConstants.METHOD_ARGUMENTS_OPEN)
        .append(ClassUtil.externalMethodArguments(signature.descriptor.toString()))
        .append(JavaTypeConstants.METHOD_ARGUMENTS_CLOSE);

    if (originalStart != NONE) {
      sb.append(':').append(originalStart);
    }
    if (originalEnd != NONE) {
      sb.append(':').append(originalEnd);
    }

    return sb.toString();
  }

  public int getTransformedStart() {
    return transformedStart;
  }

  public int getTransformedEnd() {
    return transformedEnd;
  }

  public int getOriginalStart() {
    return originalStart;
  }

  public int getOriginalEnd() {
    return originalEnd;
  }

  public void setTransformedRange(int start, int end) {
    transformedStart = start;
    transformedEnd = end;
  }

  /**
   * Set the transformed range to have the given start line, and the same size as the original
   * range.
   */
  public void setTransformedRangeBasedOnOriginalRange(int start) {
    setTransformedRange(start, start + originalEnd - originalStart);
  }

  /**
   * Set the original range to be a single line, to indicate the line where another method was
   * inlined.
   */
  public void setOriginalRangeToInlineAt(int inlineLine) {
    originalStart = inlineLine;
    originalEnd = NONE;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    LineNumberInfoSource that = (LineNumberInfoSource) o;
    return transformedStart == that.transformedStart
        && transformedEnd == that.transformedEnd
        && originalStart == that.originalStart
        && originalEnd == that.originalEnd
        && Objects.equals(signature, that.signature);
  }

  @Override
  public int hashCode() {
    return Objects.hash(signature, transformedStart, transformedEnd, originalStart, originalEnd);
  }
}
