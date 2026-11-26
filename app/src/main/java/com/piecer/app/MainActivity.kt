package com.piecer.app

import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.piecer.app.data.local.entity.TransactionType
import com.piecer.app.data.preferences.ThemeMode
import com.piecer.app.ui.components.PiecerChip
import com.piecer.app.ui.navigation.PiecerBottomBar
import com.piecer.app.ui.navigation.PiecerRoutes
import com.piecer.app.ui.navigation.PiecerTab
import com.piecer.app.ui.screens.ComponentShowcaseScreen
import com.piecer.app.ui.screens.addtransaction.AddTransactionScreen
import com.piecer.app.ui.screens.addtransaction.AddTransactionViewModel
import com.piecer.app.ui.screens.budgets.BudgetsScreen
import com.piecer.app.ui.screens.budgets.BudgetsViewModel
import com.piecer.app.ui.screens.goals.GoalsScreen
import com.piecer.app.ui.screens.goals.GoalsViewModel
import com.piecer.app.ui.screens.home.HomeScreen
import com.piecer.app.ui.screens.home.HomeViewModel
import com.piecer.app.ui.screens.more.MoreScreen
import com.piecer.app.ui.screens.onboarding.OnboardingScreen
import com.piecer.app.ui.screens.security.LockScreen
import com.piecer.app.ui.screens.settings.SettingsScreen
import com.piecer.app.ui.screens.settings.SettingsViewModel
import com.piecer.app.ui.screens.statistics.StatisticsScreen
import com.piecer.app.ui.screens.statistics.StatisticsViewModel
import com.piecer.app.ui.screens.transactions.TransactionsScreen
import com.piecer.app.ui.screens.transactions.TransactionsViewModel
import com.piecer.app.ui.screens.wallets.WalletsScreen
import com.piecer.app.ui.screens.wallets.WalletsViewModel
import com.piecer.app.ui.theme.PiecerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    private val mainViewModel: MainViewModel by viewModels()
    private val homeViewModel: HomeViewModel by viewModels()
    private val transactionsViewModel: TransactionsViewModel by viewModels()
    private val addTransactionViewModel: AddTransactionViewModel by viewModels()
    private val statisticsViewModel: StatisticsViewModel by viewModels()
    private val walletsViewModel: WalletsViewModel by viewModels()
    private val budgetsViewModel: BudgetsViewModel by viewModels()
    private val goalsViewModel: GoalsViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by mainViewModel.settings.collectAsStateWithLifecycle()
            val isLocked by mainViewModel.isLocked.collectAsStateWithLifecycle()
            val pinError by mainViewModel.pinError.collectAsStateWithLifecycle()
            val wrongPinMessage = stringResource(id = R.string.lock_wrong_pin)

            val darkTheme = when (settings.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            LaunchedEffect(settings.blockScreenshots) {
                if (settings.blockScreenshots) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            PiecerTheme(
                darkTheme = darkTheme,
                reduceMotion = settings.reduceMotion
            ) {
                when {
                    !settings.onboardingCompleted -> {
                        OnboardingScreen(onFinish = mainViewModel::completeOnboarding)
                    }
                    isLocked && settings.isPinEnabled -> {
                        LockScreen(
                            biometricEnabled = settings.biometricEnabled,
                            lockoutUntilMillis = settings.pinLockoutUntilMillis,
                            errorMessage = pinError,
                            onSubmitPin = { pin -> mainViewModel.submitPin(pin, wrongPinMessage) },
                            onBiometricClick = { launchBiometricPrompt() }
                        )
                    }
                    else -> {
                        PiecerApp(
                            homeViewModel = homeViewModel,
                            transactionsViewModel = transactionsViewModel,
                            addTransactionViewModel = addTransactionViewModel,
                            statisticsViewModel = statisticsViewModel,
                            walletsViewModel = walletsViewModel,
                            budgetsViewModel = budgetsViewModel,
                            goalsViewModel = goalsViewModel,
                            settingsViewModel = settingsViewModel
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        mainViewModel.onAppStart()
    }

    override fun onStop() {
        super.onStop()
        mainViewModel.onAppStop(isChangingConfigurations)
    }

    private fun launchBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    mainViewModel.onBiometricSuccess()
                }
            }
        )
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.lock_biometric_prompt_title))
            .setSubtitle(getString(R.string.lock_biometric_prompt_subtitle))
            .setNegativeButtonText(getString(R.string.lock_biometric_use_pin))
            .build()
        prompt.authenticate(promptInfo)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PiecerApp(
    homeViewModel: HomeViewModel,
    transactionsViewModel: TransactionsViewModel,
    addTransactionViewModel: AddTransactionViewModel,
    statisticsViewModel: StatisticsViewModel,
    walletsViewModel: WalletsViewModel,
    budgetsViewModel: BudgetsViewModel,
    goalsViewModel: GoalsViewModel,
    settingsViewModel: SettingsViewModel
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isFullScreen = currentRoute == PiecerRoutes.SHOWCASE ||
        currentRoute == PiecerRoutes.ADD_TRANSACTION ||
        currentRoute == PiecerRoutes.WALLETS ||
        currentRoute == PiecerRoutes.BUDGETS ||
        currentRoute == PiecerRoutes.GOALS ||
        currentRoute == PiecerRoutes.SETTINGS

    val context = LocalContext.current
    val isDebuggable = remember(context) {
        (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val savedMessage = stringResource(id = R.string.tx_saved_snackbar)
    val deletedMessage = stringResource(id = R.string.tx_deleted_snackbar)
    val walletSavedMessage = stringResource(id = R.string.wallets_saved_snackbar)
    val walletDeletedMessage = stringResource(id = R.string.wallets_deleted_snackbar)
    val budgetSavedMessage = stringResource(id = R.string.budgets_saved_snackbar)
    val budgetDeletedMessage = stringResource(id = R.string.budgets_deleted_snackbar)
    val goalSavedMessage = stringResource(id = R.string.goals_saved_snackbar)
    val goalDeletedMessage = stringResource(id = R.string.goals_deleted_snackbar)
    val goalTopUpMessage = stringResource(id = R.string.goals_topup_snackbar)

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
                    onEditTransaction = openEditTransaction,
                    onOpenBudgets = { navController.navigate(PiecerRoutes.BUDGETS) }
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
                StatisticsScreen(
                    viewModel = statisticsViewModel,
                    onCategoryClick = { categoryId, categoryName ->
                        transactionsViewModel.filterByCategory(categoryId, categoryName)
                        navigateToTab(PiecerTab.Transactions)
                    }
                )
            }
            composable(PiecerTab.More.route) {
                MoreScreen(
                    onOpenWallets = { navController.navigate(PiecerRoutes.WALLETS) },
                    onOpenBudgets = { navController.navigate(PiecerRoutes.BUDGETS) },
                    onOpenGoals = { navController.navigate(PiecerRoutes.GOALS) },
                    onOpenSettings = { navController.navigate(PiecerRoutes.SETTINGS) }
                )
            }
            composable(PiecerRoutes.WALLETS) {
                WalletsScreen(
                    viewModel = walletsViewModel,
                    onBack = { navController.popBackStack() },
                    onTransferClick = {
                        addTransactionViewModel.startNew(TransactionType.TRANSFER)
                        navController.navigate(PiecerRoutes.ADD_TRANSACTION)
                    },
                    onWalletSaved = {
                        scope.launch { snackbarHostState.showSnackbar(walletSavedMessage) }
                    },
                    onWalletDeleted = {
                        scope.launch { snackbarHostState.showSnackbar(walletDeletedMessage) }
                    }
                )
            }
            composable(PiecerRoutes.BUDGETS) {
                BudgetsScreen(
                    viewModel = budgetsViewModel,
                    onBack = { navController.popBackStack() },
                    onBudgetSaved = {
                        scope.launch { snackbarHostState.showSnackbar(budgetSavedMessage) }
                    },
                    onBudgetDeleted = {
                        scope.launch { snackbarHostState.showSnackbar(budgetDeletedMessage) }
                    }
                )
            }
            composable(PiecerRoutes.GOALS) {
                GoalsScreen(
                    viewModel = goalsViewModel,
                    onBack = { navController.popBackStack() },
                    onGoalSaved = {
                        scope.launch { snackbarHostState.showSnackbar(goalSavedMessage) }
                    },
                    onGoalDeleted = {
                        scope.launch { snackbarHostState.showSnackbar(goalDeletedMessage) }
                    },
                    onTopUpSaved = {
                        scope.launch { snackbarHostState.showSnackbar(goalTopUpMessage) }
                    }
                )
            }
            composable(PiecerRoutes.SETTINGS) {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onBack = { navController.popBackStack() }
                )
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
