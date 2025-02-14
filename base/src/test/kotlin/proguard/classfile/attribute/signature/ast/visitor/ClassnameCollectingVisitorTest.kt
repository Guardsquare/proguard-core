package proguard.classfile.attribute.signature.ast.visitor

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.equals.shouldBeEqual
import proguard.classfile.attribute.signature.grammars.MethodSignatureGrammar

class ClassnameCollectingVisitorTest : FunSpec({

    test("getting nested classes with generics") {
        val node = MethodSignatureGrammar.parse("<T::Landroid/os/Parcelable;>(Landroid/os/Parcel;TT;I)Lsome/Class<Lwith/Generics;>.And<Lsome/Subclass;>;")

        val result = mutableSetOf<String>()
        node!!.accept(ClassNameCollectingVisitor(), result)

        val expected = setOf("android/os/Parcelable", "android/os/Parcel", "some/Class\$And", "some/Subclass", "with/Generics", "some/Class")
        result shouldBeEqual expected
    }
})
