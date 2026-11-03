package proguard.evaluation.value

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Exhaustive
import io.kotest.property.checkAll
import io.kotest.property.exhaustive.collection
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class CompositeIntegerValueTest : BehaviorSpec({

    // helpers

    fun bothEqual(a: IntegerValue, b: IntegerValue) {
        a shouldBeEqual b
        b shouldBeEqual a
    }

    fun bothDifferent(a: IntegerValue, b: IntegerValue) {
        a shouldNotBe b
        b shouldNotBe a
    }

    val allOps: List<Byte> = listOf(
        CompositeIntegerValue.ADD,
        CompositeIntegerValue.SUBTRACT,
        CompositeIntegerValue.MULTIPLY,
        CompositeIntegerValue.DIVIDE,
        CompositeIntegerValue.REMAINDER,
        CompositeIntegerValue.SHIFT_LEFT,
        CompositeIntegerValue.SHIFT_RIGHT,
        CompositeIntegerValue.UNSIGNED_SHIFT_RIGHT,
        CompositeIntegerValue.AND,
        CompositeIntegerValue.OR,
        CompositeIntegerValue.XOR,
    )
    val ADD = CompositeIntegerValue.ADD
    val SUB = CompositeIntegerValue.SUBTRACT
    val MUL = CompositeIntegerValue.MULTIPLY

    /** Overrides the cached hash (white-box) to force collisions. */
    fun forceHash(v: CompositeIntegerValue, hash: Int) {
        CompositeIntegerValue::class.java.getDeclaredField("hash")
            .apply { isAccessible = true }
            .setInt(v, hash)
    }

    class TestContext(val forceCollisions: Boolean) {
        fun p(v: Int): IntegerValue = ParticularIntegerValue(v)

        fun c(a: IntegerValue, op: Byte, b: IntegerValue): CompositeIntegerValue {
            val node = CompositeIntegerValue(a, op, b)
            if (forceCollisions) {
                forceHash(node, 0)
            }
            return node
        }

        /**
         * Builds a DAG of depth ops.size: every level uses the previous node as BOTH operands,
         * so there are only ops.size + 1 objects, but 2^depth nodes when walked as a tree.
         */
        fun dag(ops: List<Byte>, leaf: IntegerValue): IntegerValue {
            var cur = leaf
            for (op in ops) {
                cur = c(cur, op, cur)
            }
            return cur
        }

        fun dag(depth: Int, leaf: IntegerValue, op: Byte = ADD) =
            dag(List(depth) { op }, leaf)

        /** Fully expanded tree (no sharing): 2^depth leaves. */
        fun tree(depth: Int, op: Byte = ADD, leaf: () -> IntegerValue): IntegerValue =
            if (depth == 0) leaf() else c(tree(depth - 1, op, leaf), op, tree(depth - 1, op, leaf))
    }

    val executor = Executors.newCachedThreadPool { r -> Thread(r).apply { isDaemon = true } }

    /** Fails (instead of hanging the build) when [block] shows exponential behaviour. */
    fun <T> within(seconds: Long = 10, block: () -> T): T {
        val future = executor.submit(Callable { block() })
        try {
            return future.get(seconds, TimeUnit.SECONDS)
        } catch (e: TimeoutException) {
            future.cancel(true)
            throw AssertionError("Did not finish within $seconds s: exponential behaviour?")
        } catch (e: ExecutionException) {
            throw e.cause ?: e
        }
    }

    // --- Tests ---

    listOf(true, false).forEach { forceCollisions ->
        with(TestContext(forceCollisions)) {
            val suffix = "[forceCollisions=$forceCollisions]"

            // basic equals contract

            Given("a composite value with non-composite children $suffix") {
                val a = c(p(1), ADD, p(2))

                When("compared with itself") {
                    Then("it is equal") {
                        bothEqual(a, a)
                    }
                }

                When("compared with null") {
                    Then("it is not equal") {
                        a shouldNotBe null
                    }
                }

                When("compared with a different IntegerValue subtype") {
                    Then("it is not equal in either direction") {
                        bothDifferent(a, p(3))
                        bothDifferent(a, UnknownIntegerValue())
                    }
                }

                When("compared with a structurally identical, separate instance") {
                    val b = c(p(1), ADD, p(2))
                    Then("it is equal, symmetric, with equal hash codes") {
                        (a === b) shouldBeEqual false
                        bothEqual(a, b)
                        a.hashCode() shouldBeEqual b.hashCode()
                    }
                }

                When("the left child differs") {
                    val b = c(p(9), ADD, p(2))
                    Then("it is not equal in either direction") {
                        bothDifferent(a, b)
                    }
                }

                When("the right child differs") {
                    val b = c(p(1), ADD, p(9))
                    Then("it is not equal in either direction") {
                        bothDifferent(a, b)
                    }
                }

                When("the operands are swapped") {
                    Then("it is not equal, for every non-commutative or commutative op alike") {
                        checkAll(Exhaustive.collection(allOps)) { op ->
                            bothDifferent(c(p(1), op, p(2)), c(p(2), op, p(1)))
                        }
                    }
                }
            }

            Given("two composites over the same children $suffix") {
                When("comparing all combinations of operations") {
                    Then("they are equal iff the operations are equal") {
                        val ops = Exhaustive.collection(allOps)
                        checkAll(ops, ops) { op1, op2 ->
                            val x = c(p(4), op1, p(5))
                            val y = c(p(4), op2, p(5))

                            if (op1 == op2) {
                                bothEqual(x, y)
                            } else {
                                bothDifferent(x, y)
                            }
                        }
                    }
                }
            }

            // non-composite / composite mixtures

            Given("a composite child on one side and a non-composite child on the other $suffix") {
                val inner = c(p(1), ADD, p(2))
                val withLeaf = c(p(3), ADD, p(3))

                When("the left children are mixed") {
                    val withComposite = c(inner, ADD, p(3))
                    Then("they are not equal in either direction") {
                        bothDifferent(withComposite, withLeaf)
                    }
                }

                When("the right children are mixed") {
                    val withComposite = c(p(3), ADD, inner)
                    Then("they are not equal in either direction") {
                        bothDifferent(withComposite, withLeaf)
                    }
                }
            }

            Given("composites whose children are non-composite and equal by value $suffix") {
                val a = c(p(7), ADD, p(8))

                Then("the original equals is used on the children") {
                    bothEqual(a, c(p(7), ADD, p(8)))
                    bothDifferent(a, c(p(7), ADD, p(80)))
                }
            }

            Given("a shared UnknownIntegerValue instance $suffix") {
                val u = UnknownIntegerValue()

                Then("composites over the same instance are equal") {
                    bothEqual(c(u, ADD, u), c(u, ADD, u))
                    bothEqual(c(u, ADD, p(1)), c(u, ADD, p(1)))
                    bothDifferent(c(u, ADD, p(1)), c(u, ADD, p(2)))
                }
            }

            Given("nested composites of mixed shapes $suffix") {
                fun build(leaf: Int) =
                    c(c(c(p(1), ADD, p(2)), MUL, p(leaf)), SUB, c(p(4), ADD, c(p(5), MUL, p(6))))

                Then("equal structures are equal") {
                    bothEqual(build(3), build(3))
                }

                Then("a difference deep inside is detected") {
                    bothDifferent(build(3), build(99))
                }
            }

            Given("composites whose two children are the same instance (x + x) $suffix") {
                val x = c(p(1), ADD, p(2))
                val y = c(p(1), ADD, p(2))

                When("both sides are x+x and y+y") {
                    Then("they are equal") {
                        bothEqual(c(x, ADD, x), c(y, ADD, y))
                    }
                }

                When("x+x is compared with x+z where z differs") {
                    val z = c(p(1), ADD, p(99))
                    Then("they are not equal in either direction") {
                        bothDifferent(c(x, ADD, x), c(x, ADD, z))
                        bothDifferent(c(x, ADD, x), c(y, ADD, z))
                    }
                }

                When("x+y (equal but distinct) is compared with y+y") {
                    Then("they are equal: the shortcut must not be required for equality") {
                        bothEqual(c(x, ADD, y), c(y, ADD, y))
                    }
                }

                When("x+x is compared with x-x") {
                    val c1 = c(x, ADD, x)
                    val c2 = c(x, SUB, x)
                    Then("they are not equal and the hashes differ (if not forced)") {
                        bothDifferent(c1, c2)

                        if (!forceCollisions) {
                            c1.hashCode() shouldNotBe c2.hashCode()
                        }
                    }
                }
            }

            Given("a diamond: two parents sharing one child $suffix") {
                fun diamond(leaf: Int): CompositeIntegerValue {
                    val shared = c(p(leaf), ADD, p(1))
                    val left = c(shared, MUL, p(2))
                    val right = c(shared, SUB, p(3))
                    return c(left, ADD, right)
                }

                Then("equal diamonds are equal") {
                    bothEqual(diamond(5), diamond(5))
                }

                Then("diamonds with a different shared leaf are not equal") {
                    bothDifferent(diamond(5), diamond(6))
                }

                Then("a diamond equals its fully expanded tree") {
                    val expanded = c(
                        c(c(p(5), ADD, p(1)), MUL, p(2)),
                        ADD,
                        c(c(p(5), ADD, p(1)), SUB, p(3)),
                    )
                    bothEqual(diamond(5), expanded)
                }
            }

            Given("a DAG with maximal sharing (only depth+1 objects, 2^depth tree nodes) $suffix") {
                val depth = 60

                When("two separately built, structurally equal DAGs are compared") {
                    val a = dag(depth, p(1))
                    val b = dag(depth, p(1))
                    Then("equals returns true in reasonable time") {
                        within { bothEqual(a, b) }
                    }
                }

                When("hashCode is requested") {
                    val a = dag(depth, p(1))
                    val b = dag(depth, p(1))
                    Then("it is computed in reasonable time and equal for equal DAGs") {
                        within { a.hashCode() } shouldBeEqual within { b.hashCode() }
                    }
                }

                When("the DAGs differ in the leaf") {
                    val a = dag(depth, p(1))
                    val b = dag(depth, p(2))
                    Then("equals returns false in reasonable time") {
                        within { bothDifferent(a, b) }
                    }
                }

                When("the DAGs use mixed operations per level") {
                    val ops = List(depth) { allOps[it % allOps.size] }

                    Then("equal op sequences are equal") {
                        within { bothEqual(dag(ops, p(1)), dag(ops, p(1))) }
                    }

                    Then("one differing operation in the middle makes them unequal") {
                        val other = ops.toMutableList().also { it[depth / 2] = if (it[depth / 2] == ADD) MUL else ADD }
                        within { bothDifferent(dag(ops, p(1)), dag(other, p(1))) }
                    }
                }

                When("a node has two independent exponential DAGs as children") {
                    fun build() = c(dag(depth, p(1)), ADD, dag(depth, p(2), MUL))
                    Then("equals still finishes in reasonable time") {
                        within { bothEqual(build(), build()) }
                    }
                }
            }

            Given("a shared DAG and its fully expanded tree (small enough to expand) $suffix") {
                val depth = 12 // 4096 leaves
                val shared = dag(depth, p(1))

                Then("they are equal in both directions, with equal hash codes") {
                    val expanded = tree(depth) { p(1) }
                    bothEqual(shared, expanded)
                    shared.hashCode() shouldBeEqual expanded.hashCode()
                }

                Then("they stop being equal when one leaf of the tree differs") {
                    var n = 0
                    val almost = tree(depth) { p(if (++n == 4096) 2 else 1) }
                    bothDifferent(shared, almost)
                }
            }

            Given("a mismatch sits behind a pair that was already compared $suffix") {
                val sharedA = c(p(1), ADD, p(2))
                val sharedB = c(p(1), ADD, p(2))
                val badA = c(sharedA, MUL, p(7))
                val badB = c(sharedB, MUL, p(8))
                val rootA = c(sharedA, ADD, badA)
                val rootB = c(sharedB, ADD, badB)

                Then("the overall result is false (checking that memoization doesn't bypass mismatch detection)") {
                    bothDifferent(rootA, rootB)
                }
            }

            Given("the same pair is reached via two paths and everything matches $suffix") {
                val sA = c(p(1), ADD, p(2))
                val sB = c(p(1), ADD, p(2))
                val lA = c(sA, MUL, p(3))
                val lB = c(sB, MUL, p(3))
                val rA = c(sA, SUB, p(4))
                val rB = c(sB, SUB, p(4))
                val rootA = c(lA, ADD, rA)
                val rootB = c(lB, ADD, rB)

                Then("the result is true") {
                    bothEqual(rootA, rootB)
                }
            }
        }
    }
})
