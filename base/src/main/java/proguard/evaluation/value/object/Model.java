package proguard.evaluation.value.object;

import org.jetbrains.annotations.NotNull;

/**
 * This interface can be implemented for each class that needs to be modeled during an analysis.
 *
 * <p>The data tracked and logic to handle the methods of the modeled class are
 * implementation-specific.
 */
public interface Model {

  /** Returns the type of the modeled class. */
  @NotNull
  String getType();
}
