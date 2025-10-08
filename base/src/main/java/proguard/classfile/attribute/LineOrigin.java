package proguard.classfile.attribute;

public interface LineOrigin {
  enum SimpleOrigin implements LineOrigin {
    COPIED,
    NO_LINE
  }
}
