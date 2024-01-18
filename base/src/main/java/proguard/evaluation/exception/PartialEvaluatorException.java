package proguard.evaluation.exception;

import proguard.classfile.Clazz;
import proguard.classfile.Method;
import proguard.classfile.visitor.ClassVisitor;
import proguard.classfile.visitor.MemberVisitor;
import proguard.exception.ProguardCoreException;

public class PartialEvaluatorException extends ProguardCoreException {
  private final Clazz clazz;
  private final Method method;

  public PartialEvaluatorException(
      int componentErrorId,
      Throwable cause,
      Clazz clazz,
      Method method,
      String message,
      String... errorParameters) {
    super(componentErrorId, cause, message, errorParameters);
    this.clazz = clazz;
    this.method = method;
  }

  public Clazz getClazz() {
    return clazz;
  }

  public Method getMethod() {
    return method;
  }

  public void classAccept(ClassVisitor visitor) {
    if (clazz != null) {
      clazz.accept(visitor);
    }
  }

  public void methodAccept(MemberVisitor visitor) {
    if (clazz != null && method != null) {
      method.accept(clazz, visitor);
    }
  }
}
