package proguard.dexfile.reader.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class InputStreams {

  public static byte[] toByteArray(InputStream is) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    byte[] buff = new byte[4096];
    int read;
    while ((read = is.read(buff)) > 0) {
      out.write(buff, 0, read);
    }
    return out.toByteArray();
  }
}
