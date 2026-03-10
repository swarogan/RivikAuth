package dev.rivikauth.core.database

import dev.rivikauth.core.database.dao.FidoCredentialDao
import dev.rivikauth.core.database.entity.FidoCredentialEntity
import dev.rivikauth.lib.cable.FidoCredentialStore
import javax.inject.Inject

class RoomFidoCredentialStore @Inject constructor(
    private val dao: FidoCredentialDao,
) : FidoCredentialStore {

    override suspend fun save(credential: FidoCredentialStore.StoredCredential) {
        dao.upsert(
            FidoCredentialEntity(
                id = credential.id,
                credentialId = credential.credentialId,
                rpId = credential.rpId,
                rpName = credential.rpName,
                userId = credential.userId,
                userName = credential.userName,
                userDisplayName = credential.userDisplayName,
                keyAlias = "",
                algorithm = credential.algorithm,
                discoverable = credential.discoverable,
                signCount = credential.signCount,
                createdAt = credential.createdAt,
                lastUsedAt = credential.lastUsedAt,
                encryptedPrivateKey = credential.encryptedPrivateKey,
            )
        )
    }

    override suspend fun getByRpId(rpId: String): List<FidoCredentialStore.StoredCredential> =
        dao.getByRpId(rpId).map { it.toStored() }

    override suspend fun updateSignCount(id: String, signCount: Long, lastUsedAt: Long) {
        dao.updateSignCount(id, signCount, lastUsedAt)
    }

    private fun FidoCredentialEntity.toStored() = FidoCredentialStore.StoredCredential(
        id = id,
        credentialId = credentialId,
        rpId = rpId,
        rpName = rpName,
        userId = userId,
        userName = userName,
        userDisplayName = userDisplayName,
        algorithm = algorithm,
        discoverable = discoverable,
        signCount = signCount,
        createdAt = createdAt,
        lastUsedAt = lastUsedAt,
        encryptedPrivateKey = encryptedPrivateKey,
    )
}
