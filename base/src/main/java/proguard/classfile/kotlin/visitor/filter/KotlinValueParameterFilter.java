package proguard.classfile.kotlin.visitor.filter;

import proguard.classfile.Clazz;
import proguard.classfile.kotlin.*;
import proguard.classfile.kotlin.visitor.KotlinValueParameterVisitor;

import java.util.function.Predicate;

/**
 * This {@link KotlinValueParameterVisitor} delegates to another KotlinValueParameterVisitor if the
 * predicate succeeds.
 *
 * @author James Hamilton
 */
public class KotlinValueParameterFilter
implements   KotlinValueParameterVisitor
{
    private final Predicate<KotlinValueParameterMetadata> predicate;
    private final KotlinValueParameterVisitor             acceptedVisitor;
    private final KotlinValueParameterVisitor             rejectedVisitor;


    public KotlinValueParameterFilter(Predicate<KotlinValueParameterMetadata> predicate,
                                      KotlinValueParameterVisitor             acceptedVisitor,
                                      KotlinValueParameterVisitor             rejectedVisitor)
    {
        this.predicate       = predicate;
        this.acceptedVisitor = acceptedVisitor;
        this.rejectedVisitor = rejectedVisitor;
    }


    public KotlinValueParameterFilter(Predicate<KotlinValueParameterMetadata> predicate,
                                      KotlinValueParameterVisitor             acceptedVisitor)
    {
        this(predicate, acceptedVisitor, null);
    }


    @Override
    public void visitAnyValueParameter(Clazz clazz, KotlinValueParameterMetadata kotlinValueParameterMetadata) { }


    @Override
    public void visitConstructorValParameter(Clazz                        clazz,
                                             KotlinClassKindMetadata      kotlinClassKindMetadata,
                                             KotlinConstructorMetadata    kotlinConstructorMetadata,
                                             KotlinValueParameterMetadata kotlinValueParameterMetadata)
    {
        KotlinValueParameterVisitor delegate = this.getDelegate(kotlinValueParameterMetadata);

        if (delegate != null)
        {
            kotlinValueParameterMetadata.accept(
                    clazz,
                    kotlinClassKindMetadata,
                    kotlinConstructorMetadata,
                    delegate
            );
        }
    }


    @Override
    public void visitFunctionValParameter(Clazz                        clazz,
                                          KotlinMetadata               kotlinMetadata,
                                          KotlinFunctionMetadata       kotlinFunctionMetadata,
                                          KotlinValueParameterMetadata kotlinValueParameterMetadata)
    {
        KotlinValueParameterVisitor delegate = this.getDelegate(kotlinValueParameterMetadata);

        if (delegate != null)
        {
            kotlinValueParameterMetadata.accept(
                    clazz,
                    kotlinMetadata,
                    kotlinFunctionMetadata,
                    delegate
            );
        }
    }


    @Override
    public void visitPropertyValParameter(Clazz                              clazz,
                                          KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
                                          KotlinPropertyMetadata             kotlinPropertyMetadata,
                                          KotlinValueParameterMetadata       kotlinValueParameterMetadata)
    {
        KotlinValueParameterVisitor delegate = this.getDelegate(kotlinValueParameterMetadata);

        if (delegate != null)
        {
            kotlinValueParameterMetadata.accept(
                    clazz,
                    kotlinDeclarationContainerMetadata,
                    kotlinPropertyMetadata, delegate
            );
        }
    }


    private KotlinValueParameterVisitor getDelegate(KotlinValueParameterMetadata kotlinValueParameterMetadata)
    {
        return this.predicate.test(kotlinValueParameterMetadata) ?
                this.acceptedVisitor : this.rejectedVisitor;
    }
}
