package dev.rivikauth.core.common

object Base32 {

    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

    fun decode(input: String): ByteArray {
        val cleaned = input.uppercase().replace("=", "").replace(" ", "")
        val output = ByteArray(cleaned.length * 5 / 8)
        var buffer = 0L
        var bitsLeft = 0
        var outputIndex = 0

        for (c in cleaned) {
            val value = ALPHABET.indexOf(c)
            if (value < 0) throw IllegalArgumentException("Invalid Base32 character: $c")
            buffer = (buffer shl 5) or value.toLong()
            bitsLeft += 5
            if (bitsLeft >= 8) {
                bitsLeft -= 8
                output[outputIndex++] = ((buffer shr bitsLeft) and 0xFF).toByte()
            }
        }
        return output.copyOfRange(0, outputIndex)
    }

    fun encode(data: ByteArray): String {
        val sb = StringBuilder()
        var buffer = 0
        var bitsLeft = 0

        for (b in data) {
            buffer = (buffer shl 8) or (b.toInt() and 0xFF)
            bitsLeft += 8
            while (bitsLeft >= 5) {
                bitsLeft -= 5
                sb.append(ALPHABET[(buffer shr bitsLeft) and 0x1F])
            }
        }
        if (bitsLeft > 0) {
            sb.append(ALPHABET[(buffer shl (5 - bitsLeft)) and 0x1F])
        }
        return sb.toString()
    }
}
