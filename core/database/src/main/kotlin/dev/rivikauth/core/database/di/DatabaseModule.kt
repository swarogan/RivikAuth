package dev.rivikauth.core.database.di

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteOpenHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.rivikauth.core.database.VaultDatabase
import dev.rivikauth.core.database.dao.OtpEntryDao
import dev.rivikauth.core.database.dao.FidoCredentialDao
import dev.rivikauth.core.database.dao.EntryGroupDao
import dev.rivikauth.core.database.dao.LinkedPairingDao
import dev.rivikauth.core.database.RoomFidoCredentialStore
import dev.rivikauth.core.database.RoomLinkedDeviceStore
import dev.rivikauth.lib.cable.FidoCredentialStore
import dev.rivikauth.lib.cable.LinkedDeviceStore
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        passphraseProvider: DatabasePassphraseProvider,
    ): VaultDatabase {
        System.loadLibrary("sqlcipher")
        val lazyFactory = SupportSQLiteOpenHelper.Factory { config ->
            SupportOpenHelperFactory(passphraseProvider.getPassphrase()).create(config)
        }
        return Room.databaseBuilder(context, VaultDatabase::class.java, "vault.db")
            .openHelperFactory(lazyFactory)
            .addMigrations(VaultDatabase.MIGRATION_3_4)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    @Provides
    fun provideOtpEntryDao(db: VaultDatabase): OtpEntryDao = db.otpEntryDao()

    @Provides
    fun provideFidoCredentialDao(db: VaultDatabase): FidoCredentialDao = db.fidoCredentialDao()

    @Provides
    fun provideEntryGroupDao(db: VaultDatabase): EntryGroupDao = db.entryGroupDao()

    @Provides
    fun provideLinkedPairingDao(db: VaultDatabase): LinkedPairingDao = db.linkedPairingDao()

    @Provides
    @Singleton
    fun provideFidoCredentialStore(dao: FidoCredentialDao): FidoCredentialStore =
        RoomFidoCredentialStore(dao)

    @Provides
    @Singleton
    fun provideLinkedDeviceStore(dao: LinkedPairingDao): LinkedDeviceStore =
        RoomLinkedDeviceStore(dao)
}
