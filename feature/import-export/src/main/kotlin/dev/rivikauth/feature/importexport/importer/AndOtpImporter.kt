package dev.rivikauth.feature.importexport.importer

import dev.rivikauth.core.model.HashAlgorithm
import dev.rivikauth.core.model.OtpEntry
import dev.rivikauth.core.model.OtpType
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.util.UUID

/**
 * Parses andOTP JSON export.
 *
 * Expected structure — top-level JSON array:
 * ```json
 * [
 *   {
 *     "type": "TOTP",
 *     "secret": "BASE32SECRET",
 *     "issuer": "Example",
 *     "label": "user@example.com",
 *     "algorithm": "SHA1",
 *     "digits": 6,
 *     "period": 30
 *   }
 * ]
 * ```
 */
class AndOtpImporter : Importer {

    override val name: String = "andOTP"

    override fun parse(input: InputStream): List<OtpEntry> {
        val array = JSONArray(input.bufferedReader().readText())

        val result = mutableListOf<OtpEntry>()

        for (i in 0 until array.length()) {
            val entry = array.getJSONObject(i)
            val parsed = parseEntry(entry)
            if (parsed != null) {
                result.add(parsed)
            }
        }
        return result
    }

    private fun parseEntry(entry: JSONObject): OtpEntry? {
        val typeName = entry.optString("type", "TOTP").uppercase()
        val type = when (typeName) {
            "TOTP" -> OtpType.TOTP
            "HOTP" -> OtpType.HOTP
            "STEAM" -> OtpType.STEAM
            "MOTP" -> OtpType.MOTP
            else -> return null
        }

        val secretBase32 = entry.optString("secret", "")
        if (secretBase32.isBlank()) return null

        val secret = if (type == OtpType.MOTP) {
            secretBase32.toByteArray(Charsets.UTF_8)
        } else {
            Base32.decode(secretBase32)
        }

        val issuer = entry.optString("issuer", "")
        val label = entry.optString("label", "")

        val algorithm = when (entry.optString("algorithm", "SHA1").uppercase()) {
            "SHA256" -> HashAlgorithm.SHA256
            "SHA512" -> HashAlgorithm.SHA512
            "MD5" -> HashAlgorithm.MD5
            else -> HashAlgorithm.SHA1
        }

        val digits = entry.optInt("digits", if (type == OtpType.STEAM) 5 else 6)
        val period = entry.optInt("period", if (type == OtpType.MOTP) 10 else 30)
        val counter = entry.optLong("counter", 0L)

        return OtpEntry(
            id = UUID.randomUUID().toString(),
            name = label,
            issuer = issuer,
            type = type,
            secret = secret,
            algorithm = algorithm,
            digits = digits,
            period = period,
            counter = counter,
        )
    }
}
