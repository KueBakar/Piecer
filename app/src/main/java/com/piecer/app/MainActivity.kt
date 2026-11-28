package com.piecer.app

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
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
import com.piecer.app.ui.components.SplashAnimation
import com.piecer.app.ui.navigation.PiecerBottomBar
import com.piecer.app.ui.navigation.PiecerRoutes
import com.piecer.app.ui.navigation.PiecerTab
import com.piecer.app.ui.screens.addtransaction.AddTransactionScreen
import com.piecer.app.ui.screens.addtransaction.AddTransactionViewModel
import com.piecer.app.ui.screens.budgets.BudgetsScreen
import com.piecer.app.ui.screens.budgets.BudgetsViewModel
import com.piecer.app.ui.screens.categories.CategoriesScreen
import com.piecer.app.ui.screens.categories.CategoriesViewModel
import com.piecer.app.ui.screens.debts.DebtsScreen
import com.piecer.app.ui.screens.debts.DebtsViewModel
import com.piecer.app.ui.screens.goals.GoalsScreen
import com.piecer.app.ui.screens.goals.GoalsViewModel
import com.piecer.app.ui.screens.home.HomeScreen
import com.piecer.app.ui.screens.home.HomeViewModel
import com.piecer.app.ui.screens.more.MoreScreen
import com.piecer.app.ui.screens.networth.NetWorthScreen
import com.piecer.app.ui.screens.networth.NetWorthViewModel
import com.piecer.app.ui.screens.onboarding.OnboardingScreen
import com.piecer.app.ui.screens.pieces.PiecesScreen
import com.piecer.app.ui.screens.pieces.PiecesViewModel
import com.piecer.app.ui.screens.recurring.RecurringScreen
import com.piecer.app.ui.screens.recurring.RecurringViewModel
import com.piecer.app.ui.screens.security.LockScreen
import com.piecer.app.ui.screens.settings.AboutScreen
import com.piecer.app.ui.screens.settings.CrashReportScreen
import com.piecer.app.ui.screens.settings.ReminderStatusScreen
import com.piecer.app.ui.screens.settings.ReminderStatusViewModel
import com.piecer.app.ui.screens.settings.SettingsScreen
import com.piecer.app.ui.screens.settings.SettingsViewModel
import com.piecer.app.ui.components.UpdateAvailableDialog
import com.piecer.app.ui.components.UpdateConsentDialog
import com.piecer.app.ui.screens.splitbill.SplitBillScreen
import com.piecer.app.ui.screens.splitbill.SplitBillViewModel
import com.piecer.app.ui.screens.statistics.StatisticsScreen
import com.piecer.app.ui.screens.statistics.StatisticsViewModel
import com.piecer.app.ui.screens.transactions.TransactionsScreen
import com.piecer.app.ui.screens.transactions.TransactionsViewModel
import com.piecer.app.ui.screens.wallets.WalletsScreen
import com.piecer.app.ui.screens.wallets.WalletsViewModel
import com.piecer.app.ui.screens.wrapped.WrappedScreen
import com.piecer.app.ui.screens.wrapped.WrappedViewModel
import com.piecer.app.widget.PiecerWidgetUpdater
import com.piecer.app.ui.theme.LocalReduceMotion
import com.piecer.app.ui.theme.PiecerTheme
import com.piecer.app.util.BreadcrumbLog
import com.piecer.app.util.CrashReporter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    private val piecesViewModel: PiecesViewModel by viewModels()
    private val wrappedViewModel: WrappedViewModel by viewModels()
    private val recurringViewModel: RecurringViewModel by viewModels()
    private val categoriesViewModel: CategoriesViewModel by viewModels()
    private val debtsViewModel: DebtsViewModel by viewModels()
    private val splitBillViewModel: SplitBillViewModel by viewModels()
    private val netWorthViewModel: NetWorthViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val reminderStatusViewModel: ReminderStatusViewModel by viewModels()
    private val pendingStartRouteState = mutableStateOf<String?>(null)
    private val pendingQuickTypeState = mutableStateOf<TransactionType?>(null)
    private val pendingFocusQuickEntryState = mutableStateOf(false)
    private val pendingVoiceEntryState = mutableStateOf(false)

    private fun consumeLaunchIntent(intent: android.content.Intent?) {
        if (intent == null) return
        val startRoute = intent.getStringExtra(PiecerWidgetUpdater.EXTRA_START_ROUTE)
        if (!startRoute.isNullOrBlank()) {
            pendingStartRouteState.value = startRoute
        }
        val quickTypeRaw = intent.getStringExtra(PiecerWidgetUpdater.EXTRA_QUICK_TYPE)
        if (!quickTypeRaw.isNullOrBlank()) {
            pendingQuickTypeState.value = runCatching { TransactionType.valueOf(quickTypeRaw) }.getOrNull()
        }
        if (intent.getBooleanExtra(PiecerWidgetUpdater.EXTRA_FOCUS_QUICK_ENTRY, false)) {
            pendingFocusQuickEntryState.value = true
        }
        if (intent.getBooleanExtra(PiecerWidgetUpdater.EXTRA_LAUNCH_VOICE_ENTRY, false)) {
            pendingVoiceEntryState.value = true
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeLaunchIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        consumeLaunchIntent(intent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            splashScreen.setOnExitAnimationListener { splashView -> splashView.remove() }
        }
        enableEdgeToEdge()
        setContent {
            val isAppReady by mainViewModel.isAppReady.collectAsStateWithLifecycle()
            val showSplash by mainViewModel.showSplash.collectAsStateWithLifecycle()
            val settings by mainViewModel.settings.collectAsStateWithLifecycle()
            val isLocked by mainViewModel.isLocked.collectAsStateWithLifecycle()
            val pinError by mainViewModel.pinError.collectAsStateWithLifecycle()
            val updateState by mainViewModel.updateState.collectAsStateWithLifecycle()
            val wrongPinMessage = stringResource(id = R.string.lock_wrong_pin)

            LaunchedEffect(updateState.pendingConfirmIntent) {
                val confirmIntent = updateState.pendingConfirmIntent ?: return@LaunchedEffect
                runCatching { startActivity(confirmIntent) }
                mainViewModel.consumePendingConfirmIntent()
            }

            val darkTheme = when (settings.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            val useDarkSystemBars = showSplash || darkTheme
            LaunchedEffect(useDarkSystemBars) {
                enableEdgeToEdge(
                    statusBarStyle = if (useDarkSystemBars) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        )
                    },
                    navigationBarStyle = if (useDarkSystemBars) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        )
                    }
                )
            }

            var firstFrameDrawn by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                withFrameNanos { }
                firstFrameDrawn = true
            }

            LaunchedEffect(settings.blockScreenshots) {
                val hasSecure = (window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE) != 0
                if (settings.blockScreenshots && !hasSecure) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } else if (!settings.blockScreenshots && hasSecure) {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            var canUseBiometric by remember { mutableStateOf(false) }
            var showUnseenCrashDialog by remember { mutableStateOf(false) }
            var openCrashReportRequest by remember { mutableStateOf(false) }

            LaunchedEffect(isAppReady) {
                if (isAppReady) {
                    canUseBiometric = withContext(Dispatchers.Default) { canAuthenticateBiometric() }
                    if (BuildConfig.CRASH_DIAGNOSTICS_ENABLED) {
                        showUnseenCrashDialog = withContext(Dispatchers.IO) {
                            CrashReporter.hasUnseenCrashReport(this@MainActivity)
                        }
                    }
                }
            }

            PiecerTheme(
                darkTheme = darkTheme,
                reduceMotion = settings.reduceMotion
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (isAppReady && firstFrameDrawn) {
                        if (!settings.onboardingCompleted) {
                            OnboardingScreen(onFinish = mainViewModel::completeOnboarding)
                        } else {
                            PiecerApp(
                                homeViewModel = homeViewModel,
                                transactionsViewModel = transactionsViewModel,
                                addTransactionViewModel = addTransactionViewModel,
                                statisticsViewModel = statisticsViewModel,
                                walletsViewModel = walletsViewModel,
                                budgetsViewModel = budgetsViewModel,
                                goalsViewModel = goalsViewModel,
                                piecesViewModel = piecesViewModel,
                                recurringViewModel = recurringViewModel,
                                categoriesViewModel = categoriesViewModel,
                                debtsViewModel = debtsViewModel,
                                splitBillViewModel = splitBillViewModel,
                                netWorthViewModel = netWorthViewModel,
                                settingsViewModel = settingsViewModel,
                                reminderStatusViewModel = reminderStatusViewModel,
                                wrappedViewModel = wrappedViewModel,
                                pendingStartRoute = if (isLocked && settings.isPinEnabled) null else pendingStartRouteState.value,
                                pendingQuickType = pendingQuickTypeState.value,
                                pendingFocusQuickEntry = pendingFocusQuickEntryState.value,
                                pendingVoiceEntry = pendingVoiceEntryState.value,
                                onPendingLaunchHandled = {
                                    pendingStartRouteState.value = null
                                    pendingQuickTypeState.value = null
                                },
                                onQuickEntryFocusHandled = {
                                    pendingFocusQuickEntryState.value = false
                                },
                                onVoiceEntryHandled = {
                                    pendingVoiceEntryState.value = false
                                },
                                onPinConfigured = mainViewModel::markSessionUnlocked,
                                openCrashReportRequest = openCrashReportRequest,
                                onCrashReportRequestHandled = { openCrashReportRequest = false },
                                onReplaySplash = mainViewModel::replaySplash,
                                availableUpdateVersion = if (updateState.isFeatureEnabled) updateState.availableUpdate?.versionName else null,
                                onOpenUpdateDialog = mainViewModel::openUpdateDialog
                            )

                            if (isLocked && settings.isPinEnabled) {
                                LockScreen(
                                    biometricEnabled = settings.biometricEnabled && canUseBiometric,
                                    lockoutUntilMillis = settings.pinLockoutUntilMillis,
                                    errorMessage = pinError,
                                    canAutoTriggerBiometric = !showSplash,
                                    onSubmitPin = { pin -> mainViewModel.submitPin(pin, wrongPinMessage) },
                                    onBiometricClick = { launchBiometricPrompt() }
                                )
                            }
                        }
                    }

                    if (showSplash) {
                        SplashAnimation(
                            reduceMotion = settings.reduceMotion,
                            isAppReady = isAppReady,
                            onFinished = mainViewModel::onSplashFinished
                        )
                    }

                    if (BuildConfig.CRASH_DIAGNOSTICS_ENABLED && showUnseenCrashDialog && !showSplash && (!isLocked || !settings.isPinEnabled)) {
                        AlertDialog(
                            onDismissRequest = {
                                CrashReporter.markCrashReportSeen(this@MainActivity)
                                showUnseenCrashDialog = false
                            },
                            title = { Text(text = stringResource(id = R.string.debug_crash_dialog_title)) },
                            text = { Text(text = stringResource(id = R.string.debug_crash_dialog_message)) },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        CrashReporter.markCrashReportSeen(this@MainActivity)
                                        showUnseenCrashDialog = false
                                        openCrashReportRequest = true
                                    }
                                ) {
                                    Text(text = stringResource(id = R.string.debug_crash_dialog_view))
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = {
                                        CrashReporter.markCrashReportSeen(this@MainActivity)
                                        showUnseenCrashDialog = false
                                    }
                                ) {
                                    Text(text = stringResource(id = R.string.debug_crash_dialog_dismiss))
                                }
                            }
                        )
                    }

                    if (BuildConfig.UPDATE_ENABLED &&
                        updateState.isFeatureEnabled &&
                        settings.onboardingCompleted &&
                        !settings.updateConsentShown &&
                        !showSplash &&
                        (!isLocked || !settings.isPinEnabled)
                    ) {
                        UpdateConsentDialog(
                            onAllow = { mainViewModel.answerUpdateConsent(true) },
                            onDecline = { mainViewModel.answerUpdateConsent(false) }
                        )
                    }

                    if (updateState.showUpdateDialog) {
                        UpdateAvailableDialog(
                            state = updateState,
                            reduceMotion = settings.reduceMotion,
                            onStartDownload = mainViewModel::startUpdateDownload,
                            onCancelDownload = mainViewModel::cancelUpdateDownload,
                            onDismiss = mainViewModel::dismissUpdateDialog,
                            onOpenUnknownSources = {
                                val intent = mainViewModel.createUnknownSourcesIntent()
                                if (intent != null) {
                                    runCatching { startActivity(intent) }
                                }
                            },
                            onRetryInstall = mainViewModel::retryInstallAfterPermission
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

    override fun finish() {
        BreadcrumbLog.record("ACTIVITY", "MainActivity.finish() called", syncWrite = true)
        super.finish()
    }

    private fun canAuthenticateBiometric(): Boolean {
        return try {
            val manager = BiometricManager.from(this)
            manager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK
            ) == BiometricManager.BIOMETRIC_SUCCESS
        } catch (t: Throwable) {
            BreadcrumbLog.record(
                "BIOMETRIC",
                "canAuthenticateBiometric threw ${t::class.java.simpleName}: ${t.message}",
                syncWrite = true
            )
            false
        }
    }

    private fun launchBiometricPrompt() {
        BreadcrumbLog.record("BIOMETRIC", "launchBiometricPrompt called", syncWrite = true)
        if (isFinishing || isDestroyed || supportFragmentManager.isStateSaved) {
            BreadcrumbLog.record(
                "BIOMETRIC",
                "Skipped prompt: finishing=$isFinishing destroyed=$isDestroyed stateSaved=${supportFragmentManager.isStateSaved}",
                syncWrite = true
            )
            mainViewModel.setBiometricPromptShowing(false)
            return
        }
        val fallbackMessage = getString(R.string.lock_biometric_unavailable_fallback)
        if (!canAuthenticateBiometric()) {
            mainViewModel.disableBiometricDueToFailure("canAuthenticate=false", fallbackMessage)
            return
        }
        try {
            mainViewModel.setBiometricPromptShowing(true)
            val executor = ContextCompat.getMainExecutor(this)
            val prompt = BiometricPrompt(
                this,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        BreadcrumbLog.record("BIOMETRIC", "onAuthenticationSucceeded", syncWrite = true)
                        mainViewModel.onBiometricSuccess()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        BreadcrumbLog.record(
                            "BIOMETRIC",
                            "onAuthenticationError code=$errorCode msg=$errString",
                            syncWrite = true
                        )
                        when (errorCode) {
                            BiometricPrompt.ERROR_USER_CANCELED,
                            BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                            BiometricPrompt.ERROR_CANCELED -> {
                                mainViewModel.setBiometricPromptShowing(false)
                            }
                            BiometricPrompt.ERROR_HW_UNAVAILABLE,
                            BiometricPrompt.ERROR_UNABLE_TO_PROCESS,
                            BiometricPrompt.ERROR_VENDOR,
                            BiometricPrompt.ERROR_NO_BIOMETRICS,
                            BiometricPrompt.ERROR_HW_NOT_PRESENT,
                            BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL,
                            BiometricPrompt.ERROR_SECURITY_UPDATE_REQUIRED,
                            BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> {
                                mainViewModel.disableBiometricDueToFailure(
                                    reason = "errorCode=$errorCode ($errString)",
                                    fallbackMessage = fallbackMessage
                                )
                            }
                            else -> {
                                if (mainViewModel.recordBiometricNonFatalErrorAndShouldDisable()) {
                                    mainViewModel.disableBiometricDueToFailure(
                                        reason = "repeated errorCode=$errorCode ($errString)",
                                        fallbackMessage = fallbackMessage
                                    )
                                }
                            }
                        }
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        BreadcrumbLog.record("BIOMETRIC", "onAuthenticationFailed", syncWrite = false)
                    }
                }
            )
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.lock_biometric_prompt_title))
                .setSubtitle(getString(R.string.lock_biometric_prompt_subtitle))
                .setNegativeButtonText(getString(R.string.lock_biometric_use_pin))
                .build()
            prompt.authenticate(promptInfo)
        } catch (t: Throwable) {
            BreadcrumbLog.record(
                "BIOMETRIC",
                "Exception in launchBiometricPrompt: ${t::class.java.simpleName}: ${t.message}",
                syncWrite = true
            )
            mainViewModel.disableBiometricDueToFailure(
                reason = "exception=${t::class.java.simpleName}",
                fallbackMessage = fallbackMessage
            )
        }
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
    piecesViewModel: PiecesViewModel,
    recurringViewModel: RecurringViewModel,
    categoriesViewModel: CategoriesViewModel,
    debtsViewModel: DebtsViewModel,
    splitBillViewModel: SplitBillViewModel,
    netWorthViewModel: NetWorthViewModel,
    settingsViewModel: SettingsViewModel,
    reminderStatusViewModel: ReminderStatusViewModel,
    wrappedViewModel: WrappedViewModel,
    pendingStartRoute: String? = null,
    pendingQuickType: TransactionType? = null,
    pendingFocusQuickEntry: Boolean = false,
    pendingVoiceEntry: Boolean = false,
    onPendingLaunchHandled: () -> Unit = {},
    onQuickEntryFocusHandled: () -> Unit = {},
    onVoiceEntryHandled: () -> Unit = {},
    onPinConfigured: () -> Unit,
    openCrashReportRequest: Boolean = false,
    onCrashReportRequestHandled: () -> Unit = {},
    onReplaySplash: () -> Unit = {},
    availableUpdateVersion: String? = null,
    onOpenUpdateDialog: () -> Unit = {}
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    LaunchedEffect(currentRoute) {
        if (currentRoute != null) {
            BreadcrumbLog.record("NAV", "route=$currentRoute", syncWrite = true)
        }
    }
    LaunchedEffect(openCrashReportRequest) {
        if (BuildConfig.CRASH_DIAGNOSTICS_ENABLED && openCrashReportRequest) {
            onCrashReportRequestHandled()
            navController.navigate(PiecerRoutes.CRASH_REPORT) {
                launchSingleTop = true
            }
        }
    }
    LaunchedEffect(pendingStartRoute) {
        if (pendingStartRoute == PiecerRoutes.ADD_TRANSACTION) {
            addTransactionViewModel.startNew()
            pendingQuickType?.let { addTransactionViewModel.onTypeChange(it) }
            onPendingLaunchHandled()
            navController.navigate(PiecerRoutes.ADD_TRANSACTION) {
                launchSingleTop = true
            }
        }
    }
    val isFullScreen = currentRoute == PiecerRoutes.ADD_TRANSACTION ||
        currentRoute == PiecerRoutes.WALLETS ||
        currentRoute == PiecerRoutes.BUDGETS ||
        currentRoute == PiecerRoutes.GOALS ||
        currentRoute == PiecerRoutes.PIECES ||
        currentRoute == PiecerRoutes.RECURRING ||
        currentRoute == PiecerRoutes.CATEGORIES ||
        currentRoute == PiecerRoutes.DEBTS ||
        currentRoute == PiecerRoutes.SPLIT_BILL ||
        currentRoute == PiecerRoutes.NET_WORTH ||
        currentRoute == PiecerRoutes.SETTINGS ||
        currentRoute == PiecerRoutes.REMINDER_STATUS ||
        currentRoute == PiecerRoutes.ABOUT ||
        currentRoute == PiecerRoutes.CRASH_REPORT ||
        currentRoute == PiecerRoutes.WRAPPED

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

    val reduceMotion = LocalReduceMotion.current
    val tabOrder = remember { PiecerTab.entries.map { it.route } }

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
            modifier = if (isFullScreen) Modifier else Modifier.padding(innerPadding),
            enterTransition = {
                if (reduceMotion) {
                    EnterTransition.None
                } else {
                    val fromIdx = tabOrder.indexOf(initialState.destination.route)
                    val toIdx = tabOrder.indexOf(targetState.destination.route)
                    val dir = if (fromIdx >= 0 && toIdx >= 0 && toIdx < fromIdx) -1 else 1
                    fadeIn(animationSpec = tween(180, easing = FastOutSlowInEasing)) +
                        slideInHorizontally(animationSpec = tween(180, easing = FastOutSlowInEasing)) { width ->
                            dir * (width / 24)
                        }
                }
            },
            exitTransition = {
                if (reduceMotion) {
                    ExitTransition.None
                } else {
                    val fromIdx = tabOrder.indexOf(initialState.destination.route)
                    val toIdx = tabOrder.indexOf(targetState.destination.route)
                    val dir = if (fromIdx >= 0 && toIdx >= 0 && toIdx < fromIdx) 1 else -1
                    fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing)) +
                        slideOutHorizontally(animationSpec = tween(180, easing = FastOutSlowInEasing)) { width ->
                            dir * (width / 24)
                        }
                }
            },
            popEnterTransition = {
                if (reduceMotion) {
                    EnterTransition.None
                } else {
                    fadeIn(animationSpec = tween(180, easing = FastOutSlowInEasing)) +
                        slideInHorizontally(animationSpec = tween(180, easing = FastOutSlowInEasing)) { width ->
                            -(width / 24)
                        }
                }
            },
            popExitTransition = {
                if (reduceMotion) {
                    ExitTransition.None
                } else {
                    fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing)) +
                        slideOutHorizontally(animationSpec = tween(180, easing = FastOutSlowInEasing)) { width ->
                            width / 24
                        }
                }
            }
        ) {
            composable(PiecerTab.Home.route) {
                HomeScreen(
                    viewModel = homeViewModel,
                    onSeeAllTransactions = { navigateToTab(PiecerTab.Transactions) },
                    onEditTransaction = openEditTransaction,
                    onOpenBudgets = { navController.navigate(PiecerRoutes.BUDGETS) },
                    onOpenPieces = { navController.navigate(PiecerRoutes.PIECES) },
                    onOpenWrapped = { navController.navigate(PiecerRoutes.WRAPPED) },
                    availableUpdateVersion = availableUpdateVersion,
                    onOpenUpdateDialog = onOpenUpdateDialog
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
                    onOpenPieces = { navController.navigate(PiecerRoutes.PIECES) },
                    onOpenWallets = { navController.navigate(PiecerRoutes.WALLETS) },
                    onOpenCategories = { navController.navigate(PiecerRoutes.CATEGORIES) },
                    onOpenBudgets = { navController.navigate(PiecerRoutes.BUDGETS) },
                    onOpenGoals = { navController.navigate(PiecerRoutes.GOALS) },
                    onOpenDebts = { navController.navigate(PiecerRoutes.DEBTS) },
                    onOpenSplitBill = { navController.navigate(PiecerRoutes.SPLIT_BILL) },
                    onOpenNetWorth = { navController.navigate(PiecerRoutes.NET_WORTH) },
                    onOpenRecurring = { navController.navigate(PiecerRoutes.RECURRING) },
                    onOpenSettings = { navController.navigate(PiecerRoutes.SETTINGS) },
                    onOpenAbout = { navController.navigate(PiecerRoutes.ABOUT) { launchSingleTop = true } },
                    onOpenWrapped = { navController.navigate(PiecerRoutes.WRAPPED) }
                )
            }
            composable(PiecerRoutes.CATEGORIES) {
                CategoriesScreen(
                    viewModel = categoriesViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(PiecerRoutes.DEBTS) {
                DebtsScreen(
                    viewModel = debtsViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(PiecerRoutes.SPLIT_BILL) {
                SplitBillScreen(
                    viewModel = splitBillViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(PiecerRoutes.NET_WORTH) {
                NetWorthScreen(
                    viewModel = netWorthViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(PiecerRoutes.PIECES) {
                PiecesScreen(
                    viewModel = piecesViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(PiecerRoutes.RECURRING) {
                RecurringScreen(
                    viewModel = recurringViewModel,
                    onBack = { navController.popBackStack() }
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
                    onBack = { navController.popBackStack() },
                    onPinConfigured = onPinConfigured,
                    onOpenReminderStatus = {
                        navController.navigate(PiecerRoutes.REMINDER_STATUS) {
                            launchSingleTop = true
                        }
                    },
                    onOpenAbout = {
                        navController.navigate(PiecerRoutes.ABOUT) {
                            launchSingleTop = true
                        }
                    },
                    onOpenCrashReport = {
                        if (BuildConfig.CRASH_DIAGNOSTICS_ENABLED) {
                            navController.navigate(PiecerRoutes.CRASH_REPORT) {
                                launchSingleTop = true
                            }
                        }
                    },
                    onReplaySplash = {
                        if (BuildConfig.CRASH_DIAGNOSTICS_ENABLED) {
                            onReplaySplash()
                        }
                    }
                )
            }
            composable(PiecerRoutes.REMINDER_STATUS) {
                ReminderStatusScreen(
                    viewModel = reminderStatusViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(PiecerRoutes.ABOUT) {
                AboutScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            if (BuildConfig.CRASH_DIAGNOSTICS_ENABLED) {
                composable(PiecerRoutes.CRASH_REPORT) {
                    CrashReportScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(PiecerRoutes.WRAPPED) {
                    WrappedScreen(
                        viewModel = wrappedViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
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
                    },
                    focusQuickEntry = pendingFocusQuickEntry,
                    onQuickEntryFocusHandled = onQuickEntryFocusHandled,
                    launchVoiceEntry = pendingVoiceEntry,
                    onVoiceEntryHandled = onVoiceEntryHandled
                )
            }
        }
    }
}
