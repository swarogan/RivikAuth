package dev.rivikauth.core.model

enum class OtpType {
    TOTP, HOTP, STEAM, MOTP, YANDEX;

    companion object {
        fun fromString(value: String): OtpType? = when (value.lowercase()) {
            "totp" -> TOTP
            "hotp" -> HOTP
            "steam" -> STEAM
            "motp" -> MOTP
            "yandex", "yaotp" -> YANDEX
            else -> null
        }
    }
}
