package dev.rivikauth.core.crypto.otp

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TotpTest {
    private val secret = "12345678901234567890".toByteArray()

    @Test
    fun `RFC 6238 SHA1 test vector at time 59`() {
        assertEquals("287082", Totp.generate(secret, "HmacSHA1", 6, 30, 59))
    }

    @Test
    fun `RFC 6238 SHA1 test vector at time 1111111109`() {
        assertEquals("081804", Totp.generate(secret, "HmacSHA1", 6, 30, 1111111109))
    }

    @Test
    fun `8 digit TOTP`() {
        assertEquals("94287082", Totp.generate(secret, "HmacSHA1", 8, 30, 59))
    }
}
