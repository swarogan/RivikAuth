package dev.rivikauth.service.credential

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.credentials.provider.PendingIntentHandler
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.AndroidEntryPoint
import dev.rivikauth.core.database.dao.FidoCredentialDao
import dev.rivikauth.core.database.entity.FidoCredentialEntity
import dev.rivikauth.lib.webauthn.AuthenticatorData
import dev.rivikauth.lib.webauthn.ClientData
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import javax.inject.Inject

/**
 * Activity launched by CredentialProviderService to handle passkey authentication.
 * Shows biometric prompt, signs the challenge with stored credential key,
 * and returns the assertion response.
 */
@AndroidEntryPoint
class GetPasskeyActivity : FragmentActivity() {

    @Inject lateinit var fidoCredentialDao: FidoCredentialDao

    private var credential: FidoCredentialEntity? = null
    private var authDataBytes: ByteArray? = null
    private var clientDataJsonBytes: ByteArray? = null
    private var clientDataHash: ByteArray? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            handleGetRequest()
        } catch (e: Exception) {
            Log.e(TAG, "Passkey authentication failed", e)
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    private fun handleGetRequest() {
        val providerRequest = PendingIntentHandler.retrieveProviderGetCredentialRequest(intent)
        if (providerRequest == null) {
            Log.e(TAG, "No get credential request in intent")
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        // Get the credential DB ID from intent extras
        val dbId = intent.getStringExtra("credential_db_id")
        if (dbId.isNullOrBlank()) {
            Log.e(TAG, "No credential_db_id in intent extras")
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        // Look up credential in database
        val cred = runBlocking { fidoCredentialDao.getById(dbId) }
        if (cred == null) {
            Log.e(TAG, "Credential not found in database: $dbId")
            setResult(RESULT_CANCELED)
            finish()
            return
        }
        credential = cred

        // Parse the request JSON to get the challenge
        val options = providerRequest.credentialOptions
        var challengeB64: String? = null
        for (option in options) {
            if (option is GetPublicKeyCredentialOption) {
                val requestJson = JSONObject(option.requestJson)
                challengeB64 = requestJson.getString("challenge")
                break
            }
        }
        if (challengeB64 == null) {
            Log.e(TAG, "No challenge found in request")
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        // Determine origin
        val origin = resolveOrigin(providerRequest)

        // Build clientDataJSON
        val clientDataJson = ClientData.buildJson(
            type = "webauthn.get",
            challenge = challengeB64,
            origin = origin,
            crossOrigin = false,
        )
        clientDataJsonBytes = clientDataJson.toByteArray(Charsets.UTF_8)
        clientDataHash = ClientData.hash(clientDataJson)

        // Build authenticator data (UP + UV = 0x01 | 0x04 = 0x05, no attested credential data)
        val newSignCount = cred.signCount + 1
        authDataBytes = AuthenticatorData.build(
            rpId = cred.rpId,
            flags = 0x05.toByte(),
            signCount = newSignCount,
        )

        // Load the private key from Android KeyStore
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val privateKey = keyStore.getKey(cred.keyAlias, null) as PrivateKey
        val sig = Signature.getInstance("SHA256withECDSA")
        sig.initSign(privateKey)

        // Check biometric availability
        val biometricResult = BiometricManager.from(this)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)

        if (biometricResult == BiometricManager.BIOMETRIC_SUCCESS) {
            // Use biometric prompt with CryptoObject for hardware-bound signing
            showBiometricPrompt(sig)
        } else {
            // No biometric available -- sign directly
            signAndReturn(sig)
        }
    }

    private fun showBiometricPrompt(sig: Signature) {
        val executor = ContextCompat.getMainExecutor(this)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                val authedSig = result.cryptoObject?.signature
                if (authedSig != null) {
                    signAndReturn(authedSig)
                } else {
                    Log.e(TAG, "No Signature in CryptoObject after biometric auth")
                    setResult(RESULT_CANCELED)
                    finish()
                }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                Log.e(TAG, "Biometric auth error ($errorCode): $errString")
                setResult(RESULT_CANCELED)
                finish()
            }

            override fun onAuthenticationFailed() {
                // Called on single failed attempt, BiometricPrompt handles retry internally
                Log.w(TAG, "Biometric authentication attempt failed")
            }
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Potwierdzenie tożsamości")
            .setSubtitle(credential?.rpName ?: "")
            .setNegativeButtonText("Anuluj")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        val biometricPrompt = BiometricPrompt(this, executor, callback)
        biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(sig))
    }

    private fun signAndReturn(sig: Signature) {
        try {
            val cred = credential ?: return
            val authData = authDataBytes ?: return
            val cdHash = clientDataHash ?: return
            val cdJsonBytes = clientDataJsonBytes ?: return

            // Sign: authData || clientDataHash
            sig.update(authData)
            sig.update(cdHash)
            val signature = sig.sign()

            // Update sign count in database
            val newSignCount = cred.signCount + 1
            runBlocking {
                fidoCredentialDao.updateSignCount(
                    id = cred.id,
                    signCount = newSignCount,
                    lastUsedAt = System.currentTimeMillis(),
                )
            }

            // Build assertion response JSON
            val credentialIdB64 = FidoConstants.base64UrlEncode(cred.credentialId)
            val responseJson = buildAssertionJson(
                credentialIdB64 = credentialIdB64,
                authData = authData,
                clientDataJsonBytes = cdJsonBytes,
                signature = signature,
                userHandle = cred.userId,
            )

            // Return result
            val resultIntent = Intent()
            PendingIntentHandler.setGetCredentialResponse(
                resultIntent,
                androidx.credentials.GetCredentialResponse(
                    PublicKeyCredential(responseJson),
                ),
            )
            setResult(RESULT_OK, resultIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Signing failed", e)
            setResult(RESULT_CANCELED)
        } finally {
            finish()
        }
    }

    private fun resolveOrigin(
        providerRequest: androidx.credentials.provider.ProviderGetCredentialRequest,
    ): String {
        val callingAppInfo = providerRequest.callingAppInfo
        return try {
            callingAppInfo.getOrigin(callingAppInfo.signingInfo.toString())
                ?: buildAndroidOrigin(callingAppInfo)
        } catch (_: Exception) {
            buildAndroidOrigin(callingAppInfo)
        }
    }

    private fun buildAndroidOrigin(
        callingAppInfo: androidx.credentials.provider.CallingAppInfo,
    ): String {
        val signingInfo = callingAppInfo.signingInfo
        val signatures = if (signingInfo.hasMultipleSigners()) {
            signingInfo.apkContentsSigners
        } else {
            signingInfo.signingCertificateHistory
        }

        if (signatures.isNullOrEmpty()) {
            return "android:apk-key-hash:${callingAppInfo.packageName}"
        }

        val certBytes = signatures[0].toByteArray()
        val digest = java.security.MessageDigest.getInstance("SHA-256").digest(certBytes)
        val hash = FidoConstants.base64UrlEncode(digest)
        return "android:apk-key-hash:$hash"
    }

    private fun buildAssertionJson(
        credentialIdB64: String,
        authData: ByteArray,
        clientDataJsonBytes: ByteArray,
        signature: ByteArray,
        userHandle: ByteArray,
    ): String {
        val response = JSONObject().apply {
            put("type", "public-key")
            put("id", credentialIdB64)
            put("rawId", credentialIdB64)
            put("response", JSONObject().apply {
                put("clientDataJSON", FidoConstants.base64UrlEncode(clientDataJsonBytes))
                put("authenticatorData", FidoConstants.base64UrlEncode(authData))
                put("signature", FidoConstants.base64UrlEncode(signature))
                put("userHandle", FidoConstants.base64UrlEncode(userHandle))
            })
            put("clientExtensionResults", JSONObject())
            put("authenticatorAttachment", "platform")
        }
        return response.toString()
    }

    companion object {
        private const val TAG = "GetPasskeyActivity"
    }
}
