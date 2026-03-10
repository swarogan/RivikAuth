package dev.rivikauth.app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.rivikauth.core.database.di.DatabasePassphraseProvider
import dev.rivikauth.core.database.di.VaultPassphraseHolder

@Module
@InstallIn(SingletonComponent::class)
abstract class DatabaseBindings {

    @Binds
    abstract fun bindPassphraseProvider(holder: VaultPassphraseHolder): DatabasePassphraseProvider
}
