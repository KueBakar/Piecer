package com.piecer.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.piecer.app.R
import com.piecer.app.ui.components.EmptyState
import com.piecer.app.ui.navigation.PiecerTab
import com.piecer.app.ui.theme.PiecerTheme

@Composable
fun PlaceholderTabScreen(
    tab: PiecerTab,
    modifier: Modifier = Modifier
) {
    val spacing = PiecerTheme.spacing

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(spacing.screenPadding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        EmptyState(
            icon = tab.selectedIcon,
            title = stringResource(id = tab.labelRes),
            description = stringResource(id = R.string.placeholder_subtitle)
        )
    }
}
