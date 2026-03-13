package dev.rivikauth.core.model

enum class HashAlgorithm(val macName: String) {
    SHA1("HmacSHA1"),
    SHA256("HmacSHA256"),
    SHA512("HmacSHA512"),
    MD5("HmacMD5");

    companion object {
        fun fromString(value: String): HashAlgorithm = when (value.uppercase().replace("-", "")) {
            "SHA256" -> SHA256
            "SHA512" -> SHA512
            "MD5" -> MD5
            else -> SHA1
        }
    }
}
