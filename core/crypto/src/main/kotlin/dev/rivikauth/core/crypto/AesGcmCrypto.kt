package dev.rivikauth.core.crypto

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object AesGcmCrypto {

    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val NONCE_LENGTH = 12
    private const val TAG_LENGTH_BITS = 128

    fun encrypt(plaintext: ByteArray, key: SecretKey): ByteArray {
        val nonce = ByteArray(NONCE_LENGTH).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_LENGTH_BITS, nonce))
        val ciphertextWithTag = cipher.doFinal(plaintext)
        return nonce + ciphertextWithTag
    }

    fun decrypt(combined: ByteArray, key: SecretKey): ByteArray {
        require(combined.size > NONCE_LENGTH) { "Ciphertext too short" }
        val nonce = combined.copyOfRange(0, NONCE_LENGTH)
        val ciphertextWithTag = combined.copyOfRange(NONCE_LENGTH, combined.size)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_LENGTH_BITS, nonce))
        return cipher.doFinal(ciphertextWithTag)
    }

    fun keyFromBytes(raw: ByteArray): SecretKey {
        require(raw.size == 32) { "AES-256 requires 32 bytes, got ${raw.size}" }
        return SecretKeySpec(raw, "AES")
    }

    fun generateMasterKey(): SecretKey {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256, SecureRandom())
        return keyGen.generateKey()
    }
}
