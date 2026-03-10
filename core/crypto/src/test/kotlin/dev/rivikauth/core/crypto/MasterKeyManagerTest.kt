package dev.rivikauth.core.crypto

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class MasterKeyManagerTest {

    @Test
    fun `create and unlock password slot round trip`() {
        val masterKey = AesGcmCrypto.generateMasterKey()
        val password = "testPassword123!".toCharArray()

        val slot = MasterKeyManager.createPasswordSlot(masterKey, password)
        val recovered = MasterKeyManager.unlockPasswordSlot(slot, password)

        assertArrayEquals(masterKey.encoded, recovered.encoded)
    }

    @Test
    fun `wrong password fails to unlock`() {
        val masterKey = AesGcmCrypto.generateMasterKey()
        val slot = MasterKeyManager.createPasswordSlot(masterKey, "correct".toCharArray())

        assertThrows(javax.crypto.AEADBadTagException::class.java) {
            MasterKeyManager.unlockPasswordSlot(slot, "wrong".toCharArray())
        }
    }

    @Test
    fun `slot contains non-empty fields`() {
        val masterKey = AesGcmCrypto.generateMasterKey()
        val slot = MasterKeyManager.createPasswordSlot(masterKey, "pw".toCharArray())

        assertTrue(slot.uuid.isNotBlank())
        assertTrue(slot.nonce.size == 12)
        assertTrue(slot.tag.size == 16)
        assertTrue(slot.salt.size == 16)
        assertTrue(slot.encryptedMasterKey.isNotEmpty())
    }
}
