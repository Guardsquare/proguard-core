/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2020 Guardsquare NV
 */
package proguard.classfile.visitor;

import proguard.classfile.Clazz;
import proguard.util.Counter;

import java.util.function.BiPredicate;


/**
 * This ClassVisitor delegates to a given class visitor, and then, if the given predicate succeeds
 * with the given {@link Counter} before and after values, also to a second given class visitor.
 *
 * @author Eric Lafortune
 */
public class CounterConditionalClassVisitor
implements   ClassVisitor
{
    private final BiPredicate<Integer, Integer> predicate;
    private final Counter                       counter;
    private final ClassVisitor                  classVisitor1;
    private final ClassVisitor                  classVisitor2;


    /**
     * Creates a new CounterConditionalClassVisitor.
     */
    public CounterConditionalClassVisitor(Counter                       counter,
                                          BiPredicate<Integer, Integer> predicate,
                                          ClassVisitor                  classVisitor1,
                                          ClassVisitor                  classVisitor2)
    {
        this.counter       = counter;
        this.classVisitor1 = classVisitor1;
        this.classVisitor2 = classVisitor2;
        this.predicate     = predicate;
    }


    // Implementations for ClassVisitor.

    @Override
    public void visitAnyClass(Clazz clazz)
    {
        // Remember the original count.
        int count = counter.getCount();

        clazz.accept(classVisitor1);

        // Test the predicate with old and new counts.
        if (this.predicate.test(count, counter.getCount()))
        {
            clazz.accept(classVisitor2);
        }
    }

    // Useful helper predicates.

    public static boolean hasIncreased(int before, int after)
    {
        return before < after;
    }


    public static boolean isSame(int before, int after)
    {
        return before == after;
    }
}
