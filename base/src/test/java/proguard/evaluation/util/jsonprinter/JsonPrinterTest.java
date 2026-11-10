package proguard.evaluation.util.jsonprinter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

/** Tests for {@link JsonPrinter}. */
class JsonPrinterTest {
  @Test
  void testStringValuesAreEscaped() {
    StringBuilder builder = new StringBuilder();
    new ErrorRecord(0, "Cannot generalize \"stacks\"\nsee C:\\temp\u0001").toJson(builder);

    assertEquals(
        "{\"instructionOffset\":0,\"message\":\"Cannot generalize \\\"stacks\\\"\\nsee C:\\\\temp\\u0001\"}",
        builder.toString());
  }

  @Test
  void testStringListValuesAreEscaped() {
    StringBuilder builder = new StringBuilder();
    JsonPrinter.stringListToJson("values", Arrays.asList("a\"b", "c\\d"), builder);

    assertEquals("\"values\":[\"a\\\"b\",\"c\\\\d\"]", builder.toString());
  }
}
