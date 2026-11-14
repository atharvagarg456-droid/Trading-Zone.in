package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.style.TextOverflow
import com.example.data.model.AccessRequest
import com.example.data.model.AdminSettings
import com.example.data.model.BroadcastMessage
import com.example.data.model.CustomPlanItem
import com.example.data.model.DeviceRecord
import com.example.data.model.ModerationAlert
import com.example.data.model.PricingConfig
import com.example.data.model.TodayProfitBanner
import com.example.data.model.UserProfile
import com.example.ui.components.ImageCodecHelper
import com.example.ui.components.PermanentQrAndShareCard
import com.example.ui.components.PhotoMarkingDialog
import com.example.ui.components.SwipeableGalleryItem
import com.example.ui.components.SwipeablePhotoAndProfileDialog
import com.example.ui.components.toSwipeableGalleryItem
import com.example.util.SoundAndHapticFeedback

private val EmeraldColor = Color(0xFF10B981)
private val AmberColor = Color(0xFFF59E0B)
private val CrimsonColor = Color(0xFFEF4444)
private val ElectricBlueColor = Color(0xFF3B82F6)

@Composable
fun AdminConsoleScreen(
    allUsers: List<UserProfile>,
    allRequests: List<AccessRequest>,
    moderationAlerts: List<ModerationAlert>,
    pricingConfig: PricingConfig,
    adminSettings: AdminSettings,
    appleWebAppUrl: String,
    androidApkShareUrl: String,
    onUpdateShareUrls: (appleUrl: String, androidUrl: String) -> Unit,
    editingMessageInit: BroadcastMessage?,
    replyingToMessageInit: BroadcastMessage?,
    onClearEditingMessage: () -> Unit,
    onClearReplyingToMessage: () -> Unit,
    onPublishMessage: (
        title: String,
        content: String,
        category: String,
        riskLevel: String,
        markedPhotoBase64: String,
        linkUrl: String,
        linkTitle: String,
        isPinned: Boolean,
        replyToMessageId: String,
        replyToTitle: String,
        replyToSnippet: String,
        replyToRiskLevel: String
    ) -> Unit,
    onEditMessage: (
        messageId: String,
        title: String,
        content: String,
        category: String,
        riskLevel: String,
        markedPhotoBase64: String,
        linkUrl: String,
        linkTitle: String,
        isPinned: Boolean,
        replyToMessageId: String,
        replyToTitle: String,
        replyToSnippet: String,
        replyToRiskLevel: String
    ) -> Unit,
    onApproveUserAccount: (userId: String, grantSevenDayTrial: Boolean) -> Unit,
    onReviewProfileEdit: (user: UserProfile, approve: Boolean) -> Unit,
    onReviewAccessRequest: (request: AccessRequest, approve: Boolean) -> Unit,
    onResolveAlert: (alertId: String, status: String) -> Unit,
    onBanUserFromAlert: (ModerationAlert) -> Unit,
    onSetUserCommodityMuted: (user: UserProfile, muted: Boolean) -> Unit,
    onResetUserDailyQuota: (UserProfile) -> Unit,
    onResetUserPin: (user: UserProfile, newPin: String) -> Unit,
    onUpdateUserSubscriptionDays: (userId: String, planName: String, daysLeft: Int) -> Unit,
    onSetDeviceBanStatus: (user: UserProfile, ban: Boolean) -> Unit,
    onSavePricingAndQrConfig: (
        planName: String,
        priceInr: Double,
        validityDays: Long,
        upiId: String,
        upiMobileNumber: String,
        qrCodeBase64: String,
        qrOnlyMode: Boolean,
        plan2Enabled: Boolean,
        plan2Name: String,
        plan2PriceInr: Double,
        plan2ValidityDays: Long,
        plan2QrCodeBase64: String,
        plan2QrOnlyMode: Boolean,
        plan3Enabled: Boolean,
        plan3Name: String,
        plan3PriceInr: Double,
        plan3ValidityDays: Long,
        plan3QrCodeBase64: String,
        plan3QrOnlyMode: Boolean,
        dailyReviewLimitPerUser: Long,
        commodityChatLocked: Boolean,
        trialDays: Long,
        benefits: List<String>
    ) -> Unit,
    onAddCoAdminEmail: (String) -> Unit,
    onRemoveCoAdminEmail: (String) -> Unit,
    onUpdateTodayProfitBannerAndCard: (TodayProfitBanner, Boolean) -> Unit = { _, _ -> },
    onBulkExtendSubscribers: (Int) -> Unit = {},
    onClearUnpinnedCommodityMessages: () -> Unit = {},
    isPrimaryMainAdmin: Boolean = false,
    allDevices: List<DeviceRecord> = emptyList(),
    broadcastMessages: List<BroadcastMessage> = emptyList(),
    onApproveAdminWebLoginWithPassword: (AccessRequest, Boolean, String) -> Boolean = { _, _, _ -> false },
    onAdminUpdateUserDeviceLimit: (UserProfile, Int) -> Unit = { _, _ -> },
    onToggleSingleDeviceBlocked: (String, Boolean) -> Unit = { _, _ -> },
    onRemoveDeviceSession: (String) -> Unit = {},
    onPushAdminFirmwareToAllDevices: (enabled: Boolean, versionName: String, releaseNotes: String) -> Unit = { _, _, _ -> },
    onApplyDailyMessageLimit: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(editingMessageInit, replyingToMessageInit) {
        if (editingMessageInit != null || replyingToMessageInit != null) {
            selectedTab = 0
        }
    }

    val pendingRegistrations = remember(allUsers) {
        allUsers.filter { it.accountStatus == "PENDING_APPROVAL" }
    }
    val pendingProfileEdits = remember(allUsers) {
        allUsers.filter { it.hasPendingProfileEdit }
    }
    val pendingRequests = remember(allRequests) {
        allRequests.filter { it.status == "PENDING" }
    }
    val unresolvedAlerts = remember(moderationAlerts) {
        moderationAlerts.filter { it.status == "OPEN" }
    }
    val totalApprovalsBadge = pendingRegistrations.size + pendingProfileEdits.size + pendingRequests.size

    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 12.dp,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = {
                    SoundAndHapticFeedback.playTap(context)
                    selectedTab = 0
                },
                text = { Text("Publish Message") },
                icon = { Icon(Icons.Default.Campaign, contentDescription = null) },
                modifier = Modifier.testTag("admin_tab_publish")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = {
                    SoundAndHapticFeedback.playTap(context)
                    selectedTab = 1
                },
                text = {
                    Text(
                        if (totalApprovalsBadge > 0) "Approvals ($totalApprovalsBadge)" else "Approvals"
                    )
                },
                icon = { Icon(Icons.Default.FactCheck, contentDescription = null) },
                modifier = Modifier.testTag("admin_tab_approvals")
            )
            Tab(
                selected = selectedTab == 2,
                onClick = {
                    SoundAndHapticFeedback.playTap(context)
                    selectedTab = 2
                },
                text = {
                    Text(
                        if (unresolvedAlerts.isNotEmpty()) "Alerts (${unresolvedAlerts.size})" else "Alerts"
                    )
                },
                icon = { Icon(Icons.Default.Shield, contentDescription = null) },
                modifier = Modifier.testTag("admin_tab_alerts")
            )
            Tab(
                selected = selectedTab == 3,
                onClick = {
                    SoundAndHapticFeedback.playTap(context)
                    selectedTab = 3
                },
                text = { Text("Users & Devices (${allUsers.size})") },
                icon = { Icon(Icons.Default.People, contentDescription = null) },
                modifier = Modifier.testTag("admin_tab_users")
            )
            Tab(
                selected = selectedTab == 4,
                onClick = {
                    SoundAndHapticFeedback.playTap(context)
                    selectedTab = 4
                },
                text = { Text("Plans & Controls") },
                icon = { Icon(Icons.Default.Tune, contentDescription = null) },
                modifier = Modifier.testTag("admin_tab_plans")
            )
        }

        when (selectedTab) {
            0 -> AdminPublishMessageTab(
                pricingConfig = pricingConfig,
                editingMessageInit = editingMessageInit,
                replyingToMessageInit = replyingToMessageInit,
                onClearEditingMessage = onClearEditingMessage,
                onClearReplyingToMessage = onClearReplyingToMessage,
                onPublishMessage = onPublishMessage,
                onEditMessage = onEditMessage
            )
            1 -> AdminApprovalsTab(
                isPrimaryMainAdmin = isPrimaryMainAdmin,
                pendingRegistrations = pendingRegistrations,
                pendingProfileEdits = pendingProfileEdits,
                pendingRequests = pendingRequests,
                onApproveUserAccount = onApproveUserAccount,
                onReviewProfileEdit = onReviewProfileEdit,
                onReviewAccessRequest = onReviewAccessRequest,
                onApproveAdminWebLoginWithPassword = onApproveAdminWebLoginWithPassword
            )
            2 -> AdminModerationAlertsTab(
                alerts = moderationAlerts,
                allUsers = allUsers,
                onResolveAlert = onResolveAlert,
                onMuteUser = onSetUserCommodityMuted,
                onBanUserFromAlert = onBanUserFromAlert,
                onUnbanUserAndDevice = { user -> onSetDeviceBanStatus(user, false) }
            )
            3 -> AdminUsersInspectorTab(
                allUsers = allUsers,
                allDevices = allDevices,
                onSetUserCommodityMuted = onSetUserCommodityMuted,
                onResetUserDailyQuota = onResetUserDailyQuota,
                onResetUserPin = onResetUserPin,
                onUpdateUserSubscriptionDays = onUpdateUserSubscriptionDays,
                onSetDeviceBanStatus = onSetDeviceBanStatus,
                onAdminUpdateUserDeviceLimit = onAdminUpdateUserDeviceLimit,
                onToggleSingleDeviceBlocked = onToggleSingleDeviceBlocked,
                onRemoveDeviceSession = onRemoveDeviceSession
            )
            4 -> AdminPlansAndConfigTab(
                pricingConfig = pricingConfig,
                adminSettings = adminSettings,
                broadcastMessages = broadcastMessages,
                appleWebAppUrl = appleWebAppUrl,
                androidApkShareUrl = androidApkShareUrl,
                onUpdateShareUrls = onUpdateShareUrls,
                onSavePricingAndQrConfig = onSavePricingAndQrConfig,
                onAddCoAdminEmail = onAddCoAdminEmail,
                onRemoveCoAdminEmail = onRemoveCoAdminEmail,
                onUpdateTodayProfitBannerAndCard = onUpdateTodayProfitBannerAndCard,
                onBulkExtendSubscribers = onBulkExtendSubscribers,
                onClearUnpinnedCommodityMessages = onClearUnpinnedCommodityMessages,
                onPushAdminFirmwareToAllDevices = onPushAdminFirmwareToAllDevices,
                onApplyDailyMessageLimit = onApplyDailyMessageLimit
            )
        }
    }
}

