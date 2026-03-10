package dev.rivikauth.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fido_credentials")
data class FidoCredentialEntity(
    @PrimaryKey val id: String,
    val credentialId: ByteArray,
    val rpId: String,
    val rpName: String,
    val userId: ByteArray,
    val userName: String,
    val userDisplayName: String,
    val keyAlias: String,
    val algorithm: String,
    val discoverable: Boolean,
    val signCount: Long,
    val createdAt: Long,
    val lastUsedAt: Long,
    val encryptedPrivateKey: ByteArray? = null,
)
