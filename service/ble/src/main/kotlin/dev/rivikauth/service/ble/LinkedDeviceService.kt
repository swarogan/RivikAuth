package dev.rivikauth.service.ble

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import dev.rivikauth.core.database.di.VaultPassphraseHolder
import dev.rivikauth.core.datastore.AppPrefsStore
import dev.rivikauth.lib.cable.AuthenticatorConfig
import dev.rivikauth.lib.cable.CableLinkedListener
import dev.rivikauth.lib.cable.CtapProcessor
import dev.rivikauth.lib.cable.FidoCredentialStore
import dev.rivikauth.lib.cable.LinkedDeviceStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@AndroidEntryPoint
class LinkedDeviceService : Service() {

    @Inject lateinit var credentialStore: FidoCredentialStore
    @Inject lateinit var linkedDeviceStore: LinkedDeviceStore
    @Inject lateinit var passphraseHolder: VaultPassphraseHolder
    @Inject lateinit var appPrefsStore: AppPrefsStore

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var listener: CableLinkedListener? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (!passphraseHolder.isUnlocked()) {
            Log.w(TAG, "Vault locked, cannot start listener")
            stopSelf()
            return START_NOT_STICKY
        }

        val linkedEnabled = kotlinx.coroutines.runBlocking {
            appPrefsStore.linkedDeviceEnabled().first()
        }
        if (!linkedEnabled) {
            Log.d(TAG, "Linked device disabled in settings")
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildNotification())

        val processor = CtapProcessor(
            credentialStore,
            passphraseHolder.getPassphrase(),
            AuthenticatorConfig(
                extensions = listOf("credProtect", "hmac-secret", "largeBlobKey"),
            ),
        )
        val bleAdvertiser = CableBleAdvertiser(this)

        val linkedListener = CableLinkedListener(
            ctapProcessor = processor,
            bleAdvertiser = bleAdvertiser,
            linkedDeviceStore = linkedDeviceStore,
        )
        linkedListener.onEvent = { event ->
            when (event) {
                is CableLinkedListener.Event.SessionCompleted -> {
                    Log.d(TAG, "Session completed: creation=${event.wasCreation}")
                }
                is CableLinkedListener.Event.SessionFailed -> {
                    Log.w(TAG, "Session failed: ${event.message}")
                }
                is CableLinkedListener.Event.TunnelReconnecting -> {
                    Log.d(TAG, "Reconnecting device ${event.deviceIndex}, attempt ${event.attempt}")
                }
            }
        }
        listener = linkedListener
        linkedListener.start(serviceScope)

        return START_STICKY
    }

    override fun onDestroy() {
        listener?.stop()
        listener = null
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Linked Device Listener",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Authenticator nasłuchuje na sparowane urządzenia"
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPi = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val stopIntent = Intent(this, LinkedDeviceService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPi = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentTitle("RivikAuth aktywny")
            .setContentText("Nasłuchiwanie na sparowane urządzenia")
            .setContentIntent(contentPi)
            .addAction(android.R.drawable.ic_delete, "Zatrzymaj", stopPi)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val TAG = "LinkedDeviceSvc"
        private const val CHANNEL_ID = "linked_device"
        private const val NOTIFICATION_ID = 9002
        private const val ACTION_STOP = "dev.rivikauth.STOP_LINKED"

        fun start(context: Context) {
            val intent = Intent(context, LinkedDeviceService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, LinkedDeviceService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
