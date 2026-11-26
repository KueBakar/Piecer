package com.piecer.app

import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.piecer.app.ui.components.PiecerChip
import com.piecer.app.ui.navigation.PiecerBottomBar
import com.piecer.app.ui.navigation.PiecerRoutes
import com.piecer.app.ui.navigation.PiecerTab
import com.piecer.app.ui.screens.ComponentShowcaseScreen
import com.piecer.app.ui.screens.PlaceholderTabScreen
import com.piecer.app.ui.theme.PiecerTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PiecerTheme {
                PiecerApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PiecerApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isShowcase = currentRoute == PiecerRoutes.SHOWCASE

    val context = LocalContext.current
    val isDebuggable = remember(context) {
        (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val addPlaceholderMessage = stringResource(id = R.string.placeholder_add_message)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            if (!isShowcase) {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(id = R.string.app_name),
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    actions = {
                        if (isDebuggable) {
                            PiecerChip(
                                label = stringResource(id = R.string.showcase_button),
                                selected = false,
                                onClick = { navController.navigate(PiecerRoutes.SHOWCASE) },
                                modifier = Modifier.padding(end = PiecerTheme.spacing.sm)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            }
        },
        bottomBar = {
            if (!isShowcase) {
                PiecerBottomBar(
                    currentRoute = currentRoute,
                    onTabSelected = { tab ->
                        navController.navigate(tab.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onAddClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar(addPlaceholderMessage)
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = PiecerTab.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            PiecerTab.entries.forEach { tab ->
                composable(tab.route) {
                    PlaceholderTabScreen(tab = tab)
                }
            }
            if (isDebuggable) {
                composable(PiecerRoutes.SHOWCASE) {
                    ComponentShowcaseScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
