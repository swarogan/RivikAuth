package dev.rivikauth.core.crypto.otp

import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object Hotp {
    fun generate(secret: ByteArray, algorithm: String, digits: Int, counter: Long): String {
        val counterBytes = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putLong(counter).array()
        val mac = Mac.getInstance(algorithm)
        mac.init(SecretKeySpec(secret, "RAW"))
        val hash = mac.doFinal(counterBytes)
        val offset = hash[hash.size - 1].toInt() and 0xf
        val code = ((hash[offset].toInt() and 0x7f) shl 24) or
                   ((hash[offset + 1].toInt() and 0xff) shl 16) or
                   ((hash[offset + 2].toInt() and 0xff) shl 8) or
                   (hash[offset + 3].toInt() and 0xff)
        val mod = Math.pow(10.0, digits.toDouble()).toLong()
        return (code % mod).toString().padStart(digits, '0')
    }
}
