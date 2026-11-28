package com.piecer.app.ui.screens.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.piecer.app.BuildConfig
import com.piecer.app.R
import com.piecer.app.ui.components.PiecerCard
import com.piecer.app.ui.components.PiecerCardVariant
import com.piecer.app.ui.crash.CrashActivity
import com.piecer.app.ui.theme.PiecerTheme
import com.piecer.app.util.update.AppLinks
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val spacing = PiecerTheme.spacing
    val colorScheme = MaterialTheme.colorScheme
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val openErrorMessage = stringResource(id = R.string.about_link_open_error)

    val openUrl: (String) -> Unit = { url ->
        if (!openExternalUrl(context, url)) {
            scope.launch { snackbarHostState.showSnackbar(openErrorMessage) }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.about_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(id = R.string.action_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                horizontal = spacing.screenPadding,
                vertical = spacing.md
            ),
            verticalArrangement = Arrangement.spacedBy(spacing.lg)
        ) {
            item {
                PiecerCard(variant = PiecerCardVariant.Dark) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(spacing.xs)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_splash_logo),
                            contentDescription = stringResource(id = R.string.about_app_name),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(spacing.xs))
                        Text(
                            text = stringResource(id = R.string.about_app_name),
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = stringResource(
                                id = R.string.about_version_fmt,
                                BuildConfig.VERSION_NAME,
                                BuildConfig.VERSION_CODE
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(id = R.string.about_slogan),
                            style = MaterialTheme.typography.titleSmall,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(spacing.xs))
                        Text(
                            text = stringResource(id = R.string.about_summary),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            item {
                Text(
                    text = stringResource(id = R.string.about_links_header),
                    style = MaterialTheme.typography.labelLarge,
                    color = colorScheme.onSurfaceVariant
                )
            }

            item {
                PiecerCard(variant = PiecerCardVariant.Light) {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        AboutLinkRow(
                            title = stringResource(id = R.string.about_link_source),
                            subtitle = stringResource(id = R.string.about_link_source_desc),
                            onClick = { openUrl(AppLinks.SOURCE_CODE_URL) }
                        )
                        if (AppLinks.TELEGRAM_CHANNEL_URL.isNotBlank()) {
                            HorizontalDivider(color = colorScheme.outlineVariant)
                            AboutLinkRow(
                                title = stringResource(id = R.string.about_link_telegram),
                                subtitle = stringResource(id = R.string.about_link_telegram_desc),
                                onClick = { openUrl(AppLinks.TELEGRAM_CHANNEL_URL) }
                            )
                        }
                        if (AppLinks.DONATE_URL.isNotBlank()) {
                            HorizontalDivider(color = colorScheme.outlineVariant)
                            AboutLinkRow(
                                title = stringResource(id = R.string.about_link_donate),
                                subtitle = stringResource(id = R.string.about_link_donate_desc),
                                onClick = { openUrl(AppLinks.DONATE_URL) }
                            )
                        }
                        HorizontalDivider(color = colorScheme.outlineVariant)
                        AboutLinkRow(
                            title = stringResource(id = R.string.about_last_crash_title),
                            subtitle = stringResource(id = R.string.about_last_crash_desc),
                            onClick = {
                                context.startActivity(Intent(context, CrashActivity::class.java))
                            }
                        )
                    }
                }
            }

            item {
                Text(
                    text = stringResource(id = R.string.about_licenses_header),
                    style = MaterialTheme.typography.labelLarge,
                    color = colorScheme.onSurfaceVariant
                )
            }

            item {
                PiecerCard(variant = PiecerCardVariant.Light) {
                    Text(
                        text = stringResource(id = R.string.about_licenses_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun AboutLinkRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val spacing = PiecerTheme.spacing
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        color = colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.sm, vertical = spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                contentDescription = null,
                tint = colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun openExternalUrl(context: Context, url: String): Boolean {
    return try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: Exception) {
        false
    }
}