@Composable
private fun AdminPublishMessageTab(
    pricingConfig: PricingConfig,
    editingMessageInit: BroadcastMessage?,
    replyingToMessageInit: BroadcastMessage?,
    onClearEditingMessage: () -> Unit,
    onClearReplyingToMessage: () -> Unit,
    onPublishMessage: (
        title: String,
        content: String,
        category: String,
        riskLevel: String,
        markedPhotoBase64: String,
        linkUrl: String,
        linkTitle: String,
        isPinned: Boolean,
        replyToMessageId: String,
        replyToTitle: String,
        replyToSnippet: String,
        replyToRiskLevel: String
    ) -> Unit,
    onEditMessage: (
        messageId: String,
        title: String,
        content: String,
        category: String,
        riskLevel: String,
        markedPhotoBase64: String,
        linkUrl: String,
        linkTitle: String,
        isPinned: Boolean,
        replyToMessageId: String,
        replyToTitle: String,
        replyToSnippet: String,
        replyToRiskLevel: String
    ) -> Unit
) {
    val context = LocalContext.current
    var title by remember(editingMessageInit) { mutableStateOf(editingMessageInit?.title ?: "") }
    var content by remember(editingMessageInit) { mutableStateOf(editingMessageInit?.content ?: "") }
    var category by remember(editingMessageInit) {
        mutableStateOf(
            if (editingMessageInit != null && editingMessageInit.hasDisplayCategory()) {
                editingMessageInit.category
            } else {
                "NONE"
            }
        )
    }
    var riskLevel by remember(editingMessageInit) {
        mutableStateOf(
            if (editingMessageInit != null && editingMessageInit.hasDisplayRiskLevel()) {
                editingMessageInit.riskLevel
            } else {
                "NONE"
            }
        )
    }
    var markedPhotoBase64 by remember(editingMessageInit) {
        mutableStateOf(editingMessageInit?.markedPhotoBase64 ?: "")
    }
    var linkUrl by remember(editingMessageInit) { mutableStateOf(editingMessageInit?.linkUrl ?: "") }
    var linkTitle by remember(editingMessageInit) {
        mutableStateOf(editingMessageInit?.displayLinkTitle() ?: "")
    }
    var selectedTargetTier by remember(editingMessageInit) {
        mutableStateOf(editingMessageInit?.targetMembershipTier() ?: "ALL")
    }
    var selectedSignalType by remember(editingMessageInit) {
        mutableStateOf(editingMessageInit?.signalHapticType() ?: "AUTO")
    }
    var isPinned by remember(editingMessageInit) { mutableStateOf(editingMessageInit?.isPinned ?: false) }
    var showMarkupDialog by remember { mutableStateOf(false) }

    val tierOptions = remember(pricingConfig) {
        buildList {
            add("ALL" to "👥 Send to All Members")
            add("PAID_ONLY" to "💎 Paid Members Only (No Free Trial)")
            add("VIP_HIGH_TIER" to "👑 VIP High-Tier Plans Only")
            pricingConfig.allPlansForAdmin().forEach { plan ->
                add(plan.name to "📋 ${plan.name} (₹${plan.priceInr.toInt()})")
            }
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val encoded = ImageCodecHelper.encodeUriToBase64Jpeg(
                context = context,
                uri = uri,
                maxDimension = 900,
                quality = 75
            )
            if (encoded != null) {
                markedPhotoBase64 = encoded
            }
        }
    }

    val chartBitmap = remember(markedPhotoBase64) {
        ImageCodecHelper.decodeBase64ToBitmap(markedPhotoBase64)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (editingMessageInit != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = ElectricBlueColor.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, ElectricBlueColor.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Editing Message: ${editingMessageInit.title}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = onClearEditingMessage) {
                        Text("Cancel Edit")
                    }
                }
            }
        }

        // WhatsApp-style Quoted Reply Banner
        if (replyingToMessageInit != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = EmeraldColor.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, EmeraldColor.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Reply,
                        contentDescription = "Replying To",
                        tint = EmeraldColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Replying to: ${replyingToMessageInit.title}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldColor
                        )
                        Text(
                            text = replyingToMessageInit.content.take(90),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2
                        )
                    }
                    IconButton(onClick = onClearReplyingToMessage) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel Reply")
                    }
                }
            }
        }

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Broadcast Headline (e.g., GOLD MCX / NIFTY UPDATE)") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_message_title_input")
        )

        OutlinedTextField(
            value = content,
            onValueChange = { content = it },
            label = { Text("Full Message Content (Hidden until member clicks to open)") },
            minLines = 4,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_message_body_input")
        )

        // TARGET MEMBERSHIP TIER SELECTOR ("Send to All" or "Select Membership Type")
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = AmberColor.copy(alpha = 0.10f),
            border = BorderStroke(1.dp, AmberColor.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "👑 Target Membership Audience (Who Can Unlock This Signal)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = AmberColor
                )
                Text(
                    text = "Lower-priced or trial members outside your selected tier will see a Locked VIP Card prompting them to upgrade their plan.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tierOptions.forEach { (tierKey, tierLabel) ->
                        FilterChip(
                            selected = selectedTargetTier == tierKey,
                            onClick = {
                                SoundAndHapticFeedback.playClick(context)
                                selectedTargetTier = tierKey
                            },
                            label = {
                                Text(
                                    text = tierLabel,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            },
                            modifier = Modifier.testTag("target_tier_chip_${tierKey.replace(" ", "_")}")
                        )
                    }
                }
            }
        }

        // TRADE SIGNAL HAPTIC & SOUND SEQUENCE SELECTOR (BUY / SELL / TARGET HIT)
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = EmeraldColor.copy(alpha = 0.10f),
            border = BorderStroke(1.dp, EmeraldColor.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "🔊 Trade Signal Haptic & Sound Sequence (BUY / SELL / TARGET HIT)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = EmeraldColor
                )
                Text(
                    text = "Triggers a distinct vibration & audio chord sequence on Android and Website when members open this signal.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "AUTO" to "⚡ Auto-Detect",
                        "BUY" to "🟢 BUY Signal",
                        "SELL" to "🔴 SELL Signal",
                        "TARGET HIT" to "🎯 TARGET HIT"
                    ).forEach { (sigKey, sigLabel) ->
                        FilterChip(
                            selected = selectedSignalType == sigKey,
                            onClick = {
                                selectedSignalType = sigKey
                                if (sigKey != "AUTO") {
                                    SoundAndHapticFeedback.playTradeSignal(context, sigKey)
                                } else {
                                    SoundAndHapticFeedback.playClick(context)
                                }
                            },
                            label = {
                                Text(
                                    text = sigLabel,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            },
                            modifier = Modifier.testTag("signal_haptic_chip_${sigKey.replace(" ", "_")}")
                        )
                    }
                }
            }
        }

        var showAdvancedOptions by remember { mutableStateOf(false) }
        OutlinedButton(
            onClick = {
                SoundAndHapticFeedback.playTap(context)
                showAdvancedOptions = !showAdvancedOptions
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = if (showAdvancedOptions) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (showAdvancedOptions) {
                    "Hide Optional Category, Risk & Pin Settings"
                } else {
                    "⚙️ Optional Category, Risk Level & Pin Settings"
                },
                fontWeight = FontWeight.SemiBold
            )
        }

        AnimatedVisibility(visible = showAdvancedOptions) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Category (Optional — select None to hide badge)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "NONE" to "None (Optional)",
                        "INTRADAY" to "INTRADAY",
                        "SWING" to "SWING",
                        "OPTIONS" to "OPTIONS",
                        "MCX" to "MCX",
                        "COMMODITY" to "COMMODITY"
                    ).forEach { (catKey, catLabel) ->
                        FilterChip(
                            selected = category == catKey,
                            onClick = { category = catKey },
                            label = { Text(catLabel) }
                        )
                    }
                }

                Text(
                    "Broadcast Risk Level (Optional — select None to hide badge)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "NONE" to "None (Optional)",
                        "LOW" to "LOW",
                        "MEDIUM" to "MEDIUM",
                        "HIGH" to "HIGH"
                    ).forEach { (riskKey, riskLabel) ->
                        FilterChip(
                            selected = riskLevel == riskKey,
                            onClick = { riskLevel = riskKey },
                            label = { Text(riskLabel) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Pin Message to Top of Stream", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Switch(checked = isPinned, onCheckedChange = { isPinned = it })
                }
            }
        }

        if (chartBitmap != null) {
            Image(
                bitmap = chartBitmap.asImageBitmap(),
                contentDescription = "Attached Chart",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { showMarkupDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Brush, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Annotate / Draw on Chart")
                }
                OutlinedButton(
                    onClick = { markedPhotoBase64 = "" },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Remove Photo")
                }
            }
        } else {
            OutlinedButton(
                onClick = {
                    imagePicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Attach Chart / Market Photo")
            }
        }

        Button(
            onClick = {
                if (title.isBlank() || content.isBlank()) return@Button
                val encodedLinkTitle = BroadcastMessage.encodeLinkTitleWithMeta(
                    plainLinkTitle = linkTitle.trim(),
                    targetTier = selectedTargetTier,
                    signalType = selectedSignalType
                )
                val resolvedSignal = if (selectedSignalType == "AUTO") {
                    SoundAndHapticFeedback.detectSignalType(title, content)
                } else {
                    selectedSignalType
                }
                if (resolvedSignal.isNotBlank()) {
                    SoundAndHapticFeedback.playTradeSignal(context, resolvedSignal)
                } else {
                    SoundAndHapticFeedback.playSuccess(context)
                }
                if (editingMessageInit != null) {
                    onEditMessage(
                        editingMessageInit.id,
                        title.trim(),
                        content.trim(),
                        category,
                        riskLevel,
                        markedPhotoBase64,
                        linkUrl.trim(),
                        encodedLinkTitle,
                        isPinned,
                        editingMessageInit.replyToMessageId,
                        editingMessageInit.replyToTitle,
                        editingMessageInit.replyToSnippet,
                        editingMessageInit.replyToRiskLevel
                    )
                } else {
                    onPublishMessage(
                        title.trim(),
                        content.trim(),
                        category,
                        riskLevel,
                        markedPhotoBase64,
                        linkUrl.trim(),
                        encodedLinkTitle,
                        isPinned,
                        replyingToMessageInit?.id ?: "",
                        replyingToMessageInit?.title ?: "",
                        replyingToMessageInit?.content?.take(100) ?: "",
                        replyingToMessageInit?.riskLevel ?: ""
                    )
                }
                title = ""
                content = ""
                markedPhotoBase64 = ""
                linkUrl = ""
                linkTitle = ""
                selectedTargetTier = "ALL"
                selectedSignalType = "AUTO"
                isPinned = false
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("admin_publish_submit_button")
        ) {
            Icon(Icons.Default.Send, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (editingMessageInit != null) "Save Updated Broadcast" else "Publish Masked Broadcast Alert",
                fontWeight = FontWeight.Bold
            )
        }
    }

    if (showMarkupDialog && chartBitmap != null) {
        PhotoMarkingDialog(
            sourceBitmap = chartBitmap,
            onDismiss = { showMarkupDialog = false },
            onSaveMarkedPhoto = { annotated ->
                markedPhotoBase64 = annotated
                showMarkupDialog = false
            }
        )
    }
}

