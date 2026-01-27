package proguard.testutils

import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import proguard.classfile.AccessConstants
import proguard.classfile.Clazz

val finalClass = object : Matcher<Clazz> {
    override fun test(value: Clazz): MatcherResult {
        return MatcherResult(
            value.accessFlags.and(AccessConstants.FINAL) != 0,
            { "${value.name} should be final" },
            { "${value.name} should not be final" },
        )
    }
}
