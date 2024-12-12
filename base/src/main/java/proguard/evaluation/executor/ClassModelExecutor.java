package proguard.evaluation.executor;

import java.util.Collections;
import java.util.HashSet;
import proguard.classfile.ClassPool;
import proguard.evaluation.value.object.model.ClassModel;
import proguard.util.BasicHierarchyProvider;

/** Executor for the {@link ClassModel}. */
public class ClassModelExecutor extends ReflectiveModelExecutor {

  /** Use the {@link Builder} to create instances of this executor. */
  private ClassModelExecutor(ClassPool programClassPool, ClassPool libraryClassPool) {
    super(
        new HashSet<>(Collections.singletonList(new SupportedModelInfo<>(ClassModel.class, false))),
        new BasicHierarchyProvider(programClassPool, libraryClassPool));
  }

  /** Builder for {@link ClassModelExecutor}. */
  public static class Builder implements Executor.Builder<ClassModelExecutor> {

    private final ClassPool programClassPool;
    private final ClassPool libraryClassPool;

    /** Construct the builder. */
    public Builder(ClassPool programClassPool, ClassPool libraryClassPool) {
      this.programClassPool = programClassPool;
      this.libraryClassPool = libraryClassPool;
    }

    @Override
    public ClassModelExecutor build() {
      return new ClassModelExecutor(programClassPool, libraryClassPool);
    }
  }
}
