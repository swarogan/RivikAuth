package dev.rivikauth.core.datastore

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.rivikauth.core.model.VaultSlot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.vaultDataStore: DataStore<Preferences> by preferencesDataStore(name = "vault_slots")

@Singleton
class VaultSlotStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val json = Json { ignoreUnknownKeys = true }

    private object Keys {
        val PASSWORD_SLOT = stringPreferencesKey("password_slot")
        val BIOMETRIC_SLOT = stringPreferencesKey("biometric_slot")
    }

    fun isVaultCreated(): Flow<Boolean> =
        context.vaultDataStore.data.map { prefs ->
            prefs.contains(Keys.PASSWORD_SLOT)
        }

    suspend fun getPasswordSlot(): VaultSlot.Password? {
        val prefs = context.vaultDataStore.data.first()
        val raw = prefs[Keys.PASSWORD_SLOT] ?: return null
        return json.decodeFromString<PasswordSlotDto>(raw).toDomain()
    }

    suspend fun getBiometricSlot(): VaultSlot.Biometric? {
        val prefs = context.vaultDataStore.data.first()
        val raw = prefs[Keys.BIOMETRIC_SLOT] ?: return null
        return json.decodeFromString<BiometricSlotDto>(raw).toDomain()
    }

    suspend fun savePasswordSlot(slot: VaultSlot.Password) {
        val dto = PasswordSlotDto.fromDomain(slot)
        context.vaultDataStore.edit { prefs ->
            prefs[Keys.PASSWORD_SLOT] = json.encodeToString(PasswordSlotDto.serializer(), dto)
        }
    }

    suspend fun saveBiometricSlot(slot: VaultSlot.Biometric) {
        val dto = BiometricSlotDto.fromDomain(slot)
        context.vaultDataStore.edit { prefs ->
            prefs[Keys.BIOMETRIC_SLOT] = json.encodeToString(BiometricSlotDto.serializer(), dto)
        }
    }

    suspend fun clearBiometricSlot() {
        context.vaultDataStore.edit { it.remove(Keys.BIOMETRIC_SLOT) }
    }

    suspend fun clear() {
        context.vaultDataStore.edit { it.clear() }
    }
}

@Serializable
private data class PasswordSlotDto(
    val uuid: String,
    val encryptedMasterKey: String,
    val nonce: String,
    val tag: String,
    val salt: String,
    val memoryCostKib: Int,
    val iterations: Int,
    val parallelism: Int,
) {
    fun toDomain() = VaultSlot.Password(
        uuid = uuid,
        encryptedMasterKey = Base64.decode(encryptedMasterKey, Base64.NO_WRAP),
        nonce = Base64.decode(nonce, Base64.NO_WRAP),
        tag = Base64.decode(tag, Base64.NO_WRAP),
        salt = Base64.decode(salt, Base64.NO_WRAP),
        memoryCostKib = memoryCostKib,
        iterations = iterations,
        parallelism = parallelism,
    )

    companion object {
        fun fromDomain(slot: VaultSlot.Password) = PasswordSlotDto(
            uuid = slot.uuid,
            encryptedMasterKey = Base64.encodeToString(slot.encryptedMasterKey, Base64.NO_WRAP),
            nonce = Base64.encodeToString(slot.nonce, Base64.NO_WRAP),
            tag = Base64.encodeToString(slot.tag, Base64.NO_WRAP),
            salt = Base64.encodeToString(slot.salt, Base64.NO_WRAP),
            memoryCostKib = slot.memoryCostKib,
            iterations = slot.iterations,
            parallelism = slot.parallelism,
        )
    }
}

@Serializable
private data class BiometricSlotDto(
    val uuid: String,
    val encryptedMasterKey: String,
    val nonce: String,
    val tag: String,
    val keystoreAlias: String,
) {
    fun toDomain() = VaultSlot.Biometric(
        uuid = uuid,
        encryptedMasterKey = Base64.decode(encryptedMasterKey, Base64.NO_WRAP),
        nonce = Base64.decode(nonce, Base64.NO_WRAP),
        tag = Base64.decode(tag, Base64.NO_WRAP),
        keystoreAlias = keystoreAlias,
    )

    companion object {
        fun fromDomain(slot: VaultSlot.Biometric) = BiometricSlotDto(
            uuid = slot.uuid,
            encryptedMasterKey = Base64.encodeToString(slot.encryptedMasterKey, Base64.NO_WRAP),
            nonce = Base64.encodeToString(slot.nonce, Base64.NO_WRAP),
            tag = Base64.encodeToString(slot.tag, Base64.NO_WRAP),
            keystoreAlias = slot.keystoreAlias,
        )
    }
}
