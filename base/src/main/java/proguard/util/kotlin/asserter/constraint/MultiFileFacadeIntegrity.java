package proguard.util.kotlin.asserter.constraint;

import proguard.classfile.Clazz;
import proguard.classfile.kotlin.KotlinMultiFileFacadeKindMetadata;
import proguard.util.StringUtil;

public class MultiFileFacadeIntegrity extends AbstractKotlinMetadataConstraint {

  @Override
  public void visitKotlinMultiFileFacadeMetadata(
      Clazz clazz, KotlinMultiFileFacadeKindMetadata kotlinMultiFileFacadeKindMetadata) {
    // Multi file class facades and parts are distinct Kotlin metadata types,
    // hence, a facade referencing itself as a part is invalid metadata.
    boolean referencesItself =
        kotlinMultiFileFacadeKindMetadata.partClassNames.contains(clazz.getName());
    if (referencesItself) {
      String error =
          String.format(
              "Multi file facade class \"%s\" contains a reference to itself in its part metadata: %s.",
              clazz.getName(),
              StringUtil.listToString(kotlinMultiFileFacadeKindMetadata.partClassNames));
      reporter.report(error);
    }
  }
}
