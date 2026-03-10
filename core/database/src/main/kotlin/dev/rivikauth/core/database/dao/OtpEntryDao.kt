package dev.rivikauth.core.database.dao

import androidx.room.*
import dev.rivikauth.core.database.entity.OtpEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OtpEntryDao {
    @Query("SELECT * FROM otp_entries ORDER BY sortOrder ASC")
    fun observeAll(): Flow<List<OtpEntryEntity>>

    @Query("SELECT * FROM otp_entries WHERE id = :id")
    suspend fun getById(id: String): OtpEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: OtpEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entries: List<OtpEntryEntity>)

    @Delete
    suspend fun delete(entry: OtpEntryEntity)

    @Query("DELETE FROM otp_entries")
    suspend fun deleteAll()
}
