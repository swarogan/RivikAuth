package dev.rivikauth.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.rivikauth.core.database.dao.OtpEntryDao
import dev.rivikauth.core.database.dao.FidoCredentialDao
import dev.rivikauth.core.database.dao.EntryGroupDao
import dev.rivikauth.core.database.dao.LinkedPairingDao
import dev.rivikauth.core.database.entity.OtpEntryEntity
import dev.rivikauth.core.database.entity.FidoCredentialEntity
import dev.rivikauth.core.database.entity.EntryGroupEntity
import dev.rivikauth.core.database.entity.LinkedPairingEntity

@Database(
    entities = [
        OtpEntryEntity::class,
        FidoCredentialEntity::class,
        EntryGroupEntity::class,
        LinkedPairingEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class VaultDatabase : RoomDatabase() {
    abstract fun otpEntryDao(): OtpEntryDao
    abstract fun fidoCredentialDao(): FidoCredentialDao
    abstract fun entryGroupDao(): EntryGroupDao
    abstract fun linkedPairingDao(): LinkedPairingDao

    companion object {
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE fido_credentials ADD COLUMN credProtectPolicy INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE fido_credentials ADD COLUMN encryptedCredRandom BLOB DEFAULT NULL")
                db.execSQL("ALTER TABLE fido_credentials ADD COLUMN encryptedLargeBlobKey BLOB DEFAULT NULL")
            }
        }
    }
}
