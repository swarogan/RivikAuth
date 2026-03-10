package dev.rivikauth.core.crypto

import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import dev.rivikauth.core.model.VaultSlot
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class BiometricUnlockManager(private val activity: FragmentActivity) {

    private val mgr = BiometricManager.from(activity)

    val isStrong: Boolean
        get() = mgr.canAuthenticate(BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS

    val isWeak: Boolean
        get() = mgr.canAuthenticate(BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS

    fun canUseBiometric(): Boolean = isStrong || isWeak

    fun enrollBiometric(
        masterKey: SecretKey,
        onSuccess: (VaultSlot.Biometric) -> Unit,
        onError: (String) -> Unit,
    ) {
        if (isStrong) {
            try {
                enrollStrong(masterKey, onSuccess, onError)
            } catch (e: Exception) {
                Log.w("BiometricMgr", "STRONG enroll failed, falling back to WEAK", e)
                enrollWeak(masterKey, onSuccess, onError)
            }
        } else {
            enrollWeak(masterKey, onSuccess, onError)
        }
    }

    fun unlockWithBiometric(
        slot: VaultSlot.Biometric,
        onSuccess: (SecretKey) -> Unit,
        onError: (String) -> Unit,
    ) {
        if (slot.keystoreAlias.startsWith("rivikauth_bio_")) {
            try {
                unlockStrong(slot, onSuccess, onError)
            } catch (e: Exception) {
                Log.w("BiometricMgr", "STRONG unlock failed", e)
                onError("Biometria silna niedostępna — usuń i dodaj ponownie")
            }
        } else {
            unlockWeak(slot, onSuccess, onError)
        }
    }

    // --- STRONG: hardware Keystore + CryptoObject ---

    private fun enrollStrong(
        masterKey: SecretKey,
        onSuccess: (VaultSlot.Biometric) -> Unit,
        onError: (String) -> Unit,
    ) {
        val alias = "rivikauth_bio_${UUID.randomUUID()}"
        val bioKey = BiometricKeyManager.getOrCreateKey(alias)
        val cipher = BiometricKeyManager.createEncryptCipher(bioKey)

        showPrompt("Włącz biometrię", BIOMETRIC_STRONG, cipher, onError) { authedCipher ->
            val c = authedCipher!!
            val encrypted = c.doFinal(masterKey.encoded)
            val iv = c.iv.copyOf()
            val tag = encrypted.copyOfRange(encrypted.size - 16, encrypted.size)
            val ciphertext = encrypted.copyOfRange(0, encrypted.size - 16)
            onSuccess(
                VaultSlot.Biometric(
                    uuid = UUID.randomUUID().toString(),
                    encryptedMasterKey = ciphertext,
                    nonce = iv,
                    tag = tag,
                    keystoreAlias = alias,
                )
            )
        }
    }

    private fun unlockStrong(
        slot: VaultSlot.Biometric,
        onSuccess: (SecretKey) -> Unit,
        onError: (String) -> Unit,
    ) {
        val bioKey = BiometricKeyManager.getOrCreateKey(slot.keystoreAlias)
        val cipher = BiometricKeyManager.createDecryptCipher(bioKey, slot.nonce)

        showPrompt("Odblokuj vault", BIOMETRIC_STRONG, cipher, onError) { authedCipher ->
            val masterKeyBytes = authedCipher!!.doFinal(slot.encryptedMasterKey + slot.tag)
            val masterKey = SecretKeySpec(masterKeyBytes, "AES")
            masterKeyBytes.fill(0)
            onSuccess(masterKey)
        }
    }

    // --- WEAK: software AES-GCM, biometric prompt as gate ---

    private fun enrollWeak(
        masterKey: SecretKey,
        onSuccess: (VaultSlot.Biometric) -> Unit,
        onError: (String) -> Unit,
    ) {
        showPrompt("Włącz biometrię", BIOMETRIC_WEAK, null, onError) {
            val softKey = ByteArray(32).also { SecureRandom().nextBytes(it) }
            val nonce = ByteArray(12).also { SecureRandom().nextBytes(it) }
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(softKey, "AES"), GCMParameterSpec(128, nonce))
            val encrypted = cipher.doFinal(masterKey.encoded)
            val tag = encrypted.copyOfRange(encrypted.size - 16, encrypted.size)
            val ciphertext = encrypted.copyOfRange(0, encrypted.size - 16)

            // Store softKey encoded in keystoreAlias (Base64) — NOT hardware-backed
            val softKeyB64 = android.util.Base64.encodeToString(softKey, android.util.Base64.NO_WRAP)
            softKey.fill(0)

            onSuccess(
                VaultSlot.Biometric(
                    uuid = UUID.randomUUID().toString(),
                    encryptedMasterKey = ciphertext,
                    nonce = nonce,
                    tag = tag,
                    keystoreAlias = "weak:$softKeyB64",
                )
            )
        }
    }

    private fun unlockWeak(
        slot: VaultSlot.Biometric,
        onSuccess: (SecretKey) -> Unit,
        onError: (String) -> Unit,
    ) {
        showPrompt("Odblokuj vault", BIOMETRIC_WEAK, null, onError) {
            val softKeyB64 = slot.keystoreAlias.removePrefix("weak:")
            val softKey = android.util.Base64.decode(softKeyB64, android.util.Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(softKey, "AES"),
                GCMParameterSpec(128, slot.nonce),
            )
            softKey.fill(0)
            val masterKeyBytes = cipher.doFinal(slot.encryptedMasterKey + slot.tag)
            val masterKey = SecretKeySpec(masterKeyBytes, "AES")
            masterKeyBytes.fill(0)
            onSuccess(masterKey)
        }
    }

    // --- Prompt ---

    private fun showPrompt(
        title: String,
        authenticators: Int,
        cipher: Cipher?,
        onError: (String) -> Unit,
        onSuccess: (Cipher?) -> Unit,
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                if (cipher != null) {
                    result.cryptoObject?.cipher?.let { onSuccess(it) }
                        ?: onError("Brak CryptoObject w wyniku biometrii")
                } else {
                    onSuccess(null)
                }
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onError("Błąd biometrii ($errorCode): $errString")
            }
        })
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setNegativeButtonText("Anuluj")
            .setAllowedAuthenticators(authenticators)
            .build()
        if (cipher != null) {
            prompt.authenticate(info, BiometricPrompt.CryptoObject(cipher))
        } else {
            prompt.authenticate(info)
        }
    }
}
