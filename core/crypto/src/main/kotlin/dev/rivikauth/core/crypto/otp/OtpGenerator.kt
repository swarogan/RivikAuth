package dev.rivikauth.core.crypto.otp

import dev.rivikauth.core.model.OtpEntry
import dev.rivikauth.core.model.OtpType

object OtpGenerator {
    fun generate(entry: OtpEntry, timeSeconds: Long = System.currentTimeMillis() / 1000): String {
        return when (entry.type) {
            OtpType.TOTP -> Totp.generate(entry.secret, entry.algorithm.macName, entry.digits, entry.period, timeSeconds)
            OtpType.HOTP -> Hotp.generate(entry.secret, entry.algorithm.macName, entry.digits, entry.counter)
            OtpType.STEAM -> SteamOtp.generate(entry.secret, timeSeconds, entry.period)
            OtpType.MOTP -> Motp.generate(entry.secret, entry.pin ?: "", entry.digits, entry.period, timeSeconds)
            OtpType.YANDEX -> YandexOtp.generate(entry.secret, entry.pin ?: "", entry.digits, entry.period, timeSeconds)
        }
    }
}
