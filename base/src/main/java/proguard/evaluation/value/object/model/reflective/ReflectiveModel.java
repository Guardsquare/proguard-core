package proguard.evaluation.value.object.model.reflective;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import proguard.evaluation.MethodResult;
import proguard.evaluation.ValueCalculator;
import proguard.evaluation.executor.MethodExecutionInfo;
import proguard.evaluation.value.Value;
import proguard.evaluation.value.object.model.Model;
import proguard.evaluation.value.object.model.reflective.ModelHelper.MethodExecutionContext;

/**
 * A mixin fully implementing the {@link proguard.evaluation.value.object.model.Model} interface, so
 * that the classes implementing the model don't have to do it themselves. This mixin assumes, that
 * the class implementing it follows these rules:
 *
 * <ul>
 *   <li>Has a constructor with no arguments (can be private) that will be used for creating
 *       instances for static methods, constructors and instance methods with unknown values
 *   <li>Uses {@link ModeledConstructor}, {@link ModeledStaticMethod} and {@link
 *       ModeledInstanceMethod} to mark the model methods. These methods should be private.
 * </ul>
 *
 * <p>All annotated methods should take {@link
 * proguard.evaluation.value.object.model.reflective.ModelHelper.MethodExecutionContext} as the
 * first argument, the remaining arguments are {@link Value} arguments where their number
 * corresponds to the number of arguments the modelled method expects.
 *
 * @param <T> Recursive generic type of the class implementing this interface.
 */
public interface ReflectiveModel<T extends ReflectiveModel<T>> extends Model {
  Logger log = LogManager.getLogger(ReflectiveModel.class);

  @Override
  default MethodResult init(
      MethodExecutionInfo methodExecutionInfo, ValueCalculator valueCalculator) {
    if (this != ModelHelper.getDummyObject(this.getClass())) {
      throw new IllegalStateException("Should not call init on an initialized model");
    }

    MethodExecutionContext context =
        new MethodExecutionContext(methodExecutionInfo, valueCalculator);
    return ModelHelper.executeViaHandler(
        context, ModelHelper.getConstructorHandlers(this.getClass()), this);
  }

  @Override
  default MethodResult invoke(
      MethodExecutionInfo methodExecutionInfo, ValueCalculator valueCalculator) {

    if (this == ModelHelper.getDummyObject(this.getClass())
        && methodExecutionInfo.getInstanceNonStatic().isParticular()) {
      // Should not invoke instance methods on the dummy model unless they are unknown
      throw new IllegalStateException("Should not invoke methods on dummy model");
    }

    MethodExecutionContext context =
        new MethodExecutionContext(methodExecutionInfo, valueCalculator);
    return ModelHelper.executeViaHandler(
        context, ModelHelper.getInstanceMethodHandlers(this.getClass()), this);
  }

  @Override
  default MethodResult invokeStatic(
      MethodExecutionInfo methodExecutionInfo, ValueCalculator valueCalculator) {
    if (this != ModelHelper.getDummyObject(this.getClass())) {
      throw new IllegalStateException("Should not call a static method on an initialized model");
    }

    MethodExecutionContext context =
        new MethodExecutionContext(methodExecutionInfo, valueCalculator);
    return ModelHelper.executeViaHandler(
        context, ModelHelper.getStaticMethodHandlers(this.getClass()), this);
  }
}
