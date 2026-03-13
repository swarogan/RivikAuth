package dev.rivikauth.feature.fido

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CableScanScreen(
    onDone: () -> Unit,
    rawUri: String = "",
    viewModel: CableScanViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var cameraPermissionGranted by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Auto-close on auth without success screen
    LaunchedEffect(Unit) {
        viewModel.autoDone.collect { onDone() }
    }

    // Sprawdź BT na starcie
    LaunchedEffect(Unit) { viewModel.checkBluetooth() }

    // Po powrocie z ustawień BT — sprawdź ponownie
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.checkBluetooth() }

    // Gdy rawUri podany z Scanner — od razu startuj sesję, bez kamery
    LaunchedEffect(rawUri) {
        if (rawUri.isNotEmpty()) {
            viewModel.onQrScanned(rawUri)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        cameraPermissionGranted = results[Manifest.permission.CAMERA] == true
    }

    LaunchedEffect(Unit) {
        if (rawUri.isEmpty()) {
            val permissions = mutableListOf(Manifest.permission.CAMERA)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
                permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            permissionLauncher.launch(permissions.toTypedArray())
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                permissionLauncher.launch(arrayOf(
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.BLUETOOTH_CONNECT,
                ))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.cable_title)) })
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = uiState,
                label = "cable_scan_state",
            ) { state ->
                when (state) {
                    is CableScanUiState.BluetoothOff -> {
                        StatusView(
                            icon = { Icon(Icons.Default.BluetoothDisabled, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error) },
                            title = stringResource(R.string.cable_bt_off_title),
                            subtitle = stringResource(R.string.cable_bt_off_subtitle),
                            action = {
                                Button(onClick = {
                                    context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                                }) {
                                    Text(stringResource(R.string.cable_bt_enable))
                                }
                            },
                        )
                    }

                    is CableScanUiState.Scanning -> {
                        if (cameraPermissionGranted) {
                            CableCameraPreview(
                                onQrDetected = { viewModel.onQrScanned(it) },
                            )
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                Icon(
                                    Icons.Default.QrCodeScanner,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(stringResource(R.string.cable_camera_permission_required))
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = {
                                    permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
                                }) {
                                    Text(stringResource(R.string.cable_grant_permission))
                                }
                            }
                        }
                    }

                    is CableScanUiState.Advertising -> {
                        StatusView(
                            icon = { Icon(Icons.Default.Bluetooth, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary) },
                            title = stringResource(R.string.cable_advertising_title),
                            subtitle = stringResource(R.string.cable_advertising_subtitle),
                        )
                    }

                    is CableScanUiState.Connecting -> {
                        StatusView(
                            icon = { CircularProgressIndicator(Modifier.size(64.dp)) },
                            title = stringResource(R.string.cable_connecting_title),
                            subtitle = stringResource(R.string.cable_connecting_subtitle),
                        )
                    }

                    is CableScanUiState.Handshaking -> {
                        StatusView(
                            icon = { Icon(Icons.Default.Sync, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary) },
                            title = stringResource(R.string.cable_handshaking_title),
                            subtitle = stringResource(R.string.cable_handshaking_subtitle),
                        )
                    }

                    is CableScanUiState.Processing -> {
                        StatusView(
                            icon = { CircularProgressIndicator(Modifier.size(64.dp)) },
                            title = stringResource(R.string.cable_processing_title),
                            subtitle = stringResource(R.string.cable_processing_subtitle),
                        )
                    }

                    is CableScanUiState.Success -> {
                        StatusView(
                            icon = { Icon(Icons.Default.Check, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary) },
                            title = stringResource(R.string.cable_success_title),
                            subtitle = stringResource(R.string.cable_success_subtitle),
                            action = {
                                Button(onClick = onDone) { Text(stringResource(R.string.cable_close)) }
                            },
                        )
                    }

                    is CableScanUiState.Error -> {
                        StatusView(
                            icon = { Icon(Icons.Default.Error, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error) },
                            title = stringResource(R.string.cable_error_title),
                            subtitle = state.message,
                            action = {
                                Button(onClick = { viewModel.resetState() }) {
                                    Text(stringResource(R.string.cable_retry))
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusView(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
    ) {
        icon()
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (action != null) {
            Spacer(modifier = Modifier.height(24.dp))
            action()
        }
    }
}

@Composable
private fun CableCameraPreview(
    onQrDetected: (String) -> Unit,
) {
    val executor = remember { Executors.newSingleThreadExecutor() }
    val reader = remember { MultiFormatReader() }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(R.string.cable_scan_instruction),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp),
            textAlign = TextAlign.Center,
        )

        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).also { previewView ->
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }

                        val analysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also { imageAnalysis ->
                                imageAnalysis.setAnalyzer(executor) { imageProxy ->
                                    decodeCableQr(imageProxy, reader)?.let(onQrDetected)
                                    imageProxy.close()
                                }
                            }

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                ctx as androidx.lifecycle.LifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                analysis,
                            )
                        } catch (_: Exception) {
                            // Camera not available
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                }
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxSize(),
        )
    }
}

private fun decodeCableQr(imageProxy: ImageProxy, reader: MultiFormatReader): String? {
    val plane = imageProxy.planes[0]
    val buffer = plane.buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)

    val width = imageProxy.width
    val height = imageProxy.height
    val source = PlanarYUVLuminanceSource(
        bytes, width, height, 0, 0, width, height, false
    )
    val bitmap = BinaryBitmap(HybridBinarizer(source))

    return try {
        val result = reader.decodeWithState(bitmap).text
        if (result.startsWith("FIDO:/")) result else null
    } catch (_: NotFoundException) {
        null
    } finally {
        reader.reset()
    }
}
