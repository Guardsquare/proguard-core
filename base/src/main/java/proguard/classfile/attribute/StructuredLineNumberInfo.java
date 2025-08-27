package proguard.classfile.attribute;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Line number info with extra information for lines that came from a different source than the
 * current method (e.g. inlined from a different method). Tracking this information is necessary for
 * printing a correct mapping file.
 *
 * <p>The structured line number info contains:
 *
 * <ul>
 *   <li>Source string indicating the originating class/method of the line.
 *   <li>Optional start/end line number indicating the original line numbers for a block of inlined
 *       instructions.
 *   <li>A list of origins, indicating the transformations these lines underwent, e.g. multiple
 *       levels of inlining
 *   <li>A block ID, that identifies a group of lines that belongs to a singular block.
 * </ul>
 *
 * <p>Since these transformations operate on blocks of lines, and the mapping file identifies blocks
 * rather than singular lines, {@code StructuredLineNumberInfo} objects are generated through a
 * {@link StructuredLineNumberInfo.Block} object, which is a factory object that generates {@code
 * StructuredLineNumberInfo}s with consistent block IDs.
 */
public class StructuredLineNumberInfo extends LineNumberInfo {

  private final int blockId;
  private final String sourceMethod;
  private final int sourceLineStart;
  private final int sourceLineEnd;
  private final List<Origin> origin;

  // Required to handle the legacy expectation that lines from the same block use the same string
  // object. Do not rely on this behavior as it may change in the future.
  private static final Map<Integer, String> sourceMap = new HashMap<>();

  public String getSourceMethod() {
    return sourceMethod;
  }

  public int getSourceLineStart() {
    return sourceLineStart;
  }

  public int getSourceLineEnd() {
    return sourceLineEnd;
  }

  /**
   * Get the block ID which identifies related line numbers which form a single block in the mapping
   * file.
   */
  public int getBlockId() {
    return blockId;
  }

  /**
   * Get a chronological list of manipulations that this line went through. True origin is the first
   * element, followed by any subsequent manipulations (e.g. multiple levels of inlining, copying,
   * etc.).
   */
  public List<Origin> getOrigin() {
    return origin;
  }

  private StructuredLineNumberInfo(
      int u2startPc,
      int u2lineNumber,
      int blockId,
      List<Origin> origin,
      String sourceMethod,
      int sourceLineStart,
      int sourceLineEnd) {
    super(u2startPc, u2lineNumber);
    this.blockId = blockId;
    this.origin = origin;
    this.sourceMethod = sourceMethod;
    this.sourceLineStart = sourceLineStart;
    this.sourceLineEnd = sourceLineEnd;
  }

  @Override
  public String getSource() {
    // Unfortunately, some code relies on comparing the source object identity to determine blocks.
    // Do not rely on this as it may change in the future.
    if (sourceMethod != null && sourceLineStart != -1 && sourceLineEnd != -1) {
      return sourceMap.computeIfAbsent(
          blockId, id -> sourceMethod + ":" + sourceLineStart + ":" + sourceLineEnd);
    } else {
      return sourceMethod;
    }
  }

  public Block getBlock(Origin... addedOrigins) {
    List<Origin> origins = new ArrayList<>();
    origins.addAll(origin);
    origins.addAll(Arrays.asList(addedOrigins));
    return new Block(blockId, origins, sourceMethod, sourceLineStart, sourceLineEnd);
  }

  public interface Origin {}

  public enum SimpleOrigin implements Origin {
    COPIED
  }

  /**
   * Factory for {@link StructuredLineNumberInfo} objects. Line numbers that form a single block in
   * the mapping file should be generated with the same {@code Block}.
   */
  public static class Block {
    private static int idCounter = 0;

    private static synchronized int getNewId() {
      return idCounter++;
    }

    private final int blockId;
    private final String sourceMethod;

    private final int sourceLineStart;
    private final int sourceLineEnd;

    private final List<Origin> origin;

    private Block(
        int blockId,
        List<Origin> origin,
        String sourceMethod,
        int sourceLineStart,
        int sourceLineEnd) {
      this.blockId = blockId;
      this.sourceMethod = sourceMethod;
      this.sourceLineStart = sourceLineStart;
      this.sourceLineEnd = sourceLineEnd;
      this.origin = origin;
    }

    public Block(List<Origin> origin, String sourceMethod, int sourceLineStart, int sourceLineEnd) {
      this(getNewId(), origin, sourceMethod, sourceLineStart, sourceLineEnd);
    }

    public Block(Origin origin, String sourceMethod, int sourceLineStart, int sourceLineEnd) {
      this(
          getNewId(),
          new ArrayList<>(Arrays.asList(origin)),
          sourceMethod,
          sourceLineStart,
          sourceLineEnd);
    }

    public Block(Origin origin) {
      this(origin, null, -1, -1);
    }

    public StructuredLineNumberInfo line(int u2startPc, int u2lineNumber) {
      return new StructuredLineNumberInfo(
          u2startPc, u2lineNumber, blockId, origin, sourceMethod, sourceLineStart, sourceLineEnd);
    }
  }
}
