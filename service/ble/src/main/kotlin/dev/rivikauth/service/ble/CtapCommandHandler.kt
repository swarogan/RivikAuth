package dev.rivikauth.service.ble

import java.nio.ByteBuffer
import java.security.SecureRandom

/**
 * Handles CTAP HID protocol commands.
 * Routes CTAPHID_INIT, CTAPHID_CBOR, CTAPHID_PING, CTAPHID_MSG.
 */
class CtapCommandHandler(
    private val channelManager: ChannelManager,
) {

    companion object {
        const val CMD_PING: Byte = 0x01
        const val CMD_MSG: Byte = 0x03
        const val CMD_INIT: Byte = 0x06
        const val CMD_CBOR: Byte = 0x10
        const val CMD_ERROR: Byte = 0x3F
        const val CMD_KEEPALIVE: Byte = 0x3B

        // Error codes
        const val ERR_INVALID_CMD: Byte = 0x01
        const val ERR_INVALID_CHANNEL: Byte = 0x0B
    }

    /**
     * Process a CTAP HID command.
     * @param channelId 4-byte channel ID
     * @param command CTAP command byte
     * @param data Command payload
     * @return Response data, or null if no response
     */
    fun handle(channelId: ByteArray, command: Byte, data: ByteArray): Pair<Byte, ByteArray>? {
        return when (command) {
            CMD_INIT -> handleInit(channelId, data)
            CMD_PING -> Pair(CMD_PING, data) // Echo back
            CMD_CBOR -> handleCbor(channelId, data)
            CMD_MSG -> handleMsg(channelId, data)
            else -> Pair(CMD_ERROR, byteArrayOf(ERR_INVALID_CMD))
        }
    }

    private fun handleInit(channelId: ByteArray, data: ByteArray): Pair<Byte, ByteArray> {
        // CTAPHID_INIT response:
        // nonce (8 bytes from request) + new CID (4 bytes) + protocol version + device capabilities
        val nonce = if (data.size >= 8) data.copyOfRange(0, 8) else ByteArray(8)
        val newCid = channelManager.allocate()

        val response = ByteBuffer.allocate(17).apply {
            put(nonce)           // 8 bytes nonce echo
            put(newCid)          // 4 bytes new channel ID
            put(2)               // CTAPHID protocol version
            put(1)               // Major device version
            put(0)               // Minor device version
            put(0)               // Build device version
            put(0x04.toByte())   // Capabilities: CAPABILITY_CBOR
        }.array()

        return Pair(CMD_INIT, response)
    }

    private fun handleCbor(channelId: ByteArray, data: ByteArray): Pair<Byte, ByteArray> {
        if (channelManager.isBroadcast(channelId)) {
            return Pair(CMD_ERROR, byteArrayOf(ERR_INVALID_CHANNEL))
        }
        // TODO: Parse CBOR command (authenticatorMakeCredential, authenticatorGetAssertion, etc.)
        // For now, return error indicating not implemented
        return Pair(CMD_CBOR, byteArrayOf(0x01)) // CTAP2_ERR_INVALID_COMMAND
    }

    private fun handleMsg(channelId: ByteArray, data: ByteArray): Pair<Byte, ByteArray> {
        if (channelManager.isBroadcast(channelId)) {
            return Pair(CMD_ERROR, byteArrayOf(ERR_INVALID_CHANNEL))
        }
        // TODO: Handle U2F/CTAP1 messages
        return Pair(CMD_ERROR, byteArrayOf(ERR_INVALID_CMD))
    }
}
