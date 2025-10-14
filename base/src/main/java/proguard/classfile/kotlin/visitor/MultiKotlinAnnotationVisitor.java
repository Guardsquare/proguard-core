package proguard.classfile.kotlin.visitor;

import proguard.classfile.Clazz;
import proguard.classfile.kotlin.KotlinAnnotatable;
import proguard.classfile.kotlin.KotlinAnnotation;

public class MultiKotlinAnnotationVisitor implements KotlinAnnotationVisitor {
  private final KotlinAnnotationVisitor[] kotlinAnnotationVisitors;

  public MultiKotlinAnnotationVisitor(KotlinAnnotationVisitor... kotlinAnnotationVisitors) {
    this.kotlinAnnotationVisitors = kotlinAnnotationVisitors;
  }

  @Override
  public void visitAnyAnnotation(
      Clazz clazz, KotlinAnnotatable annotatable, KotlinAnnotation annotation) {
    for (KotlinAnnotationVisitor kotlinAnnotationVisitor : kotlinAnnotationVisitors) {
      annotation.accept(clazz, annotatable, kotlinAnnotationVisitor);
    }
  }
}
