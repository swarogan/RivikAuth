package dev.rivikauth.core.database

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import dev.rivikauth.core.database.dao.LinkedPairingDao
import dev.rivikauth.core.database.entity.LinkedPairingEntity
import dev.rivikauth.lib.cable.AuthenticatorIdentity
import dev.rivikauth.lib.cable.LinkedClientData
import dev.rivikauth.lib.cable.LinkedDeviceStore
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import javax.inject.Inject

class RoomLinkedDeviceStore @Inject constructor(
    private val dao: LinkedPairingDao,
) : LinkedDeviceStore {

    override suspend fun getIdentity(): AuthenticatorIdentity {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val entry = keyStore.getEntry(IDENTITY_ALIAS, null)

        val keyPair = if (entry is KeyStore.PrivateKeyEntry) {
            KeyPair(entry.certificate.publicKey, entry.privateKey)
        } else {
            generateIdentityKeyPair()
        }

        return AuthenticatorIdentity(keyPair, AUTHENTICATOR_NAME)
    }

    override suspend fun savePairing(
        contactId: ByteArray,
        pairedSecret: ByteArray,
        peerIdentityKey: ByteArray,
    ) {
        dao.upsert(
            LinkedPairingEntity(
                contactId = contactId,
                pairedSecret = pairedSecret,
                peerIdentityKey = peerIdentityKey,
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    override suspend fun findByContactId(contactId: ByteArray): LinkedClientData? {
        val entity = dao.findByContactId(contactId) ?: return null
        return LinkedClientData(
            contactId = entity.contactId,
            pairedSecret = entity.pairedSecret,
            identityPublicKey = entity.peerIdentityKey,
        )
    }

    override suspend fun removePairing(contactId: ByteArray) {
        dao.deleteByContactId(contactId)
    }

    override suspend fun listAll(): List<LinkedClientData> =
        dao.getAll().map { entity ->
            LinkedClientData(
                contactId = entity.contactId,
                pairedSecret = entity.pairedSecret,
                identityPublicKey = entity.peerIdentityKey,
            )
        }

    private fun generateIdentityKeyPair(): KeyPair {
        val spec = KeyGenParameterSpec.Builder(
            IDENTITY_ALIAS,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_AGREE_KEY,
        )
            .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
            .setDigests(KeyProperties.DIGEST_SHA256)
            .build()

        return KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, ANDROID_KEYSTORE)
            .apply { initialize(spec) }
            .generateKeyPair()
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val IDENTITY_ALIAS = "rivikauth_cable_identity"
        private const val AUTHENTICATOR_NAME = "RivikAuth"
    }
}
