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
import java.util.Map;
import java.util.Optional;
import proguard.evaluation.executor.instancehandler.ExecutorInstanceHandler;
import proguard.evaluation.executor.instancehandler.ExecutorMethodInstanceHandler;
import proguard.evaluation.executor.matcher.ExecutorClassMatcher;
import proguard.evaluation.executor.matcher.ExecutorMatcher;
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
  @Override
  public Optional<InstanceCopyResult> getInstanceOrCopyIfMutable(ReferenceValue instanceValue) {

    if (!(instanceValue instanceof ParticularReferenceValue)) {
      return Optional.empty();
    }

    AnalyzedObject instanceObject = instanceValue.getValue();

    if (instanceObject.isModeled()) {
      // Types we want to execute reflective are allowed to be modeled in some situations by the
      // analysis, in this case execution via reflection is not possible
      return Optional.empty();
    }

    if (instanceObject.isNull()) {
      // A null instance would throw a NPE but the analysis does not support it, so we just do not
      // perform the execution
      return Optional.empty();
    }

    String type = instanceObject.getType();
    if (type == null) {
      throw new IllegalStateException("Unexpected null type on non-null instance object!");
    }
    switch (type) {
      case TYPE_JAVA_LANG_STRING_BUILDER:
        return Optional.of(
            new InstanceCopyResult(
                AnalyzedObjectFactory.createPrecise(
                    new StringBuilder((StringBuilder) instanceObject.getPreciseValue())),
                true));
      case TYPE_JAVA_LANG_STRING_BUFFER:
        return Optional.of(
            new InstanceCopyResult(
                AnalyzedObjectFactory.createPrecise(
                    new StringBuffer((StringBuffer) instanceObject.getPreciseValue())),
                true));
      case TYPE_JAVA_LANG_STRING:
        return Optional.of(new InstanceCopyResult(instanceObject, false));
      default:
        return Optional.empty();
    }
  }

  @Override
  public ExecutorMatcher getExecutorMatcher() {
    return new ExecutorClassMatcher(
        new CollectionMatcher(
            NAME_JAVA_LANG_STRING, NAME_JAVA_LANG_STRING_BUILDER, NAME_JAVA_LANG_STRING_BUFFER));
  }

  @Override
  public ExecutorInstanceHandler getDefaultInstanceHandler() {
    Map<String, StringMatcher> matcherMap = new HashMap<>();
    matcherMap.put(NAME_JAVA_LANG_STRING, new FixedStringMatcher("toString"));
    matcherMap.put(NAME_JAVA_LANG_STRING_BUFFER, new ConstantMatcher(true));
    matcherMap.put(NAME_JAVA_LANG_STRING_BUILDER, new ConstantMatcher(true));

    return new ExecutorMethodInstanceHandler(matcherMap);
  }

  /** A builder for {@link StringReflectionExecutor}. */
  public static class Builder implements Executor.Builder<StringReflectionExecutor> {
    @Override
    public StringReflectionExecutor build() {
      return new StringReflectionExecutor();
    }
  }
}