@Composable
private fun AdminApprovalsTab(
    isPrimaryMainAdmin: Boolean,
    pendingRegistrations: List<UserProfile>,
    pendingProfileEdits: List<UserProfile>,
    pendingRequests: List<AccessRequest>,
    onApproveUserAccount: (userId: String, grantSevenDayTrial: Boolean) -> Unit,
    onReviewProfileEdit: (user: UserProfile, approve: Boolean) -> Unit,
    onReviewAccessRequest: (request: AccessRequest, approve: Boolean) -> Unit,
    onApproveAdminWebLoginWithPassword: (AccessRequest, Boolean, String) -> Boolean
) {
    val context = LocalContext.current
    var inspectedScreenshot by remember { mutableStateOf<String?>(null) }
    val adminWebAuthRequests = remember(pendingRequests) {
        pendingRequests.filter { it.isAdminWebAuthOnly() }
    }
    val unbanAppealRequests = remember(pendingRequests) {
        pendingRequests.filter { it.isUnbanAppealRequest() }
    }
    val paymentAndTrialRequests = remember(pendingRequests) {
        pendingRequests.filter { it.requestType != "ADMIN_WEB_AUTH" }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Section 0A: Second-Chance Unban Appeals (Banned by Mistake)
        if (unbanAppealRequests.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = EmeraldColor.copy(alpha = 0.12f)
                    ),
                    border = BorderStroke(1.5.dp, EmeraldColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_unban_appeals_card")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "🙏 Second-Chance Unban Appeals (${unbanAppealRequests.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = EmeraldColor
                        )
                        Text(
                            text = "Members or hardware devices banned by mistake requesting a second chance:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        unbanAppealRequests.forEach { appealReq ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, EmeraldColor.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "${appealReq.userName} (${appealReq.userEmail})",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Device ID: ${appealReq.deviceId} • IP: ${appealReq.ipAddress}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Appeal Note: \"${appealReq.extractUnbanAppealReason()}\"",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AmberColor,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = {
                                                SoundAndHapticFeedback.playSuccess(context)
                                                onReviewAccessRequest(appealReq, true)
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = EmeraldColor,
                                                contentColor = Color.Black
                                            ),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("✅ Unban & Give 2nd Chance", fontWeight = FontWeight.Bold)
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                SoundAndHapticFeedback.playAlertOrError(context)
                                                onReviewAccessRequest(appealReq, false)
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Reject")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 0: Admin & Co-Admin Website Login Verification (Main Admin + App Password Required)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (adminWebAuthRequests.isNotEmpty()) {
                        AmberColor.copy(alpha = 0.14f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                ),
                border = BorderStroke(
                    1.5.dp,
                    if (adminWebAuthRequests.isNotEmpty()) AmberColor else EmeraldColor.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_web_login_verification_card")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Main Admin Web Verification",
                            tint = if (adminWebAuthRequests.isNotEmpty()) AmberColor else EmeraldColor
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Admin / Co-Admin Website Login Verification (${adminWebAuthRequests.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "When an Admin or Co-Admin logs in on the Website, the Main Admin must verify and approve it here using the Main Admin App Password.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (adminWebAuthRequests.isEmpty()) {
                        Text(
                            text = "No pending Admin or Co-Admin Website login requests right now.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        adminWebAuthRequests.forEach { webReq ->
                            var mainAdminPassInput by remember(webReq.id) { mutableStateOf("") }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, AmberColor.copy(alpha = 0.6f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Web Login Request: ${webReq.userName} (${webReq.userEmail})",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Browser / Device: ${webReq.deviceId} • Ref: ${webReq.utrReference}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    if (isPrimaryMainAdmin) {
                                        OutlinedTextField(
                                            value = mainAdminPassInput,
                                            onValueChange = { mainAdminPassInput = it },
                                            label = { Text("Enter Main Admin App Password to Verify") },
                                            visualTransformation = PasswordVisualTransformation(),
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(
                                                onClick = {
                                                    val ok = onApproveAdminWebLoginWithPassword(
                                                        webReq,
                                                        true,
                                                        mainAdminPassInput
                                                    )
                                                    if (ok) {
                                                        SoundAndHapticFeedback.playSuccess(context)
                                                        mainAdminPassInput = ""
                                                    } else {
                                                        SoundAndHapticFeedback.playAlertOrError(context)
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = EmeraldColor,
                                                    contentColor = Color.Black
                                                ),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Verify & Authorize Web", fontWeight = FontWeight.Bold)
                                            }
                                            OutlinedButton(
                                                onClick = {
                                                    SoundAndHapticFeedback.playAlertOrError(context)
                                                    onApproveAdminWebLoginWithPassword(webReq, false, mainAdminPassInput)
                                                },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Block Web Login")
                                            }
                                        }
                                    } else {
                                        Text(
                                            text = "🔒 Only the Main Admin (Owner) can verify and approve Admin/Co-Admin Website logins from the official Android app.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = CrimsonColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section A: New Member Registrations
        item {
            Text(
                text = "New Member Registration Queue (${pendingRegistrations.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        if (pendingRegistrations.isEmpty()) {
            item {
                Text(
                    text = "No new member registrations awaiting approval.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(pendingRegistrations, key = { "reg_${it.userId}" }) { user ->
                val userPhotoBmp = remember(user.photoBase64) {
                    ImageCodecHelper.decodeBase64ToBitmap(user.photoBase64)
                }
                Card(
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (userPhotoBmp != null) {
                                Image(
                                    bitmap = userPhotoBmp.asImageBitmap(),
                                    contentDescription = "User Photo",
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(user.fullName.ifBlank { user.email }, fontWeight = FontWeight.Bold)
                                Text("${user.email} • ${user.phoneNumber}", style = MaterialTheme.typography.bodySmall)
                                Text("City: ${user.city} • IP: ${user.ipAddress}", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    SoundAndHapticFeedback.playSuccess(context)
                                    onApproveUserAccount(user.userId, false)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldColor, contentColor = Color.Black),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Approve", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = {
                                    SoundAndHapticFeedback.playSuccess(context)
                                    onApproveUserAccount(user.userId, true)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Approve + 7d Trial")
                            }
                        }
                    }
                }
            }
        }

        // Section B: Profile Edit Requests
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Profile Name & Photo Edit Requests (${pendingProfileEdits.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        if (pendingProfileEdits.isEmpty()) {
            item {
                Text(
                    text = "No pending profile or photo change requests.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(pendingProfileEdits, key = { "edit_${it.userId}" }) { user ->
                val pendingBmp = remember(user.pendingPhotoBase64) {
                    ImageCodecHelper.decodeBase64ToBitmap(user.pendingPhotoBase64)
                }
                Card(
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Current: ${user.fullName} (${user.phoneNumber})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Requested: ${user.pendingFullName} • ${user.pendingPhoneNumber} • ${user.pendingCity}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (pendingBmp != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Image(
                                bitmap = pendingBmp.asImageBitmap(),
                                contentDescription = "Requested Photo",
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    SoundAndHapticFeedback.playSuccess(context)
                                    onReviewProfileEdit(user, true)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldColor, contentColor = Color.Black),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Approve Edit", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = {
                                    SoundAndHapticFeedback.playAlertOrError(context)
                                    onReviewProfileEdit(user, false)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Reject Edit")
                            }
                        }
                    }
                }
            }
        }

        // Section C: Trial & UPI QR Payment Requests
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Subscription & 7-Day Trial Requests (${paymentAndTrialRequests.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        if (paymentAndTrialRequests.isEmpty()) {
            item {
                Text(
                    text = "No pending subscription or trial requests.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(paymentAndTrialRequests, key = { "sub_${it.id}" }) { req ->
                val proofBmp = remember(req.paymentScreenshotBase64) {
                    ImageCodecHelper.decodeBase64ToBitmap(req.paymentScreenshotBase64)
                }
                Card(
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "${req.userName} (${req.userEmail})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Plan: ${req.planName} • ₹${req.amountInr.toInt()} • ${req.validityDays} Days",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "UTR / Ref: ${req.utrReference}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (proofBmp != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Image(
                                bitmap = proofBmp.asImageBitmap(),
                                contentDescription = "Payment Proof",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                                    .clip(RoundedCornerShape(10.dp)),
                                contentScale = ContentScale.Crop
                            )
                            TextButton(onClick = { inspectedScreenshot = req.paymentScreenshotBase64 }) {
                                Icon(Icons.Default.ZoomIn, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Inspect Full Payment Screenshot")
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    SoundAndHapticFeedback.playSuccess(context)
                                    onReviewAccessRequest(req, true)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldColor, contentColor = Color.Black),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Approve (${req.validityDays}d)", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = {
                                    SoundAndHapticFeedback.playAlertOrError(context)
                                    onReviewAccessRequest(req, false)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Reject")
                            }
                        }
                    }
                }
            }
        }
    }

    inspectedScreenshot?.let { screenshot ->
        val fullBmp = remember(screenshot) { ImageCodecHelper.decodeBase64ToBitmap(screenshot) }
        AlertDialog(
            onDismissRequest = { inspectedScreenshot = null },
            title = { Text("UPI Payment Proof Inspector") },
            text = {
                if (fullBmp != null) {
                    Image(
                        bitmap = fullBmp.asImageBitmap(),
                        contentDescription = "Full Payment Screenshot",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(380.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit
                    )
                }
            },
            confirmButton = {
                Button(onClick = { inspectedScreenshot = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun AdminModerationAlertsTab(
    alerts: List<ModerationAlert>,
    allUsers: List<UserProfile>,
    onResolveAlert: (alertId: String, status: String) -> Unit,
    onMuteUser: (user: UserProfile, muted: Boolean) -> Unit,
    onBanUserFromAlert: (ModerationAlert) -> Unit,
    onUnbanUserAndDevice: (UserProfile) -> Unit = {}
) {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Blocked Phone/ID & Moderation Alerts (${alerts.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Includes automatic blocks when a user tries to share a phone number, email, UPI ID, or social handle in Commodity. If banned by mistake, tap 'Unban & 2nd Chance'.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(alerts, key = { it.id }) { alert ->
            val isResolved = alert.status != "OPEN"
            val matchedUser = allUsers.find { it.userId == alert.userId }
            val isCurrentlyBanned = alert.status == "BANNED" || matchedUser?.accountStatus == "BANNED"
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isResolved) {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    } else {
                        CrimsonColor.copy(alpha = 0.12f)
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "${alert.userName} (${alert.userEmail})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Attempted Content: \"${alert.originalUneditedText}\"",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Triggered Pattern: ${alert.detectedWords} • Device: ${alert.deviceId} • Status: ${alert.status}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isCurrentlyBanned) AmberColor else CrimsonColor
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (matchedUser != null) {
                            OutlinedButton(
                                onClick = { onMuteUser(matchedUser, !matchedUser.isCommodityMuted) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (matchedUser.isCommodityMuted) "Unmute" else "Block Msg")
                            }
                        }
                        if (!isResolved) {
                            Button(
                                onClick = { onResolveAlert(alert.id, "RESOLVED") },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Resolve")
                            }
                        }
                        if (isCurrentlyBanned) {
                            Button(
                                onClick = {
                                    SoundAndHapticFeedback.playSuccess(context)
                                    val targetProfile = matchedUser ?: UserProfile(
                                        userId = alert.userId,
                                        email = alert.userEmail,
                                        fullName = alert.userName,
                                        deviceId = alert.deviceId,
                                        ipAddress = alert.ipAddress
                                    )
                                    onUnbanUserAndDevice(targetProfile)
                                    onResolveAlert(alert.id, "RESOLVED")
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldColor,
                                    contentColor = Color.Black
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("✅ Unban & 2nd Chance", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = { onBanUserFromAlert(alert) },
                                colors = ButtonDefaults.buttonColors(containerColor = CrimsonColor),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Ban Device")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminUsersInspectorTab(
    allUsers: List<UserProfile>,
    allDevices: List<DeviceRecord>,
    onSetUserCommodityMuted: (user: UserProfile, muted: Boolean) -> Unit,
    onResetUserDailyQuota: (UserProfile) -> Unit,
    onResetUserPin: (user: UserProfile, newPin: String) -> Unit,
    onUpdateUserSubscriptionDays: (userId: String, planName: String, daysLeft: Int) -> Unit,
    onSetDeviceBanStatus: (user: UserProfile, ban: Boolean) -> Unit,
    onAdminUpdateUserDeviceLimit: (UserProfile, Int) -> Unit,
    onToggleSingleDeviceBlocked: (String, Boolean) -> Unit,
    onRemoveDeviceSession: (String) -> Unit
) {
    val context = LocalContext.current
    var inspectedUser by remember { mutableStateOf<UserProfile?>(null) }
    var swipeGalleryItems by remember { mutableStateOf<List<SwipeableGalleryItem>>(emptyList()) }
    var swipeInitialIndex by remember { mutableIntStateOf(0) }

    val bannedUsers = remember(allUsers) {
        allUsers.filter { it.accountStatus == "BANNED" }
    }
    val bannedDevices = remember(allDevices) {
        allDevices.filter { it.isBanned }
    }

    val allUserGalleryItems = remember(allUsers, allDevices) {
        allUsers.map { u ->
            val devCount = allDevices.count { it.boundUserId == u.userId && !it.isBanned }.coerceAtLeast(1)
            u.toSwipeableGalleryItem(activeDevicesCount = devCount)
        }
    }

    if (swipeGalleryItems.isNotEmpty()) {
        SwipeablePhotoAndProfileDialog(
            items = swipeGalleryItems,
            initialIndex = swipeInitialIndex,
            dialogTitle = "Swipe Member Profiles & Photos",
            onDismiss = { swipeGalleryItems = emptyList() }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Dedicated Banned Users & Hardware Devices Recovery Card ("Give a 2nd Chance")
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (bannedUsers.isNotEmpty() || bannedDevices.isNotEmpty()) {
                        AmberColor.copy(alpha = 0.12f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                ),
                border = BorderStroke(
                    1.5.dp,
                    if (bannedUsers.isNotEmpty() || bannedDevices.isNotEmpty()) AmberColor else EmeraldColor.copy(alpha = 0.45f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_banned_recovery_card")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "🛡️ Banned Users & Devices — Give a 2nd Chance (${bannedUsers.size} Users • ${bannedDevices.size} Devices)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (bannedUsers.isNotEmpty() || bannedDevices.isNotEmpty()) AmberColor else EmeraldColor
                    )
                    if (bannedUsers.isEmpty() && bannedDevices.isEmpty()) {
                        Text(
                            text = "No banned users or blocked hardware devices right now. If anyone is banned by mistake, they will appear here for 1-tap unbanning.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        bannedUsers.forEach { bUser ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, AmberColor.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${bUser.fullName.ifBlank { bUser.email }} (BANNED)",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = CrimsonColor
                                        )
                                        Text(
                                            text = "${bUser.email} • Device: ${bUser.deviceId}",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            SoundAndHapticFeedback.playSuccess(context)
                                            onSetDeviceBanStatus(bUser, false)
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = EmeraldColor,
                                            contentColor = Color.Black
                                        ),
                                        modifier = Modifier.testTag("unban_user_button_${bUser.userId}")
                                    ) {
                                        Text("✅ Unban & 2nd Chance", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        bannedDevices
                            .filter { dev -> bannedUsers.none { u -> u.deviceId == dev.deviceId || u.userId == dev.boundUserId } }
                            .forEach { bDev ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, AmberColor.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Hardware Device: ${bDev.deviceModel.ifBlank { bDev.deviceId }} (BLOCKED)",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = CrimsonColor
                                            )
                                            Text(
                                                text = "ID: ${bDev.deviceId} • IP: ${bDev.ipAddress}",
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                        Button(
                                            onClick = {
                                                SoundAndHapticFeedback.playSuccess(context)
                                                onToggleSingleDeviceBlocked(bDev.deviceId, false)
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = EmeraldColor,
                                                contentColor = Color.Black
                                            ),
                                            modifier = Modifier.testTag("unban_device_button_${bDev.deviceId}")
                                        ) {
                                            Text("✅ Unban Device", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Member Directory, Active Devices & Security (${allUsers.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tap any member or photo to swipe through profiles, inspect logged-in devices, or set device limits.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (allUserGalleryItems.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        SoundAndHapticFeedback.playClick(context)
                        swipeInitialIndex = 0
                        swipeGalleryItems = allUserGalleryItems
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_swipe_all_profiles_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Swipe, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "👆 Swipe All Member Profiles & Photos (${allUserGalleryItems.size})",
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        items(allUsers, key = { it.userId.ifBlank { it.id } }) { user ->
            val avatarBmp = remember(user.photoBase64) {
                ImageCodecHelper.decodeBase64ToBitmap(user.photoBase64)
            }
            val userDevices = remember(allDevices, user.userId) {
                allDevices.filter { it.boundUserId == user.userId }
            }
            val activeDevCount = userDevices.count { !it.isBanned }.coerceAtLeast(1)
            val maxDevLimit = user.maxDevicesLimit(defaultLimit = 2)
            val isUserOrDeviceBanned = user.accountStatus == "BANNED" || userDevices.any { it.isBanned }

            Card(
                onClick = {
                    SoundAndHapticFeedback.playTap(context)
                    inspectedUser = user
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_user_row_${user.userId}")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .clickable {
                                SoundAndHapticFeedback.playClick(context)
                                val idx = allUsers.indexOfFirst { it.userId == user.userId }.coerceAtLeast(0)
                                swipeInitialIndex = idx
                                swipeGalleryItems = allUserGalleryItems
                            }
                    ) {
                        if (avatarBmp != null) {
                            Image(
                                bitmap = avatarBmp.asImageBitmap(),
                                contentDescription = "Swipe User Photo",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Person, contentDescription = null)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = user.fullName.ifBlank { user.email },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${user.email} • 📱 $activeDevCount/$maxDevLimit Devices • 2FA: ${if (user.isTwoStepEnabled()) "ON" else "OFF"}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Pass: ${user.extractAppPassword().ifBlank { "Set" }} | PIN: ${user.appLockPin.ifBlank { "Not Set" }} | Model: ${user.deviceModel.ifBlank { "Mobile" }}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (isUserOrDeviceBanned) {
                        Button(
                            onClick = {
                                SoundAndHapticFeedback.playSuccess(context)
                                onSetDeviceBanStatus(user, false)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldColor,
                                contentColor = Color.Black
                            ),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text("Unban", fontWeight = FontWeight.Bold)
                        }
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = "Inspect")
                }
            }
        }
    }

    inspectedUser?.let { user ->
        var daysInput by remember(user) { mutableStateOf(user.daysRemaining().toString()) }
        var pinInput by remember(user) { mutableStateOf(user.appLockPin.ifBlank { "1234" }) }
        var selectedMaxDevices by remember(user.securityQuestion) {
            mutableIntStateOf(user.maxDevicesLimit(defaultLimit = 2))
        }
        val userDevices = remember(allDevices, user.userId) {
            allDevices.filter { it.boundUserId == user.userId }
        }
        val isBanned = user.accountStatus == "BANNED"

        AlertDialog(
            onDismissRequest = { inspectedUser = null },
            title = { Text(user.fullName.ifBlank { user.email }) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Email: ${user.email}", style = MaterialTheme.typography.bodySmall)
                    Text("Phone: ${user.phoneNumber} • City: ${user.city}", style = MaterialTheme.typography.bodySmall)
                    Text("Primary Device: ${user.deviceModel} (${user.deviceId})", style = MaterialTheme.typography.labelSmall)
                    Text("IP Address: ${user.ipAddress}", style = MaterialTheme.typography.labelSmall)

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text("📱 Active Devices & Login Limit Control", fontWeight = FontWeight.Bold, color = EmeraldColor)
                    Text(
                        text = "Set Max Login Devices Allowed for ${user.fullName.ifBlank { "this user" }}:",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(1, 2, 3, 5).forEach { limitOpt ->
                            FilterChip(
                                selected = selectedMaxDevices == limitOpt,
                                onClick = {
                                    SoundAndHapticFeedback.playTap(context)
                                    selectedMaxDevices = limitOpt
                                    onAdminUpdateUserDeviceLimit(user, limitOpt)
                                },
                                label = { Text("${limitOpt}x") }
                            )
                        }
                    }

                    if (userDevices.isEmpty()) {
                        Text(
                            text = "• ${user.deviceModel.ifBlank { "Mobile" }} (${user.deviceId}) — Active",
                            style = MaterialTheme.typography.labelSmall
                        )
                    } else {
                        userDevices.forEach { dev ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (dev.isBanned) CrimsonColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "${dev.deviceModel.ifBlank { "Mobile/Web" }} • ${if (dev.isBanned) "BLOCKED" else "ACTIVE"}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (dev.isBanned) CrimsonColor else EmeraldColor
                                    )
                                    Text(
                                        text = "ID: ${dev.deviceId} • IP: ${dev.ipAddress}",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        OutlinedButton(
                                            onClick = {
                                                SoundAndHapticFeedback.playAlertOrError(context)
                                                onToggleSingleDeviceBlocked(dev.deviceId, !dev.isBanned)
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(if (dev.isBanned) "Unblock" else "Block Device")
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                SoundAndHapticFeedback.playTap(context)
                                                onRemoveDeviceSession(dev.deviceId)
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Sign Out")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text("Security Credentials (Admin View)", fontWeight = FontWeight.Bold)
                    Text(
                        text = "Account Password: ${user.extractAppPassword().ifBlank { "Standard" }} • 2-Step PIN: ${user.appLockPin.ifBlank { "Not Set" }} (${if (user.isTwoStepEnabled()) "2FA ON" else "2FA Optional"})",
                        color = EmeraldColor,
                        fontWeight = FontWeight.Bold
                    )
                    Text("Security Question: ${user.displaySecurityQuestion()}")
                    Text(
                        text = "Security Answer: ${user.securityAnswer.ifBlank { "Not Set" }}",
                        color = AmberColor,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 8 && it.all(Char::isDigit)) pinInput = it },
                        label = { Text("Reset User App PIN") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Button(
                        onClick = {
                            SoundAndHapticFeedback.playSuccess(context)
                            onResetUserPin(user, pinInput)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Force Update User PIN")
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text("Commodity & Subscription Controls", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { onSetUserCommodityMuted(user, !user.isCommodityMuted) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (user.isCommodityMuted) "Unmute Chat" else "Block Msg")
                        }
                        OutlinedButton(
                            onClick = { onResetUserDailyQuota(user) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Reset Quota")
                        }
                    }

                    OutlinedTextField(
                        value = daysInput,
                        onValueChange = { daysInput = it.filter(Char::isDigit) },
                        label = { Text("Subscription Days Remaining") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Button(
                        onClick = {
                            val days = daysInput.toIntOrNull() ?: 0
                            onUpdateUserSubscriptionDays(
                                user.userId,
                                user.activePlanName.ifBlank { "Admin Custom Plan" },
                                days
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Update Days Remaining")
                    }

                    if (isBanned) {
                        Button(
                            onClick = {
                                onSetDeviceBanStatus(user, false)
                                inspectedUser = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldColor, contentColor = Color.Black),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Unban Device & Account")
                        }
                    } else {
                        Button(
                            onClick = {
                                onSetDeviceBanStatus(user, true)
                                inspectedUser = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Suspend Account & Ban Device")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { inspectedUser = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun AdminPlansAndConfigTab(
    pricingConfig: PricingConfig,
    adminSettings: AdminSettings,
    broadcastMessages: List<BroadcastMessage> = emptyList(),
    appleWebAppUrl: String,
    androidApkShareUrl: String,
    onUpdateShareUrls: (appleUrl: String, androidUrl: String) -> Unit,
    onSavePricingAndQrConfig: (
        planName: String,
        priceInr: Double,
        validityDays: Long,
        upiId: String,
        upiMobileNumber: String,
        qrCodeBase64: String,
        qrOnlyMode: Boolean,
        plan2Enabled: Boolean,
        plan2Name: String,
        plan2PriceInr: Double,
        plan2ValidityDays: Long,
        plan2QrCodeBase64: String,
        plan2QrOnlyMode: Boolean,
        plan3Enabled: Boolean,
        plan3Name: String,
        plan3PriceInr: Double,
        plan3ValidityDays: Long,
        plan3QrCodeBase64: String,
        plan3QrOnlyMode: Boolean,
        dailyReviewLimitPerUser: Long,
        commodityChatLocked: Boolean,
        trialDays: Long,
        benefits: List<String>
    ) -> Unit,
    onAddCoAdminEmail: (String) -> Unit,
    onRemoveCoAdminEmail: (String) -> Unit,
    onUpdateTodayProfitBannerAndCard: (TodayProfitBanner, Boolean) -> Unit = { _, _ -> },
    onBulkExtendSubscribers: (Int) -> Unit = {},
    onClearUnpinnedCommodityMessages: () -> Unit = {},
    onPushAdminFirmwareToAllDevices: (enabled: Boolean, versionName: String, releaseNotes: String) -> Unit = { _, _, _ -> },
    onApplyDailyMessageLimit: (Long) -> Unit = {}
) {
    val context = LocalContext.current

    var plan1Enabled by remember(pricingConfig) { mutableStateOf(pricingConfig.isPlan1Enabled()) }
    var plan1Name by remember(pricingConfig) { mutableStateOf(pricingConfig.planName) }
    var plan1Price by remember(pricingConfig) { mutableStateOf(pricingConfig.priceInr.toInt().toString()) }
    var plan1Days by remember(pricingConfig) { mutableStateOf(pricingConfig.validityDays.toString()) }
    var plan1Qr by remember(pricingConfig) { mutableStateOf(pricingConfig.qrCodeBase64) }
    var plan1QrOnly by remember(pricingConfig) { mutableStateOf(pricingConfig.qrOnlyMode) }

    var plan2Enabled by remember(pricingConfig) { mutableStateOf(pricingConfig.plan2Enabled) }
    var plan2Name by remember(pricingConfig) { mutableStateOf(pricingConfig.plan2Name) }
    var plan2Price by remember(pricingConfig) { mutableStateOf(pricingConfig.plan2PriceInr.toInt().toString()) }
    var plan2Days by remember(pricingConfig) { mutableStateOf(pricingConfig.plan2ValidityDays.toString()) }
    var plan2Qr by remember(pricingConfig) { mutableStateOf(pricingConfig.plan2QrCodeBase64) }
    var plan2QrOnly by remember(pricingConfig) { mutableStateOf(pricingConfig.plan2QrOnlyMode) }

    var plan3Enabled by remember(pricingConfig) { mutableStateOf(pricingConfig.plan3Enabled) }
    var plan3Name by remember(pricingConfig) { mutableStateOf(pricingConfig.plan3Name) }
    var plan3Price by remember(pricingConfig) { mutableStateOf(pricingConfig.plan3PriceInr.toInt().toString()) }
    var plan3Days by remember(pricingConfig) { mutableStateOf(pricingConfig.plan3ValidityDays.toString()) }
    var plan3Qr by remember(pricingConfig) { mutableStateOf(pricingConfig.plan3QrCodeBase64) }
    var plan3QrOnly by remember(pricingConfig) { mutableStateOf(pricingConfig.plan3QrOnlyMode) }

    var customPlans by remember(pricingConfig) { mutableStateOf(pricingConfig.customPlans()) }
    var announcementTicker by remember(pricingConfig) { mutableStateOf(pricingConfig.announcementTicker()) }
    var showAddCustomPlanModal by remember { mutableStateOf(false) }
    var showPnlEditorModal by remember { mutableStateOf(false) }

    val currentFw = remember(pricingConfig) { pricingConfig.firmwareConfig() }
    var fwEnabledToggle by remember(currentFw) { mutableStateOf(currentFw.enabled) }
    var fwVersionName by remember(currentFw) { mutableStateOf(currentFw.versionName) }
    var fwReleaseNotes by remember(currentFw) { mutableStateOf(currentFw.releaseNotes) }

    var upiId by remember(pricingConfig) { mutableStateOf(pricingConfig.upiId) }
    var upiMobile by remember(pricingConfig) { mutableStateOf(pricingConfig.upiMobileNumber) }
    var dailyLimit by remember(pricingConfig) { mutableStateOf(pricingConfig.dailyReviewLimitPerUser.toString()) }
    var applyLimitToggle by remember { mutableStateOf(true) }
    var trialDays by remember(pricingConfig) { mutableStateOf(pricingConfig.trialDays.toString()) }
    var isChatLocked by remember(pricingConfig) { mutableStateOf(pricingConfig.commodityChatLocked) }
    var newCoAdminEmail by remember { mutableStateOf("") }
    var activeQrSlot by remember { mutableIntStateOf(1) }

    val qrPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val encoded = ImageCodecHelper.encodeUriToBase64Jpeg(
                context = context,
                uri = uri,
                maxDimension = 700,
                quality = 75
            )
            if (encoded != null) {
                when (activeQrSlot) {
                    1 -> plan1Qr = encoded
                    2 -> plan2Qr = encoded
                    3 -> plan3Qr = encoded
                }
            }
        }
    }

    fun triggerSaveAll(
        updatedCustomPlans: List<CustomPlanItem> = customPlans,
        updatedP1Enabled: Boolean = plan1Enabled,
        updatedP2Enabled: Boolean = plan2Enabled,
        updatedP3Enabled: Boolean = plan3Enabled
    ) {
        val encodedBenefits = PricingConfig.buildBenefitsWithMeta(
            displayBenefits = pricingConfig.displayBenefits(),
            plan1Enabled = updatedP1Enabled,
            customPlans = updatedCustomPlans,
            todayProfit = pricingConfig.todayProfitBanner(),
            announcementTicker = announcementTicker,
            firmwareConfig = pricingConfig.firmwareConfig()
        )
        onSavePricingAndQrConfig(
            plan1Name,
            plan1Price.toDoubleOrNull() ?: 2499.0,
            plan1Days.toLongOrNull() ?: 30L,
            upiId,
            upiMobile,
            plan1Qr,
            plan1QrOnly,
            updatedP2Enabled,
            plan2Name,
            plan2Price.toDoubleOrNull() ?: 5999.0,
            plan2Days.toLongOrNull() ?: 90L,
            plan2Qr,
            plan2QrOnly,
            updatedP3Enabled,
            plan3Name,
            plan3Price.toDoubleOrNull() ?: 9999.0,
            plan3Days.toLongOrNull() ?: 180L,
            plan3Qr,
            plan3QrOnly,
            dailyLimit.toLongOrNull() ?: 10L,
            isChatLocked,
            trialDays.toLongOrNull() ?: 7L,
            encodedBenefits
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Admin Permanent Website QR Code & Apple / Android Share Hub
        PermanentQrAndShareCard(
            appleWebAppUrl = appleWebAppUrl,
            androidApkUrl = androidApkShareUrl,
            isAdmin = true,
            onUpdateUrls = onUpdateShareUrls
        )

        // 🚀 ADMIN MANDATORY FIRMWARE UPDATE PUSH TO ALL DEVICES & WEBSITE
        Card(
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.5.dp, ElectricBlueColor),
            colors = CardDefaults.cardColors(
                containerColor = ElectricBlueColor.copy(alpha = 0.08f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_firmware_push_card")
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = null,
                            tint = ElectricBlueColor
                        )
                        Column {
                            Text(
                                text = "🚀 Admin Mandatory Firmware Push",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Toggle ON & Push to force a mandatory full-screen Firmware Update on all member phones and website browsers",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = fwEnabledToggle,
                        onCheckedChange = { enabled ->
                            SoundAndHapticFeedback.playClick(context)
                            fwEnabledToggle = enabled
                            onPushAdminFirmwareToAllDevices(enabled, fwVersionName, fwReleaseNotes)
                        },
                        modifier = Modifier.testTag("admin_firmware_toggle")
                    )
                }

                OutlinedTextField(
                    value = fwVersionName,
                    onValueChange = { fwVersionName = it },
                    label = { Text("Admin Firmware Build Version") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = fwReleaseNotes,
                    onValueChange = { fwReleaseNotes = it },
                    label = { Text("Mandatory Update Release Notes") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        fwEnabledToggle = true
                        SoundAndHapticFeedback.playTradeSignal(context, "TARGET HIT")
                        onPushAdminFirmwareToAllDevices(true, fwVersionName, fwReleaseNotes)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("admin_push_firmware_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricBlueColor,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.SystemUpdate, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Upload & Force Firmware Update to All Devices",
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // SUPER ADMIN COMMAND SUITE (Today's P&L, Announcement Ticker, Bulk Subscriber Extensions)
        Card(
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.5.dp, EmeraldColor.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = EmeraldColor)
                    Text(
                        text = "Super Admin Command Center",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                val currentPnl = pricingConfig.todayProfitBanner()
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Today's Profit / Loss: ${currentPnl.amountText} (${currentPnl.instrumentText})",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (currentPnl.isProfit) EmeraldColor else CrimsonColor
                            )
                            Text(
                                text = if (currentPnl.enabled) "Visible on Commodity & Broadcasts with Telegram Reactions" else "Currently Hidden",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Button(onClick = { showPnlEditorModal = true }) {
                            Text("Edit / Post P&L")
                        }
                    }
                }

                OutlinedTextField(
                    value = announcementTicker,
                    onValueChange = { announcementTicker = it },
                    label = { Text("Live Announcement Ticker (Shown on Broadcast Stream)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onBulkExtendSubscribers(7) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("+7d All Active")
                    }
                    OutlinedButton(
                        onClick = { onBulkExtendSubscribers(30) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("+30d All Active")
                    }
                    OutlinedButton(
                        onClick = onClearUnpinnedCommodityMessages,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Clear Chat")
                    }
                }
            }
        }

        Text(
            text = "Commodity Controls & Multi-Plan Studio (ON / OFF / Add / Delete)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Pause All Member Posts in Commodity", fontWeight = FontWeight.SemiBold)
            Switch(checked = isChatLocked, onCheckedChange = { isChatLocked = it })
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = dailyLimit,
                onValueChange = { dailyLimit = it.filter(Char::isDigit) },
                label = { Text("Daily Msg Limit / User") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = trialDays,
                onValueChange = { trialDays = it.filter(Char::isDigit) },
                label = { Text("Free Trial Days") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        // Explicit Apply Toggle & Button for Daily Message Limit and Plans
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = EmeraldColor.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, EmeraldColor.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Switch(
                        checked = applyLimitToggle,
                        onCheckedChange = {
                            SoundAndHapticFeedback.playClick(context)
                            applyLimitToggle = it
                            if (it) {
                                val parsed = (dailyLimit.toLongOrNull() ?: 10L).coerceIn(1L, 100L)
                                onApplyDailyMessageLimit(parsed)
                            }
                        },
                        modifier = Modifier.testTag("admin_apply_limit_toggle")
                    )
                    Column {
                        Text(
                            text = "Apply Daily Limit & Plan Rules",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Tap Apply to enforce ${dailyLimit.ifBlank { "10" }} msgs/day & active plans",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Button(
                    onClick = {
                        SoundAndHapticFeedback.playTradeSignal(context, "BUY")
                        val parsed = (dailyLimit.toLongOrNull() ?: 10L).coerceIn(1L, 100L)
                        onApplyDailyMessageLimit(parsed)
                        triggerSaveAll()
                    },
                    enabled = applyLimitToggle,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldColor,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier.testTag("admin_apply_limit_button")
                ) {
                    Text("Apply Now", fontWeight = FontWeight.ExtraBold)
                }
            }
        }

        OutlinedTextField(
            value = upiId,
            onValueChange = { upiId = it },
            label = { Text("Official UPI ID") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = upiMobile,
            onValueChange = { upiMobile = it },
            label = { Text("Official UPI Mobile Number") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Plan 1
        Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Plan #1 (Primary Tier)", fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(if (plan1Enabled) "ON" else "OFF", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Switch(
                            checked = plan1Enabled,
                            onCheckedChange = {
                                plan1Enabled = it
                                triggerSaveAll(updatedP1Enabled = it)
                            }
                        )
                    }
                }
                OutlinedTextField(value = plan1Name, onValueChange = { plan1Name = it }, label = { Text("Plan Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = plan1Price, onValueChange = { plan1Price = it.filter(Char::isDigit) }, label = { Text("Price (INR)") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = plan1Days, onValueChange = { plan1Days = it.filter(Char::isDigit) }, label = { Text("Validity (Days)") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("QR-Only Mode (Hide UPI ID)", style = MaterialTheme.typography.bodySmall)
                    Switch(checked = plan1QrOnly, onCheckedChange = { plan1QrOnly = it })
                }
                OutlinedButton(
                    onClick = {
                        activeQrSlot = 1
                        qrPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (plan1Qr.isBlank()) "Upload Plan #1 QR Image" else "Replace Plan #1 QR Image")
                }
            }
        }

        // Plan 2
        Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Plan #2 (Quarterly Tier)", fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(if (plan2Enabled) "ON" else "OFF", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Switch(
                            checked = plan2Enabled,
                            onCheckedChange = {
                                plan2Enabled = it
                                triggerSaveAll(updatedP2Enabled = it)
                            }
                        )
                    }
                }
                OutlinedTextField(value = plan2Name, onValueChange = { plan2Name = it }, label = { Text("Plan Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = plan2Price, onValueChange = { plan2Price = it.filter(Char::isDigit) }, label = { Text("Price (INR)") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = plan2Days, onValueChange = { plan2Days = it.filter(Char::isDigit) }, label = { Text("Validity (Days)") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("QR-Only Mode (Hide UPI ID)", style = MaterialTheme.typography.bodySmall)
                    Switch(checked = plan2QrOnly, onCheckedChange = { plan2QrOnly = it })
                }
                OutlinedButton(
                    onClick = {
                        activeQrSlot = 2
                        qrPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (plan2Qr.isBlank()) "Upload Plan #2 QR Image" else "Replace Plan #2 QR Image")
                }
            }
        }

        // Plan 3
        Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Plan #3 (Institutional Tier)", fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(if (plan3Enabled) "ON" else "OFF", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Switch(
                            checked = plan3Enabled,
                            onCheckedChange = {
                                plan3Enabled = it
                                triggerSaveAll(updatedP3Enabled = it)
                            }
                        )
                    }
                }
                OutlinedTextField(value = plan3Name, onValueChange = { plan3Name = it }, label = { Text("Plan Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = plan3Price, onValueChange = { plan3Price = it.filter(Char::isDigit) }, label = { Text("Price (INR)") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = plan3Days, onValueChange = { plan3Days = it.filter(Char::isDigit) }, label = { Text("Validity (Days)") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("QR-Only Mode (Hide UPI ID)", style = MaterialTheme.typography.bodySmall)
                    Switch(checked = plan3QrOnly, onCheckedChange = { plan3QrOnly = it })
                }
                OutlinedButton(
                    onClick = {
                        activeQrSlot = 3
                        qrPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (plan3Qr.isBlank()) "Upload Plan #3 QR Image" else "Replace Plan #3 QR Image")
                }
            }
        }

        // Custom Plans Studio (Add / Toggle ON-OFF / Delete Custom Plans)
        Card(
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Custom Plans (${customPlans.size}/5)", fontWeight = FontWeight.ExtraBold)
                        Text(
                            "Create special event plans, weekly passes, or custom tiers and toggle them ON/OFF anytime",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = { showAddCustomPlanModal = true },
                        enabled = customPlans.size < 5
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Plan")
                    }
                }

                if (customPlans.isEmpty()) {
                    Text(
                        "No extra custom plans added yet. Tap '+ Add Plan' to create one.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    customPlans.forEach { cp ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${cp.name} (${cp.badge})", fontWeight = FontWeight.Bold)
                                    Text(
                                        "₹${cp.priceInr.toInt()} • ${cp.validityDays} Days • ${if (cp.enabled) "ON (Live)" else "OFF (Hidden)"}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Switch(
                                    checked = cp.enabled,
                                    onCheckedChange = { enabled ->
                                        val next = customPlans.map {
                                            if (it.id == cp.id) it.copy(enabled = enabled) else it
                                        }
                                        customPlans = next
                                        triggerSaveAll(updatedCustomPlans = next)
                                    }
                                )
                                IconButton(
                                    onClick = {
                                        val next = customPlans.filterNot { it.id == cp.id }
                                        customPlans = next
                                        triggerSaveAll(updatedCustomPlans = next)
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete Custom Plan",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Co-Admins Management
        Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Authorized Admin Emails (Max 3)", fontWeight = FontWeight.Bold)
                adminSettings.adminEmails.forEach { email ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(email, style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = { onRemoveCoAdminEmail(email) }) {
                            Text("Remove")
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newCoAdminEmail,
                        onValueChange = { newCoAdminEmail = it },
                        label = { Text("Add Co-Admin Google Email") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            if (newCoAdminEmail.isNotBlank()) {
                                onAddCoAdminEmail(newCoAdminEmail.trim())
                                newCoAdminEmail = ""
                            }
                        }
                    ) {
                        Text("Add")
                    }
                }
            }
        }

        Button(
            onClick = { triggerSaveAll() },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("save_pricing_config_button")
        ) {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Save All Plans & Platform Settings", fontWeight = FontWeight.Bold)
        }
    }

    if (showAddCustomPlanModal) {
        var cpName by remember { mutableStateOf("") }
        var cpPrice by remember { mutableStateOf("1499") }
        var cpDays by remember { mutableStateOf("45") }
        var cpBadge by remember { mutableStateOf("Special Tier") }

        AlertDialog(
            onDismissRequest = { showAddCustomPlanModal = false },
            title = { Text("Create Custom Plan", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = cpName,
                        onValueChange = { cpName = it },
                        label = { Text("Plan Name (e.g. Weekly Gold Sniper)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = cpPrice,
                        onValueChange = { cpPrice = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Price (₹ INR)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = cpDays,
                        onValueChange = { cpDays = it.filter(Char::isDigit) },
                        label = { Text("Validity (Days)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = cpBadge,
                        onValueChange = { cpBadge = it },
                        label = { Text("Badge Text (e.g. VIP / Limited)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val next = customPlans + CustomPlanItem(
                            id = "cp_${System.currentTimeMillis() % 100000}",
                            name = cpName.trim().ifBlank { "Custom VIP Plan" },
                            priceInr = cpPrice.toDoubleOrNull() ?: 1499.0,
                            validityDays = cpDays.toLongOrNull() ?: 45L,
                            enabled = true,
                            badge = cpBadge.trim().ifBlank { "Custom Tier" }
                        )
                        customPlans = next
                        triggerSaveAll(updatedCustomPlans = next)
                        showAddCustomPlanModal = false
                    }
                ) {
                    Text("Add & Activate Plan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustomPlanModal = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showPnlEditorModal) {
        OwnerProfitEditorDialog(
            initialBanner = pricingConfig.todayProfitBanner(),
            broadcastMessages = broadcastMessages,
            onDismiss = { showPnlEditorModal = false },
            onSave = { updatedBanner, alsoPostInFeed ->
                onUpdateTodayProfitBannerAndCard(updatedBanner, alsoPostInFeed)
                showPnlEditorModal = false
            }
        )
    }
}
