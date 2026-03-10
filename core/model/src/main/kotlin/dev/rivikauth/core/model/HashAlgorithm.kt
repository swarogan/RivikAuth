package dev.rivikauth.core.model

enum class HashAlgorithm(val macName: String) {
    SHA1("HmacSHA1"),
    SHA256("HmacSHA256"),
    SHA512("HmacSHA512"),
    MD5("HmacMD5");
}
