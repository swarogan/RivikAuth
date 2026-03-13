package dev.rivikauth.feature.scanner

import dev.rivikauth.core.model.HashAlgorithm
import dev.rivikauth.core.model.OtpEntry
import dev.rivikauth.core.model.OtpType
import java.net.URI
import java.net.URLDecoder
import java.util.UUID

/**
 * Parses otpauth:// URIs into OtpEntry domain objects.
 * Supports: otpauth://totp/, otpauth://hotp/, otpauth://steam/, motp://.
 * Uses java.net.URI to work without Google Play Services.
 */
object OtpAuthUriParser {

    fun parse(uriString: String): OtpEntry {
        val uri = URI(uriString)
        val scheme = uri.scheme?.lowercase()

        return when (scheme) {
            "otpauth" -> parseOtpAuth(uri)
            "motp" -> parseMotp(uri)
            else -> throw IllegalArgumentException("Nieobsługiwany schemat: $scheme (oczekiwano otpauth:// lub motp://)")
        }
    }

    private fun parseOtpAuth(uri: URI): OtpEntry {
        val host = uri.host?.lowercase()
            ?: throw IllegalArgumentException("Missing OTP type in URI")

        val type = when (host) {
            "totp" -> OtpType.TOTP
            "hotp" -> OtpType.HOTP
            "steam" -> OtpType.STEAM
            "yaotp" -> OtpType.YANDEX
            else -> throw IllegalArgumentException("Unknown OTP type: $host")
        }

        val path = uri.path?.removePrefix("/") ?: ""
        val (issuer, name) = parseLabel(path)
        val params = parseQuery(uri.rawQuery)

        val secret = params["secret"]
            ?: throw IllegalArgumentException("Missing secret parameter")

        val algorithm = HashAlgorithm.fromString(params["algorithm"] ?: "SHA1")

        val digits = params["digits"]?.toIntOrNull() ?: when (type) {
            OtpType.STEAM -> 5
            else -> 6
        }

        val period = params["period"]?.toIntOrNull() ?: 30
        val counter = params["counter"]?.toLongOrNull() ?: 0L
        val issuerParam = params["issuer"] ?: issuer

        return OtpEntry(
            id = UUID.randomUUID().toString(),
            name = name,
            issuer = issuerParam,
            type = type,
            secret = base32Decode(secret),
            algorithm = algorithm,
            digits = digits,
            period = period,
            counter = counter,
        )
    }

    private fun parseMotp(uri: URI): OtpEntry {
        val path = (uri.host ?: "") + (uri.path ?: "")
        val (issuer, name) = parseLabel(path)
        val params = parseQuery(uri.rawQuery)
        val secret = params["secret"]
            ?: throw IllegalArgumentException("Missing secret for mOTP")
        val pin = params["pin"] ?: ""

        return OtpEntry(
            id = UUID.randomUUID().toString(),
            name = name,
            issuer = issuer,
            type = OtpType.MOTP,
            secret = secret.toByteArray(Charsets.UTF_8),
            algorithm = HashAlgorithm.SHA1,
            digits = 6,
            period = 10,
            counter = 0,
            pin = pin,
        )
    }

    private fun parseLabel(label: String): Pair<String, String> {
        val decoded = URLDecoder.decode(label, "UTF-8")
        return if (":" in decoded) {
            val parts = decoded.split(":", limit = 2)
            parts[0].trim() to parts[1].trim()
        } else {
            "" to decoded.trim()
        }
    }

    private fun parseQuery(query: String?): Map<String, String> {
        if (query.isNullOrEmpty()) return emptyMap()
        return query.split("&").associate { param ->
            val parts = param.split("=", limit = 2)
            val key = URLDecoder.decode(parts[0], "UTF-8")
            val value = if (parts.size > 1) URLDecoder.decode(parts[1], "UTF-8") else ""
            key to value
        }
    }

    private val BASE32_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

    fun base32Decode(input: String): ByteArray {
        val cleaned = input.uppercase().replace("=", "").replace(" ", "")
        val output = ByteArray(cleaned.length * 5 / 8)
        var buffer = 0L
        var bitsLeft = 0
        var outputIndex = 0

        for (c in cleaned) {
            val value = BASE32_CHARS.indexOf(c)
            if (value < 0) throw IllegalArgumentException("Invalid Base32 character: $c")
            buffer = (buffer shl 5) or value.toLong()
            bitsLeft += 5
            if (bitsLeft >= 8) {
                bitsLeft -= 8
                output[outputIndex++] = ((buffer shr bitsLeft) and 0xFF).toByte()
            }
        }
        return output.copyOfRange(0, outputIndex)
    }
}
