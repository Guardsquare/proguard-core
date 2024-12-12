package proguard.evaluation.executor.model;

import java.util.Collections;
import java.util.HashSet;
import proguard.classfile.ClassPool;
import proguard.evaluation.executor.Executor;
import proguard.evaluation.executor.ReflectiveModelExecutor;
import proguard.evaluation.value.object.model.ClassLoaderModel;
import proguard.util.BasicHierarchyProvider;

/** Executor for the {@link ClassLoaderModel}. */
public class ClassLoaderModelExecutor extends ReflectiveModelExecutor {

  /** Use the {@link Builder} to create instances of this executor. */
  private ClassLoaderModelExecutor(ClassPool programClassPool, ClassPool libraryClassPool) {
    super(
        new HashSet<>(
            Collections.singletonList(new SupportedModelInfo<>(ClassLoaderModel.class, false))),
        new BasicHierarchyProvider(programClassPool, libraryClassPool));
  }

  /** Builder for {@link ClassLoaderModelExecutor}. */
  public static class Builder implements Executor.Builder<ClassLoaderModelExecutor> {

    private final ClassPool programClassPool;
    private final ClassPool libraryClassPool;

    /** Construct the builder. */
    public Builder(ClassPool programClassPool, ClassPool libraryClassPool) {
      this.programClassPool = programClassPool;
      this.libraryClassPool = libraryClassPool;
    }

    @Override
    public ClassLoaderModelExecutor build() {
      return new ClassLoaderModelExecutor(programClassPool, libraryClassPool);
    }
  }
}
