package dev.rivikauth.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.rivikauth.core.crypto.otp.OtpGenerator
import dev.rivikauth.core.model.HashAlgorithm
import dev.rivikauth.core.model.OtpEntry
import dev.rivikauth.core.model.OtpType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VaultFlowTest {

    private val rfcSecret = "12345678901234567890".toByteArray()

    @Test
    fun totpGeneration_rfc6238TestVector_sha1() {
        // RFC 6238 test vector: SHA1, T = 59s, period = 30, digits = 8
        // counter = floor(59 / 30) = 1
        val entry = OtpEntry(
            id = "test-totp-1",
            name = "RFC6238",
            issuer = "Test",
            type = OtpType.TOTP,
            secret = rfcSecret,
            algorithm = HashAlgorithm.SHA1,
            digits = 8,
            period = 30,
        )
        val code = OtpGenerator.generate(entry, timeSeconds = 59L)
        assertEquals("94287082", code)
    }

    @Test
    fun hotpGeneration_rfc4226TestVector_counter0() {
        // RFC 4226 test vector: SHA1, counter = 0, digits = 6
        val entry = OtpEntry(
            id = "test-hotp-0",
            name = "RFC4226",
            issuer = "Test",
            type = OtpType.HOTP,
            secret = rfcSecret,
            algorithm = HashAlgorithm.SHA1,
            digits = 6,
            period = 30,
            counter = 0,
        )
        val code = OtpGenerator.generate(entry)
        assertEquals("755224", code)
    }

    @Test
    fun hotpGeneration_rfc4226TestVector_counter1() {
        // RFC 4226 test vector: SHA1, counter = 1, digits = 6
        val entry = OtpEntry(
            id = "test-hotp-1",
            name = "RFC4226",
            issuer = "Test",
            type = OtpType.HOTP,
            secret = rfcSecret,
            algorithm = HashAlgorithm.SHA1,
            digits = 6,
            period = 30,
            counter = 1,
        )
        val code = OtpGenerator.generate(entry)
        assertEquals("287082", code)
    }

    @Test
    fun totpGeneration_produces6DigitCode() {
        val entry = OtpEntry(
            id = "test-totp-6d",
            name = "SixDigit",
            issuer = "Test",
            type = OtpType.TOTP,
            secret = rfcSecret,
            algorithm = HashAlgorithm.SHA1,
            digits = 6,
            period = 30,
        )
        val code = OtpGenerator.generate(entry, timeSeconds = 1000L)
        assertEquals(6, code.length)
        assertTrue(code.all { it.isDigit() })
    }

    @Test
    fun totpGeneration_sameTimeProducesSameCode() {
        val entry = OtpEntry(
            id = "test-totp-det",
            name = "Deterministic",
            issuer = "Test",
            type = OtpType.TOTP,
            secret = rfcSecret,
            algorithm = HashAlgorithm.SHA256,
            digits = 6,
            period = 30,
        )
        val code1 = OtpGenerator.generate(entry, timeSeconds = 12345L)
        val code2 = OtpGenerator.generate(entry, timeSeconds = 12345L)
        assertEquals(code1, code2)
    }

    @Test
    fun totpGeneration_differentPeriodsProduceDifferentCodes() {
        val entry30 = OtpEntry(
            id = "test-totp-30s",
            name = "Period30",
            issuer = "Test",
            type = OtpType.TOTP,
            secret = rfcSecret,
            algorithm = HashAlgorithm.SHA1,
            digits = 6,
            period = 30,
        )
        val entry60 = entry30.copy(id = "test-totp-60s", period = 60)

        // At time = 45s: counter30 = 1, counter60 = 0 -- different codes expected
        val code30 = OtpGenerator.generate(entry30, timeSeconds = 45L)
        val code60 = OtpGenerator.generate(entry60, timeSeconds = 45L)
        assertTrue(code30 != code60)
    }
}
