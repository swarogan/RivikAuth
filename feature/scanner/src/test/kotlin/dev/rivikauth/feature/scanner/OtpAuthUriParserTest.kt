package dev.rivikauth.feature.scanner

import dev.rivikauth.core.common.Base32
import dev.rivikauth.core.common.OtpAuthUriParser
import dev.rivikauth.core.model.HashAlgorithm
import dev.rivikauth.core.model.OtpType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class OtpAuthUriParserTest {

    @Test
    fun `parse TOTP URI with all parameters`() {
        val uri = "otpauth://totp/Example:alice@example.com?secret=JBSWY3DPEHPK3PXP&algorithm=SHA256&digits=8&period=60&issuer=Example"
        val entry = OtpAuthUriParser.parse(uri)
        assertEquals(OtpType.TOTP, entry.type)
        assertEquals("alice@example.com", entry.name)
        assertEquals("Example", entry.issuer)
        assertEquals(HashAlgorithm.SHA256, entry.algorithm)
        assertEquals(8, entry.digits)
        assertEquals(60, entry.period)
    }

    @Test
    fun `parse HOTP URI with counter`() {
        val uri = "otpauth://hotp/Service:user?secret=JBSWY3DPEHPK3PXP&counter=42"
        val entry = OtpAuthUriParser.parse(uri)
        assertEquals(OtpType.HOTP, entry.type)
        assertEquals(42L, entry.counter)
        assertEquals("user", entry.name)
        assertEquals("Service", entry.issuer)
    }

    @Test
    fun `parse minimal TOTP URI uses defaults`() {
        val uri = "otpauth://totp/myapp?secret=JBSWY3DPEHPK3PXP"
        val entry = OtpAuthUriParser.parse(uri)
        assertEquals(OtpType.TOTP, entry.type)
        assertEquals(HashAlgorithm.SHA1, entry.algorithm)
        assertEquals(6, entry.digits)
        assertEquals(30, entry.period)
    }

    @Test
    fun `parse Steam URI`() {
        val uri = "otpauth://steam/Steam:gamer123?secret=JBSWY3DPEHPK3PXP"
        val entry = OtpAuthUriParser.parse(uri)
        assertEquals(OtpType.STEAM, entry.type)
        assertEquals(5, entry.digits)
    }

    @Test
    fun `parse mOTP URI`() {
        val uri = "motp://issuer:name?secret=abc123&pin=1234"
        val entry = OtpAuthUriParser.parse(uri)
        assertEquals(OtpType.MOTP, entry.type)
        assertEquals("1234", entry.pin)
    }

    @Test
    fun `missing secret throws`() {
        val uri = "otpauth://totp/Test?algorithm=SHA1"
        assertThrows(IllegalArgumentException::class.java) {
            OtpAuthUriParser.parse(uri)
        }
    }

    @Test
    fun `unsupported scheme throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            OtpAuthUriParser.parse("https://example.com")
        }
    }

    @Test
    fun `base32 decode known value`() {
        // RFC 4648 test vector: "foobar" = MZXW6YTBOI======
        val decoded = Base32.decode("MZXW6YTBOI")
        assertEquals("foobar", String(decoded))
    }
}
