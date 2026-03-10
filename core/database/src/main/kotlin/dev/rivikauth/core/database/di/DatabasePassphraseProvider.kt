package dev.rivikauth.core.database.di

interface DatabasePassphraseProvider {
    fun getPassphrase(): ByteArray
}
