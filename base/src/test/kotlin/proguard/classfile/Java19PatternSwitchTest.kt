/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2022 Guardsquare NV
 */

package proguard.classfile

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldNotBe
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.JavaSource
import proguard.testutils.RequiresJavaVersion
import proguard.testutils.currentJavaVersion

/**
 * Test the pattern matching switch in Java 19.
 * The preview in Java 19 changed the guard syntax from `&&` to `when`.
 */
@RequiresJavaVersion(19, 19)
class Java19PatternSwitchTest : FreeSpec({

    val javacArguments = if (currentJavaVersion in 19..19)
        listOf("--enable-preview", "--release", currentJavaVersion.toString()) else emptyList()

    "Test PatternSwitch" - {
        "Instance of" - {
            val className = "PatternSwitchInstanceOf"
            val (programClassPool, _) = ClassPoolBuilder.fromSource(
                JavaSource(
                    """$className.java""",
                    """
                    public class $className {
                        public static void main(String[] args) {
                            Object o = 274L;
                            String formatted = switch (o) {
                                case Integer i -> String.format("%d", i);
                                case Long l    -> String.format("%d", l);
                                case Double d  -> String.format("%f", d);
                                case String s  -> String.format("%s", s);
                                default        -> o.toString();
                            };
                            System.out.println(formatted);
                        }
                    }
                    """.trimIndent()
                ),
                javacArguments = javacArguments
            )

            "$className class should be in program class pool" {
                programClassPool.getClass(className) shouldNotBe null
            }
        }

        "Null" - {
            val className = "PatternSwitchNull"
            val (programClassPool, _) = ClassPoolBuilder.fromSource(
                JavaSource(
                    """$className.java""",
                    """
                    public class $className {
                        public static void main(String[] args) {
                            String s = args[0];
                            String formatted = switch (s) {
                                case null         -> "Null";
                                case "Foo", "Bar" -> "Foo|Bar";
                                default           -> "Default";
                            };
                            System.out.println(formatted);
                        }
                    }
                    """.trimIndent()
                ),
                javacArguments = javacArguments
            )
            "$className class should be in program class pool" {
                programClassPool.getClass(className) shouldNotBe null
            }
        }

        "Null or something" - {
            val className = "PatternSwitchNullOr"
            val (programClassPool, _) = ClassPoolBuilder.fromSource(
                JavaSource(
                    """$className.java""",
                    """
                    public class $className {
                        public static void foo(Object o) {
                            String formatted = switch (o) {
                                case null, String s -> "Null or String: " + s;
                                default             -> "Else";
                            };
                            System.out.println(formatted);
                        }
                    }
                    """.trimIndent()
                ),
                javacArguments = javacArguments
            )

            "$className class should be in program class pool" {
                programClassPool.getClass(className) shouldNotBe null
            }
        }

        "Guarded" - {
            val className = "PatternSwitchGuarded"
            val (programClassPool, _) = ClassPoolBuilder.fromSource(
                JavaSource(
                    """$className.java""",
                    """
                    public class $className {
                        public static void foo(Object o) {
                            double value = switch (o) {
                                case String s when s.length() > 0 -> Double.parseDouble(s);
                                default -> -1d;
                            };
                        }
                    }
                    """.trimIndent()
                ),
                javacArguments = javacArguments
            )

            "$className class should be in program class pool" {
                programClassPool.getClass(className) shouldNotBe null
            }
        }

        "Guarded with parenthesis" - {
            val className = "PatternSwitchGuardedParenthesis"
            val (programClassPool, _) = ClassPoolBuilder.fromSource(
                JavaSource(
                    """$className.java""",
                    """
                    public class $className {
                        public static void foo(Object o) {
                            double value = switch (o) {
                                case String s when s.length() > 0 && !(s.contains("//") || s.contains("#")) -> Double.parseDouble(s);
                                default -> -1d;
                            };
                        }
                    }
                    """.trimIndent()
                ),
                javacArguments = javacArguments
            )

            "$className class should be in program class pool" {
                programClassPool.getClass(className) shouldNotBe null
            }
        }

        "Exhaustive with sealed interface and records" - {
            val className = "PatternSwitchSealed"
            val (programClassPool, _) = ClassPoolBuilder.fromSource(
                JavaSource(
                    """$className.java""",
                    """
                    public class $className {
                        sealed interface ISealed permits Foo, Bar, Rec {}
                        final class Foo implements ISealed {}
                        final class Bar implements ISealed {}
                        record Rec(int i) implements ISealed {}
                    
                        static int foo(ISealed s) {
                            return switch (s) {
                                case Foo foo -> 1;
                                case Bar bar -> 2;
                                case Rec rec -> 3;
                            };
                        }
                    }
                    """.trimIndent()
                ),
                javacArguments = javacArguments
            )

            "$className class should be in program class pool" {
                programClassPool.getClass(className) shouldNotBe null
            }
        }

        "Exhaustive with generic sealed interface" - {
            val className = "PatternSwitchSealedGeneric"
            val (programClassPool, _) = ClassPoolBuilder.fromSource(
                JavaSource(
                    """$className.java""",
                    """
                    public class $className {
                        sealed interface IGenericSealed<T> permits Foo, Bar {}
                        final class Foo<T> implements IGenericSealed<CharSequence> {}
                        final class Bar<T> implements IGenericSealed<T> {}
                        
                        static int foo(IGenericSealed<Integer> seal) {
                            return switch (seal) {
                                case Bar<Integer> barI -> 42;
                            };
                        }
                    }
                    """.trimIndent()
                ),
                javacArguments = javacArguments
            )

            "$className class should be in program class pool" {
                programClassPool.getClass(className) shouldNotBe null
            }
        }
    }
})
