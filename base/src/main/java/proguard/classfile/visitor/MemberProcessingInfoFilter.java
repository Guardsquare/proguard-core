package proguard.classfile.visitor;

import java.util.function.Predicate;
import proguard.classfile.Clazz;
import proguard.classfile.Member;

/**
 * Delegate all member visits to another given visitor, depending on if the given predicate passes
 * or not.
 */
public class MemberProcessingInfoFilter implements MemberVisitor {

  private final Predicate<Object> predicate;
  private final MemberVisitor acceptedMemberVisitor;
  private final MemberVisitor rejectedMemberVisitor;

  public MemberProcessingInfoFilter(
      Predicate<Object> predicate, MemberVisitor acceptedMemberVisitor) {
    this(predicate, acceptedMemberVisitor, null);
  }

  public MemberProcessingInfoFilter(
      Predicate<Object> predicate,
      MemberVisitor acceptedMemberVisitor,
      MemberVisitor rejectedMemberVisitor) {
    this.predicate = predicate;
    this.acceptedMemberVisitor = acceptedMemberVisitor;
    this.rejectedMemberVisitor = rejectedMemberVisitor;
  }

  // Implementation for MemberVisitor.

  @Override
  public void visitAnyMember(Clazz clazz, Member member) {
    MemberVisitor delegate = getDelegate(member);
    if (delegate != null) member.accept(clazz, delegate);
  }

  // Helper methods.

  private MemberVisitor getDelegate(Member member) {
    return predicate.test(member.getProcessingInfo())
        ? acceptedMemberVisitor
        : rejectedMemberVisitor;
  }
}
