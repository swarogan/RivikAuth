package dev.rivikauth.feature.otp

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.rivikauth.core.model.OtpEntry

private val FavoriteDot = Color(0xFFFFC107)

private val LetterColors = arrayOf(
    Color(0xFFEF5350), // A
    Color(0xFFEC407A), // B
    Color(0xFFAB47BC), // C
    Color(0xFF7E57C2), // D
    Color(0xFF5C6BC0), // E
    Color(0xFF42A5F5), // F
    Color(0xFF29B6F6), // G
    Color(0xFF26C6DA), // H
    Color(0xFF26A69A), // I
    Color(0xFF66BB6A), // J
    Color(0xFF9CCC65), // K
    Color(0xFFD4E157), // L
    Color(0xFFFFEE58), // M
    Color(0xFFFFCA28), // N
    Color(0xFFFFA726), // O
    Color(0xFFFF7043), // P
    Color(0xFF8D6E63), // Q
    Color(0xFF90A4AE), // R
    Color(0xFF78909C), // S
    Color(0xFFEF5350), // T
    Color(0xFFAB47BC), // U
    Color(0xFF5C6BC0), // V
    Color(0xFF29B6F6), // W
    Color(0xFF26A69A), // X
    Color(0xFF66BB6A), // Y
    Color(0xFFFFA726), // Z
)

private fun letterColor(char: Char): Color {
    val index = char.uppercaseChar() - 'A'
    return if (index in LetterColors.indices) LetterColors[index] else Color(0xFF757575)
}

private val UrgentColor = Color(0xFFEF5350)

enum class CodeTimerState { Normal, Urgent, Refreshed }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OtpEntryCard(
    entry: OtpEntry,
    code: String?,
    timerState: CodeTimerState = CodeTimerState.Normal,
    onCopy: (String) -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val displayCode = code ?: "------"

    val codeColor by animateColorAsState(
        targetValue = when (timerState) {
            CodeTimerState.Urgent -> UrgentColor
            CodeTimerState.Refreshed -> Color.White
            CodeTimerState.Normal -> MaterialTheme.colorScheme.onSurface
        },
        animationSpec = tween(500, easing = LinearEasing),
        label = "codeColor",
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { code?.let(onCopy) },
                onLongClick = onLongPress,
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 12.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        ) {
            // Ikonka
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(42.dp),
            ) {
                val firstChar = entry.issuer.firstOrNull() ?: entry.name.firstOrNull() ?: '?'
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(letterColor(firstChar)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = firstChar.uppercase(),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = Color.White,
                    )
                }
                if (entry.favorite) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 1.dp, y = 1.dp)
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(FavoriteDot),
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = buildAnnotatedString {
                        if (entry.issuer.isNotEmpty()) {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(entry.issuer)
                            }
                            append(" (${entry.name})")
                        } else {
                            append(entry.name)
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = formatCode(displayCode),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = codeColor,
                    )
                    IconButton(
                        onClick = { code?.let(onCopy) },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = stringResource(R.string.otp_copy),
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
        }
    }
}

private fun formatCode(code: String): String =
    if (code.length == 6) "${code.substring(0, 3)} ${code.substring(3)}"
    else code
