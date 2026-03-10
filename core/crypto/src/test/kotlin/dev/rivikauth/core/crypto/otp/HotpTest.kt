package dev.rivikauth.core.crypto.otp

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class HotpTest {
    private val secret = "12345678901234567890".toByteArray()

    @Test fun `counter 0`() = assertEquals("755224", Hotp.generate(secret, "HmacSHA1", 6, 0))
    @Test fun `counter 1`() = assertEquals("287082", Hotp.generate(secret, "HmacSHA1", 6, 1))
    @Test fun `counter 2`() = assertEquals("359152", Hotp.generate(secret, "HmacSHA1", 6, 2))
    @Test fun `counter 3`() = assertEquals("969429", Hotp.generate(secret, "HmacSHA1", 6, 3))
    @Test fun `counter 4`() = assertEquals("338314", Hotp.generate(secret, "HmacSHA1", 6, 4))
    @Test fun `counter 5`() = assertEquals("254676", Hotp.generate(secret, "HmacSHA1", 6, 5))
    @Test fun `counter 6`() = assertEquals("287922", Hotp.generate(secret, "HmacSHA1", 6, 6))
    @Test fun `counter 7`() = assertEquals("162583", Hotp.generate(secret, "HmacSHA1", 6, 7))
    @Test fun `counter 8`() = assertEquals("399871", Hotp.generate(secret, "HmacSHA1", 6, 8))
    @Test fun `counter 9`() = assertEquals("520489", Hotp.generate(secret, "HmacSHA1", 6, 9))
}
