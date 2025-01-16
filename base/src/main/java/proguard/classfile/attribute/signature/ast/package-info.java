/**
 * AST nodes for the descriptor and signature grammar as defined in the JVM spec. <b>WARNING!</b>
 * The distinction between signature and a descriptor is different in the JVM spec and in PGC. This
 * package matches the JVM spec.
 *
 * <p>The structure of the classes is closely follows the structure of the grammar to simplify
 * parsing and long-term maintenance. For any other uses than parsing, it is recommended to
 * transform the ASTs into a more suitable form such as {@link proguard.classfile.MethodDescriptor}
 * or a {@link proguard.classfile.MethodSignature}.
 *
 * <p>To serialize the AST back to their textual form, use the {@literal toString()} method.
 *
 * @see <a
 *     href="https://docs.oracle.com/javase/specs/jvms/se23/html/jvms-4.html#jvms-4.7.9.1">signature
 *     grammar</a>
 * @see <a
 *     href="https://docs.oracle.com/javase/specs/jvms/se23/html/jvms-4.html#jvms-4.3">descriptor
 *     grammar</a>
 */
package proguard.classfile.attribute.signature.ast;
