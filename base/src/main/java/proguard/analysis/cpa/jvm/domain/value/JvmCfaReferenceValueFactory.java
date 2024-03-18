package proguard.analysis.cpa.jvm.domain.value;

import org.jetbrains.annotations.NotNull;
import proguard.analysis.cpa.jvm.cfa.JvmCfa;
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode;
import proguard.analysis.datastructure.CodeLocation;
import proguard.classfile.Clazz;
import proguard.classfile.Method;
import proguard.evaluation.ParticularReferenceValueFactory;
import proguard.evaluation.value.IdentifiedReferenceValue;
import proguard.evaluation.value.ParticularReferenceValue;
import proguard.evaluation.value.ReferenceValue;
import proguard.evaluation.value.object.AnalyzedObject;

/**
 * This {@link ParticularReferenceValueFactory} creates {@link IdentifiedReferenceValue} and {@link
 * ParticularReferenceValue}s using the creation site as the unique identifier.
 *
 * <p>The identifier will be the {@link JvmCfaNode} of the specified creation site.
 */
public class JvmCfaReferenceValueFactory extends ParticularReferenceValueFactory {
  private final JvmCfa cfa;

  public JvmCfaReferenceValueFactory(JvmCfa cfa) {
    this.cfa = cfa;
  }

  @Override
  public ReferenceValue createReferenceValue(
      String type,
      Clazz referencedClass,
      boolean mayBeExtension,
      boolean mayBeNull,
      Clazz creationClass,
      Method creationMethod,
      int creationOffset) {
    return createReferenceValueForId(
        type,
        referencedClass,
        mayBeExtension,
        mayBeNull,
        cfa.getFunctionNode(creationClass, creationMethod, creationOffset));
  }

  @Override
  public ReferenceValue createReferenceValue(
      Clazz referencedClass,
      boolean mayBeExtension,
      boolean mayBeNull,
      CodeLocation creationLocation,
      @NotNull AnalyzedObject value) {
    checkReferenceValue(value);
    checkCreationLocation(creationLocation);
    return createReferenceValueForId(
        referencedClass,
        mayBeExtension,
        mayBeNull,
        cfa.getFunctionNode(
            creationLocation.clazz, (Method) creationLocation.member, creationLocation.offset),
        value);
  }
}
