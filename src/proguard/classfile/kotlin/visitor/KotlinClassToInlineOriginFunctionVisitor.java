package proguard.classfile.kotlin.visitor;

import proguard.classfile.Clazz;
import proguard.classfile.kotlin.KotlinDeclarationContainerMetadata;
import proguard.classfile.kotlin.KotlinMetadata;
import proguard.classfile.kotlin.visitor.filter.KotlinFunctionFilter;

import static proguard.classfile.TypeConstants.INNER_CLASS_SEPARATOR;

/**
 * This {@link KotlinMetadataVisitor} travels to the function of the provided
 * anonymous object origin and delegates to the given {@link KotlinFunctionVisitor}.
 * 
 * e.g. kotlin/properties/Delegates$observable$1 -> visits the observable.
 *
 * @author James Hamilton
 */
public class KotlinClassToInlineOriginFunctionVisitor
implements   KotlinMetadataVisitor
{
    private final String anonymousObjectOriginName;
    private final KotlinFunctionVisitor kotlinFunctionVisitor;

    public KotlinClassToInlineOriginFunctionVisitor(String                anonymousObjectOriginName,
                                                    KotlinFunctionVisitor kotlinFunctionVisitor)
    {
        this.anonymousObjectOriginName = anonymousObjectOriginName;
        this.kotlinFunctionVisitor     = kotlinFunctionVisitor;
    }

    @Override
    public void visitAnyKotlinMetadata(Clazz clazz, KotlinMetadata kotlinMetadata) { }


    @Override
    public void visitKotlinDeclarationContainerMetadata(Clazz                              clazz,
                                                        KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata)
    {
        if (this.anonymousObjectOriginName == null)
        {
            return;
        }

        int index    = this.anonymousObjectOriginName.indexOf(INNER_CLASS_SEPARATOR) + 1;
        int endIndex = this.anonymousObjectOriginName.indexOf(INNER_CLASS_SEPARATOR, index);
        if (endIndex == -1)
        {
            endIndex = this.anonymousObjectOriginName.length();
        }

        String funName = this.anonymousObjectOriginName.substring(index, endIndex);

        kotlinDeclarationContainerMetadata.functionsAccept(clazz,
            new KotlinFunctionFilter(fun -> fun.name.equals(funName),
                this.kotlinFunctionVisitor));
    }
}
