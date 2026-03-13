package dev.rivikauth.service.nfc

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import dev.rivikauth.core.database.di.VaultPassphraseHolder
import dev.rivikauth.core.datastore.AppPrefsStore
import dev.rivikauth.lib.cable.CtapProcessor
import dev.rivikauth.lib.cable.FidoCredentialStore
import dev.rivikauth.lib.webauthn.CtapNfcFramer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject

@AndroidEntryPoint
class FidoNfcHceService : HostApduService() {

    @Inject lateinit var passphraseHolder: VaultPassphraseHolder
    @Inject lateinit var credentialStore: FidoCredentialStore
    @Inject lateinit var appPrefsStore: AppPrefsStore

    private var pendingResponse: ByteArray? = null
    private var pendingOffset: Int = 0
    private var commandBuffer = java.io.ByteArrayOutputStream()
    private var selected = false

    override fun processCommandApdu(commandApdu: ByteArray, extras: Bundle?): ByteArray {
        val apdu = CtapNfcFramer.parseApdu(commandApdu)
        Log.d(TAG, "APDU INS=0x${"%02x".format(apdu.ins)} dataLen=${apdu.data.size}")

        return when (apdu.ins) {
            CtapNfcFramer.INS_SELECT -> handleSelect(apdu)
            CtapNfcFramer.INS_CTAP_MSG -> handleCtapMsg(apdu)
            CtapNfcFramer.INS_GET_RESPONSE -> handleGetResponse(apdu)
            else -> CtapNfcFramer.buildErrorResponse(
                CtapNfcFramer.SW_INS_NOT_SUPPORTED_HI,
                CtapNfcFramer.SW_INS_NOT_SUPPORTED_LO,
            )
        }
    }

    private fun handleSelect(apdu: CtapNfcFramer.NfcApduCommand): ByteArray {
        if (!apdu.data.contentEquals(CtapNfcFramer.FIDO2_AID)) {
            return CtapNfcFramer.buildErrorResponse(
                CtapNfcFramer.SW_WRONG_DATA_HI,
                CtapNfcFramer.SW_WRONG_DATA_LO,
            )
        }
        resetSession()
        selected = true
        Log.d(TAG, "FIDO2 applet selected")
        return CtapNfcFramer.buildSelectResponse()
    }

    private fun handleCtapMsg(apdu: CtapNfcFramer.NfcApduCommand): ByteArray {
        if (!selected) {
            return CtapNfcFramer.buildErrorResponse(
                CtapNfcFramer.SW_CONDITIONS_NOT_SATISFIED_HI,
                CtapNfcFramer.SW_CONDITIONS_NOT_SATISFIED_LO,
            )
        }

        // Check NFC enabled preference
        val nfcEnabled = runBlocking { appPrefsStore.nfcEnabled().first() }
        if (!nfcEnabled) {
            Log.w(TAG, "NFC authenticator disabled in settings")
            return CtapNfcFramer.buildErrorResponse(
                CtapNfcFramer.SW_CONDITIONS_NOT_SATISFIED_HI,
                CtapNfcFramer.SW_CONDITIONS_NOT_SATISFIED_LO,
            )
        }

        // Check vault unlocked
        if (!passphraseHolder.isUnlocked()) {
            Log.w(TAG, "Vault locked, sending notification")
            sendUnlockNotification()
            return CtapNfcFramer.buildErrorResponse(
                CtapNfcFramer.SW_CONDITIONS_NOT_SATISFIED_HI,
                CtapNfcFramer.SW_CONDITIONS_NOT_SATISFIED_LO,
            )
        }

        // Accumulate chained command data
        commandBuffer.write(apdu.data)

        // If CLA bit 4 is set, this is a chained command — wait for more
        if (apdu.cla.toInt() and 0x10 != 0) {
            return CtapNfcFramer.buildErrorResponse(
                CtapNfcFramer.SW_SUCCESS_HI,
                CtapNfcFramer.SW_SUCCESS_LO,
            )
        }

        val ctapCommand = commandBuffer.toByteArray()
        commandBuffer.reset()

        val masterKey = SecretKeySpec(passphraseHolder.getPassphrase(), "AES")
        val processor = CtapProcessor(credentialStore, masterKey.encoded)

        val ctapResponse = runBlocking { processor.processCommand(ctapCommand) }
        Log.d(TAG, "CTAP response: ${ctapResponse.size} bytes")

        val maxChunk = if (apdu.le > 0) apdu.le else MAX_RESPONSE_LEN
        val response = CtapNfcFramer.buildCtapResponse(ctapResponse, maxChunk)

        if (response.sw1 == CtapNfcFramer.SW_MORE_DATA) {
            pendingResponse = ctapResponse
            pendingOffset = maxChunk
        }

        return response.toBytes()
    }

    private fun handleGetResponse(apdu: CtapNfcFramer.NfcApduCommand): ByteArray {
        val buffer = pendingResponse ?: return CtapNfcFramer.buildErrorResponse(
            CtapNfcFramer.SW_CONDITIONS_NOT_SATISFIED_HI,
            CtapNfcFramer.SW_CONDITIONS_NOT_SATISFIED_LO,
        )

        val maxChunk = if (apdu.le > 0) apdu.le else MAX_RESPONSE_LEN
        val response = CtapNfcFramer.buildGetResponse(buffer, pendingOffset, maxChunk)

        if (response.sw1 == CtapNfcFramer.SW_MORE_DATA) {
            pendingOffset += maxChunk
        } else {
            pendingResponse = null
            pendingOffset = 0
        }

        return response.toBytes()
    }

    override fun onDeactivated(reason: Int) {
        Log.d(TAG, "NFC deactivated, reason=$reason")
        resetSession()
    }

    private fun resetSession() {
        pendingResponse = null
        pendingOffset = 0
        commandBuffer.reset()
        selected = false
    }

    private fun sendUnlockNotification() {
        val nm = getSystemService(NotificationManager::class.java)

        val channel = NotificationChannel(
            CHANNEL_ID, "FIDO2 NFC", NotificationManager.IMPORTANCE_HIGH,
        )
        nm.createNotificationChannel(channel)

        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentTitle(getString(R.string.nfc_unlock_title))
            .setContentText(getString(R.string.nfc_unlock_text))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        nm.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val TAG = "FidoNfcHce"
        private const val MAX_RESPONSE_LEN = 256
        private const val CHANNEL_ID = "fido_nfc"
        private const val NOTIFICATION_ID = 9001
    }
}
