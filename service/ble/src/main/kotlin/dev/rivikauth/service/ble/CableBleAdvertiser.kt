package dev.rivikauth.service.ble

import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import dev.rivikauth.lib.cable.CableSession
import java.util.UUID

class CableBleAdvertiser(
    private val context: Context,
) : CableSession.BleAdvertiserCallback {

    private var advertiser: BluetoothLeAdvertiser? = null
    private var callback: AdvertiseCallback? = null

    companion object {
        private const val TAG = "CableBleAdvertiser"
        private val FIDO_CABLE_UUID = ParcelUuid(
            UUID.fromString("0000FFF9-0000-1000-8000-00805F9B34FB")
        )

        private fun advertiseErrorMessage(errorCode: Int): String = when (errorCode) {
            AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE -> "dane za duże"
            AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "za dużo advertiserów"
            AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED -> "już uruchomiony"
            AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR -> "błąd wewnętrzny BLE"
            AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> "brak obsługi BLE advertising"
            else -> "nieznany błąd ($errorCode)"
        }
    }

    override fun startAdvertising(eid: ByteArray) {
        Log.w(TAG, "startAdvertising() called, EID size=${eid.size} bytes, EID hex=${eid.joinToString("") { "%02x".format(it) }}")

        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = bluetoothManager?.adapter
        if (adapter == null || !adapter.isEnabled) {
            throw IllegalStateException("Włącz Bluetooth — jest wymagany do komunikacji caBLE z komputerem")
        }

        advertiser = adapter.bluetoothLeAdvertiser
        if (advertiser == null) {
            throw IllegalStateException("Urządzenie nie obsługuje BLE advertising")
        }

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(false)
            .setTimeout(0) // no timeout
            .build()

        val data = AdvertiseData.Builder()
            .addServiceData(FIDO_CABLE_UUID, eid)
            .setIncludeDeviceName(false)
            .setIncludeTxPowerLevel(false)
            .build()

        callback = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                Log.w(TAG, "BLE advertising STARTED OK — mode=${settingsInEffect?.mode}, txPower=${settingsInEffect?.txPowerLevel}")
            }

            override fun onStartFailure(errorCode: Int) {
                Log.e(TAG, "BLE advertising FAILED: ${advertiseErrorMessage(errorCode)}")
            }
        }

        // Nie łapiemy SecurityException — niech propaguje do CableSession
        advertiser!!.startAdvertising(settings, data, callback)
        Log.w(TAG, "startAdvertising() call completed (callback will confirm)")
    }

    override fun stopAdvertising() {
        try {
            callback?.let { advertiser?.stopAdvertising(it) }
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing Bluetooth permissions for stop", e)
        }
        callback = null
        advertiser = null
        Log.w(TAG, "BLE advertising stopped")
    }
}
