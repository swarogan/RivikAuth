package dev.rivikauth.core.database

import android.util.Base64
import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromByteArray(bytes: ByteArray?): String? =
        bytes?.let { Base64.encodeToString(it, Base64.NO_WRAP) }

    @TypeConverter
    fun toByteArray(encoded: String?): ByteArray? =
        encoded?.let { Base64.decode(it, Base64.NO_WRAP) }
}
