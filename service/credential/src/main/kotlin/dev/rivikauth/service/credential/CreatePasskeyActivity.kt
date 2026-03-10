package dev.rivikauth.service.credential

import android.content.Intent
import android.os.Bundle
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.provider.PendingIntentHandler
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.AndroidEntryPoint
import dev.rivikauth.core.database.dao.FidoCredentialDao
import dev.rivikauth.core.database.entity.FidoCredentialEntity
import dev.rivikauth.lib.webauthn.AttestationObject
import dev.rivikauth.lib.webauthn.AuthenticatorData
import dev.rivikauth.lib.webauthn.ClientData
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyPairGenerator
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.util.UUID
import javax.inject.Inject

/**
 * Activity launched by CredentialProviderService to handle passkey creation.
 * Receives the creation request via PendingIntent, generates EC P-256 key in KeyStore,
 * builds AttestationObject, stores the credential, and returns the response.
 */
@AndroidEntryPoint
class CreatePasskeyActivity : FragmentActivity() {

    @Inject lateinit var fidoCredentialDao: FidoCredentialDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            handleCreateRequest()
        } catch (e: Exception) {
            Log.e(TAG, "Passkey creation failed", e)
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    private fun handleCreateRequest() {
        val providerRequest = PendingIntentHandler.retrieveProviderCreateCredentialRequest(intent)
        if (providerRequest == null) {
            Log.e(TAG, "No create credential request in intent")
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        val callingRequest = providerRequest.callingRequest as? CreatePublicKeyCredentialRequest
        if (callingRequest == null) {
            Log.e(TAG, "Request is not a CreatePublicKeyCredentialRequest")
            setResult(RESULT_CANCELED)
            finish()
            return
        }
        val requestData = JSONObject(callingRequest.requestJson)

        // Parse RP info
        val rpObj = requestData.getJSONObject("rp")
        val rpId = rpObj.getString("id")
        val rpName = rpObj.optString("name", rpId)

        // Parse user info
        val userObj = requestData.getJSONObject("user")
        val userIdB64 = userObj.getString("id")
        val userId = FidoConstants.base64UrlDecode(userIdB64)
        val userName = userObj.getString("name")
        val userDisplayName = userObj.optString("displayName", userName)

        // Parse challenge
        val challengeB64 = requestData.getString("challenge")

        // Determine origin
        val origin = resolveOrigin(providerRequest)

        // Generate credential ID
        val credentialId = FidoConstants.generateCredentialId()
        val credentialIdB64 = FidoConstants.base64UrlEncode(credentialId)

        // Generate EC P-256 key pair in Android KeyStore
        val keyAlias = "rivikauth_fido_$credentialIdB64"
        val keySpec = KeyGenParameterSpec.Builder(keyAlias, KeyProperties.PURPOSE_SIGN)
            .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
            .setDigests(KeyProperties.DIGEST_SHA256)
            .build()
        val kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore")
        kpg.initialize(keySpec)
        val keyPair = kpg.generateKeyPair()

        // Build attested credential data
        val publicKey = keyPair.public as ECPublicKey
        val attestedCredentialData = AuthenticatorData.buildAttestedCredentialData(
            aaguid = FidoConstants.AAGUID,
            credentialId = credentialId,
            publicKey = publicKey,
            algorithm = -7L, // ES256
        )

        // Build authenticator data (UP + UV + AT = 0x01 | 0x04 | 0x40 = 0x45)
        val authData = AuthenticatorData.build(
            rpId = rpId,
            flags = 0x45.toByte(),
            signCount = 0L,
            attestedCredentialData = attestedCredentialData,
        )

        // Build clientDataJSON
        val clientDataJson = ClientData.buildJson(
            type = "webauthn.create",
            challenge = challengeB64,
            origin = origin,
            crossOrigin = false,
        )
        val clientDataJsonBytes = clientDataJson.toByteArray(Charsets.UTF_8)

        // Build attestation object (none/self-attestation)
        val attestationObject = AttestationObject.buildNone(authData)

        // Store credential in database
        val entity = FidoCredentialEntity(
            id = UUID.randomUUID().toString(),
            credentialId = credentialId,
            rpId = rpId,
            rpName = rpName,
            userId = userId,
            userName = userName,
            userDisplayName = userDisplayName,
            keyAlias = keyAlias,
            algorithm = "ES256",
            discoverable = true,
            signCount = 0L,
            createdAt = System.currentTimeMillis(),
            lastUsedAt = 0L,
        )
        runBlocking { fidoCredentialDao.upsert(entity) }

        // Build response JSON
        val responseJson = buildResponseJson(
            credentialIdB64 = credentialIdB64,
            clientDataJsonBytes = clientDataJsonBytes,
            attestationObject = attestationObject,
        )

        // Return result
        val resultIntent = Intent()
        PendingIntentHandler.setCreateCredentialResponse(
            resultIntent,
            CreatePublicKeyCredentialResponse(responseJson),
        )
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    private fun resolveOrigin(
        providerRequest: androidx.credentials.provider.ProviderCreateCredentialRequest,
    ): String {
        val callingAppInfo = providerRequest.callingAppInfo
        // For Android apps, construct the origin from the package signing certificate hash.
        // getOrigin(privilegedAllowlist) is for browsers with privileged access.
        // For native Android apps the origin is: "android:apk-key-hash:<base64url sha256 of signing cert>"
        return try {
            callingAppInfo.getOrigin(callingAppInfo.signingInfo.toString())
                ?: buildAndroidOrigin(callingAppInfo)
        } catch (_: Exception) {
            // Fallback: build origin from package name signature hash
            buildAndroidOrigin(callingAppInfo)
        }
    }

    private fun buildAndroidOrigin(
        callingAppInfo: androidx.credentials.provider.CallingAppInfo,
    ): String {
        val packageName = callingAppInfo.packageName
        val signingInfo = callingAppInfo.signingInfo
        val signatures = if (signingInfo.hasMultipleSigners()) {
            signingInfo.apkContentsSigners
        } else {
            signingInfo.signingCertificateHistory
        }

        if (signatures.isNullOrEmpty()) {
            return "android:apk-key-hash:$packageName"
        }

        val certBytes = signatures[0].toByteArray()
        val digest = java.security.MessageDigest.getInstance("SHA-256").digest(certBytes)
        val hash = FidoConstants.base64UrlEncode(digest)
        return "android:apk-key-hash:$hash"
    }

    private fun buildResponseJson(
        credentialIdB64: String,
        clientDataJsonBytes: ByteArray,
        attestationObject: ByteArray,
    ): String {
        val response = JSONObject().apply {
            put("type", "public-key")
            put("id", credentialIdB64)
            put("rawId", credentialIdB64)
            put("response", JSONObject().apply {
                put("clientDataJSON", FidoConstants.base64UrlEncode(clientDataJsonBytes))
                put("attestationObject", FidoConstants.base64UrlEncode(attestationObject))
                put("transports", JSONArray().apply { put("internal") })
            })
            put("clientExtensionResults", JSONObject())
            put("authenticatorAttachment", "platform")
        }
        return response.toString()
    }

    companion object {
        private const val TAG = "CreatePasskeyActivity"
    }
}
