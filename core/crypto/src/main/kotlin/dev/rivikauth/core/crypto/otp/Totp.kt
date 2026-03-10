package dev.rivikauth.core.crypto.otp

object Totp {
    fun generate(secret: ByteArray, algorithm: String, digits: Int, period: Int, timeSeconds: Long): String {
        val counter = timeSeconds / period
        return Hotp.generate(secret, algorithm, digits, counter)
    }

    fun getMillisTillNextRotation(period: Int): Long {
        val p = period * 1000L
        return p - (System.currentTimeMillis() % p)
    }
}
