package dev.rivikauth.core.crypto

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import javax.crypto.AEADBadTagException
import javax.crypto.KeyGenerator

class AesGcmCryptoTest {

    private fun generateKey() = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()

    @Test
    fun `encrypt then decrypt returns original plaintext`() {
        val key = generateKey()
        val plaintext = "hello world".toByteArray()
        val encrypted = AesGcmCrypto.encrypt(plaintext, key)
        val decrypted = AesGcmCrypto.decrypt(encrypted, key)
        assertArrayEquals(plaintext, decrypted)
    }

    @Test
    fun `encrypted output is nonce plus ciphertext plus tag`() {
        val key = generateKey()
        val plaintext = ByteArray(100)
        val encrypted = AesGcmCrypto.encrypt(plaintext, key)
        assertEquals(12 + 100 + 16, encrypted.size)
    }

    @Test
    fun `decrypt with wrong key throws`() {
        val key1 = generateKey()
        val key2 = generateKey()
        val encrypted = AesGcmCrypto.encrypt("data".toByteArray(), key1)
        assertThrows(AEADBadTagException::class.java) {
            AesGcmCrypto.decrypt(encrypted, key2)
        }
    }

    @Test
    fun `two encryptions produce different ciphertexts`() {
        val key = generateKey()
        val plaintext = "same data".toByteArray()
        val enc1 = AesGcmCrypto.encrypt(plaintext, key)
        val enc2 = AesGcmCrypto.encrypt(plaintext, key)
        assertFalse(enc1.contentEquals(enc2))
    }

    @Test
    fun `tampered ciphertext fails`() {
        val key = generateKey()
        val encrypted = AesGcmCrypto.encrypt("data".toByteArray(), key)
        encrypted[encrypted.size - 1] = (encrypted[encrypted.size - 1] + 1).toByte()
        assertThrows(AEADBadTagException::class.java) {
            AesGcmCrypto.decrypt(encrypted, key)
        }
    }

    @Test
    fun `keyFromBytes rejects wrong size`() {
        assertThrows(IllegalArgumentException::class.java) {
            AesGcmCrypto.keyFromBytes(ByteArray(16))
        }
    }

    @Test
    fun `generateMasterKey returns 256 bit key`() {
        val key = AesGcmCrypto.generateMasterKey()
        assertEquals(32, key.encoded.size)
        assertEquals("AES", key.algorithm)
    }
}
