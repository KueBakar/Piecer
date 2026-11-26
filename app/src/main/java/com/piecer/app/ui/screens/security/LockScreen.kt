package com.piecer.app.ui.screens.security

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.piecer.app.R
import com.piecer.app.ui.theme.PiecerTheme
import kotlinx.coroutines.delay

@Composable
fun LockScreen(
    biometricEnabled: Boolean,
    lockoutUntilMillis: Long,
    errorMessage: String?,
    onSubmitPin: (String) -> Unit,
    onBiometricClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = PiecerTheme.spacing
    val colors = PiecerTheme.colors
    val colorScheme = MaterialTheme.colorScheme

    var pin by remember { mutableStateOf("") }
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val remainingLockoutSeconds = ((lockoutUntilMillis - nowMillis + 999L) / 1000L)
        .coerceAtLeast(0L)
    val isLockedOut = remainingLockoutSeconds > 0L

    LaunchedEffect(lockoutUntilMillis) {
        while (System.currentTimeMillis() < lockoutUntilMillis) {
            nowMillis = System.currentTimeMillis()
            delay(500L)
        }
        nowMillis = System.currentTimeMillis()
    }

    LaunchedEffect(biometricEnabled, isLockedOut) {
        if (biometricEnabled && !isLockedOut) {
            onBiometricClick()
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = spacing.screenPadding, vertical = spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                Spacer(modifier = Modifier.height(spacing.lg))
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(spacing.iconLg)
                    )
                }
                Text(
                    text = stringResource(id = R.string.lock_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = colorScheme.onBackground
                )
                Text(
                    text = stringResource(id = R.string.lock_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.mutedText,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(spacing.md))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(6) { index ->
                        val filled = index < pin.length
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(
                                    if (filled) colorScheme.primary
                                    else colorScheme.surfaceVariant
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(spacing.xs))

                when {
                    isLockedOut -> {
                        Text(
                            text = stringResource(
                                id = R.string.lock_lockout_message,
                                remainingLockoutSeconds.toInt()
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.coral,
                            textAlign = TextAlign.Center
                        )
                    }
                    errorMessage != null -> {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.coral,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9")
                )
                rows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        row.forEach { digit ->
                            PinKeyButton(
                                label = digit,
                                enabled = !isLockedOut,
                                onClick = {
                                    if (pin.length < 6) pin += digit
                                }
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (biometricEnabled) {
                        PinIconButton(
                            icon = Icons.Rounded.Fingerprint,
                            contentDescription = stringResource(id = R.string.lock_biometric_button),
                            enabled = !isLockedOut,
                            onClick = onBiometricClick
                        )
                    } else {
                        PinIconButton(
                            icon = Icons.AutoMirrored.Rounded.Backspace,
                            contentDescription = stringResource(id = R.string.lock_backspace),
                            enabled = !isLockedOut && pin.isNotEmpty(),
                            onClick = { if (pin.isNotEmpty()) pin = pin.dropLast(1) }
                        )
                    }

                    PinKeyButton(
                        label = "0",
                        enabled = !isLockedOut,
                        onClick = {
                            if (pin.length < 6) pin += "0"
                        }
                    )

                    if (biometricEnabled && pin.isEmpty()) {
                        PinIconButton(
                            icon = Icons.AutoMirrored.Rounded.Backspace,
                            contentDescription = stringResource(id = R.string.lock_backspace),
                            enabled = false,
                            onClick = {}
                        )
                    } else {
                        PinIconButton(
                            icon = Icons.Rounded.Check,
                            contentDescription = stringResource(id = R.string.lock_submit),
                            enabled = !isLockedOut && pin.length >= 4,
                            onClick = {
                                val submitted = pin
                                pin = ""
                                onSubmitPin(submitted)
                            }
                        )
                    }
                }

                if (biometricEnabled && pin.isNotEmpty()) {
                    Text(
                        text = stringResource(id = R.string.lock_clear_pin),
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.mutedText,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(enabled = !isLockedOut) { pin = "" }
                            .padding(horizontal = spacing.md, vertical = spacing.xs)
                    )
                }
            }
        }
    }
}

@Composable
private fun PinKeyButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(colorScheme.surface)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.headlineMedium,
            color = if (enabled) colorScheme.onSurface else colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PinIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(colorScheme.surfaceVariant.copy(alpha = if (enabled) 1f else 0.4f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) colorScheme.onSurface else colorScheme.onSurfaceVariant
        )
    }
}
