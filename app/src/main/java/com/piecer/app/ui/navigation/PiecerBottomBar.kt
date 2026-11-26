package com.piecer.app.ui.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.piecer.app.R
import com.piecer.app.ui.theme.PiecerTheme

@Composable
fun PiecerBottomBar(
    currentRoute: String?,
    onTabSelected: (PiecerTab) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = PiecerTheme.spacing
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = colorScheme.surface,
        border = BorderStroke(spacing.borderWidth, colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(spacing.bottomBarHeight)
                .padding(horizontal = spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            BottomNavItem(
                tab = PiecerTab.Home,
                selected = currentRoute == PiecerTab.Home.route,
                onClick = { onTabSelected(PiecerTab.Home) },
                modifier = Modifier.weight(1f)
            )
            BottomNavItem(
                tab = PiecerTab.Transactions,
                selected = currentRoute == PiecerTab.Transactions.route,
                onClick = { onTabSelected(PiecerTab.Transactions) },
                modifier = Modifier.weight(1f)
            )

            // Tombol (+) di tengah
            Surface(
                onClick = onAddClick,
                modifier = Modifier.size(spacing.fabSize),
                shape = CircleShape,
                color = colorScheme.primary,
                contentColor = colorScheme.onPrimary
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = stringResource(id = R.string.nav_add),
                        modifier = Modifier.size(spacing.iconLg)
                    )
                }
            }

            BottomNavItem(
                tab = PiecerTab.Statistics,
                selected = currentRoute == PiecerTab.Statistics.route,
                onClick = { onTabSelected(PiecerTab.Statistics) },
                modifier = Modifier.weight(1f)
            )
            BottomNavItem(
                tab = PiecerTab.More,
                selected = currentRoute == PiecerTab.More.route,
                onClick = { onTabSelected(PiecerTab.More) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    tab: PiecerTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = PiecerTheme.spacing
    val colorScheme = MaterialTheme.colorScheme
    val contentColor = if (selected) colorScheme.onSurface else colorScheme.onSurfaceVariant

    Column(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = spacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
            contentDescription = stringResource(id = tab.labelRes),
            tint = contentColor,
            modifier = Modifier.size(spacing.iconLg)
        )
        Spacer(modifier = Modifier.height(spacing.xs))
        Text(
            text = stringResource(id = tab.labelRes),
            style = MaterialTheme.typography.labelSmall,
            color = contentColor
        )
    }
}
