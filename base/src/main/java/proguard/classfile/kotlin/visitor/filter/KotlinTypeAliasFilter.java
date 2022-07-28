package proguard.classfile.kotlin.visitor.filter;

import proguard.classfile.Clazz;
import proguard.classfile.kotlin.KotlinDeclarationContainerMetadata;
import proguard.classfile.kotlin.KotlinTypeAliasMetadata;
import proguard.classfile.kotlin.visitor.KotlinTypeAliasVisitor;

import java.util.function.Predicate;

/**
 * Delegates to another {@link KotlinTypeAliasVisitor} if the predicate succeeds;
 * otherwise to the rejected visitor.
 *
 * @author James Hamilton
 */
public class KotlinTypeAliasFilter implements KotlinTypeAliasVisitor
{
    private final Predicate<KotlinTypeAliasMetadata> predicate;
    private final KotlinTypeAliasVisitor             acceptedVisitor;
    private final KotlinTypeAliasVisitor             rejectedVisitor;

    public KotlinTypeAliasFilter(Predicate<KotlinTypeAliasMetadata> predicate,
                                 KotlinTypeAliasVisitor             acceptedVisitor,
                                 KotlinTypeAliasVisitor             rejectedVisitor)
    {
        this.predicate       = predicate;
        this.acceptedVisitor = acceptedVisitor;
        this.rejectedVisitor = rejectedVisitor;
    }

    public KotlinTypeAliasFilter(Predicate<KotlinTypeAliasMetadata> predicate,
                                 KotlinTypeAliasVisitor             acceptedVisitor)
    {
        this(predicate, acceptedVisitor, null);
    }


    @Override
    public void visitTypeAlias(Clazz                              clazz,
                               KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
                               KotlinTypeAliasMetadata            kotlinTypeAliasMetadata)
    {
        KotlinTypeAliasVisitor delegate =
                this.predicate.test(kotlinTypeAliasMetadata) ? this.acceptedVisitor : this.rejectedVisitor;

        if (delegate != null)
        {
            kotlinTypeAliasMetadata.accept(clazz, kotlinDeclarationContainerMetadata, delegate);
        }
    }
}
