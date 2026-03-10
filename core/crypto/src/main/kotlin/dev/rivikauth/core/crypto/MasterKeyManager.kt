package dev.rivikauth.core.crypto

import dev.rivikauth.core.model.VaultSlot
import java.util.UUID
import javax.crypto.SecretKey

object MasterKeyManager {

    fun createPasswordSlot(
        masterKey: SecretKey,
        password: CharArray,
        deriveKey: (CharArray, ByteArray) -> SecretKey = { pw, s ->
            Argon2KeyDerivation.deriveKeyFallback(pw, s)
        },
    ): VaultSlot.Password {
        val salt = Argon2KeyDerivation.generateSalt()
        val derivedKey = deriveKey(password, salt)
        val encrypted = AesGcmCrypto.encrypt(masterKey.encoded, derivedKey)
        val nonce = encrypted.copyOfRange(0, 12)
        val ciphertextWithTag = encrypted.copyOfRange(12, encrypted.size)
        return VaultSlot.Password(
            uuid = UUID.randomUUID().toString(),
            encryptedMasterKey = ciphertextWithTag.copyOfRange(0, ciphertextWithTag.size - 16),
            nonce = nonce,
            tag = ciphertextWithTag.copyOfRange(ciphertextWithTag.size - 16, ciphertextWithTag.size),
            salt = salt,
        )
    }

    fun unlockPasswordSlot(
        slot: VaultSlot.Password,
        password: CharArray,
        deriveKey: (CharArray, ByteArray) -> SecretKey = { pw, s ->
            Argon2KeyDerivation.deriveKeyFallback(pw, s)
        },
    ): SecretKey {
        val derivedKey = deriveKey(password, slot.salt)
        val combined = slot.nonce + slot.encryptedMasterKey + slot.tag
        val masterKeyBytes = AesGcmCrypto.decrypt(combined, derivedKey)
        return AesGcmCrypto.keyFromBytes(masterKeyBytes).also { masterKeyBytes.fill(0) }
    }
}
