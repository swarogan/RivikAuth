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
import dev.rivikauth.lib.cable.AuthenticatorConfig
import dev.rivikauth.lib.cable.CtapProcessor
import dev.rivikauth.lib.cable.FidoCredentialStore
import dev.rivikauth.lib.nfc.NfcCtapHandler
import dev.rivikauth.lib.webauthn.CtapNfcFramer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class FidoNfcHceService : HostApduService() {

    @Inject lateinit var passphraseHolder: VaultPassphraseHolder
    @Inject lateinit var credentialStore: FidoCredentialStore
    @Inject lateinit var appPrefsStore: AppPrefsStore

    private var handler: NfcCtapHandler? = null

    override fun processCommandApdu(commandApdu: ByteArray, extras: Bundle?): ByteArray {
        Log.d(TAG, "APDU received, ${commandApdu.size} bytes")

        // Gate: NFC enabled preference
        val nfcEnabled = runBlocking { appPrefsStore.nfcEnabled().first() }
        if (!nfcEnabled) {
            Log.w(TAG, "NFC authenticator disabled in settings")
            return CtapNfcFramer.buildErrorResponse(
                CtapNfcFramer.SW_CONDITIONS_NOT_SATISFIED_HI,
                CtapNfcFramer.SW_CONDITIONS_NOT_SATISFIED_LO,
            )
        }

        // Gate: vault must be unlocked
        if (!passphraseHolder.isUnlocked()) {
            Log.w(TAG, "Vault locked, sending notification")
            sendUnlockNotification()
            return CtapNfcFramer.buildErrorResponse(
                CtapNfcFramer.SW_CONDITIONS_NOT_SATISFIED_HI,
                CtapNfcFramer.SW_CONDITIONS_NOT_SATISFIED_LO,
            )
        }

        return getOrCreateHandler().processApdu(commandApdu)
    }

    override fun onDeactivated(reason: Int) {
        Log.d(TAG, "NFC deactivated, reason=$reason")
        handler?.reset()
        handler = null
    }

    private fun getOrCreateHandler(): NfcCtapHandler {
        handler?.let { return it }
        val processor = CtapProcessor(
            credentialStore,
            passphraseHolder.getPassphrase(),
            AuthenticatorConfig(
                extensions = listOf("credProtect", "hmac-secret", "largeBlobKey"),
                transports = listOf("nfc"),
            ),
        )
        val newHandler = NfcCtapHandler(
            commandProcessor = { data -> runBlocking { processor.processCommand(data) } },
            maxResponseLen = MAX_RESPONSE_LEN,
        )
        handler = newHandler
        return newHandler
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
