package proguard.classfile.util.kotlin;

/**
 * The different fields of the Kotlin metadata annotation. <b>Names of these enum values should not
 * be changed!</b>
 */
public enum KotlinMetadataType {
  k,
  mv,
  d1,
  d2,
  xi,
  xs,
  pn,

  @Deprecated
  bv // was removed but older metadata will still contain it.
}
