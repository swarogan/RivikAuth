package dev.rivikauth.service.credential

import android.util.Base64
import java.security.SecureRandom

/**
 * FIDO2/WebAuthn constants and helpers for the credential provider.
 */
object FidoConstants {

    /** Self-attestation AAGUID: 16 zero bytes. */
    val AAGUID: ByteArray = ByteArray(16)

    private const val CREDENTIAL_ID_LENGTH = 32

    private const val BASE64URL_FLAGS = Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING

    /**
     * Generate a random credential ID (32 bytes) using SecureRandom.
     */
    fun generateCredentialId(): ByteArray {
        val credentialId = ByteArray(CREDENTIAL_ID_LENGTH)
        SecureRandom().nextBytes(credentialId)
        return credentialId
    }

    /**
     * Encode bytes to Base64url (no wrap, no padding, URL-safe).
     */
    fun base64UrlEncode(data: ByteArray): String =
        Base64.encodeToString(data, BASE64URL_FLAGS)

    /**
     * Decode Base64url string to bytes.
     */
    fun base64UrlDecode(encoded: String): ByteArray =
        Base64.decode(encoded, BASE64URL_FLAGS)
}
