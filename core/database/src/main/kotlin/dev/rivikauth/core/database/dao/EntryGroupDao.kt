package dev.rivikauth.core.database.dao

import androidx.room.*
import dev.rivikauth.core.database.entity.EntryGroupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryGroupDao {
    @Query("SELECT * FROM entry_groups ORDER BY sortOrder ASC")
    fun observeAll(): Flow<List<EntryGroupEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(group: EntryGroupEntity)

    @Delete
    suspend fun delete(group: EntryGroupEntity)
}
