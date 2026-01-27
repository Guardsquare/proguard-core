package proguard.classfile;

import java.util.Objects;
import proguard.classfile.visitor.MemberVisitor;

/** Container class for a pair of class + member. */
public class ClassMemberPair {
  public final Clazz clazz;
  public final Member member;

  public ClassMemberPair(Clazz clazz, Member member) {
    this.clazz = clazz;
    this.member = member;
  }

  public void accept(MemberVisitor memberVisitor) {
    this.member.accept(this.clazz, memberVisitor);
  }

  public String getName() {
    return this.member.getName(this.clazz);
  }

  public String getDescriptor() {
    return this.member.getDescriptor(this.clazz);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ClassMemberPair that = (ClassMemberPair) o;
    return Objects.equals(clazz, that.clazz) && Objects.equals(member, that.member);
  }

  @Override
  public int hashCode() {
    return Objects.hash(clazz, member);
  }

  @Override
  public String toString() {
    return clazz.getName()
        + "."
        + this.member.getName(this.clazz)
        + this.member.getDescriptor(this.clazz);
  }
}
