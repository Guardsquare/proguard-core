package proguard.dexfile.reader;

import java.util.List;
import java.util.function.Consumer;
import proguard.dexfile.reader.visitors.DexFileVisitor;

public interface BaseDexFileReader {

  int getDexVersion();

  void accept(DexFileVisitor dv);

  List<String> getClassNames();

  void accept(DexFileVisitor dv, int config);

  void accept(DexFileVisitor dv, int classIdx, int config);

  void accept(Consumer<String> stringConsumer);
}
