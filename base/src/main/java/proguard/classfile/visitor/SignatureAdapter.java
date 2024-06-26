package proguard.classfile.visitor;

import java.util.function.Consumer;
import proguard.classfile.Clazz;
import proguard.classfile.Member;
import proguard.classfile.Signature;

/** This {@link MemberVisitor} provides the consumer with a corresponding {@link Signature}. */
public class SignatureAdapter<T extends Signature> implements MemberVisitor {
  private final Consumer<T> consumer;

  public SignatureAdapter(Consumer<T> consumer) {
    this.consumer = consumer;
  }

  @Override
  public void visitAnyMember(Clazz clazz, Member member) {
    this.consumer.accept((T) Signature.of(clazz, member));
  }
}
