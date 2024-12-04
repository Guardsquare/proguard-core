package proguard.evaluation.value.object.model.reflective;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to identify methods that model behavior for constructors of a class modeled in {@link
 * proguard.evaluation.value.object.model.Model}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ModeledConstructor {
  /** The descriptor of the modeled constructor. */
  String descriptor();
}
