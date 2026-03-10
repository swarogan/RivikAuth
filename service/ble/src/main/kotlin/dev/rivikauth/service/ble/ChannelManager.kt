package dev.rivikauth.service.ble

import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Manages CTAP HID channel ID allocation.
 */
class ChannelManager {

    private val nextChannelId = AtomicInteger(1)
    private val activeChannels = ConcurrentHashMap<Int, ChannelState>()

    companion object {
        /** Broadcast channel used for INIT commands. */
        val BROADCAST_CID = byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte())
    }

    /**
     * Allocate a new channel ID.
     * @return 4-byte channel ID
     */
    fun allocate(): ByteArray {
        val id = nextChannelId.getAndIncrement()
        val cid = ByteBuffer.allocate(4).putInt(id).array()
        activeChannels[id] = ChannelState()
        return cid
    }

    /**
     * Check if a channel ID is the broadcast channel.
     */
    fun isBroadcast(cid: ByteArray): Boolean =
        cid.contentEquals(BROADCAST_CID)

    /**
     * Get the state for a channel, or null if not allocated.
     */
    fun getState(cid: ByteArray): ChannelState? {
        val id = ByteBuffer.wrap(cid).int
        return activeChannels[id]
    }

    /**
     * Release a channel ID.
     */
    fun release(cid: ByteArray) {
        val id = ByteBuffer.wrap(cid).int
        activeChannels.remove(id)
    }

    data class ChannelState(
        var pendingCommand: Byte = 0,
        var pendingData: ByteArray = ByteArray(0),
    )
}
