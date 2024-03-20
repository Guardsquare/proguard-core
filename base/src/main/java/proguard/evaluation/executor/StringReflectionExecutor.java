/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2023 Guardsquare NV
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package proguard.evaluation.executor;

import static proguard.classfile.ClassConstants.NAME_JAVA_LANG_STRING;
import static proguard.classfile.ClassConstants.NAME_JAVA_LANG_STRING_BUFFER;
import static proguard.classfile.ClassConstants.NAME_JAVA_LANG_STRING_BUILDER;
import static proguard.classfile.ClassConstants.TYPE_JAVA_LANG_STRING;
import static proguard.classfile.ClassConstants.TYPE_JAVA_LANG_STRING_BUFFER;
import static proguard.classfile.ClassConstants.TYPE_JAVA_LANG_STRING_BUILDER;

import java.util.HashMap;
import java.util.Optional;
import proguard.evaluation.executor.instancehandler.ExecutorMethodInstanceHandler;
import proguard.evaluation.executor.matcher.ExecutorClassMatcher;
import proguard.evaluation.value.ParticularReferenceValue;
import proguard.evaluation.value.ReferenceValue;
import proguard.evaluation.value.object.AnalyzedObject;
import proguard.evaluation.value.object.AnalyzedObjectFactory;
import proguard.util.CollectionMatcher;
import proguard.util.ConstantMatcher;
import proguard.util.FixedStringMatcher;
import proguard.util.StringMatcher;

/**
 * This {@link ReflectionExecutor} supports methods of the classes {@link String}, {@link
 * StringBuilder} and {@link StringBuffer}.
 */
public class StringReflectionExecutor extends ReflectionExecutor {
  public StringReflectionExecutor() {
    executorMatcher =
        new ExecutorClassMatcher(
            new CollectionMatcher(
                NAME_JAVA_LANG_STRING,
                NAME_JAVA_LANG_STRING_BUILDER,
                NAME_JAVA_LANG_STRING_BUFFER));
    instanceHandler =
        new ExecutorMethodInstanceHandler(
            new HashMap<String, StringMatcher>() {
              {
                // Because strings are immutable, assume that string methods always return a new
                // instance. This is technically incorrect (methods like substring may return the
                // instance directly) but acceptable, as the evaluation compares string values
                // and not IDs.
                put(NAME_JAVA_LANG_STRING, new FixedStringMatcher("toString"));

                // StringBuffer and StringBuilder make use of the Builder pattern - all methods
                // which return their own type return the 'this' pointer.
                put(NAME_JAVA_LANG_STRING_BUFFER, new ConstantMatcher(true));
                put(NAME_JAVA_LANG_STRING_BUILDER, new ConstantMatcher(true));
              }
            });
  }

  @Override
  public Optional<AnalyzedObject> getInstanceCopyIfMutable(ReferenceValue instanceValue) {

    if (!(instanceValue instanceof ParticularReferenceValue)) {
      return Optional.empty();
    }

    AnalyzedObject instanceObject = instanceValue.getValue();

    if (instanceObject.isModeled()) {
      throw new IllegalStateException(
          "Should not use reflection on a modeled value, are you sure you are matching the expected executor?");
    }

    if (instanceObject.isNull()) {
      return Optional.empty();
    }

    String type = instanceObject.getType();
    if (type == null) {
      throw new IllegalStateException("Null type on non-null instance object!");
    }
    switch (type) {
      case TYPE_JAVA_LANG_STRING_BUILDER:
        return Optional.of(
            AnalyzedObjectFactory.createPrecise(
                new StringBuilder((StringBuilder) instanceObject.getPreciseValue())));
      case TYPE_JAVA_LANG_STRING_BUFFER:
        return Optional.of(
            AnalyzedObjectFactory.createPrecise(
                new StringBuffer((StringBuffer) instanceObject.getPreciseValue())));
      case TYPE_JAVA_LANG_STRING:
        return Optional.of(instanceObject);
      default:
        return Optional.empty();
    }
  }

  public static class Builder extends Executor.Builder<StringReflectionExecutor> {
    public StringReflectionExecutor build() {
      return new StringReflectionExecutor();
    }
  }
}
