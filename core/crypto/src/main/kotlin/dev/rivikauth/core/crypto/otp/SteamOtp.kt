package dev.rivikauth.core.crypto.otp

import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object SteamOtp {
    private const val ALPHABET = "23456789BCDFGHJKMNPQRTVWXY"
    private const val DIGITS = 5

    fun generate(secret: ByteArray, timeSeconds: Long, period: Int = 30): String {
        val counter = timeSeconds / period
        val counterBytes = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putLong(counter).array()
        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(secret, "RAW"))
        val hash = mac.doFinal(counterBytes)
        val offset = hash[hash.size - 1].toInt() and 0xf
        var code = ((hash[offset].toInt() and 0x7f) shl 24) or
                   ((hash[offset + 1].toInt() and 0xff) shl 16) or
                   ((hash[offset + 2].toInt() and 0xff) shl 8) or
                   (hash[offset + 3].toInt() and 0xff)
        return buildString {
            repeat(DIGITS) {
                append(ALPHABET[code % ALPHABET.length])
                code /= ALPHABET.length
            }
        }
    }
}
