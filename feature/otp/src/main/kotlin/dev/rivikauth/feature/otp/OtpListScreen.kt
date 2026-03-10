package dev.rivikauth.feature.otp

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.rivikauth.core.model.OtpEntry
import kotlinx.coroutines.android.awaitFrame

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpListScreen(
    viewModel: OtpListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var searchVisible by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var entryToDelete by remember { mutableStateOf<OtpEntry?>(null) }
    var menuEntry by remember { mutableStateOf<OtpEntry?>(null) }

    val filteredEntries = remember(uiState.entries, uiState.searchQuery) {
        val q = uiState.searchQuery.lowercase()
        if (q.isEmpty()) uiState.entries
        else uiState.entries.filter {
            it.name.lowercase().contains(q) || it.issuer.lowercase().contains(q)
        }
    }

    val favorites = filteredEntries.filter { it.favorite }
    val others = filteredEntries.filter { !it.favorite }

    // Dialog potwierdzenia usunięcia
    if (entryToDelete != null) {
        val entry = entryToDelete!!
        val label = entry.issuer.ifEmpty { entry.name }
        AlertDialog(
            onDismissRequest = { entryToDelete = null },
            title = { Text(stringResource(R.string.otp_delete_title)) },
            text = { Text(stringResource(R.string.otp_delete_message, label)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteEntry(entry.id)
                    entryToDelete = null
                }) {
                    Text(stringResource(R.string.otp_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { entryToDelete = null }) {
                    Text(stringResource(R.string.otp_cancel))
                }
            },
        )
    }

    // Wspólny timer TOTP (30s)
    var totpProgress by remember { mutableFloatStateOf(1f) }
    var totpRemaining by remember { mutableFloatStateOf(30f) }
    var codeTimerState by remember { mutableStateOf(CodeTimerState.Normal) }

    LaunchedEffect(Unit) {
        while (true) {
            awaitFrame()
            val nowMs = System.currentTimeMillis()
            val elapsed = (nowMs % 30_000L).toFloat() / 30_000f
            totpProgress = 1f - elapsed
            totpRemaining = totpProgress * 30f

            if (codeTimerState != CodeTimerState.Refreshed && totpRemaining <= 5f) {
                codeTimerState = CodeTimerState.Urgent
            }
        }
    }

    // Wykryj faktyczną zmianę kodów (nie reset timera)
    val currentCodes = uiState.codes
    var prevCodes by remember { mutableStateOf(currentCodes) }
    LaunchedEffect(currentCodes) {
        if (prevCodes.isNotEmpty() && currentCodes != prevCodes) {
            codeTimerState = CodeTimerState.Refreshed
            prevCodes = currentCodes
            kotlinx.coroutines.delay(1500)
            codeTimerState = CodeTimerState.Normal
        } else {
            prevCodes = currentCodes
        }
    }

    val urgent = codeTimerState == CodeTimerState.Urgent
    val timerColor by animateColorAsState(
        targetValue = if (urgent) Color(0xFFE53935) else MaterialTheme.colorScheme.primary,
        animationSpec = tween(300, easing = LinearEasing),
        label = "timerBarColor",
    )
    val trackColor by animateColorAsState(
        targetValue = if (urgent) Color(0x33E53935) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
        animationSpec = tween(300, easing = LinearEasing),
        label = "timerBarTrack",
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.otp_title)) },
                actions = {
                    if (uiState.entries.isNotEmpty()) {
                        IconButton(onClick = {
                            searchVisible = !searchVisible
                            if (!searchVisible) {
                                viewModel.setSearchQuery("")
                                focusManager.clearFocus()
                            }
                        }) {
                            Icon(
                                if (searchVisible) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = stringResource(R.string.otp_search),
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Wspólny pasek postępu TOTP
            if (filteredEntries.isNotEmpty()) {
                LinearProgressIndicator(
                    progress = { totpProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = timerColor,
                    trackColor = trackColor,
                )
            }
            AnimatedVisibility(
                visible = searchVisible,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                TextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::setSearchQuery,
                    placeholder = { Text(stringResource(R.string.otp_search)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                )

                LaunchedEffect(searchVisible) {
                    if (searchVisible) focusRequester.requestFocus()
                }
            }

            if (filteredEntries.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        Icons.Outlined.QrCodeScanner,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.otp_empty_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.otp_empty_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (favorites.isNotEmpty()) {
                        items(favorites, key = { it.id }) { entry ->
                            OtpEntryWithMenu(
                                entry = entry,
                                code = uiState.codes[entry.id],
                                timerState = codeTimerState,
                                showMenu = menuEntry?.id == entry.id,
                                onCopy = { code -> copyToClipboard(context, code) },
                                onFavoriteToggle = { viewModel.toggleFavorite(entry.id) },
                                onLongPress = { menuEntry = entry },
                                onDismissMenu = { menuEntry = null },
                                onDelete = {
                                    menuEntry = null
                                    entryToDelete = entry
                                },
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                    items(others, key = { it.id }) { entry ->
                        OtpEntryWithMenu(
                            entry = entry,
                            code = uiState.codes[entry.id],
                            timerState = codeTimerState,
                            showMenu = menuEntry?.id == entry.id,
                            onCopy = { code -> copyToClipboard(context, code) },
                            onFavoriteToggle = { viewModel.toggleFavorite(entry.id) },
                            onLongPress = { menuEntry = entry },
                            onDismissMenu = { menuEntry = null },
                            onDelete = {
                                menuEntry = null
                                entryToDelete = entry
                            },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OtpEntryWithMenu(
    entry: OtpEntry,
    code: String?,
    timerState: CodeTimerState,
    showMenu: Boolean,
    onCopy: (String) -> Unit,
    onFavoriteToggle: () -> Unit,
    onLongPress: () -> Unit,
    onDismissMenu: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        OtpEntryCard(
            entry = entry,
            code = code,
            timerState = timerState,
            onCopy = onCopy,
            onLongPress = onLongPress,
        )

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = onDismissMenu,
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.otp_action_copy)) },
                onClick = {
                    code?.let(onCopy)
                    onDismissMenu()
                },
                leadingIcon = { Icon(Icons.Default.ContentCopy, null) },
            )
            DropdownMenuItem(
                text = {
                    Text(
                        if (entry.favorite) stringResource(R.string.otp_action_unfavorite)
                        else stringResource(R.string.otp_action_favorite)
                    )
                },
                onClick = {
                    onFavoriteToggle()
                    onDismissMenu()
                },
                leadingIcon = {
                    Icon(
                        if (entry.favorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        null,
                    )
                },
            )
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(R.string.otp_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                },
                onClick = onDelete,
                leadingIcon = {
                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                },
            )
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("OTP Code", text))
}
