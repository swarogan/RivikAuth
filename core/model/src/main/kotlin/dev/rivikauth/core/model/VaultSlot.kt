package dev.rivikauth.core.model

sealed class VaultSlot {
    abstract val uuid: String
    abstract val encryptedMasterKey: ByteArray
    abstract val nonce: ByteArray
    abstract val tag: ByteArray

    data class Password(
        override val uuid: String,
        override val encryptedMasterKey: ByteArray,
        override val nonce: ByteArray,
        override val tag: ByteArray,
        val salt: ByteArray,
        val memoryCostKib: Int = 19_456,
        val iterations: Int = 2,
        val parallelism: Int = 1,
    ) : VaultSlot() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Password) return false
            return uuid == other.uuid
        }
        override fun hashCode() = uuid.hashCode()
    }

    data class Biometric(
        override val uuid: String,
        override val encryptedMasterKey: ByteArray,
        override val nonce: ByteArray,
        override val tag: ByteArray,
        val keystoreAlias: String,
    ) : VaultSlot() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Biometric) return false
            return uuid == other.uuid
        }
        override fun hashCode() = uuid.hashCode()
    }
}
