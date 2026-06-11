package proguard.io

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.bouncycastle.asn1.x500.X500NameBuilder
import org.bouncycastle.asn1.x500.style.BCStyle
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.cert.X509Certificate
import java.util.Date

class PKCS7OutputStreamTest : BehaviorSpec({

    // Helper function to generate test certificates using bouncyCastle
    fun generateCertificate(configureNameBuilder: (X500NameBuilder) -> Unit): X509Certificate {
        val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
        keyPairGenerator.initialize(2048)
        val keyPair = keyPairGenerator.generateKeyPair()

        val builder = X500NameBuilder(BCStyle.INSTANCE)
        configureNameBuilder(builder)
        val issuer = builder.build()

        val certBuilder = JcaX509v3CertificateBuilder(
            issuer,
            BigInteger.ONE,
            Date(System.currentTimeMillis() - 10000),
            Date(System.currentTimeMillis() + 10000),
            issuer,
            keyPair.public,
        )
        val signer = JcaContentSignerBuilder("SHA256withRSA").build(keyPair.private)
        val certHolder = certBuilder.build(signer)
        return JcaX509CertificateConverter().getCertificate(certHolder)
    }

    // Helper function to pipe the certificate through PKCS7OutputStream
    fun processSignatureAndGetRawOutput(certificate: X509Certificate): String {
        val outputStream = ByteArrayOutputStream()
        val derOutputStream = DEROutputStream(outputStream)
        val pkcs7OutputStream = PKCS7OutputStream(derOutputStream)

        pkcs7OutputStream.writeSignature(
            certificate,
            "SHA256",
            "RSA",
            byteArrayOf(0x01, 0x02, 0x03), // Dummy signature
        )
        pkcs7OutputStream.flush()

        return String(outputStream.toByteArray(), Charsets.UTF_8)
    }

    Given("a PKCS7OutputStream writing a certificate") {
        When("the distinguished name contains escaped quotes") {
            val certificate = generateCertificate { builder ->
                builder.addRDN(BCStyle.C, "US")
                builder.addRDN(BCStyle.O, "JSC \"Example Bank\"")
                builder.addRDN(BCStyle.CN, "Example Business Mobile")
            }

            val rawOutputString = processSignatureAndGetRawOutput(certificate)

            Then("it writes the cleanly unescaped organization name") {
                val expectedUnescaped = "JSC \"Example Bank\""
                val incorrectEscaped = "JSC \\\"Example Bank\\\""

                rawOutputString.shouldContain(expectedUnescaped)
                rawOutputString.shouldNotContain(incorrectEscaped)
            }
        }

        When("the distinguished name has multiple attributes") {
            val certificate = generateCertificate { builder ->
                builder.addRDN(BCStyle.C, "US")
                builder.addRDN(BCStyle.O, "Example Corp")
                builder.addRDN(BCStyle.OU, "App Dept")
                builder.addRDN(BCStyle.CN, "example.com")
            }

            val rawOutputString = processSignatureAndGetRawOutput(certificate)

            Then("it writes the attributes into the stream in the correct order") {
                val indexOfCountry = rawOutputString.indexOf("US")
                val indexOfOrg = rawOutputString.indexOf("Example Corp")
                val indexOfOrgUnit = rawOutputString.indexOf("App Dept")
                val indexOfCommonName = rawOutputString.indexOf("example.com")

                (indexOfCountry > -1) shouldBe true
                (indexOfCountry < indexOfOrg) shouldBe true
                (indexOfOrg < indexOfOrgUnit) shouldBe true
                (indexOfOrgUnit < indexOfCommonName) shouldBe true
            }
        }

        When("the distinguished name contains an unescaped comma inside the value") {
            val certificate = generateCertificate { builder ->
                builder.addRDN(BCStyle.C, "US")
                builder.addRDN(BCStyle.O, "Example, Inc.")
                builder.addRDN(BCStyle.CN, "Example Business Mobile")
            }

            val rawOutputString = processSignatureAndGetRawOutput(certificate)

            Then("it correctly keeps the comma as part of the organization name") {
                rawOutputString.shouldContain("Example, Inc.")
            }
        }

        When("the distinguished name contains both escaped quotes and commas inside a value") {
            val certificate = generateCertificate { builder ->
                builder.addRDN(BCStyle.C, "US")
                builder.addRDN(BCStyle.O, "JSC \"Example, Inc.\"")
                builder.addRDN(BCStyle.CN, "Example Business Mobile")
            }

            val rawOutputString = processSignatureAndGetRawOutput(certificate)

            Then("it correctly parses the value, keeping the comma and unescaping the quotes") {
                val expectedUnescaped = "JSC \"Example, Inc.\""
                val incorrectEscaped = "JSC \\\"Example, Inc.\\\""

                rawOutputString.shouldContain(expectedUnescaped)
                rawOutputString.shouldNotContain(incorrectEscaped)
            }
        }
    }
})
