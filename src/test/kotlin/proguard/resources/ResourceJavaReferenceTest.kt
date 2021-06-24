package proguard.resources

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import proguard.classfile.ProgramClass
import proguard.resources.file.ResourceJavaReference

class ResourceJavaReferenceTest : FreeSpec({
    "Given two ResourceJavaReferences" - {
        "When referencedClass is null" - {
            val resourceJavaRef1 = ResourceJavaReference("Ref1")
            "When they have equal contents" - {
                val resourceJavaRef2 = ResourceJavaReference("Ref1")
                "Then both instances should be equal and hashCode should match" {
                    resourceJavaRef1 shouldBe resourceJavaRef2
                    resourceJavaRef1.hashCode() shouldBe resourceJavaRef2.hashCode()
                }
            }

            "When they have unequal contents" - {
                val resourceJavaRef2 = ResourceJavaReference("Ref2")
                "Then both instances should not be equal" {
                    resourceJavaRef1 shouldNotBe resourceJavaRef2
                }
            }
        }

        "When referencedClass is not null" - {
            val resourceJavaRef1 = ResourceJavaReference("Ref1")
            val referenceClass = ProgramClass()
            resourceJavaRef1.referencedClass = referenceClass

            "When they have equal contents" - {
                val resourceJavaRef2 = ResourceJavaReference("Ref1")
                resourceJavaRef2.referencedClass = referenceClass
                "Then both instances should be equal and hashCode should match" {
                    resourceJavaRef1 shouldBe resourceJavaRef2
                    resourceJavaRef1.hashCode() shouldBe resourceJavaRef2.hashCode()
                }
            }

            "When they have same externalClassName but different referencedClass" - {
                val resourceJavaRef2 = ResourceJavaReference("Ref1")
                resourceJavaRef2.referencedClass = ProgramClass()
                "Then both instances should be equal and hashCode should match" {
                    resourceJavaRef1 shouldBe resourceJavaRef2
                    resourceJavaRef1.hashCode() shouldBe resourceJavaRef2.hashCode()
                }
            }

            "When they have different externalClassName and different referencedClass" - {
                val resourceJavaRef2 = ResourceJavaReference("Ref2")
                resourceJavaRef2.referencedClass = ProgramClass()
                "Then both instances should not be equal" {
                    resourceJavaRef1 shouldNotBe resourceJavaRef2
                }
            }

            "When they have different externalClassName but same referencedClass" - {
                val resourceJavaRef2 = ResourceJavaReference("Ref2")
                resourceJavaRef2.referencedClass = referenceClass
                "Then both instances should not be equal" {
                    resourceJavaRef1 shouldNotBe resourceJavaRef2
                }
            }
        }
    }

    "Given one ResourceJavaReference and one Object instance " - {
        val resourceJavaRef = ResourceJavaReference("Ref")
        val obj = Object()
        "Then both instances should not be equal" {
            resourceJavaRef shouldNotBe obj
        }
    }

    "Given one ResourceJavaReference object and null reference" - {
        val resourceJavaRef = ResourceJavaReference("Ref")
        "Then both instances should not be equal" {
            resourceJavaRef shouldNotBe null
        }
    }
})
