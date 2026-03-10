package dev.rivikauth.core.crypto.otp

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SteamOtpTest {
    @Test
    fun `steam code is 5 chars from alphabet`() {
        val secret = "12345678901234567890".toByteArray()
        val code = SteamOtp.generate(secret, 59)
        assertEquals(5, code.length)
        assertTrue(code.all { it in "23456789BCDFGHJKMNPQRTVWXY" })
    }

    @Test
    fun `same time produces same code`() {
        val secret = "testsecret123456".toByteArray()
        val c1 = SteamOtp.generate(secret, 1000)
        val c2 = SteamOtp.generate(secret, 1000)
        assertEquals(c1, c2)
    }
}
