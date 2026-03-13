package dev.rivikauth.feature.importexport.importer

import dev.rivikauth.core.model.HashAlgorithm
import dev.rivikauth.core.model.OtpEntry
import dev.rivikauth.core.model.OtpType
import org.json.JSONObject
import java.io.InputStream
import java.util.UUID

/**
 * Parses Aegis JSON vault export (plain-text variant).
 *
 * Expected structure:
 * ```json
 * {
 *   "version": 2,
 *   "db": {
 *     "entries": [
 *       {
 *         "type": "totp",
 *         "name": "account name",
 *         "issuer": "Example",
 *         "info": {
 *           "secret": "BASE32SECRET",
 *           "algo": "SHA1",
 *           "digits": 6,
 *           "period": 30,
 *           "counter": 0,
 *           "pin": ""
 *         }
 *       }
 *     ]
 *   }
 * }
 * ```
 */
class AegisImporter : Importer {

    override val name: String = "Aegis"

    override fun parse(input: InputStream): List<OtpEntry> {
        val json = JSONObject(input.bufferedReader().readText())

        val db = json.optJSONObject("db")
            ?: throw IllegalArgumentException("Missing 'db' field in Aegis export")

        val entriesArray = db.optJSONArray("entries")
            ?: throw IllegalArgumentException("Missing 'entries' array in Aegis export")

        val result = mutableListOf<OtpEntry>()

        for (i in 0 until entriesArray.length()) {
            val entry = entriesArray.getJSONObject(i)
            val parsed = parseEntry(entry)
            if (parsed != null) {
                result.add(parsed)
            }
        }
        return result
    }

    private fun parseEntry(entry: JSONObject): OtpEntry? {
        val type = OtpType.fromString(entry.optString("type", "")) ?: return null

        val name = entry.optString("name", "")
        val issuer = entry.optString("issuer", "")
        val info = entry.optJSONObject("info") ?: return null

        val secretBase32 = info.optString("secret", "")
        if (secretBase32.isBlank()) return null

        val secret = if (type == OtpType.MOTP) {
            secretBase32.toByteArray(Charsets.UTF_8)
        } else {
            Base32.decode(secretBase32)
        }

        val algorithm = HashAlgorithm.fromString(info.optString("algo", "SHA1"))
        val digits = info.optInt("digits", if (type == OtpType.STEAM) 5 else 6)
        val period = info.optInt("period", if (type == OtpType.MOTP) 10 else 30)
        val counter = info.optLong("counter", 0L)
        val pin = info.optString("pin", "").ifBlank { null }

        return OtpEntry(
            id = UUID.randomUUID().toString(),
            name = name,
            issuer = issuer,
            type = type,
            secret = secret,
            algorithm = algorithm,
            digits = digits,
            period = period,
            counter = counter,
            pin = pin,
        )
    }

}
