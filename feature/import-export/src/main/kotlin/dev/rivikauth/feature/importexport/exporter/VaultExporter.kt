package dev.rivikauth.feature.importexport.exporter

import dev.rivikauth.core.model.OtpEntry
import dev.rivikauth.feature.importexport.importer.Base32
import org.json.JSONArray
import org.json.JSONObject

object VaultExporter {

    /**
     * Exports entries as a plain-text JSON string.
     *
     * Output format:
     * ```json
     * {
     *   "version": 1,
     *   "entries": [
     *     {
     *       "type": "TOTP",
     *       "name": "...",
     *       "issuer": "...",
     *       "secret": "BASE32...",
     *       "algorithm": "SHA1",
     *       "digits": 6,
     *       "period": 30,
     *       "counter": 0,
     *       "pin": null,
     *       "note": null
     *     }
     *   ]
     * }
     * ```
     */
    fun exportPlaintext(entries: List<OtpEntry>): String {
        val entriesArray = JSONArray()

        for (entry in entries) {
            val obj = JSONObject().apply {
                put("type", entry.type.name)
                put("name", entry.name)
                put("issuer", entry.issuer)
                put("secret", Base32.encode(entry.secret))
                put("algorithm", entry.algorithm.name)
                put("digits", entry.digits)
                put("period", entry.period)
                put("counter", entry.counter)
                put("pin", entry.pin ?: JSONObject.NULL)
                put("note", entry.note ?: JSONObject.NULL)
            }
            entriesArray.put(obj)
        }

        val root = JSONObject().apply {
            put("version", 1)
            put("entries", entriesArray)
        }

        return root.toString(2)
    }
}
