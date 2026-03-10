package dev.rivikauth.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "entry_groups")
data class EntryGroupEntity(
    @PrimaryKey val id: String,
    val name: String,
    val sortOrder: Int,
)
