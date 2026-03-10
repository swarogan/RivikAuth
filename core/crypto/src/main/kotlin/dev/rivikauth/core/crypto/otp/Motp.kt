package dev.rivikauth.core.crypto.otp

import java.security.MessageDigest

object Motp {
    fun generate(secret: ByteArray, pin: String, digits: Int, period: Int, timeSeconds: Long): String {
        val counter = timeSeconds / period
        val secretHex = secret.joinToString("") { "%02x".format(it) }
        val input = "$counter$secretHex$pin"
        val md5 = MessageDigest.getInstance("MD5").digest(input.toByteArray())
        val hexHash = md5.joinToString("") { "%02x".format(it) }
        return hexHash.take(digits)
    }
}
