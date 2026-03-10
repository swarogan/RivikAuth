package dev.rivikauth.core.crypto

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Argon2InstrumentedTest {

    @Test
    fun deriveKey_producesConsistentResults() {
        val password = "testpassword".toCharArray()
        val salt = ByteArray(16) { it.toByte() }

        val key1 = Argon2KeyDerivation.deriveKey(password, salt)
        val key2 = Argon2KeyDerivation.deriveKey(password, salt)

        assertEquals(32, key1.encoded.size)
        assertArrayEquals(key1.encoded, key2.encoded)
    }

    @Test
    fun deriveKey_differentPasswordsProduceDifferentKeys() {
        val salt = ByteArray(16) { it.toByte() }

        val key1 = Argon2KeyDerivation.deriveKey("password1".toCharArray(), salt)
        val key2 = Argon2KeyDerivation.deriveKey("password2".toCharArray(), salt)

        assertFalse(key1.encoded.contentEquals(key2.encoded))
    }

    @Test
    fun deriveKey_differentSaltsProduceDifferentKeys() {
        val password = "samepassword".toCharArray()
        val salt1 = ByteArray(16) { it.toByte() }
        val salt2 = ByteArray(16) { (it + 1).toByte() }

        val key1 = Argon2KeyDerivation.deriveKey(password, salt1)
        val key2 = Argon2KeyDerivation.deriveKey(password, salt2)

        assertFalse(key1.encoded.contentEquals(key2.encoded))
    }

    @Test
    fun aesGcm_encryptDecryptRoundTrip() {
        val key = AesGcmCrypto.keyFromBytes(ByteArray(32) { (it * 7).toByte() })
        val plaintext = "Hello, RivikAuthenticator!".toByteArray()

        val encrypted = AesGcmCrypto.encrypt(plaintext, key)
        val decrypted = AesGcmCrypto.decrypt(encrypted, key)

        assertArrayEquals(plaintext, decrypted)
    }

    @Test
    fun aesGcm_encryptProducesDifferentCiphertexts() {
        val key = AesGcmCrypto.keyFromBytes(ByteArray(32) { (it * 3).toByte() })
        val plaintext = "deterministic input".toByteArray()

        val encrypted1 = AesGcmCrypto.encrypt(plaintext, key)
        val encrypted2 = AesGcmCrypto.encrypt(plaintext, key)

        // Different nonces should produce different ciphertexts
        assertFalse(encrypted1.contentEquals(encrypted2))
    }

    @Test
    fun fullFlow_deriveKeyThenEncryptDecrypt() {
        val password = "vault-master-password".toCharArray()
        val salt = Argon2KeyDerivation.generateSalt()

        val derivedKey = Argon2KeyDerivation.deriveKey(password, salt)
        val plaintext = "secret OTP data".toByteArray()

        val encrypted = AesGcmCrypto.encrypt(plaintext, derivedKey)
        val decrypted = AesGcmCrypto.decrypt(encrypted, derivedKey)

        assertArrayEquals(plaintext, decrypted)
    }

    @Test
    fun masterKeyManager_createAndUnlockSlot() {
        val masterKey = AesGcmCrypto.generateMasterKey()
        val password = "unlock-me".toCharArray()

        // Use Argon2 (native) for both create and unlock
        val argon2Derive: (CharArray, ByteArray) -> javax.crypto.SecretKey =
            { pw, s -> Argon2KeyDerivation.deriveKey(pw, s) }

        val slot = MasterKeyManager.createPasswordSlot(masterKey, password, argon2Derive)
        val recovered = MasterKeyManager.unlockPasswordSlot(slot, password, argon2Derive)

        assertArrayEquals(masterKey.encoded, recovered.encoded)
    }
}
