package dev.rivikauth.core.model

enum class CoseAlgorithm(val coseId: Int, val jcaName: String) {
    ES256(-7, "SHA256withECDSA"),
    ES384(-35, "SHA384withECDSA"),
    RS256(-257, "SHA256withRSA");
}
