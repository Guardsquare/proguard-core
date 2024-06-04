package proguard.dexfile.reader;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import proguard.dexfile.reader.visitors.DexFileVisitor;

public class MultiDexFileReader implements BaseDexFileReader {
  private final List<BaseDexFileReader> readers = new ArrayList<>();
  private final List<Item> items = new ArrayList<>();

  public MultiDexFileReader(Collection<? extends BaseDexFileReader> readers) {
    this.readers.addAll(readers);
    init();
  }

  void init() {
    Set<String> classes = new HashSet<>();
    for (BaseDexFileReader reader : readers) {
      List<String> classNames = reader.getClassNames();
      for (int i = 0; i < classNames.size(); i++) {
        String className = classNames.get(i);
        if (classes.add(className)) {
          items.add(new Item(i, reader, className));
        }
      }
    }
  }

  @Override
  public int getDexVersion() {
    int max = DexConstants.DEX_035;
    for (BaseDexFileReader r : readers) {
      int v = r.getDexVersion();
      if (v > max) {
        max = v;
      }
    }
    return max;
  }

  @Override
  public void accept(DexFileVisitor dv) {
    accept(dv, 0);
  }

  @Override
  public List<String> getClassNames() {
    return new AbstractList<String>() {
      @Override
      public String get(int index) {
        return items.get(index).className;
      }

      @Override
      public int size() {
        return items.size();
      }
    };
  }

  @Override
  public void accept(DexFileVisitor dv, int config) {
    int size = items.size();
    for (int i = 0; i < size; i++) {
      accept(dv, i, config);
    }
  }

  @Override
  public void accept(DexFileVisitor dv, int classIdx, int config) {
    Item item = items.get(classIdx);
    item.reader.accept(dv, item.idx, config);
  }

  @Override
  public void accept(Consumer<String> stringConsumer) {
    for (BaseDexFileReader reader : readers) {
      reader.accept(stringConsumer);
    }
  }

  static class Item {
    int idx;
    BaseDexFileReader reader;
    String className;

    public Item(int i, BaseDexFileReader reader, String className) {
      idx = i;
      this.reader = reader;
      this.className = className;
    }
  }
}
