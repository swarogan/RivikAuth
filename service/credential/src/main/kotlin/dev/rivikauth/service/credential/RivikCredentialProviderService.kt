package dev.rivikauth.service.credential

import android.app.PendingIntent
import android.content.Intent
import android.os.CancellationSignal
import android.os.OutcomeReceiver
import android.util.Log
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.CreateCredentialUnknownException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.provider.BeginCreateCredentialRequest
import androidx.credentials.provider.BeginCreateCredentialResponse
import androidx.credentials.provider.BeginGetCredentialRequest
import androidx.credentials.provider.BeginGetCredentialResponse
import androidx.credentials.provider.BeginGetPublicKeyCredentialOption
import androidx.credentials.provider.CreateEntry
import androidx.credentials.provider.CredentialProviderService
import androidx.credentials.provider.ProviderClearCredentialStateRequest
import androidx.credentials.provider.PublicKeyCredentialEntry
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dev.rivikauth.core.database.dao.FidoCredentialDao
import kotlinx.coroutines.runBlocking
import org.json.JSONObject

class RivikCredentialProviderService : CredentialProviderService() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ServiceEntryPoint {
        fun fidoCredentialDao(): FidoCredentialDao
    }

    private val dao: FidoCredentialDao by lazy {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            ServiceEntryPoint::class.java,
        )
        entryPoint.fidoCredentialDao()
    }

    override fun onBeginCreateCredentialRequest(
        request: BeginCreateCredentialRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<BeginCreateCredentialResponse, CreateCredentialException>,
    ) {
        try {
            val createIntent = Intent(applicationContext, CreatePasskeyActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                applicationContext,
                PENDING_INTENT_CREATE_REQUEST_CODE,
                createIntent,
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

            val createEntry = CreateEntry.Builder(
                "RivikAuthenticator",
                pendingIntent,
            )
                .setDescription("Utwórz passkey w RivikAuthenticator")
                .build()

            callback.onResult(
                BeginCreateCredentialResponse.Builder()
                    .addCreateEntry(createEntry)
                    .build()
            )
        } catch (e: Exception) {
            Log.e(TAG, "onBeginCreateCredentialRequest failed", e)
            callback.onError(
                CreateCredentialUnknownException(e.message)
            )
        }
    }

    override fun onBeginGetCredentialRequest(
        request: BeginGetCredentialRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<BeginGetCredentialResponse, GetCredentialException>,
    ) {
        try {
            val responseBuilder = BeginGetCredentialResponse.Builder()
            var requestCode = PENDING_INTENT_GET_BASE_REQUEST_CODE

            for (option in request.beginGetCredentialOptions) {
                if (option is BeginGetPublicKeyCredentialOption) {
                    val requestJson = JSONObject(option.requestJson)
                    val rpId = requestJson.getString("rpId")

                    val credentials = runBlocking { dao.getByRpId(rpId) }

                    for (credential in credentials) {
                        val getIntent = Intent(applicationContext, GetPasskeyActivity::class.java)
                            .putExtra("credential_db_id", credential.id)

                        val pendingIntent = PendingIntent.getActivity(
                            applicationContext,
                            requestCode++,
                            getIntent,
                            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                        )

                        val entry = PublicKeyCredentialEntry.Builder(
                            applicationContext,
                            credential.userDisplayName.ifBlank { credential.userName },
                            pendingIntent,
                            option,
                        )
                            .build()

                        responseBuilder.addCredentialEntry(entry)
                    }
                }
            }

            callback.onResult(responseBuilder.build())
        } catch (e: Exception) {
            Log.e(TAG, "onBeginGetCredentialRequest failed", e)
            callback.onError(
                GetCredentialUnknownException(e.message)
            )
        }
    }

    override fun onClearCredentialStateRequest(
        request: ProviderClearCredentialStateRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<Void?, ClearCredentialException>,
    ) {
        // No cached credential state to clear -- credentials live in the database
        callback.onResult(null)
    }

    companion object {
        private const val TAG = "RivikCredProviderSvc"
        private const val PENDING_INTENT_CREATE_REQUEST_CODE = 1000
        private const val PENDING_INTENT_GET_BASE_REQUEST_CODE = 2000
    }
}
