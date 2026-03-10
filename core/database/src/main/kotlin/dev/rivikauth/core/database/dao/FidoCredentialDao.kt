package dev.rivikauth.core.database.dao

import androidx.room.*
import dev.rivikauth.core.database.entity.FidoCredentialEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FidoCredentialDao {
    @Query("SELECT * FROM fido_credentials ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<FidoCredentialEntity>>

    @Query("SELECT * FROM fido_credentials WHERE rpId = :rpId")
    suspend fun getByRpId(rpId: String): List<FidoCredentialEntity>

    @Query("SELECT * FROM fido_credentials WHERE id = :id")
    suspend fun getById(id: String): FidoCredentialEntity?

    @Query("SELECT * FROM fido_credentials WHERE credentialId = :credentialId")
    suspend fun getByCredentialId(credentialId: ByteArray): FidoCredentialEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(credential: FidoCredentialEntity)

    @Query("UPDATE fido_credentials SET signCount = :signCount, lastUsedAt = :lastUsedAt WHERE id = :id")
    suspend fun updateSignCount(id: String, signCount: Long, lastUsedAt: Long)

    @Delete
    suspend fun delete(credential: FidoCredentialEntity)

    @Query("DELETE FROM fido_credentials WHERE credentialId = :credentialId")
    suspend fun deleteByCredentialId(credentialId: ByteArray)
}
