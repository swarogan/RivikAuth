package dev.rivikauth.core.model

data class FidoCredential(
    val credentialId: ByteArray,
    val rpId: String,
    val rpName: String,
    val userId: ByteArray,
    val userName: String,
    val userDisplayName: String,
    val keyAlias: String,
    val algorithm: CoseAlgorithm = CoseAlgorithm.ES256,
    val discoverable: Boolean = true,
    val signCount: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsedAt: Long = 0,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FidoCredential) return false
        return credentialId.contentEquals(other.credentialId)
    }
    override fun hashCode() = credentialId.contentHashCode()
}
