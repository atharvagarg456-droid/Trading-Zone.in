package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.DeviceTelemetry
import com.example.data.local.MarketInstrumentQuote
import com.example.data.local.NetworkMonitor
import com.example.data.local.SecurityPreferences
import com.example.data.model.AppThemePreference
import com.example.data.model.BroadcastMessage
import com.example.data.model.ChatBubbleStylePreference
import com.example.data.model.FontScalePreference
import com.example.data.repository.TradingRepository
import com.example.service.TradingBackgroundService
import com.example.ui.components.AdminMarketSearchAndPinDialog
import com.example.ui.components.LiveMarketDetailDialog
import com.example.ui.components.LiveMarketTopTickerStrip
import com.example.ui.components.LiveMarketTradeSetupDialog
import com.example.ui.components.MandatoryFirmwareUpdateScreen
import com.example.ui.components.NotificationHelper
import com.example.ui.components.PermanentQrAndShareCard
import com.example.ui.screens.*
import com.example.ui.theme.TradingTheme
import com.example.ui.viewmodel.TradingViewModel
import com.example.util.SoundAndHapticFeedback
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class MainNavTab {
    STREAM,
    COMMODITY,
    MEMBERSHIP,
    SETTINGS,
    ADMIN
}

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Screenshot & Screen Recording Protection
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        // Request 120Hz / peak supported hardware refresh rate for ultra-smooth scrolling & animations
        runCatching {
            val attrs = window.attributes
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val maxMode = display?.supportedModes?.maxByOrNull { it.refreshRate }
                if (maxMode != null) {
                    attrs.preferredDisplayModeId = maxMode.modeId
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                @Suppress("DEPRECATION")
                val maxMode = windowManager.defaultDisplay?.supportedModes?.maxByOrNull { it.refreshRate }
                if (maxMode != null) {
                    attrs.preferredDisplayModeId = maxMode.modeId
                }
            }
            @Suppress("DEPRECATION")
            attrs.preferredRefreshRate = 120f
            window.attributes = attrs
        }

        enableEdgeToEdge()
        NotificationHelper.ensureChannels(this)

        val appContext = applicationContext
        val databaseId = getString(R.string.firestore_database_id)
        val firestore = Firebase.firestore(databaseId)
        val repository = TradingRepository(firestore)
        val securityPrefs = SecurityPreferences(appContext)
        val deviceId = DeviceTelemetry.getPermanentDeviceId(appContext)
        val deviceModel = DeviceTelemetry.getDeviceModelLabel()

        setContent {
            var themePref by remember { mutableStateOf(securityPrefs.getThemePreference()) }
            var fontScalePref by remember { mutableStateOf(securityPrefs.getFontScalePreference()) }
            val isNetworkConnected by remember {
                NetworkMonitor.observeNetworkConnectivity(appContext)
            }.collectAsStateWithLifecycle(initialValue = NetworkMonitor.isCurrentlyConnected(appContext))

            var currentUser by remember { mutableStateOf<FirebaseUser?>(Firebase.auth.currentUser) }
            var ipAddress by remember { mutableStateOf("0.0.0.0") }

            DisposableEffect(Unit) {
                val listener = FirebaseAuth.AuthStateListener { auth ->
                    currentUser = auth.currentUser
                }
                Firebase.auth.addAuthStateListener(listener)
                onDispose {
                    Firebase.auth.removeAuthStateListener(listener)
                }
            }

            LaunchedEffect(Unit) {
                ipAddress = withContext(Dispatchers.IO) {
                    DeviceTelemetry.getDeviceIpAddress()
                }
            }

            // Track offline transitions in preferences for the 1-hour offline mobile data reminder
            LaunchedEffect(isNetworkConnected) {
                if (!isNetworkConnected) {
                    if (securityPrefs.getOfflineSinceMillis() <= 0L) {
                        securityPrefs.setOfflineSinceMillis(System.currentTimeMillis())
                    }
                } else {
                    securityPrefs.setOfflineSinceMillis(0L)
                    securityPrefs.setLastOfflineNotificationSentMillis(0L)
                }
            }

            var showStartupSplash by remember { mutableStateOf(true) }

            TradingTheme(
                themePreference = themePref,
                fontScaleFactor = fontScalePref.scaleFactor
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    val activeUser = currentUser
                    if (activeUser == null) {
                        GoogleSignInWelcomeScreen(
                            deviceId = deviceId,
                            ipAddress = ipAddress,
                            isNetworkConnected = isNetworkConnected,
                            onAuthSuccess = {
                                currentUser = Firebase.auth.currentUser
                            }
                        )
                    } else {
                        val vm: TradingViewModel = viewModel(
                            key = "vm_${activeUser.uid}",
                            factory = object : ViewModelProvider.Factory {
                                @Suppress("UNCHECKED_CAST")
                                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                    return TradingViewModel(
                                        repository = repository,
                                        securityPreferences = securityPrefs,
                                        currentUserId = activeUser.uid,
                                        currentUserEmail = activeUser.email.orEmpty(),
                                        currentUserDisplayName = activeUser.displayName.orEmpty(),
                                        deviceId = deviceId,
                                        deviceModel = deviceModel,
                                        ipAddress = ipAddress,
                                        networkConnectivityFlow = NetworkMonitor.observeNetworkConnectivity(appContext),
                                        onNewOrEditedMessageAlert = { msg ->
                                            val sig = msg.resolvedSignalType()
                                            if (sig.isNotBlank()) {
                                                SoundAndHapticFeedback.playTradeSignal(appContext, sig)
                                            }
                                            NotificationHelper.showBroadcastNotification(
                                                context = appContext,
                                                isEdited = msg.isEdited
                                            )
                                        },
                                        onCyclePaymentReminderAlert = { isExpired, daysLeft ->
                                            NotificationHelper.showCyclePaymentReminderNotification(
                                                context = appContext,
                                                isExpired = isExpired,
                                                daysLeft = daysLeft
                                            )
                                        },
                                        onAdminApprovalOrSecurityAlert = { title, body ->
                                            SoundAndHapticFeedback.playAlertOrError(appContext)
                                            NotificationHelper.showAdminApprovalNotification(
                                                context = appContext,
                                                title = title,
                                                body = body
                                            )
                                        }
                                    ) as T
                                }
                            }
                        )

                        AuthenticatedTradingWorkspace(
                            viewModel = vm,
                            themePreference = themePref,
                            onChangeTheme = { newTheme ->
                                securityPrefs.setThemePreference(newTheme)
                                themePref = newTheme
                            },
                            onFontScaleChanged = { newScale ->
                                fontScalePref = newScale
                            },
                            onSignedOut = {
                                TradingBackgroundService.stop(appContext)
                                currentUser = null
                            }
                        )
                    }

                    // Ultrasound Startup Splash Animation with Premium Logo
                    com.example.ui.components.TradingZoneStartupSplashOverlay(
                        visible = showStartupSplash,
                        onSplashFinished = { showStartupSplash = false }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticatedTradingWorkspace(
    viewModel: TradingViewModel,
    themePreference: AppThemePreference,
    onChangeTheme: (AppThemePreference) -> Unit,
    onFontScaleChanged: (FontScalePreference) -> Unit = {},
    onSignedOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val snackbarHostState = remember { SnackbarHostState() }

    val isNetworkConnected by viewModel.isNetworkConnected.collectAsStateWithLifecycle()
    val isAppLockSetupComplete by viewModel.isAppLockSetupComplete.collectAsStateWithLifecycle()
    val isSessionUnlocked by viewModel.isSessionUnlocked.collectAsStateWithLifecycle()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsStateWithLifecycle()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val offlineDataReminderEnabled by viewModel.offlineDataReminderEnabled.collectAsStateWithLifecycle()
    val offlineReminderIntervalMinutes by viewModel.offlineReminderIntervalMinutes.collectAsStateWithLifecycle()
    val cycleExpiryNotificationsEnabled by viewModel.cycleExpiryNotificationsEnabled.collectAsStateWithLifecycle()
    val highRiskVibrationEnabled by viewModel.highRiskVibrationEnabled.collectAsStateWithLifecycle()
    val compactFeedMode by viewModel.compactFeedMode.collectAsStateWithLifecycle()
    val largeTextMode by viewModel.largeTextMode.collectAsStateWithLifecycle()
    val fontScalePreference by viewModel.fontScalePreference.collectAsStateWithLifecycle()
    val chatBubbleStylePreference by viewModel.chatBubbleStylePreference.collectAsStateWithLifecycle()
    val showLiveMarketTicker by viewModel.showLiveMarketTicker.collectAsStateWithLifecycle()
    val autoScrollFeedEnabled by viewModel.autoScrollFeedEnabled.collectAsStateWithLifecycle()
    val defaultLandingTab by viewModel.defaultLandingTab.collectAsStateWithLifecycle()
    val liveMarketQuotes by viewModel.liveMarketQuotes.collectAsStateWithLifecycle()
    val localSecurityQuestion by viewModel.localSecurityQuestion.collectAsStateWithLifecycle()
    val appleWebAppUrl by viewModel.appleWebAppUrl.collectAsStateWithLifecycle()
    val androidApkShareUrl by viewModel.androidApkShareUrl.collectAsStateWithLifecycle()

    val isAdmin by viewModel.isUserAdmin.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val deviceRecord by viewModel.deviceRecord.collectAsStateWithLifecycle()
    val myDevices by viewModel.myDevices.collectAsStateWithLifecycle()
    val broadcastMessages by viewModel.broadcastMessages.collectAsStateWithLifecycle()
    val openedMessageIds by viewModel.openedMessageIds.collectAsStateWithLifecycle()
    val allMessageReadReceipts by viewModel.allMessageReadReceiptsForAdmin.collectAsStateWithLifecycle()
    val communityReviews by viewModel.communityReviews.collectAsStateWithLifecycle()
    val emojiReactionsByTarget by viewModel.emojiReactionsByTarget.collectAsStateWithLifecycle()
    val myReactedKeys by viewModel.myReactedKeys.collectAsStateWithLifecycle()
    val onlinePeopleCount by viewModel.onlinePeopleCount.collectAsStateWithLifecycle()
    val pricingConfig by viewModel.pricingConfig.collectAsStateWithLifecycle()
    val adminSettings by viewModel.adminSettings.collectAsStateWithLifecycle()
    val myAccessRequests by viewModel.myAccessRequests.collectAsStateWithLifecycle()
    val allUsersForAdmin by viewModel.allUsersForAdmin.collectAsStateWithLifecycle()
    val allDevicesForAdmin by viewModel.allDevicesForAdmin.collectAsStateWithLifecycle()
    val allAccessRequestsForAdmin by viewModel.allAccessRequestsForAdmin.collectAsStateWithLifecycle()
    val moderationAlertsForAdmin by viewModel.moderationAlertsForAdmin.collectAsStateWithLifecycle()
    val installedFirmwareStamp by viewModel.installedFirmwareStamp.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val screenshotWarningMessage by viewModel.screenshotWarningMessage.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

    var selectedTab by remember {
        val initialTab = when (defaultLandingTab) {
            "COMMODITY" -> MainNavTab.COMMODITY
            "MEMBERSHIP" -> MainNavTab.MEMBERSHIP
            else -> MainNavTab.STREAM
        }
        mutableStateOf(initialTab)
    }
    var editingMessage by remember { mutableStateOf<BroadcastMessage?>(null) }
    var replyingToMessage by remember { mutableStateOf<BroadcastMessage?>(null) }
    var showQuickQrShareDialog by remember { mutableStateOf(false) }
    var showTelegramQuickDrawer by remember { mutableStateOf(false) }
    var showAdminMarketSearchDialog by remember { mutableStateOf(false) }
    var selectedMarketQuoteForDetail by remember { mutableStateOf<MarketInstrumentQuote?>(null) }
    var activeTradeSetupRequest by remember { mutableStateOf<Triple<MarketInstrumentQuote, String, String>?>(null) }

    val totalPendingAdminApprovals = remember(isAdmin, allUsersForAdmin, allAccessRequestsForAdmin, moderationAlertsForAdmin) {
        if (!isAdmin) {
            0
        } else {
            val pendingRegsAndEdits = allUsersForAdmin.count {
                it.accountStatus == "PENDING_APPROVAL" || it.hasPendingProfileEdit
            }
            val pendingReqs = allAccessRequestsForAdmin.count { it.status == "PENDING" }
            val openAlerts = moderationAlertsForAdmin.count { it.status != "RESOLVED" }
            pendingRegsAndEdits + pendingReqs + openAlerts
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* handled gracefully */ }

    LaunchedEffect(viewModel.currentUserId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        TradingBackgroundService.start(context)
    }

    // Anti-Screenshot Detection Observer (1st attempt warns user + alerts Admin; 2nd attempt blocks for 24 hours)
    DisposableEffect(viewModel.currentUserId, isAdmin) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: android.net.Uri?) {
                super.onChange(selfChange, uri)
                if (!isAdmin) {
                    val uriStr = uri?.toString()?.lowercase().orEmpty()
                    if (uriStr.contains("media") || uriStr.contains("images") || uriStr.contains("screenshot")) {
                        SoundAndHapticFeedback.playAlertOrError(context)
                        viewModel.recordScreenshotStrike()
                    }
                }
            }
        }
        try {
            context.contentResolver.registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true,
                observer
            )
        } catch (_: Exception) {
        }
        onDispose {
            try {
                context.contentResolver.unregisterContentObserver(observer)
            } catch (_: Exception) {
            }
        }
    }

    LaunchedEffect(statusMessage) {
        statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    val isDeviceBanned = deviceRecord?.isBanned == true || userProfile?.accountStatus == "BANNED"
    val hasCompletedRegistration = userProfile != null && userProfile?.fullName?.isNotBlank() == true
    val isApprovedMember = isAdmin || userProfile?.accountStatus == "APPROVED"
    val maxAllowedDevices = userProfile?.maxDevicesLimit(pricingConfig.defaultMaxDevicesPerUser()) ?: 2
    val activeUnbannedDevices = remember(myDevices) { myDevices.filter { !it.isBanned } }
    val isDeviceLimitExceeded = !isAdmin &&
        activeUnbannedDevices.size > maxAllowedDevices &&
        activeUnbannedDevices.take(maxAllowedDevices).none { it.deviceId == viewModel.deviceId }

    val firmwareConfig = remember(pricingConfig) { pricingConfig.firmwareConfig() }
    val requiresMandatoryFirmwareUpdate = firmwareConfig.enabled &&
        installedFirmwareStamp != firmwareConfig.uniqueVersionStamp()

    val showMainTabs = isAppLockSetupComplete &&
        isSessionUnlocked &&
        !isDeviceBanned &&
        !isDeviceLimitExceeded &&
        !requiresMandatoryFirmwareUpdate &&
        hasCompletedRegistration &&
        isApprovedMember

    val signOutAction: () -> Unit = {
        performGoogleSignOut(
            credentialManager = credentialManager,
            scope = scope,
            onSignOutComplete = onSignedOut
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable {
                                    com.example.util.SoundAndHapticFeedback.playTap(context)
                                    showTelegramQuickDrawer = true
                                }
                                .testTag("top_bar_trading_zone_title_button")
                        ) {
                            com.example.ui.components.TradingZonePremiumLogo(
                                size = 34.dp,
                                animated = true
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Trading Zone",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Open Quick Customization Menu",
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    text = if (isAdmin) "Tap for Quick Controls • Live" else "Tap for Quick Customization",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    actions = {
                        // Live Admin Pending Approvals & Security Alerts Badge in Top Bar
                        if (isAdmin && totalPendingAdminApprovals > 0) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(0xFFF59E0B).copy(alpha = 0.22f),
                                border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .clickable {
                                        com.example.util.SoundAndHapticFeedback.playClick(context)
                                        selectedTab = MainNavTab.ADMIN
                                    }
                                    .testTag("top_bar_admin_approvals_badge")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = "Pending Approvals",
                                        tint = Color(0xFFF59E0B),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "$totalPendingAdminApprovals",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFF59E0B)
                                    )
                                }
                            }
                        }

                        // 1-Tap Look-Changer UI Theme Switcher in TopAppBar
                        IconButton(
                            onClick = {
                                com.example.util.SoundAndHapticFeedback.playTap(context)
                                val entries = AppThemePreference.entries
                                val nextIdx = (entries.indexOf(themePreference) + 1) % entries.size
                                onChangeTheme(entries[nextIdx])
                            },
                            modifier = Modifier.testTag("top_bar_look_changer_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = "Switch Look-Changer UI Theme (${themePreference.title})",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }

                        // Quick Permanent QR & Share Website/App button for Apple & Android devices
                        IconButton(
                            onClick = {
                                com.example.util.SoundAndHapticFeedback.playTap(context)
                                showQuickQrShareDialog = true
                            },
                            modifier = Modifier.testTag("top_bar_share_qr_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode2,
                                contentDescription = "Share Apple Website & Permanent QR Code",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Live Offline / No Network Connected Symbol Pill in TopAppBar
                        if (!isNetworkConnected) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(0xFFEF4444).copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, Color(0xFFEF4444)),
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .testTag("top_bar_no_network_symbol")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SignalWifiOff,
                                        contentDescription = "No Network Connected",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "No Net",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFEF4444)
                                    )
                                }
                            }
                        } else if (showMainTabs && userProfile != null) {
                            val days = userProfile?.daysRemaining() ?: 0
                            val entitled = isAdmin || (userProfile?.isEntitledNow() == true)
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (entitled) {
                                    Color(0xFF10B981).copy(alpha = 0.16f)
                                } else {
                                    Color(0xFFF59E0B).copy(alpha = 0.16f)
                                },
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                Text(
                                    text = when {
                                        isAdmin -> "ADMIN"
                                        userProfile?.entitlementStatus == "TRIAL_ACTIVE" && days > 0 ->
                                            "TRIAL • ${days}d"
                                        days > 0 -> "ACTIVE • ${days}d"
                                        else -> "EXPIRED"
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (entitled) Color(0xFF10B981) else Color(0xFFF59E0B)
                                )
                            }
                        }

                        // Telegram-style Three-Dot (⋮) Quick Customization & Admin Tools Menu Button
                        IconButton(
                            onClick = {
                                com.example.util.SoundAndHapticFeedback.playTap(context)
                                showTelegramQuickDrawer = true
                            },
                            modifier = Modifier.testTag("top_bar_three_dot_menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Open Telegram-Style Quick Customization & Tools Drawer"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // Investing.com-Style Top Live Market Ticker Strip (Pinned instruments with real-time ticks)
                if (showMainTabs && showLiveMarketTicker && liveMarketQuotes.isNotEmpty()) {
                    LiveMarketTopTickerStrip(
                        quotes = liveMarketQuotes,
                        isAdmin = isAdmin,
                        onSelectQuote = { selectedMarketQuoteForDetail = it },
                        onOpenAdminSearchDialog = { showAdminMarketSearchDialog = true }
                    )
                }

                // Prominent Animated Offline / No-Network Banner across the screen
                Box(modifier = Modifier.padding(horizontal = 12.dp)) {
                    NoNetworkStatusBanner(isNetworkConnected = isNetworkConnected)
                }

                // Live Cross-Device 4-Digit Quick Login Code Banner on already-unlocked devices
                if (isSessionUnlocked) {
                    CrossDeviceLoginCodeBanner(
                        pendingOtpRequests = myAccessRequests,
                        currentDeviceId = viewModel.deviceId,
                        onApproveOtpRequest = { req ->
                            viewModel.approveCrossDeviceLoginOtp(req)
                        }
                    )
                }
            }
        },
        bottomBar = {
            if (showMainTabs) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = selectedTab == MainNavTab.STREAM,
                        onClick = {
                            com.example.util.SoundAndHapticFeedback.playTap(context)
                            selectedTab = MainNavTab.STREAM
                        },
                        icon = { Icon(Icons.Default.Campaign, contentDescription = "Messages") },
                        label = { Text("Messages") },
                        modifier = Modifier.testTag("nav_stream")
                    )
                    NavigationBarItem(
                        selected = selectedTab == MainNavTab.COMMODITY,
                        onClick = {
                            com.example.util.SoundAndHapticFeedback.playTap(context)
                            selectedTab = MainNavTab.COMMODITY
                        },
                        icon = { Icon(Icons.Default.Forum, contentDescription = "Commodity") },
                        label = { Text("Commodity") },
                        modifier = Modifier.testTag("nav_reviews")
                    )
                    NavigationBarItem(
                        selected = selectedTab == MainNavTab.MEMBERSHIP,
                        onClick = {
                            com.example.util.SoundAndHapticFeedback.playTap(context)
                            selectedTab = MainNavTab.MEMBERSHIP
                        },
                        icon = { Icon(Icons.Default.QrCode2, contentDescription = "Plans") },
                        label = { Text("Plans") },
                        modifier = Modifier.testTag("nav_membership")
                    )
                    NavigationBarItem(
                        selected = selectedTab == MainNavTab.SETTINGS,
                        onClick = {
                            com.example.util.SoundAndHapticFeedback.playTap(context)
                            selectedTab = MainNavTab.SETTINGS
                        },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") },
                        modifier = Modifier.testTag("nav_settings")
                    )
                    if (isAdmin) {
                        NavigationBarItem(
                            selected = selectedTab == MainNavTab.ADMIN,
                            onClick = {
                                com.example.util.SoundAndHapticFeedback.playTap(context)
                                selectedTab = MainNavTab.ADMIN
                            },
                            icon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = "Admin") },
                            label = { Text("Admin") },
                            modifier = Modifier.testTag("nav_admin")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                !isAppLockSetupComplete -> {
                    AppLockSetupScreen(
                        userEmail = viewModel.currentUserEmail,
                        isAdmin = isAdmin,
                        onCompleteSetup = { pin, bio, question, answer ->
                            viewModel.completeInitialAppLockSetup(pin, bio, question, answer)
                        },
                        onSignOut = signOutAction
                    )
                }
                !isSessionUnlocked -> {
                    AppLockUnlockScreen(
                        userEmail = viewModel.currentUserEmail,
                        isAdmin = isAdmin,
                        isTwoStepEnabled = if (isAdmin) true else (userProfile?.isTwoStepEnabled() ?: false),
                        isBiometricEnabled = isBiometricEnabled,
                        securityQuestion = userProfile?.displaySecurityQuestion()?.ifBlank { localSecurityQuestion }
                            ?: localSecurityQuestion.ifBlank { "What is my favorite commodity or stock?" },
                        onVerifyAppPassword = { viewModel.verifyAppPassword(it) },
                        onVerifyPin = { viewModel.verifyPinAndUnlock(it) },
                        onRecoverPin = { answer, newPin ->
                            viewModel.recoverPinWithSecurityAnswer(answer, newPin)
                        },
                        onBiometricUnlockSuccess = { viewModel.unlockViaBiometricSuccess() },
                        onSignOut = signOutAction,
                        myAccessRequests = myAccessRequests,
                        currentDeviceId = viewModel.deviceId,
                        onRequestCrossDeviceOtp = { code4Digit ->
                            viewModel.requestCrossDeviceLoginOtp(code4Digit)
                        }
                    )
                }
                !hasCompletedRegistration -> {
                    MandatoryPhotoRegistrationScreen(
                        userEmail = viewModel.currentUserEmail,
                        initialDisplayName = viewModel.currentUserDisplayName,
                        initialSecurityQuestion = localSecurityQuestion,
                        deviceId = viewModel.deviceId,
                        deviceModel = viewModel.deviceModel,
                        ipAddress = viewModel.ipAddress,
                        isBusy = isBusy,
                        statusMessage = statusMessage,
                        onSubmitRegistration = { fullName, phone, city, photo, question, answer, terms ->
                            viewModel.submitUserRegistration(
                                fullName = fullName,
                                phoneNumber = phone,
                                city = city,
                                photoBase64 = photo,
                                securityQuestion = question,
                                securityAnswer = answer,
                                termsAccepted = terms
                            )
                        },
                        onSignOut = signOutAction
                    )
                }
                isDeviceBanned || !isApprovedMember -> {
                    userProfile?.let { profile ->
                        AccountStatusGateScreen(
                            profile = profile,
                            isDeviceBanned = isDeviceBanned,
                            onSignOut = signOutAction,
                            myAccessRequests = myAccessRequests,
                            onSubmitUnbanAppeal = { reason ->
                                viewModel.submitUnbanAppeal(reason)
                            }
                        )
                    }
                }
                isDeviceLimitExceeded -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            shape = RoundedCornerShape(22.dp),
                            border = BorderStroke(1.5.dp, Color(0xFFF59E0B)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "📱 Maximum Login Devices Reached (${activeUnbannedDevices.size} / $maxAllowedDevices)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFF59E0B)
                                )
                                Text(
                                    text = "Your account is currently set to allow a maximum of $maxAllowedDevices active device(s). Sign out or block one of your older logged-in devices below to continue on this mobile:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                activeUnbannedDevices.forEach { dev ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = dev.deviceModel.ifBlank { "Logged-In Device" },
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "ID: ${dev.deviceId} • IP: ${dev.ipAddress}",
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }
                                            Button(
                                                onClick = {
                                                    com.example.util.SoundAndHapticFeedback.playTap(context)
                                                    viewModel.removeDeviceSession(dev.deviceId)
                                                }
                                            ) {
                                                Text("Sign Out")
                                            }
                                        }
                                    }
                                }
                                OutlinedButton(
                                    onClick = signOutAction,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Sign Out of This Device Instead")
                                }
                            }
                        }
                    }
                }
                requiresMandatoryFirmwareUpdate -> {
                    MandatoryFirmwareUpdateScreen(
                        firmwareConfig = firmwareConfig,
                        isAdmin = isAdmin,
                        onApplyUpdateNow = {
                            viewModel.applyMandatoryFirmwareUpdateNow()
                        },
                        onAdminDisableGate = if (isAdmin) {
                            {
                                viewModel.pushAdminFirmwareToAllDevices(
                                    enabled = false,
                                    versionName = firmwareConfig.versionName,
                                    releaseNotes = firmwareConfig.releaseNotes
                                )
                            }
                        } else {
                            null
                        }
                    )
                }
                else -> {
                    if (selectedTab != MainNavTab.STREAM) {
                        BackHandler {
                            selectedTab = MainNavTab.STREAM
                        }
                    }

                    // Super-smooth 120Hz directional slide + micro-scale + fade transition
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            val forward = targetState.ordinal >= initialState.ordinal
                            val slideIn = slideInHorizontally(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMedium
                                ),
                                initialOffsetX = { fullWidth -> if (forward) fullWidth / 5 else -fullWidth / 5 }
                            ) + fadeIn(animationSpec = tween(160)) + scaleIn(
                                initialScale = 0.985f,
                                animationSpec = spring(stiffness = Spring.StiffnessMedium)
                            )
                            val slideOut = slideOutHorizontally(
                                animationSpec = tween(130),
                                targetOffsetX = { fullWidth -> if (forward) -fullWidth / 6 else fullWidth / 6 }
                            ) + fadeOut(animationSpec = tween(120)) + scaleOut(
                                targetScale = 0.985f,
                                animationSpec = tween(130)
                            )
                            slideIn togetherWith slideOut
                        },
                        label = "MainTabTransition"
                    ) { destination ->
                        when (destination) {
                            MainNavTab.STREAM -> {
                                BroadcastStreamScreen(
                                    messages = broadcastMessages,
                                    openedMessageIds = openedMessageIds,
                                    allMessageReadReceipts = allMessageReadReceipts,
                                    userProfile = userProfile,
                                    deviceRecord = deviceRecord,
                                    pricingConfig = pricingConfig,
                                    onlinePeopleCount = onlinePeopleCount,
                                    compactFeedMode = compactFeedMode,
                                    largeTextMode = largeTextMode,
                                    isAdmin = isAdmin,
                                    emojiReactionsByTarget = emojiReactionsByTarget,
                                    myReactedKeys = myReactedKeys,
                                    onMarkMessageOpened = { msg -> viewModel.markMessageAsOpened(msg) },
                                    onToggleReaction = { targetId, emoji ->
                                        viewModel.toggleTelegramReaction(targetId, emoji)
                                    },
                                    onRequestFreeTrial = { viewModel.requestOneWeekFreeTrial() },
                                    onNavigateToMembership = { selectedTab = MainNavTab.MEMBERSHIP },
                                    onNavigateToReviews = { selectedTab = MainNavTab.COMMODITY },
                                    onReplyToMessageRequest = { msg ->
                                        replyingToMessage = msg
                                        editingMessage = null
                                        selectedTab = MainNavTab.ADMIN
                                    },
                                    onEditMessageRequest = { msg ->
                                        editingMessage = msg
                                        replyingToMessage = null
                                        selectedTab = MainNavTab.ADMIN
                                    },
                                    onDeleteMessageRequest = { msgId ->
                                        viewModel.deleteMessage(msgId)
                                    },
                                    chatBubbleStyle = chatBubbleStylePreference,
                                    liveMarketQuotes = liveMarketQuotes,
                                    onOpenAdminMarketSearch = { showAdminMarketSearchDialog = true },
                                    onSelectMarketQuote = { selectedMarketQuoteForDetail = it },
                                    onOpenTradeSetupForQuote = { quote ->
                                        activeTradeSetupRequest = Triple(quote, "BUY", "15m")
                                    },
                                    onQuickPublishBroadcast = { title, content, targetTier, signalType, photoBase64 ->
                                        val encodedLinkTitle = BroadcastMessage.encodeLinkTitleWithMeta(
                                            plainLinkTitle = "",
                                            targetTier = targetTier,
                                            signalType = signalType
                                        )
                                        viewModel.publishMessage(
                                            title = title,
                                            content = content,
                                            category = "NONE",
                                            riskLevel = "MEDIUM",
                                            markedPhotoBase64 = photoBase64,
                                            linkUrl = "",
                                            linkTitle = encodedLinkTitle,
                                            isPinned = false,
                                            replyToMessageId = "",
                                            replyToTitle = "",
                                            replyToSnippet = "",
                                            replyToRiskLevel = ""
                                        )
                                    }
                                )
                            }
                            MainNavTab.COMMODITY -> {
                                CommunityReviewsScreen(
                                    profile = userProfile,
                                    pricingConfig = pricingConfig,
                                    broadcastMessages = broadcastMessages,
                                    reviews = communityReviews,
                                    onlinePeopleCount = onlinePeopleCount,
                                    currentUserId = viewModel.currentUserId,
                                    isAdmin = isAdmin,
                                    isBusy = isBusy,
                                    emojiReactionsByTarget = emojiReactionsByTarget,
                                    myReactedKeys = myReactedKeys,
                                    onSubmitReview = { msgId, msgTitle, risk, topic, opinion, rating ->
                                        viewModel.submitCommunityReview(
                                            tradeMessageId = msgId,
                                            tradeMessageTitle = msgTitle,
                                            tradeRiskLevel = risk,
                                            topicTag = topic,
                                            opinionText = opinion,
                                            rating = rating
                                        )
                                    },
                                    onTogglePinReview = { reviewId, pinned ->
                                        viewModel.setCommodityMessagePinned(reviewId, pinned)
                                    },
                                    onDeleteReview = { viewModel.deleteCommunityReview(it) },
                                    onToggleCommodityChatLock = { locked ->
                                        viewModel.setCommodityChatLocked(locked)
                                    },
                                    onToggleReaction = { targetId, emoji ->
                                        viewModel.toggleTelegramReaction(targetId, emoji)
                                    },
                                    onUpdateTodayProfitBannerAndCard = { banner, alsoPostInFeed ->
                                        viewModel.updateTodayProfitBannerAndOptionalCard(banner, alsoPostInFeed)
                                    },
                                    onClearUnpinnedCommodityMessages = {
                                        viewModel.clearUnpinnedCommodityMessages()
                                    },
                                    onApplyDailyMessageLimit = { newLimit ->
                                        viewModel.applyDailyMessageLimitNow(newLimit)
                                    },
                                    chatBubbleStyle = chatBubbleStylePreference,
                                    compactFeedMode = compactFeedMode,
                                    liveMarketQuotes = liveMarketQuotes,
                                    onOpenAdminMarketSearch = { showAdminMarketSearchDialog = true },
                                    onSelectMarketQuote = { selectedMarketQuoteForDetail = it },
                                    onOpenTradeSetupForQuote = { quote ->
                                        activeTradeSetupRequest = Triple(quote, "BUY", "15m")
                                    },
                                    onUpdateCommodityModerationRules = { slowSec, blockLinks, customWords ->
                                        viewModel.updateCommodityModerationRules(slowSec, blockLinks, customWords)
                                    }
                                )
                            }
                            MainNavTab.MEMBERSHIP -> {
                                MembershipScreen(
                                    profile = userProfile,
                                    deviceRecord = deviceRecord,
                                    pricingConfig = pricingConfig,
                                    myRequests = myAccessRequests,
                                    isBusy = isBusy,
                                    isAdmin = isAdmin,
                                    onRequestTrial = { viewModel.requestOneWeekFreeTrial() },
                                    onSubmitUpiPayment = { planName, amount, days, utr, screenshot ->
                                        viewModel.submitUpiPayment(
                                            planName = planName,
                                            amountInr = amount,
                                            validityDays = days,
                                            utrReference = utr,
                                            paymentScreenshotBase64 = screenshot
                                        )
                                    },
                                    onTogglePlanEnabled = { plan, enabled ->
                                        viewModel.togglePlanEnabled(plan, enabled)
                                    },
                                    onUpsertPlanInStudio = { slotIndex, planId, name, priceInr, validityDays, badge, qrCodeBase64, qrOnlyMode, enabled ->
                                        viewModel.upsertPlanInStudio(
                                            slotIndex = slotIndex,
                                            planId = planId,
                                            name = name,
                                            priceInr = priceInr,
                                            validityDays = validityDays,
                                            badge = badge,
                                            qrCodeBase64 = qrCodeBase64,
                                            qrOnlyMode = qrOnlyMode,
                                            enabled = enabled
                                        )
                                    },
                                    onDeletePlanInStudio = { plan ->
                                        viewModel.deletePlanInStudio(plan)
                                    },
                                    onBulkExtendSubscribers = { extraDays ->
                                        viewModel.bulkExtendActiveSubscribers(extraDays)
                                    },
                                    onApplyDailyMessageLimit = { newLimit ->
                                        viewModel.applyDailyMessageLimitNow(newLimit)
                                    }
                                )
                            }
                            MainNavTab.SETTINGS -> {
                                SettingsScreen(
                                    userProfile = userProfile,
                                    isAdmin = isAdmin,
                                    currentDeviceId = viewModel.deviceId,
                                    myDevices = myDevices,
                                    appleWebAppUrl = appleWebAppUrl,
                                    androidApkShareUrl = androidApkShareUrl,
                                    onUpdateShareUrls = { appleUrl, androidUrl ->
                                        viewModel.updateShareUrls(appleUrl, androidUrl)
                                    },
                                    themePreference = themePreference,
                                    isBiometricEnabled = isBiometricEnabled,
                                    notificationsEnabled = notificationsEnabled,
                                    offlineDataReminderEnabled = offlineDataReminderEnabled,
                                    offlineReminderIntervalMinutes = offlineReminderIntervalMinutes,
                                    cycleExpiryNotificationsEnabled = cycleExpiryNotificationsEnabled,
                                    highRiskVibrationEnabled = highRiskVibrationEnabled,
                                    compactFeedMode = compactFeedMode,
                                    largeTextMode = largeTextMode,
                                    securityQuestion = localSecurityQuestion,
                                    onChangeTheme = onChangeTheme,
                                    onToggleBiometric = { viewModel.setBiometricPreference(it) },
                                    onToggleNotifications = { viewModel.setNotificationsPreference(it) },
                                    onToggleOfflineDataReminder = { viewModel.setOfflineDataReminderPreference(it) },
                                    onChangeOfflineReminderInterval = { viewModel.setOfflineReminderIntervalPreference(it) },
                                    onToggleCycleExpiryNotifications = { viewModel.setCycleExpiryNotificationsPreference(it) },
                                    onToggleHighRiskVibration = { viewModel.setHighRiskVibrationPreference(it) },
                                    onToggleCompactFeedMode = { viewModel.setCompactFeedMode(it) },
                                    onToggleLargeTextMode = { viewModel.setLargeTextMode(it) },
                                    onUpdatePin = { cur, next -> viewModel.updateAppLockPin(cur, next) },
                                    onUpdateSecurityQuestion = { q, a -> viewModel.updateSecurityQuestionAndAnswer(q, a) },
                                    onUpdateSecurityPreferences = { newPass, twoStepEn, maxDev ->
                                        viewModel.updateMySecurityPreferences(newPass, twoStepEn, maxDev)
                                    },
                                    onToggleDeviceBlocked = { devId, blocked ->
                                        viewModel.toggleSingleDeviceBlocked(devId, blocked)
                                    },
                                    onRemoveDeviceSession = { devId ->
                                        viewModel.removeDeviceSession(devId)
                                    },
                                    onRequestProfileEdit = { name, phone, city, photo ->
                                        viewModel.requestProfileEdit(name, phone, city, photo)
                                    },
                                    fontScalePreference = fontScalePreference,
                                    chatBubbleStylePreference = chatBubbleStylePreference,
                                    showLiveMarketTicker = showLiveMarketTicker,
                                    autoScrollFeedEnabled = autoScrollFeedEnabled,
                                    defaultLandingTab = defaultLandingTab,
                                    pricingConfig = pricingConfig,
                                    onChangeFontScale = { newScale ->
                                        viewModel.setFontScalePreference(newScale)
                                        onFontScaleChanged(newScale)
                                    },
                                    onChangeChatBubbleStyle = { newStyle ->
                                        viewModel.setChatBubbleStylePreference(newStyle)
                                    },
                                    onToggleShowLiveMarketTicker = { viewModel.setShowLiveMarketTicker(it) },
                                    onToggleAutoScrollFeed = { viewModel.setAutoScrollFeedEnabled(it) },
                                    onChangeDefaultLandingTab = { viewModel.setDefaultLandingTab(it) },
                                    onOpenAdminMarketSearch = { showAdminMarketSearchDialog = true },
                                    onUpdateAnnouncementTicker = { viewModel.updateAnnouncementTicker(it) },
                                    onUpdateCommodityModerationRules = { slowSec, blockLinks, customWords ->
                                        viewModel.updateCommodityModerationRules(slowSec, blockLinks, customWords)
                                    },
                                    onLockNow = { viewModel.lockSessionNow() },
                                    onSignOut = signOutAction
                                )
                            }
                            MainNavTab.ADMIN -> {
                                AdminConsoleScreen(
                                    allUsers = allUsersForAdmin,
                                    allRequests = allAccessRequestsForAdmin,
                                    moderationAlerts = moderationAlertsForAdmin,
                                    pricingConfig = pricingConfig,
                                    adminSettings = adminSettings,
                                    appleWebAppUrl = appleWebAppUrl,
                                    androidApkShareUrl = androidApkShareUrl,
                                    onUpdateShareUrls = { appleUrl, androidUrl ->
                                        viewModel.updateShareUrls(appleUrl, androidUrl)
                                    },
                                    editingMessageInit = editingMessage,
                                    replyingToMessageInit = replyingToMessage,
                                    onClearEditingMessage = { editingMessage = null },
                                    onClearReplyingToMessage = { replyingToMessage = null },
                                    onPublishMessage = { title, content, cat, risk, img, url, lTitle, pinned, rId, rTitle, rSnippet, rRisk ->
                                        viewModel.publishMessage(
                                            title, content, cat, risk, img, url, lTitle, pinned, rId, rTitle, rSnippet, rRisk
                                        )
                                        replyingToMessage = null
                                    },
                                    onEditMessage = { id, title, content, cat, risk, img, url, lTitle, pinned, rId, rTitle, rSnippet, rRisk ->
                                        viewModel.editMessage(
                                            id, title, content, cat, risk, img, url, lTitle, pinned, rId, rTitle, rSnippet, rRisk
                                        )
                                        editingMessage = null
                                    },
                                    onApproveUserAccount = { uid, trial -> viewModel.approveUserAccount(uid, trial) },
                                    onReviewProfileEdit = { u, approve -> viewModel.reviewProfileEditRequest(u, approve) },
                                    onReviewAccessRequest = { req, approve -> viewModel.reviewAccessRequest(req, approve) },
                                    onResolveAlert = { id, status -> viewModel.resolveModerationAlert(id, status) },
                                    onBanUserFromAlert = { alert -> viewModel.banUserFromModerationAlert(alert) },
                                    onSetUserCommodityMuted = { u, muted -> viewModel.setUserCommodityMuted(u, muted) },
                                    onResetUserDailyQuota = { u -> viewModel.adminResetUserDailyCommodityQuota(u) },
                                    onResetUserPin = { u, pin -> viewModel.adminResetUserPin(u, pin) },
                                    onUpdateUserSubscriptionDays = { uid, plan, days ->
                                        viewModel.updateUserSubscriptionDays(uid, plan, days)
                                    },
                                    onSetDeviceBanStatus = { u, ban, durMillis, durLabel ->
                                        viewModel.setDeviceBanStatus(u, ban, durMillis, durLabel)
                                    },
                                    onSavePricingAndQrConfig = { p1Name, p1Price, p1Days, upi, mobile, p1Qr, p1Only, p2En, p2Name, p2Price, p2Days, p2Qr, p2Only, p3En, p3Name, p3Price, p3Days, p3Qr, p3Only, limit, locked, trialDays, benefits ->
                                        viewModel.savePricingAndQrConfig(
                                            p1Name, p1Price, p1Days, upi, mobile, p1Qr, p1Only,
                                            p2En, p2Name, p2Price, p2Days, p2Qr, p2Only,
                                            p3En, p3Name, p3Price, p3Days, p3Qr, p3Only,
                                            limit, locked, trialDays, benefits
                                        )
                                    },
                                    onAddCoAdminEmail = { viewModel.addCoAdminEmail(it) },
                                    onRemoveCoAdminEmail = { viewModel.removeCoAdminEmail(it) },
                                    onUpdateTodayProfitBannerAndCard = { banner, alsoPostInFeed ->
                                        viewModel.updateTodayProfitBannerAndOptionalCard(banner, alsoPostInFeed)
                                    },
                                    onBulkExtendSubscribers = { extraDays ->
                                        viewModel.bulkExtendActiveSubscribers(extraDays)
                                    },
                                    onClearUnpinnedCommodityMessages = {
                                        viewModel.clearUnpinnedCommodityMessages()
                                    },
                                    isPrimaryMainAdmin = viewModel.isPrimaryMainAdmin,
                                    allDevices = allDevicesForAdmin,
                                    broadcastMessages = broadcastMessages,
                                    onApproveAdminWebLoginWithPassword = { req, approve, mainPass ->
                                        viewModel.approveAdminWebLoginWithPassword(req, approve, mainPass)
                                    },
                                    onAdminUpdateUserDeviceLimit = { u, limit ->
                                        viewModel.adminUpdateUserDeviceLimit(u, limit)
                                    },
                                    onToggleSingleDeviceBlocked = { devId, blocked ->
                                        viewModel.toggleSingleDeviceBlocked(devId, blocked)
                                    },
                                    onRemoveDeviceSession = { devId ->
                                        viewModel.removeDeviceSession(devId)
                                    },
                                    onPushAdminFirmwareToAllDevices = { enabled, ver, notes ->
                                        viewModel.pushAdminFirmwareToAllDevices(enabled, ver, notes)
                                    },
                                    onApplyDailyMessageLimit = { newLimit ->
                                        viewModel.applyDailyMessageLimitNow(newLimit)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Screenshot Policy Warning / Auto-Ban Dialog
    screenshotWarningMessage?.let { warningText ->
        AlertDialog(
            onDismissRequest = { viewModel.clearScreenshotWarning() },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFFEF4444))
                    Text("Screenshot Security Policy", fontWeight = FontWeight.ExtraBold)
                }
            },
            text = {
                Text(
                    text = warningText,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.clearScreenshotWarning() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("I Understand", fontWeight = FontWeight.Bold)
                }
            },
            modifier = Modifier.testTag("screenshot_warning_dialog")
        )
    }

    // Telegram-Style Quick Customization & Admin Tools Drawer Dialog (Opened via "Trading Zone" title or ⋮ button)
    if (showTelegramQuickDrawer) {
        AlertDialog(
            onDismissRequest = { showTelegramQuickDrawer = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = Color(0xFF10B981))
                        Text("Trading Zone Quick Hub", fontWeight = FontWeight.ExtraBold)
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. Theme Skin Customization
                    Text(
                        text = "🎨 Instant Theme Skins",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF10B981)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AppThemePreference.entries.forEach { pref ->
                            FilterChip(
                                selected = themePreference == pref,
                                onClick = {
                                    com.example.util.SoundAndHapticFeedback.playTap(context)
                                    onChangeTheme(pref)
                                },
                                label = { Text(pref.title, maxLines = 1, softWrap = false) }
                            )
                        }
                    }

                    HorizontalDivider()

                    // 2. Font Scale & Card Style Customization
                    Text(
                        text = "🔤 Font Scale & Card Shape",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF10B981)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FontScalePreference.entries.forEach { scaleOpt ->
                            FilterChip(
                                selected = fontScalePreference == scaleOpt,
                                onClick = {
                                    com.example.util.SoundAndHapticFeedback.playTap(context)
                                    viewModel.setFontScalePreference(scaleOpt)
                                    onFontScaleChanged(scaleOpt)
                                },
                                label = { Text(scaleOpt.title, maxLines = 1, softWrap = false) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ChatBubbleStylePreference.entries.forEach { styleOpt ->
                            FilterChip(
                                selected = chatBubbleStylePreference == styleOpt,
                                onClick = {
                                    com.example.util.SoundAndHapticFeedback.playTap(context)
                                    viewModel.setChatBubbleStylePreference(styleOpt)
                                },
                                label = { Text(styleOpt.title, maxLines = 1, softWrap = false) }
                            )
                        }
                    }

                    HorizontalDivider()

                    // 3. Display, Live Ticker, Sound & Notification Customization
                    Text(
                        text = "⚡ Display, Live Market Ticker & Haptics",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF10B981)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Show Top Live Market Ticker", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        Switch(checked = showLiveMarketTicker, onCheckedChange = { viewModel.setShowLiveMarketTicker(it) })
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Compact Feed Layout", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        Switch(checked = compactFeedMode, onCheckedChange = { viewModel.setCompactFeedMode(it) })
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Large Readable Text", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        Switch(checked = largeTextMode, onCheckedChange = { viewModel.setLargeTextMode(it) })
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Trade Signal Haptic Vibrations", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        Switch(checked = highRiskVibrationEnabled, onCheckedChange = { viewModel.setHighRiskVibrationPreference(it) })
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Instant Trade & Approval Notifications", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        Switch(checked = notificationsEnabled, onCheckedChange = { viewModel.setNotificationsPreference(it) })
                    }

                    // 4. Admin Quick Tools (Search & Pin Live Market, Room Lock, Daily Msg Limit, Slow-Mode, Approvals)
                    if (isAdmin) {
                        HorizontalDivider()
                        Text(
                            text = "👑 Admin Quick Controls & Live Market",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFF59E0B)
                        )
                        Button(
                            onClick = {
                                com.example.util.SoundAndHapticFeedback.playTap(context)
                                showTelegramQuickDrawer = false
                                showAdminMarketSearchDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF10B981),
                                contentColor = Color.Black
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("📈 Search & Pin Live Market (Gold, Nifty...)", fontWeight = FontWeight.ExtraBold, maxLines = 1)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (pricingConfig.commodityChatLocked) "Commodity Room Locked" else "Commodity Room Open",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            Switch(
                                checked = pricingConfig.commodityChatLocked,
                                onCheckedChange = { viewModel.setCommodityChatLocked(it) }
                            )
                        }

                        Text(
                            text = "Daily Message Limit per Member:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(1L, 2L, 3L, 5L, 10L).forEach { lim ->
                                FilterChip(
                                    selected = pricingConfig.dailyReviewLimitPerUser == lim,
                                    onClick = {
                                        com.example.util.SoundAndHapticFeedback.playSuccess(context)
                                        viewModel.applyDailyMessageLimitNow(lim)
                                    },
                                    label = { Text("${lim}/day", maxLines = 1, softWrap = false) }
                                )
                            }
                        }

                        val modCfg = pricingConfig.commodityModerationConfig()
                        Text(
                            text = "Commodity Slow-Mode Cooldown:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(0 to "Off", 10 to "10s", 30 to "30s", 60 to "60s").forEach { (sec, label) ->
                                FilterChip(
                                    selected = modCfg.slowModeSeconds == sec,
                                    onClick = {
                                        com.example.util.SoundAndHapticFeedback.playTap(context)
                                        viewModel.updateCommodityModerationRules(
                                            slowModeSeconds = sec,
                                            blockExternalLinks = modCfg.blockExternalLinks,
                                            customBlockedWordsCsv = modCfg.customBlockedWordsCsv
                                        )
                                    },
                                    label = { Text(label, maxLines = 1, softWrap = false) }
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    showTelegramQuickDrawer = false
                                    selectedTab = MainNavTab.ADMIN
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = if (totalPendingAdminApprovals > 0) "🔔 Approvals ($totalPendingAdminApprovals)" else "Admin Studio",
                                    maxLines = 1
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    showTelegramQuickDrawer = false
                                    selectedTab = MainNavTab.SETTINGS
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Full Settings", maxLines = 1)
                            }
                        }
                    }

                    HorizontalDivider()

                    // 5. Simplified 2-Button Share (Website & Android App)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                showTelegramQuickDrawer = false
                                showQuickQrShareDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF10B981),
                                contentColor = Color.Black
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Website & APK QR", fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showTelegramQuickDrawer = false }) {
                    Text("Close")
                }
            },
            modifier = Modifier.testTag("telegram_quick_customization_drawer")
        )
    }

    // Investing.com-Style Admin Market Search & Pin Modal
    if (showAdminMarketSearchDialog && isAdmin) {
        AdminMarketSearchAndPinDialog(
            allQuotes = liveMarketQuotes,
            onTogglePinSymbol = { symbolId, pin ->
                viewModel.togglePinnedMarketSymbol(symbolId, pin)
            },
            onSetCustomPriceOverride = { symbolId, price ->
                viewModel.setCustomMarketBenchmarkOverride(symbolId, price)
            },
            onSelectQuoteForDetail = { quote ->
                selectedMarketQuoteForDetail = quote
            },
            onOpenTradeSetupForQuote = { quote, sigType ->
                activeTradeSetupRequest = Triple(quote, sigType, "15m")
            },
            onDismiss = { showAdminMarketSearchDialog = false }
        )
    }

    // Investing.com-Style Live Market Instrument Detail Dialog (Live-ticking multi-timeframe candlesticks)
    selectedMarketQuoteForDetail?.let { initialQuote ->
        val liveQuote = liveMarketQuotes.firstOrNull { it.symbolId == initialQuote.symbolId } ?: initialQuote
        LiveMarketDetailDialog(
            quote = liveQuote,
            isAdmin = isAdmin,
            onTogglePin = {
                viewModel.togglePinnedMarketSymbol(liveQuote.symbolId, !liveQuote.isPinned)
                selectedMarketQuoteForDetail = liveQuote.copy(isPinned = !liveQuote.isPinned)
            },
            onOpenTradeSetupPage = { sigType, tf ->
                selectedMarketQuoteForDetail = null
                activeTradeSetupRequest = Triple(liveQuote, sigType, tf)
            },
            onDismiss = { selectedMarketQuoteForDetail = null }
        )
    }

    // Dedicated "Set Target & Write Message" Page for Live Market Calls
    activeTradeSetupRequest?.let { (initialQuote, initialSig, initialTf) ->
        val tierOpts = remember(pricingConfig) {
            val dynamicPlans = pricingConfig.availablePlans().map { it.name to "🎯 ${it.name}" }
            listOf(
                "ALL" to "🌐 All Members",
                "PAID_ONLY" to "💎 Paid Members Only"
            ) + dynamicPlans
        }
        LiveMarketTradeSetupDialog(
            allQuotes = liveMarketQuotes,
            initialQuote = initialQuote,
            initialSignalType = initialSig,
            initialTimeframe = initialTf,
            availableTiers = tierOpts,
            onPublishToBroadcast = { title, formattedContentWithSetup, category, riskLevel, targetTier, signalType ->
                val encodedLinkTitle = BroadcastMessage.encodeLinkTitleWithMeta(
                    plainLinkTitle = "",
                    targetTier = targetTier,
                    signalType = signalType
                )
                viewModel.publishMessage(
                    title = title,
                    content = formattedContentWithSetup,
                    category = category,
                    riskLevel = riskLevel,
                    markedPhotoBase64 = "",
                    linkUrl = "",
                    linkTitle = encodedLinkTitle,
                    isPinned = false,
                    replyToMessageId = "",
                    replyToTitle = "",
                    replyToSnippet = "",
                    replyToRiskLevel = ""
                )
                activeTradeSetupRequest = null
            },
            onPostToCommodity = { topicTag, formattedOpinionWithSetup ->
                viewModel.submitCommunityReview(
                    tradeMessageId = "commodity_general",
                    tradeMessageTitle = "Live Market Setup • $topicTag",
                    tradeRiskLevel = "MEDIUM",
                    topicTag = topicTag,
                    opinionText = formattedOpinionWithSetup,
                    rating = 5
                )
                activeTradeSetupRequest = null
            },
            onDismiss = { activeTradeSetupRequest = null }
        )
    }

    if (showQuickQrShareDialog) {
        AlertDialog(
            onDismissRequest = { showQuickQrShareDialog = false },
            title = { Text("Share Website & Android App") },
            text = {
                PermanentQrAndShareCard(
                    appleWebAppUrl = appleWebAppUrl,
                    androidApkUrl = androidApkShareUrl,
                    isAdmin = isAdmin,
                    onUpdateUrls = { appleUrl, androidUrl ->
                        viewModel.updateShareUrls(appleUrl, androidUrl)
                    }
                )
            },
            confirmButton = {
                Button(onClick = { showQuickQrShareDialog = false }) {
                    Text("Done")
                }
            }
        )
    }
}
