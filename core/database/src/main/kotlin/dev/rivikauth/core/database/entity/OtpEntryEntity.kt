package dev.rivikauth.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "otp_entries")
data class OtpEntryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val issuer: String,
    val type: String,
    val secret: ByteArray,
    val algorithm: String,
    val digits: Int,
    val period: Int,
    val counter: Long,
    val pin: String?,
    val groupIds: String,
    val sortOrder: Int,
    val note: String?,
    val favorite: Boolean,
    val iconData: ByteArray?,
    val createdAt: Long,
    val updatedAt: Long,
)
