package proguard.classfile.util.renderer;

import proguard.classfile.Clazz;
import proguard.classfile.Member;
import proguard.classfile.constant.Constant;

/**
 * This utility class serves as a base class for {@link FieldViewModel} and {@link MethodViewModel}.
 * It keeps track of the Member to be rendered and its associated Clazz.
 *
 * @author Kymeng Tang
 */
public abstract class MemberViewModel extends ProcessableViewModel
{
    protected Pair<Clazz, Member> model;
    protected Object              processingInfo;

    /**
     * A utility class for keeping track of pairs, e.g.{@link Clazz} and {@link Member}
     * @param <K>   A generic class.
     * @param <V>   Another generic class.
     */
    public static class Pair<K, V>
    {
        public K key;
        public V value;
        public Pair(K key, V value)
        {
            this.key = key;
            this.value = value;
        }
    }
    /**
     * A constructor to keep track of the {@link Member} object to be rendered and its associated
     * {@link Clazz}
     *
     * @param clazz         The {@link Clazz} that the {@link Constant} entry belongs to.
     * @param member        The {@link Member} object to be rendered.
     */
    protected MemberViewModel(Clazz clazz, Member member)
    {
        this.model = new Pair<>(clazz, member);
        this.processingFlags = renderProcessingFlags(member.getProcessingFlags());
        this.processingInfo = member.getProcessingInfo();
    }
}
