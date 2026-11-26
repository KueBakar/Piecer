package com.piecer.app.ui.screens.settings

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import com.piecer.app.R
import com.piecer.app.ui.theme.PiecerTheme
import com.piecer.app.util.BreadcrumbLog
import com.piecer.app.util.CrashReporter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrashReportScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val spacing = PiecerTheme.spacing
    val colors = PiecerTheme.colors
    val colorScheme = MaterialTheme.colorScheme

    var crashReport by remember { mutableStateOf<String?>(null) }
    var breadcrumbsText by remember { mutableStateOf("") }

    fun reload() {
        crashReport = CrashReporter.readCrashReport(context)
        breadcrumbsText = BreadcrumbLog.formatted()
    }

    LaunchedEffect(Unit) {
        CrashReporter.markCrashReportSeen(context)
        reload()
    }

    val emptyMessage = stringResource(id = R.string.crash_report_empty)
    val breadcrumbsHeader = stringResource(id = R.string.crash_report_breadcrumbs_header)
    val copiedMessage = stringResource(id = R.string.crash_report_copied)
    val clearedMessage = stringResource(id = R.string.crash_report_cleared)
    val shareSubject = stringResource(id = R.string.crash_report_share_subject)

    val fullText = remember(crashReport, breadcrumbsText, emptyMessage, breadcrumbsHeader) {
        buildString {
            if (crashReport != null) {
                append(crashReport)
            } else {
                appendLine(emptyMessage)
                appendLine()
                appendLine("--- $breadcrumbsHeader ---")
                append(breadcrumbsText.ifBlank { "-" })
            }
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.crash_report_title),
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
                    containerColor = colorScheme.background,
                    titleContentColor = colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = spacing.screenPadding, vertical = spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(fullText))
                        scope.launch {
                            snackbarHostState.showSnackbar(copiedMessage)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Rounded.ContentCopy, contentDescription = null)
                    Spacer(modifier = Modifier.padding(horizontal = spacing.xs))
                    Text(text = stringResource(id = R.string.crash_report_copy))
                }

                OutlinedButton(
                    onClick = {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, shareSubject)
                            putExtra(Intent.EXTRA_TEXT, fullText)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, shareSubject))
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Rounded.Share, contentDescription = null)
                    Spacer(modifier = Modifier.padding(horizontal = spacing.xs))
                    Text(text = stringResource(id = R.string.crash_report_share))
                }
            }

            TextButton(
                onClick = {
                    CrashReporter.clearCrashReport(context)
                    reload()
                    scope.launch {
                        snackbarHostState.showSnackbar(clearedMessage)
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = null,
                    tint = colors.coral
                )
                Spacer(modifier = Modifier.padding(horizontal = spacing.xs))
                Text(
                    text = stringResource(id = R.string.crash_report_clear),
                    color = colors.coral
                )
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = MaterialTheme.shapes.large,
                color = colorScheme.surface
            ) {
                SelectionContainer {
                    Text(
                        text = fullText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                        color = colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(spacing.md)
                    )
                }
            }

            Spacer(modifier = Modifier.height(spacing.xs))
        }
    }
}
