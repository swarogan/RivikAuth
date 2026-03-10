package dev.rivikauth.core.model

enum class AttestationFormat(val formatId: String) {
    PACKED("packed"),
    TPM("tpm"),
    ANDROID_KEY("android-key"),
    ANDROID_SAFETYNET("android-safetynet"),
    FIDO_U2F("fido-u2f"),
    APPLE("apple"),
    NONE("none");

    companion object {
        fun fromId(id: String): AttestationFormat =
            entries.first { it.formatId == id }
    }
}
