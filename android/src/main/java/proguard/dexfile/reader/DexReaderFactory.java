package proguard.dexfile.reader;

import static proguard.dexfile.reader.DexConstants.DEX_041;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import proguard.dexfile.reader.util.InputStreams;

public class DexReaderFactory {

  public static BaseDexFileReader createSingleReader(InputStream in) throws IOException {
    return createSingleReader(InputStreams.toByteArray(in));
  }

  public static BaseDexFileReader createSingleReader(byte[] data) {
    return createSingleReader(ByteBuffer.wrap(data));
  }

  public static BaseDexFileReader createSingleReader(ByteBuffer in) {
    List<DexFileReader> readers = new LinkedList<>();
    int offset = 0;
    boolean end = true;
    do {
      DexFileReader reader = new DexFileReader(in, offset);
      readers.add(reader);
      if (reader.dex_version >= DEX_041 && reader.header_size >= 0x78) {
        end = (reader.container_offset + reader.file_size >= reader.container_size);
        offset += reader.file_size;
      }
    } while (!end);

    return readers.size() > 1 ? new DexContainerReader(readers) : readers.get(0);
  }
}
