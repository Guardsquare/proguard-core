package proguard.classfile.attribute;

public interface LineNumberInfoBlock {
  LineNumberInfo line(int u2StartPc, int u2LineNumber);
}
