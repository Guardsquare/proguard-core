package proguard.io;

import java.io.IOException;

public class MultiDataEntryReader implements DataEntryReader {
  private final DataEntryReader[] readers;

  public MultiDataEntryReader(DataEntryReader... readers) {
    this.readers = readers;
  }

  @Override
  public void read(DataEntry dataEntry) throws IOException {
    for (DataEntryReader reader : readers) {
      reader.read(dataEntry);
    }
  }
}
