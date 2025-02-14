package proguard.classfile.attribute.signature.grammars

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class MethodSignatureGrammarTest : FunSpec({
    val signatures = listOf(
        "<T:Ljava/lang/Object;>(Landroid/os/Parcel;Landroid/os/Parcelable\$Creator<TT;>;)TT;",
        "<T::Landroid/os/Parcelable;>(Landroid/os/Parcel;TT;I)V",
        "(Landroidx/core/util/Consumer<Landroid/content/res/Configuration;>;)V",
        "<I:Ljava/lang/Object;O:Ljava/lang/Object;>(Landroidx/activity/result/contract/ActivityResultContract<TI;TO;>;Landroidx/activity/result/ActivityResultCallback<TO;>;)Landroidx/activity/result/ActivityResultLauncher<TI;>;",
    )

    signatures.forEach { sig ->
        test("parse and convert back") {
            MethodSignatureGrammar.parse(sig) shouldNotBeNull {
                toString() shouldBe sig
            }
        }
    }
})
