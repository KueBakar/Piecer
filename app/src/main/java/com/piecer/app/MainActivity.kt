package com.piecer.app

import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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
import com.piecer.app.ui.screens.addtransaction.AddTransactionScreen
import com.piecer.app.ui.screens.addtransaction.AddTransactionViewModel
import com.piecer.app.ui.screens.home.HomeScreen
import com.piecer.app.ui.screens.home.HomeViewModel
import com.piecer.app.ui.screens.transactions.TransactionsScreen
import com.piecer.app.ui.screens.transactions.TransactionsViewModel
import com.piecer.app.ui.theme.PiecerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val homeViewModel: HomeViewModel by viewModels()
    private val transactionsViewModel: TransactionsViewModel by viewModels()
    private val addTransactionViewModel: AddTransactionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PiecerTheme {
                PiecerApp(
                    homeViewModel = homeViewModel,
                    transactionsViewModel = transactionsViewModel,
                    addTransactionViewModel = addTransactionViewModel
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PiecerApp(
    homeViewModel: HomeViewModel,
    transactionsViewModel: TransactionsViewModel,
    addTransactionViewModel: AddTransactionViewModel
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isFullScreen = currentRoute == PiecerRoutes.SHOWCASE ||
        currentRoute == PiecerRoutes.ADD_TRANSACTION

    val context = LocalContext.current
    val isDebuggable = remember(context) {
        (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val savedMessage = stringResource(id = R.string.tx_saved_snackbar)
    val deletedMessage = stringResource(id = R.string.tx_deleted_snackbar)

    val navigateToTab: (PiecerTab) -> Unit = { tab ->
        navController.navigate(tab.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val openAddTransaction: () -> Unit = {
        addTransactionViewModel.startNew()
        navController.navigate(PiecerRoutes.ADD_TRANSACTION)
    }

    val openEditTransaction: (Long) -> Unit = { id ->
        addTransactionViewModel.startEdit(id)
        navController.navigate(PiecerRoutes.ADD_TRANSACTION)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            if (!isFullScreen) {
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
            if (!isFullScreen) {
                PiecerBottomBar(
                    currentRoute = currentRoute,
                    onTabSelected = navigateToTab,
                    onAddClick = openAddTransaction
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = PiecerTab.Home.route,
            modifier = if (isFullScreen) Modifier else Modifier.padding(innerPadding)
        ) {
            composable(PiecerTab.Home.route) {
                HomeScreen(
                    viewModel = homeViewModel,
                    onSeeAllTransactions = { navigateToTab(PiecerTab.Transactions) },
                    onEditTransaction = openEditTransaction
                )
            }
            composable(PiecerTab.Transactions.route) {
                TransactionsScreen(
                    viewModel = transactionsViewModel,
                    onEditTransaction = openEditTransaction,
                    onTransactionDeleted = {
                        scope.launch {
                            snackbarHostState.showSnackbar(deletedMessage)
                        }
                    }
                )
            }
            composable(PiecerTab.Statistics.route) {
                PlaceholderTabScreen(tab = PiecerTab.Statistics)
            }
            composable(PiecerTab.More.route) {
                PlaceholderTabScreen(tab = PiecerTab.More)
            }
            composable(PiecerRoutes.ADD_TRANSACTION) {
                AddTransactionScreen(
                    viewModel = addTransactionViewModel,
                    onClose = { navController.popBackStack() },
                    onSaved = {
                        navController.popBackStack()
                        scope.launch {
                            snackbarHostState.showSnackbar(savedMessage)
                        }
                    }
                )
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
