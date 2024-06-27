package proguard.classfile.util;

import java.util.Iterator;

/**
 * This class wraps {@link InternalTypeEnumeration} to provide an Iterable interface for use with
 * enhanced for loops.
 */
public class InternalTypeIterable implements Iterable<String> {
  private final InternalTypeEnumeration internalTypeEnumeration;

  public InternalTypeIterable(String descriptor) {
    this.internalTypeEnumeration = new InternalTypeEnumeration(descriptor);
  }

  @Override
  public Iterator<String> iterator() {
    internalTypeEnumeration.reset();
    return internalTypeEnumeration;
  }
}
