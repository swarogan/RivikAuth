package dev.rivikauth.core.crypto

import java.security.SecureRandom
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

object Argon2KeyDerivation {

    const val SALT_LENGTH = 16
    const val DEFAULT_MEMORY_KIB = 19_456
    const val DEFAULT_ITERATIONS = 2
    const val DEFAULT_PARALLELISM = 1
    private const val HASH_LENGTH = 32

    fun generateSalt(): ByteArray =
        ByteArray(SALT_LENGTH).also { SecureRandom().nextBytes(it) }

    /**
     * Derives an AES-256 key from password using Argon2id.
     * This method requires argon2kt native library (works on Android device/emulator only).
     * For JVM unit tests, use deriveKeyFallback().
     */
    fun deriveKey(
        password: CharArray,
        salt: ByteArray,
        memoryCostKib: Int = DEFAULT_MEMORY_KIB,
        iterations: Int = DEFAULT_ITERATIONS,
        parallelism: Int = DEFAULT_PARALLELISM,
    ): SecretKey {
        // argon2kt uses JNI — this will only work on Android
        val passwordBytes = String(password).toByteArray(Charsets.UTF_8)
        return try {
            val argon2 = com.lambdapioneer.argon2kt.Argon2Kt()
            val result = argon2.hash(
                mode = com.lambdapioneer.argon2kt.Argon2Mode.ARGON2_ID,
                password = passwordBytes,
                salt = salt,
                tCostInIterations = iterations,
                mCostInKibibyte = memoryCostKib,
                parallelism = parallelism,
                hashLengthInBytes = HASH_LENGTH,
            )
            SecretKeySpec(result.rawHashAsByteArray(), "AES")
        } finally {
            passwordBytes.fill(0)
        }
    }

    /**
     * PBKDF2 fallback for JVM unit tests where argon2kt native lib is not available.
     */
    fun deriveKeyFallback(
        password: CharArray,
        salt: ByteArray,
        iterations: Int = 100_000,
    ): SecretKey {
        val factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = javax.crypto.spec.PBEKeySpec(password, salt, iterations, 256)
        val derived = factory.generateSecret(spec).encoded
        return SecretKeySpec(derived, "AES")
    }
}
