package dev.rivikauth.service.ble

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothHidDeviceAppQosSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.util.Log
import dev.rivikauth.lib.webauthn.CtapHidFramer

/**
 * Bluetooth HID Device service that acts as a FIDO2 roaming authenticator.
 * Registers as a Bluetooth HID device with the FIDO HID Report Descriptor,
 * receives CTAP commands from connected hosts, and sends responses.
 */
class FidoBleHidService(private val context: Context) {

    private val channelManager = ChannelManager()
    private val commandHandler = CtapCommandHandler(channelManager)
    private var hidDevice: BluetoothHidDevice? = null
    private var connectedDevice: BluetoothDevice? = null

    companion object {
        private const val TAG = "FidoBleHidService"
        private const val SDP_NAME = "RivikAuthenticator"
        private const val SDP_DESCRIPTION = "FIDO2 Authenticator"
        private const val SDP_PROVIDER = "RivikAuth"
    }

    private val hidCallback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            Log.d(TAG, "HID app status: registered=$registered")
        }

        override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
            when (state) {
                BluetoothProfile.STATE_CONNECTED -> {
                    connectedDevice = device
                    Log.d(TAG, "HID connected: ${device?.address}")
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    connectedDevice = null
                    Log.d(TAG, "HID disconnected")
                }
            }
        }

        override fun onGetReport(device: BluetoothDevice?, type: Byte, id: Byte, bufferSize: Int) {
            Log.d(TAG, "onGetReport type=$type id=$id")
        }

        override fun onSetReport(device: BluetoothDevice?, type: Byte, id: Byte, data: ByteArray?) {
            Log.d(TAG, "onSetReport type=$type id=$id dataSize=${data?.size}")
        }

        override fun onInterruptData(device: BluetoothDevice?, reportId: Byte, data: ByteArray?) {
            data ?: return
            handleIncomingData(device, data)
        }
    }

    /**
     * Register the HID device with the Bluetooth subsystem.
     */
    fun register() {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bluetoothManager.adapter ?: run {
            Log.e(TAG, "Bluetooth not available")
            return
        }

        adapter.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
                if (profile == BluetoothProfile.HID_DEVICE && proxy is BluetoothHidDevice) {
                    hidDevice = proxy
                    val sdp = BluetoothHidDeviceAppSdpSettings(
                        SDP_NAME, SDP_DESCRIPTION, SDP_PROVIDER,
                        BluetoothHidDevice.SUBCLASS1_NONE,
                        HidReportDescriptor.DESCRIPTOR
                    )
                    try {
                        proxy.registerApp(sdp, null, null, context.mainExecutor, hidCallback)
                    } catch (e: SecurityException) {
                        Log.e(TAG, "Missing Bluetooth permission", e)
                    }
                }
            }

            override fun onServiceDisconnected(profile: Int) {
                hidDevice = null
            }
        }, BluetoothProfile.HID_DEVICE)
    }

    /**
     * Unregister the HID device.
     */
    fun unregister() {
        try {
            hidDevice?.unregisterApp()
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing Bluetooth permission", e)
        }
        hidDevice = null
    }

    private fun handleIncomingData(device: BluetoothDevice?, data: ByteArray) {
        // Unframe single packet (simplified — full impl would buffer continuation packets)
        val packets = listOf(data.copyOf(CtapHidFramer.PACKET_SIZE.coerceAtMost(data.size)))
        try {
            val (channelId, command, payload) = CtapHidFramer.unframe(packets)
            val response = commandHandler.handle(channelId, command, payload) ?: return
            val responsePackets = CtapHidFramer.frame(channelId, response.first, response.second)

            responsePackets.forEach { packet ->
                try {
                    hidDevice?.sendReport(device, 0, packet)
                } catch (e: SecurityException) {
                    Log.e(TAG, "Missing Bluetooth permission for sendReport", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling HID data", e)
        }
    }
}
