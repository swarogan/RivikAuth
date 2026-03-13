package dev.rivikauth.feature.importexport.importer

import dev.rivikauth.core.model.HashAlgorithm
import dev.rivikauth.core.model.OtpEntry
import dev.rivikauth.core.model.OtpType
import org.json.JSONObject
import java.io.InputStream
import java.util.UUID

/**
 * Parses 2FAS JSON export.
 *
 * Expected structure:
 * ```json
 * {
 *   "services": [
 *     {
 *       "secret": "BASE32SECRET",
 *       "otp": {
 *         "issuer": "Example",
 *         "account": "user@example.com",
 *         "tokenType": "TOTP",
 *         "algorithm": "SHA1",
 *         "digits": 6,
 *         "period": 30,
 *         "counter": 0
 *       }
 *     }
 *   ]
 * }
 * ```
 */
class TwoFASImporter : Importer {

    override val name: String = "2FAS"

    override fun parse(input: InputStream): List<OtpEntry> {
        val json = JSONObject(input.bufferedReader().readText())

        val services = json.optJSONArray("services")
            ?: throw IllegalArgumentException("Missing 'services' array in 2FAS export")

        val result = mutableListOf<OtpEntry>()

        for (i in 0 until services.length()) {
            val service = services.getJSONObject(i)
            val parsed = parseService(service)
            if (parsed != null) {
                result.add(parsed)
            }
        }
        return result
    }

    private fun parseService(service: JSONObject): OtpEntry? {
        val secretBase32 = service.optString("secret", "")
        if (secretBase32.isBlank()) return null

        val otp = service.optJSONObject("otp") ?: return null

        val issuer = otp.optString("issuer", "")
        val account = otp.optString("account", "")
        val tokenType = otp.optString("tokenType", "TOTP").uppercase()

        val type = OtpType.fromString(tokenType) ?: OtpType.TOTP

        val algorithm = HashAlgorithm.fromString(otp.optString("algorithm", "SHA1"))

        val digits = otp.optInt("digits", if (type == OtpType.STEAM) 5 else 6)
        val period = otp.optInt("period", 30)
        val counter = otp.optLong("counter", 0L)

        return OtpEntry(
            id = UUID.randomUUID().toString(),
            name = account,
            issuer = issuer,
            type = type,
            secret = Base32.decode(secretBase32),
            algorithm = algorithm,
            digits = digits,
            period = period,
            counter = counter,
        )
    }
}
