package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
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
import com.example.data.model.AppThemePreference
import com.example.data.model.ChatBubbleStylePreference
import com.example.data.model.DEFAULT_TERMS_AND_CONDITIONS
import com.example.data.model.DeviceRecord
import com.example.data.model.FontScalePreference
import com.example.data.model.PricingConfig
import com.example.data.model.UserProfile
import com.example.ui.components.ImageCodecHelper
import com.example.ui.components.PermanentQrAndShareCard
import com.example.ui.components.SwipeableGalleryItem
import com.example.ui.components.SwipeablePhotoAndProfileDialog
import com.example.ui.components.toSwipeableGalleryItem
import com.example.util.SoundAndHapticFeedback

@Composable
fun SettingsScreen(
    userProfile: UserProfile?,
    isAdmin: Boolean,
    currentDeviceId: String = "",
    myDevices: List<DeviceRecord> = emptyList(),
    appleWebAppUrl: String,
    androidApkShareUrl: String,
    onUpdateShareUrls: (appleUrl: String, androidUrl: String) -> Unit,
    themePreference: AppThemePreference,
    isBiometricEnabled: Boolean,
    notificationsEnabled: Boolean,
    offlineDataReminderEnabled: Boolean,
    offlineReminderIntervalMinutes: Int,
    cycleExpiryNotificationsEnabled: Boolean,
    highRiskVibrationEnabled: Boolean,
    compactFeedMode: Boolean,
    largeTextMode: Boolean,
    securityQuestion: String,
    onChangeTheme: (AppThemePreference) -> Unit,
    onToggleBiometric: (Boolean) -> Unit,
    onToggleNotifications: (Boolean) -> Unit,
    onToggleOfflineDataReminder: (Boolean) -> Unit,
    onChangeOfflineReminderInterval: (Int) -> Unit,
    onToggleCycleExpiryNotifications: (Boolean) -> Unit,
    onToggleHighRiskVibration: (Boolean) -> Unit,
    onToggleCompactFeedMode: (Boolean) -> Unit,
    onToggleLargeTextMode: (Boolean) -> Unit,
    onUpdatePin: (currentPin: String, newPin: String) -> Boolean,
    onUpdateSecurityQuestion: (question: String, answer: String) -> Unit,
    onUpdateSecurityPreferences: (newPassword: String?, twoStepEnabled: Boolean, maxDevicesLimit: Int) -> Unit = { _, _, _ -> },
    onToggleDeviceBlocked: (deviceId: String, blocked: Boolean) -> Unit = { _, _ -> },
    onRemoveDeviceSession: (deviceId: String) -> Unit = {},
    onRequestProfileEdit: (fullName: String, phone: String, city: String, photoBase64: String) -> Unit,
    fontScalePreference: FontScalePreference = FontScalePreference.NORMAL,
    chatBubbleStylePreference: ChatBubbleStylePreference = ChatBubbleStylePreference.MODERN_GLASS,
    showLiveMarketTicker: Boolean = true,
    autoScrollFeedEnabled: Boolean = true,
    defaultLandingTab: String = "STREAM",
    pricingConfig: PricingConfig = PricingConfig(),
    onChangeFontScale: (FontScalePreference) -> Unit = {},
    onChangeChatBubbleStyle: (ChatBubbleStylePreference) -> Unit = {},
    onToggleShowLiveMarketTicker: (Boolean) -> Unit = {},
    onToggleAutoScrollFeed: (Boolean) -> Unit = {},
    onChangeDefaultLandingTab: (String) -> Unit = {},
    onOpenAdminMarketSearch: () -> Unit = {},
    onUpdateAnnouncementTicker: (String) -> Unit = {},
    onUpdateCommodityModerationRules: (slowModeSeconds: Int, blockLinks: Boolean, customWordsCsv: String) -> Unit = { _, _, _ -> },
    onLockNow: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    var soundFxEnabled by remember { mutableStateOf(SoundAndHapticFeedback.isSoundEnabled(context)) }
    var hapticFxEnabled by remember { mutableStateOf(SoundAndHapticFeedback.isHapticEnabled(context)) }

    // Profile Edit Request State
    var editName by remember(userProfile?.fullName) { mutableStateOf(userProfile?.fullName ?: "") }
    var editPhone by remember(userProfile?.phoneNumber) { mutableStateOf(userProfile?.phoneNumber ?: "") }
    var editCity by remember(userProfile?.city) { mutableStateOf(userProfile?.city ?: "") }
    var editPhotoBase64 by remember(userProfile?.photoBase64) {
        mutableStateOf(userProfile?.photoBase64 ?: "")
    }
    var showProfileEditor by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val encoded = ImageCodecHelper.encodeUriToBase64Jpeg(
                context = context,
                uri = uri,
                maxDimension = 360,
                quality = 72
            )
            if (encoded != null) {
                editPhotoBase64 = encoded
            }
        }
    }

    val avatarBitmap = remember(userProfile?.photoBase64) {
        ImageCodecHelper.decodeBase64ToBitmap(userProfile?.photoBase64)
    }
    val editAvatarBitmap = remember(editPhotoBase64) {
        ImageCodecHelper.decodeBase64ToBitmap(editPhotoBase64)
    }

    var showPinDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showSwipeProfileViewer by remember { mutableStateOf(false) }

    val profileGalleryItems = remember(userProfile, editPhotoBase64, myDevices) {
        buildList {
            if (userProfile != null) {
                val activeCount = myDevices.count { !it.isBanned }.coerceAtLeast(1)
                add(userProfile.toSwipeableGalleryItem(activeDevicesCount = activeCount))
                if (userProfile.pendingPhotoBase64.isNotBlank() &&
                    userProfile.pendingPhotoBase64 != userProfile.photoBase64
                ) {
                    add(
                        SwipeableGalleryItem(
                            id = "${userProfile.userId}_pending",
                            title = "${userProfile.pendingFullName.ifBlank { userProfile.fullName }} (Pending Approval)",
                            subtitle = userProfile.email,
                            imageBase64OrUrl = userProfile.pendingPhotoBase64,
                            badgeText = "PENDING PHOTO",
                            details = listOf(
                                "Requested Name" to userProfile.pendingFullName.ifBlank { userProfile.fullName },
                                "Requested Phone" to userProfile.pendingPhoneNumber.ifBlank { userProfile.phoneNumber },
                                "Requested City" to userProfile.pendingCity.ifBlank { userProfile.city }
                            )
                        )
                    )
                }
            }
        }
    }

    if (showSwipeProfileViewer && profileGalleryItems.isNotEmpty()) {
        SwipeablePhotoAndProfileDialog(
            items = profileGalleryItems,
            initialIndex = 0,
            dialogTitle = "Swipe Profile & Photos",
            onDismiss = { showSwipeProfileViewer = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Member Profile & Admin-Approved Edit Request Card
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .clickable {
                                SoundAndHapticFeedback.playClick(context)
                                showSwipeProfileViewer = true
                            }
                            .testTag("profile_avatar_swipe_button")
                    ) {
                        if (avatarBitmap != null) {
                            Image(
                                bitmap = avatarBitmap.asImageBitmap(),
                                contentDescription = "Swipe Profile Photo",
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
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Avatar",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                SoundAndHapticFeedback.playClick(context)
                                showSwipeProfileViewer = true
                            }
                    ) {
                        Text(
                            text = userProfile?.fullName?.ifBlank { userProfile.email } ?: "Member",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = userProfile?.email ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "👆 Tap to Swipe Profile & Photo",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    OutlinedButton(
                        onClick = { showProfileEditor = !showProfileEditor },
                        modifier = Modifier.testTag("toggle_profile_edit_button")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Request Edit", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit")
                    }
                }

                if (userProfile?.hasPendingProfileEdit == true) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.HourglassTop,
                                contentDescription = "Pending Approval",
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Profile change request (${userProfile.pendingFullName}) is awaiting Admin approval.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                if (showProfileEditor) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Request Profile & Photo Update (Requires Admin Approval)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (editAvatarBitmap != null) {
                            Image(
                                bitmap = editAvatarBitmap.asImageBitmap(),
                                contentDescription = "New Profile Photo",
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                        }
                        OutlinedButton(
                            onClick = {
                                photoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = "Pick Photo", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Choose New Photo")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Full Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editPhone,
                        onValueChange = { editPhone = it },
                        label = { Text("Mobile Number") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editCity,
                        onValueChange = { editCity = it },
                        label = { Text("City / State") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            onRequestProfileEdit(editName, editPhone, editCity, editPhotoBase64)
                            showProfileEditor = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("submit_profile_edit_request_button")
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Submit Profile Update for Admin Approval")
                    }
                }
            }
        }

        // 1B. Permanent Website QR Code & Apple iPhone / Android Share Hub
        PermanentQrAndShareCard(
            appleWebAppUrl = appleWebAppUrl,
            androidApkUrl = androidApkShareUrl,
            isAdmin = isAdmin,
            onUpdateUrls = onUpdateShareUrls
        )

        // 2. Customizable Notification Center (Including 1-Hour Offline Mobile Data Reminder)
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = "Notifications",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Customizable Notification Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Control background push alerts, privacy masking ('Trading Zone • 1 new message'), and offline mobile data reminders.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Toggle 1: Instant Broadcast Notifications
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Instant Message Push Notifications",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Push 'Trading Zone • 1 new message' even when app is closed",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = onToggleNotifications,
                        modifier = Modifier.testTag("switch_instant_notifications")
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Toggle 2: Offline Mobile Data Reminder Notification (Default 1 Hour)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Offline Mobile Data Reminder",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Alert me if mobile data / Wi-Fi is disconnected so I don't miss instant messages",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = offlineDataReminderEnabled,
                        onCheckedChange = onToggleOfflineDataReminder,
                        modifier = Modifier.testTag("switch_offline_data_reminder")
                    )
                }

                if (offlineDataReminderEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Offline Reminder Interval:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            30 to "30 Min",
                            60 to "1 Hour (Default)",
                            120 to "2 Hours"
                        ).forEach { (minutes, label) ->
                            FilterChip(
                                selected = offlineReminderIntervalMinutes == minutes,
                                onClick = { onChangeOfflineReminderInterval(minutes) },
                                label = { Text(label) },
                                modifier = Modifier.testTag("chip_offline_interval_$minutes")
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Toggle 3: Priority Vibration
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Priority Vibration on New Alerts",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Vibrate device when a new Trading Zone broadcast arrives",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = highRiskVibrationEnabled,
                        onCheckedChange = onToggleHighRiskVibration,
                        modifier = Modifier.testTag("switch_vibration")
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Toggle 4: Cycle Expiry Reminder
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Membership Cycle Expiry Reminders",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Notify when 2 days or fewer remain in your subscription cycle",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = cycleExpiryNotificationsEnabled,
                        onCheckedChange = onToggleCycleExpiryNotifications,
                        modifier = Modifier.testTag("switch_cycle_expiry")
                    )
                }
            }
        }

        // 3. Appearance & Smooth Display Preferences
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Look-Changer UI Themes & Smooth Animations",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Instant ultrasound spring transitions across 6 pro terminal skins",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AppThemePreference.entries.forEach { mode ->
                        FilterChip(
                            selected = themePreference == mode,
                            onClick = { onChangeTheme(mode) },
                            label = { Text("${mode.title} • ${mode.subtitle}") },
                            modifier = Modifier.testTag("theme_chip_${mode.name}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("UI Sound Effects (Pro Terminal Audio)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text("Play crisp audio cues on taps, trade updates & alerts", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = soundFxEnabled,
                        onCheckedChange = {
                            soundFxEnabled = it
                            SoundAndHapticFeedback.setSoundEnabled(context, it)
                            if (it) SoundAndHapticFeedback.playTap(context)
                        }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Tactile Haptic Feedback", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text("Crisp vibration feedback on buttons, reactions & tab switches", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = hapticFxEnabled,
                        onCheckedChange = {
                            hapticFxEnabled = it
                            SoundAndHapticFeedback.setHapticEnabled(context, it)
                            if (it) SoundAndHapticFeedback.playTap(context)
                        }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Compact Message Cards", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = compactFeedMode, onCheckedChange = onToggleCompactFeedMode)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Large Readable Typography", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = largeTextMode, onCheckedChange = onToggleLargeTextMode)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Font Scale Customization
                Text(
                    text = "Typography Font Scale (90% – 130%)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FontScalePreference.entries.forEach { scaleOpt ->
                        FilterChip(
                            selected = fontScalePreference == scaleOpt,
                            onClick = {
                                SoundAndHapticFeedback.playTap(context)
                                onChangeFontScale(scaleOpt)
                            },
                            label = { Text(scaleOpt.title) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Chat Bubble Style Customization
                Text(
                    text = "Message & Commodity Card Corner Style",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ChatBubbleStylePreference.entries.forEach { bubbleOpt ->
                        FilterChip(
                            selected = chatBubbleStylePreference == bubbleOpt,
                            onClick = {
                                SoundAndHapticFeedback.playTap(context)
                                onChangeChatBubbleStyle(bubbleOpt)
                            },
                            label = { Text(bubbleOpt.title) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Default Startup Tab Customization
                Text(
                    text = "Default Startup Screen When Opening App",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "STREAM" to "📢 Messages",
                        "COMMODITY" to "💬 Commodity",
                        "MEMBERSHIP" to "💎 Plans"
                    ).forEach { (tabKey, tabLabel) ->
                        FilterChip(
                            selected = defaultLandingTab == tabKey,
                            onClick = {
                                SoundAndHapticFeedback.playTap(context)
                                onChangeDefaultLandingTab(tabKey)
                            },
                            label = { Text(tabLabel) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Show Top Live Market Ticker Strip", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text("Real-time Gold, Silver, Nifty & Crude ticker bar at top of screen", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = showLiveMarketTicker, onCheckedChange = onToggleShowLiveMarketTicker)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto-Scroll Live Broadcast Stream", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text("Automatically jump to new trade calls as they arrive", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = autoScrollFeedEnabled, onCheckedChange = onToggleAutoScrollFeed)
                }
            }
        }

        // 3A-Admin. Admin Live Customization & Market Pin Studio (Visible to Admin)
        if (isAdmin) {
            val modCfg = remember(pricingConfig) { pricingConfig.commodityModerationConfig() }
            var tickerDraft by remember(pricingConfig) { mutableStateOf(pricingConfig.announcementTicker()) }
            var slowModeDraft by remember(modCfg.slowModeSeconds) { mutableIntStateOf(modCfg.slowModeSeconds) }
            var blockLinksDraft by remember(modCfg.blockExternalLinks) { mutableStateOf(modCfg.blockExternalLinks) }
            var customWordsDraft by remember(modCfg.customBlockedWordsCsv) { mutableStateOf(modCfg.customBlockedWordsCsv) }

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.5.dp, Color(0xFFF59E0B).copy(alpha = 0.55f)),
                modifier = Modifier.fillMaxWidth()
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "👑 Admin Live Market & Room Customization",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFF59E0B)
                            )
                            Text(
                                text = "Search & pin live market instruments (Investing.com style), customize announcement ticker, and set Commodity slow-mode / link filters.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Button(
                        onClick = {
                            SoundAndHapticFeedback.playTap(context)
                            onOpenAdminMarketSearch()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981),
                            contentColor = Color.Black
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("📈 Search & Pin Live Market Instruments (Gold, Nifty, Crude...)", fontWeight = FontWeight.ExtraBold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = tickerDraft,
                            onValueChange = { tickerDraft = it },
                            label = { Text("Live Announcement Banner Ticker") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                SoundAndHapticFeedback.playSuccess(context)
                                onUpdateAnnouncementTicker(tickerDraft)
                            }
                        ) {
                            Text("Save")
                        }
                    }

                    Text(
                        text = "Commodity Room Slow-Mode Cooldown:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(0 to "Off (Instant)", 10 to "10 Seconds", 30 to "30 Seconds", 60 to "60 Seconds").forEach { (sec, label) ->
                            FilterChip(
                                selected = slowModeDraft == sec,
                                onClick = {
                                    SoundAndHapticFeedback.playTap(context)
                                    slowModeDraft = sec
                                    onUpdateCommodityModerationRules(sec, blockLinksDraft, customWordsDraft)
                                },
                                label = { Text(label) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Block External URLs & Links in Commodity", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("Automatically blur http/https/www/t.me links posted by members", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = blockLinksDraft,
                            onCheckedChange = {
                                blockLinksDraft = it
                                onUpdateCommodityModerationRules(slowModeDraft, it, customWordsDraft)
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customWordsDraft,
                            onValueChange = { customWordsDraft = it },
                            label = { Text("Custom Blocked Words (comma-separated)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                SoundAndHapticFeedback.playSuccess(context)
                                onUpdateCommodityModerationRules(slowModeDraft, blockLinksDraft, customWordsDraft)
                            }
                        ) {
                            Text("Apply")
                        }
                    }
                }
            }
        }

        // 3B. Where You're Logged In — Active Devices, Block Device & Max Devices Limit
        val currentLimit = remember(userProfile?.securityQuestion) {
            userProfile?.maxDevicesLimit(defaultLimit = 2) ?: 2
        }
        val currentTwoStep = remember(userProfile?.securityQuestion, isAdmin) {
            if (isAdmin) true else (userProfile?.isTwoStepEnabled() ?: false)
        }
        var selectedMaxDevices by remember(currentLimit) { mutableIntStateOf(currentLimit) }
        var twoStepToggle by remember(currentTwoStep) { mutableStateOf(currentTwoStep) }
        var newAccountPassword by remember { mutableStateOf("") }
        val activeNonBlockedCount = remember(myDevices) { myDevices.count { !it.isBanned } }

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.5.dp, Color(0xFF10B981).copy(alpha = 0.45f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("active_devices_security_card")
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Active Devices",
                            tint = Color(0xFF10B981)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Where You're Logged In (Devices)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "See which mobile or web devices are logged into your account, block unfamiliar devices, or set your device limit.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (activeNonBlockedCount > selectedMaxDevices) {
                            Color(0xFFEF4444).copy(alpha = 0.18f)
                        } else {
                            Color(0xFF10B981).copy(alpha = 0.18f)
                        }
                    ) {
                        Text(
                            text = "${activeNonBlockedCount.coerceAtLeast(1)} / $selectedMaxDevices Devices",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (activeNonBlockedCount > selectedMaxDevices) Color(0xFFEF4444) else Color(0xFF10B981)
                        )
                    }
                }

                // Max Login Devices Limit Selector
                Text(
                    text = "Maximum Login Devices Allowed for My Account:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(1, 2, 3, 5).forEach { limitOption ->
                        FilterChip(
                            selected = selectedMaxDevices == limitOption,
                            onClick = {
                                SoundAndHapticFeedback.playTap(context)
                                selectedMaxDevices = limitOption
                                onUpdateSecurityPreferences(null, twoStepToggle, limitOption)
                            },
                            label = {
                                Text(if (limitOption == 1) "1 Device Only" else "$limitOption Devices")
                            }
                        )
                    }
                }

                // List of Logged-In Mobile & Web Devices
                if (myDevices.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${userProfile?.deviceModel?.ifBlank { "Android Mobile" } ?: "Current Mobile"} (THIS DEVICE)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                                Text(
                                    text = "ID: ${currentDeviceId.ifBlank { userProfile?.deviceId ?: "Active" }} • IP: ${userProfile?.ipAddress ?: "Online"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(0xFF10B981).copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "ACTIVE NOW",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                            }
                        }
                    }
                } else {
                    myDevices.forEach { dev ->
                        val isThisDevice = dev.deviceId == currentDeviceId
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (dev.isBanned) {
                                Color(0xFFEF4444).copy(alpha = 0.12f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                            },
                            border = BorderStroke(
                                1.dp,
                                when {
                                    dev.isBanned -> Color(0xFFEF4444).copy(alpha = 0.5f)
                                    isThisDevice -> Color(0xFF10B981).copy(alpha = 0.6f)
                                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = buildString {
                                                append(dev.deviceModel.ifBlank { "Mobile / Web Device" })
                                                if (isThisDevice) append(" • THIS DEVICE")
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (dev.isBanned) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Device ID: ${dev.deviceId} • IP: ${dev.ipAddress.ifBlank { "Verified" }}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = if (dev.isBanned) {
                                            Color(0xFFEF4444).copy(alpha = 0.2f)
                                        } else {
                                            Color(0xFF10B981).copy(alpha = 0.2f)
                                        }
                                    ) {
                                        Text(
                                            text = if (dev.isBanned) "BLOCKED" else "ACTIVE",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (dev.isBanned) Color(0xFFEF4444) else Color(0xFF10B981)
                                        )
                                    }
                                }

                                if (!isThisDevice || isAdmin) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = {
                                                SoundAndHapticFeedback.playAlertOrError(context)
                                                onToggleDeviceBlocked(dev.deviceId, !dev.isBanned)
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(if (dev.isBanned) "Unblock Device" else "Block Device")
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                SoundAndHapticFeedback.playTap(context)
                                                onRemoveDeviceSession(dev.deviceId)
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Sign Out Device")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // Instagram-Style Password + Optional 2-Step Verification Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isAdmin) {
                                "2-Step Verification (Mandatory for Admin)"
                            } else {
                                "Optional 2-Step Verification (Step 2 PIN)"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Require both your Account Password and your 4–8 digit Step 2 PIN when signing in",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = twoStepToggle,
                        enabled = !isAdmin,
                        onCheckedChange = { enabled ->
                            SoundAndHapticFeedback.playTap(context)
                            twoStepToggle = enabled
                            onUpdateSecurityPreferences(null, enabled, selectedMaxDevices)
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newAccountPassword,
                        onValueChange = { newAccountPassword = it },
                        label = { Text("Update Account Password (min 6 chars)") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            if (newAccountPassword.trim().length >= 6) {
                                SoundAndHapticFeedback.playSuccess(context)
                                onUpdateSecurityPreferences(
                                    newAccountPassword.trim(),
                                    twoStepToggle,
                                    selectedMaxDevices
                                )
                                newAccountPassword = ""
                            } else {
                                SoundAndHapticFeedback.playAlertOrError(context)
                            }
                        }
                    ) {
                        Text("Save")
                    }
                }
            }
        }

        // 4. App Lock PIN & Security Question
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "App Lock PIN & Recovery Question",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Current Security Question: ${userProfile?.displaySecurityQuestion()?.ifBlank { securityQuestion } ?: securityQuestion.ifBlank { "Not configured" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Biometric Fingerprint Unlock", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = isBiometricEnabled, onCheckedChange = onToggleBiometric)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = {
                            SoundAndHapticFeedback.playTap(context)
                            showPinDialog = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("change_pin_button")
                    ) {
                        Icon(Icons.Default.Password, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Change PIN")
                    }
                    Button(
                        onClick = {
                            SoundAndHapticFeedback.playTap(context)
                            onLockNow()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("lock_app_now_button")
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Lock App Now")
                    }
                }
            }
        }

        // 5. Legal Terms, Platform Credits & Sign Out
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Trading Zone Compliance & Platform Info",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Platform Operator: Atharva Garg\nAcademic Guide: Mr. Vikash Chandra Garg\nStrict Policy: Disclosing personal identity, phone numbers, or IDs in Commodity leads to immediate account suspension with NO REFUND.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { showTermsDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Gavel, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Terms & Policy")
                    }
                    Button(
                        onClick = onSignOut,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sign_out_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sign Out")
                    }
                }
            }
        }
    }

    if (showPinDialog) {
        var currentPin by remember { mutableStateOf("") }
        var newPin by remember { mutableStateOf("") }
        var newQuestion by remember { mutableStateOf(securityQuestion) }
        var newAnswer by remember { mutableStateOf("") }
        var errorMsg by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("Update App PIN & Security Question") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = currentPin,
                        onValueChange = { if (it.length <= 8 && it.all(Char::isDigit)) currentPin = it },
                        label = { Text("Current App PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { if (it.length <= 8 && it.all(Char::isDigit)) newPin = it },
                        label = { Text("New 4–8 Digit PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newQuestion,
                        onValueChange = { newQuestion = it },
                        label = { Text("Custom Recovery Security Question") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newAnswer,
                        onValueChange = { newAnswer = it },
                        label = { Text("Recovery Answer") },
                        singleLine = true
                    )
                    errorMsg?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (currentPin.isNotBlank() && newPin.length >= 4) {
                            if (!onUpdatePin(currentPin, newPin)) {
                                errorMsg = "Current PIN is incorrect."
                                return@Button
                            }
                        }
                        if (newQuestion.isNotBlank() && newAnswer.isNotBlank()) {
                            onUpdateSecurityQuestion(newQuestion.trim(), newAnswer.trim())
                        }
                        showPinDialog = false
                    }
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            title = { Text("Terms, Conditions & No-Refund Policy") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = DEFAULT_TERMS_AND_CONDITIONS.trim(),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showTermsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
