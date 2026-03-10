package dev.rivikauth.core.model

import kotlinx.serialization.Serializable

@Serializable
data class OtpEntry(
    val id: String,
    val name: String,
    val issuer: String,
    val type: OtpType,
    val secret: ByteArray,
    val algorithm: HashAlgorithm = HashAlgorithm.SHA1,
    val digits: Int = 6,
    val period: Int = 30,
    val counter: Long = 0,
    val pin: String? = null,
    val groupIds: Set<String> = emptySet(),
    val sortOrder: Int = 0,
    val note: String? = null,
    val favorite: Boolean = false,
    val iconData: ByteArray? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OtpEntry) return false
        return id == other.id
    }
    override fun hashCode() = id.hashCode()
}
