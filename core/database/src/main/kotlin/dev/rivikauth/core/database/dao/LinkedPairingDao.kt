package dev.rivikauth.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.rivikauth.core.database.entity.LinkedPairingEntity

@Dao
interface LinkedPairingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(pairing: LinkedPairingEntity)

    @Query("SELECT * FROM linked_pairings WHERE contactId = :contactId")
    suspend fun findByContactId(contactId: ByteArray): LinkedPairingEntity?

    @Query("DELETE FROM linked_pairings WHERE contactId = :contactId")
    suspend fun deleteByContactId(contactId: ByteArray)
}
