package proguard.resources.file.visitor;

import java.util.Set;
import proguard.resources.file.ResourceFile;
import proguard.resources.file.ResourceJavaReference;

/**
 * Removes any {@link ResourceJavaReference}s from resource files for which no class reference was
 * found.
 */
public class ResourceJavaReferenceCleaner implements ResourceFileVisitor {
  @Override
  public void visitResourceFile(ResourceFile resourceFile) {
    Set<ResourceJavaReference> references = resourceFile.references;

    if (references != null) {
      references.removeIf(reference -> reference.referencedClass == null);
    }
  }
}
