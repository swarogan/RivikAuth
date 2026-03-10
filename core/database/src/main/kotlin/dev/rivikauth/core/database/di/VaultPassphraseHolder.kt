package dev.rivikauth.core.database.di

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VaultPassphraseHolder @Inject constructor() : DatabasePassphraseProvider {

    @Volatile
    private var passphrase: ByteArray? = null

    override fun getPassphrase(): ByteArray =
        passphrase ?: throw IllegalStateException("Vault not unlocked")

    fun setPassphrase(value: ByteArray) {
        passphrase = value
    }

    fun isUnlocked(): Boolean = passphrase != null

    fun clear() {
        passphrase?.fill(0)
        passphrase = null
    }
}
