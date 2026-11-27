package com.piecer.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.piecer.app.R
import com.piecer.app.ui.theme.PiecerTheme
import com.piecer.app.util.update.UpdatePhase
import com.piecer.app.util.update.UpdateUiState
import java.util.Locale

@Composable
fun UpdateConsentDialog(
    onAllow: () -> Unit,
    onDecline: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDecline,
        title = {
            Text(
                text = stringResource(id = R.string.update_consent_title),
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Text(
                text = stringResource(id = R.string.update_consent_message),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onAllow) {
                Text(text = stringResource(id = R.string.update_consent_allow))
            }
        },
        dismissButton = {
            TextButton(onClick = onDecline) {
                Text(text = stringResource(id = R.string.update_consent_decline))
            }
        }
    )
}

@Composable
fun UpdateAvailableDialog(
    state: UpdateUiState,
    reduceMotion: Boolean,
    onStartDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onDismiss: () -> Unit,
    onOpenUnknownSources: () -> Unit,
    onRetryInstall: () -> Unit
) {
    val spacing = PiecerTheme.spacing
    val colorScheme = MaterialTheme.colorScheme
    val metadata = state.availableUpdate ?: (state.phase as? UpdatePhase.Error)?.metadata ?: return

    val isBusy = state.phase is UpdatePhase.Downloading ||
        state.phase is UpdatePhase.Verifying ||
        state.phase is UpdatePhase.Installing

    AlertDialog(
        onDismissRequest = {
            if (!isBusy) onDismiss()
        },
        title = {
            Text(
                text = stringResource(id = R.string.update_dialog_title, metadata.versionName),
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                Text(
                    text = stringResource(
                        id = R.string.update_dialog_size,
                        formatBytes(metadata.sizeBytes)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )

                Text(
                    text = stringResource(id = R.string.update_dialog_notes_label),
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = metadata.releaseNotes.ifBlank {
                        stringResource(id = R.string.update_dialog_notes_empty)
                    },
                    style = MaterialTheme.typography.bodyMedium
                )

                when (val phase = state.phase) {
                    is UpdatePhase.Downloading -> {
                        val total = phase.totalBytes.coerceAtLeast(1L)
                        val fraction = (phase.bytesRead.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                        val percent = (fraction * 100f).toInt().coerceIn(0, 100)
                        LinearProgressIndicator(
                            progress = { fraction },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = stringResource(
                                id = R.string.update_dialog_downloading,
                                formatBytes(phase.bytesRead),
                                formatBytes(total),
                                percent
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }

                    is UpdatePhase.Verifying -> {
                        if (reduceMotion) {
                            LinearProgressIndicator(
                                progress = { 0.85f },
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                        Text(
                            text = stringResource(id = R.string.update_dialog_verifying),
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }

                    is UpdatePhase.Installing -> {
                        if (reduceMotion) {
                            LinearProgressIndicator(
                                progress = { 1f },
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                        Text(
                            text = stringResource(id = R.string.update_dialog_installing),
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }

                    is UpdatePhase.NeedsUnknownSourcesPermission -> {
                        Text(
                            text = stringResource(id = R.string.update_dialog_needs_permission),
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.error
                        )
                    }

                    is UpdatePhase.Error -> {
                        Text(
                            text = phase.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.error
                        )
                    }

                    else -> Unit
                }
            }
        },
        confirmButton = {
            when (state.phase) {
                is UpdatePhase.Downloading -> {
                    TextButton(onClick = onCancelDownload) {
                        Text(text = stringResource(id = R.string.update_dialog_action_cancel))
                    }
                }

                is UpdatePhase.NeedsUnknownSourcesPermission -> {
                    TextButton(onClick = onOpenUnknownSources) {
                        Text(text = stringResource(id = R.string.update_dialog_action_open_settings))
                    }
                }

                is UpdatePhase.Error -> {
                    TextButton(onClick = onStartDownload) {
                        Text(text = stringResource(id = R.string.update_dialog_action_retry))
                    }
                }

                is UpdatePhase.Verifying, is UpdatePhase.Installing -> Unit

                else -> {
                    TextButton(onClick = onStartDownload) {
                        Text(text = stringResource(id = R.string.update_dialog_action_update))
                    }
                }
            }
        },
        dismissButton = {
            when (state.phase) {
                is UpdatePhase.NeedsUnknownSourcesPermission -> {
                    TextButton(onClick = onRetryInstall) {
                        Text(text = stringResource(id = R.string.update_dialog_action_retry))
                    }
                }

                is UpdatePhase.Downloading, is UpdatePhase.Verifying, is UpdatePhase.Installing -> Unit

                else -> {
                    TextButton(onClick = onDismiss) {
                        Text(text = stringResource(id = R.string.update_dialog_action_later))
                    }
                }
            }
        }
    )
}

fun formatBytes(bytes: Long): String {
    if (bytes < 1024L) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024.0) return String.format(Locale("in", "ID"), "%.1f KB", kb)
    val mb = kb / 1024.0
    return String.format(Locale("in", "ID"), "%.1f MB", mb)
}
