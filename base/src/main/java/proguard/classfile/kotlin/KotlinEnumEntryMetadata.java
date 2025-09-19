package proguard.classfile.kotlin;

import java.util.List;
import proguard.classfile.Clazz;
import proguard.classfile.Field;
import proguard.classfile.kotlin.visitor.KotlinAnnotationVisitor;
import proguard.util.Processable;
import proguard.util.SimpleProcessable;

public class KotlinEnumEntryMetadata extends SimpleProcessable
    implements Processable, KotlinAnnotatable {

  public String name;
  public Field referencedEnumEntry;
  public List<KotlinAnnotation> annotations;

  public KotlinEnumEntryMetadata(String name) {
    this.name = name;
  }

  @Override
  public void annotationsAccept(Clazz clazz, KotlinAnnotationVisitor kotlinAnnotationVisitor) {
    for (KotlinAnnotation annotation : annotations) {
      kotlinAnnotationVisitor.visitEnumEntryAnnotation(clazz, this, annotation);
    }
  }
}
