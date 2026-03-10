package dev.rivikauth.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import dev.rivikauth.core.database.dao.OtpEntryDao
import dev.rivikauth.core.database.dao.FidoCredentialDao
import dev.rivikauth.core.database.dao.EntryGroupDao
import dev.rivikauth.core.database.entity.OtpEntryEntity
import dev.rivikauth.core.database.entity.FidoCredentialEntity
import dev.rivikauth.core.database.entity.EntryGroupEntity

@Database(
    entities = [OtpEntryEntity::class, FidoCredentialEntity::class, EntryGroupEntity::class],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class VaultDatabase : RoomDatabase() {
    abstract fun otpEntryDao(): OtpEntryDao
    abstract fun fidoCredentialDao(): FidoCredentialDao
    abstract fun entryGroupDao(): EntryGroupDao
}
