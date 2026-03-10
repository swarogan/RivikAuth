package dev.rivikauth.core.crypto.otp

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object YandexOtp {
    fun generate(secret: ByteArray, pin: String, digits: Int, period: Int, timeSeconds: Long): String {
        val pinBytes = pin.toByteArray(Charsets.UTF_8)
        val combined = pinBytes + secret
        var hashedKey = MessageDigest.getInstance("SHA-256").digest(combined)
        if (hashedKey[0] == 0.toByte()) hashedKey = hashedKey.copyOfRange(1, hashedKey.size)

        val counter = timeSeconds / period
        val counterBytes = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putLong(counter).array()
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(hashedKey, "RAW"))
        val hash = mac.doFinal(counterBytes)

        val offset = hash[hash.size - 1].toInt() and 0xf
        var code = 0L
        for (i in 0 until 8) {
            code = code or ((hash[offset + i].toLong() and 0xff) shl (8 * (7 - i)))
        }
        code = code and Long.MAX_VALUE

        val base = 26L
        var mod = 1L
        repeat(digits) { mod *= base }
        var remainder = code % mod
        return buildString {
            repeat(digits) {
                append(('a' + (remainder % base).toInt()))
                remainder /= base
            }
        }
    }
}
