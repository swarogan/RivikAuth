package dev.rivikauth.feature.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen() {
    val context = LocalContext.current
    val versionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
        } catch (_: Exception) { "?" }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.about_title)) })
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.rivikauth_logo),
                contentDescription = null,
                modifier = Modifier.size(80.dp),
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "RivikAuthenticator",
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                stringResource(R.string.about_author),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "v$versionName (${BuildConfig.GIT_HASH})",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                stringResource(R.string.about_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Column(horizontalAlignment = Alignment.Start) {
                LinkRow(
                    icon = Icons.Outlined.Language,
                    text = stringResource(R.string.about_website),
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(getStringResource(context, R.string.about_website_url)))
                        )
                    },
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinkRow(
                    icon = GithubIcon,
                    text = stringResource(R.string.about_github),
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(getStringResource(context, R.string.about_github_url)))
                        )
                    },
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinkRow(
                    icon = XIcon,
                    text = stringResource(R.string.about_x),
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(getStringResource(context, R.string.about_x_url)))
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun LinkRow(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium.copy(
                textDecoration = TextDecoration.Underline,
            ),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

private val GithubIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "GitHub",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 0.297f)
            curveTo(5.37f, 0.297f, 0f, 5.67f, 0f, 12.297f)
            curveTo(0f, 17.6f, 3.438f, 22.097f, 8.205f, 23.682f)
            curveTo(8.805f, 23.795f, 9.025f, 23.424f, 9.025f, 23.105f)
            curveTo(9.025f, 22.82f, 9.015f, 22.065f, 9.01f, 21.065f)
            curveTo(5.672f, 21.789f, 4.968f, 19.455f, 4.968f, 19.455f)
            curveTo(4.422f, 18.07f, 3.633f, 17.7f, 3.633f, 17.7f)
            curveTo(2.546f, 16.956f, 3.717f, 16.971f, 3.717f, 16.971f)
            curveTo(4.922f, 17.055f, 5.555f, 18.207f, 5.555f, 18.207f)
            curveTo(6.625f, 20.042f, 8.364f, 19.512f, 9.048f, 19.205f)
            curveTo(9.156f, 18.429f, 9.467f, 17.9f, 9.81f, 17.6f)
            curveTo(7.145f, 17.3f, 4.344f, 16.268f, 4.344f, 11.67f)
            curveTo(4.344f, 10.36f, 4.809f, 9.29f, 5.579f, 8.45f)
            curveTo(5.444f, 8.147f, 5.039f, 6.927f, 5.684f, 5.274f)
            curveTo(5.684f, 5.274f, 6.689f, 4.952f, 8.984f, 6.504f)
            curveTo(9.944f, 6.237f, 10.964f, 6.105f, 11.984f, 6.099f)
            curveTo(13.004f, 6.105f, 14.024f, 6.237f, 14.984f, 6.504f)
            curveTo(17.264f, 4.952f, 18.269f, 5.274f, 18.269f, 5.274f)
            curveTo(18.914f, 6.927f, 18.509f, 8.147f, 18.389f, 8.45f)
            curveTo(19.154f, 9.29f, 19.619f, 10.36f, 19.619f, 11.67f)
            curveTo(19.619f, 16.28f, 16.814f, 17.295f, 14.144f, 17.59f)
            curveTo(14.564f, 17.95f, 14.954f, 18.686f, 14.954f, 19.81f)
            curveTo(14.954f, 21.416f, 14.939f, 22.706f, 14.939f, 23.096f)
            curveTo(14.939f, 23.411f, 15.149f, 23.786f, 15.764f, 23.666f)
            curveTo(20.565f, 22.092f, 24f, 17.592f, 24f, 12.297f)
            curveTo(24f, 5.67f, 18.627f, 0.297f, 12f, 0.297f)
            close()
        }
    }.build()
}

private val XIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "X",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(18.244f, 2.25f)
            lineTo(21.552f, 2.25f)
            lineTo(14.325f, 10.51f)
            lineTo(22.827f, 21.75f)
            lineTo(16.17f, 21.75f)
            lineTo(10.956f, 14.933f)
            lineTo(4.99f, 21.75f)
            lineTo(1.68f, 21.75f)
            lineTo(9.41f, 12.915f)
            lineTo(1.254f, 2.25f)
            lineTo(8.08f, 2.25f)
            lineTo(12.793f, 8.481f)
            close()
            moveTo(17.083f, 19.77f)
            lineTo(18.916f, 19.77f)
            lineTo(7.084f, 4.126f)
            lineTo(5.117f, 4.126f)
            close()
        }
    }.build()
}

private fun getStringResource(context: android.content.Context, resId: Int): String =
    context.getString(resId)
