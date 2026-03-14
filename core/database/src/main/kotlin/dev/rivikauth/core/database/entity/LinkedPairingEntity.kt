package dev.rivikauth.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "linked_pairings")
data class LinkedPairingEntity(
    @PrimaryKey
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val contactId: ByteArray,
    val pairedSecret: ByteArray,
    val peerIdentityKey: ByteArray,
    val createdAt: Long,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LinkedPairingEntity) return false
        return contactId.contentEquals(other.contactId)
    }

    override fun hashCode(): Int = contactId.contentHashCode()
}
