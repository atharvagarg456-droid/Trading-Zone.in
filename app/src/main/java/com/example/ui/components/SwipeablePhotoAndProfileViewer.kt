package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.BroadcastMessage
import com.example.data.model.FirmwareUpdateConfig
import com.example.data.model.PRIMARY_ADMIN_EMAIL
import com.example.data.model.UserProfile
import com.example.util.SoundAndHapticFeedback
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SwipeableGalleryItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val imageBase64OrUrl: String,
    val badgeText: String = "",
    val details: List<Pair<String, String>> = emptyList()
)

fun UserProfile.toSwipeableGalleryItem(activeDevicesCount: Int = 1): SwipeableGalleryItem {
    val isMainAdmin = email.equals(PRIMARY_ADMIN_EMAIL, ignoreCase = true)
    val statusBadge = when {
        isMainAdmin -> "MAIN ADMIN"
        accountStatus == "BANNED" -> "BANNED"
        entitlementStatus == "PAID_ACTIVE" && isEntitledNow() -> activePlanName.ifBlank { "PAID ACTIVE" }
        entitlementStatus == "TRIAL_ACTIVE" && isEntitledNow() -> "7-DAY FREE TRIAL"
        accountStatus == "PENDING_APPROVAL" -> "PENDING APPROVAL"
        else -> "EXPIRED / INACTIVE"
    }
    val detailsList = buildList {
        add("Email ID" to email.ifBlank { "N/A" })
        if (phoneNumber.isNotBlank()) add("Phone Number" to phoneNumber)
        if (city.isNotBlank()) add("City" to city)
        add("Plan Tier" to activePlanName.ifBlank {
            if (entitlementStatus == "TRIAL_ACTIVE") "7-Day Free Trial" else "No Active Plan"
        })
        add("Validity Left" to "${daysRemaining()} Days")
        if (deviceModel.isNotBlank()) add("Primary Device" to deviceModel)
        add("Device Limit" to "$activeDevicesCount / ${maxDevicesLimit()} Devices")
        add("2-Step Security" to if (isTwoStepEnabled()) "Enabled (App + Password)" else "Password Protected")
    }
    val resolvedPhoto = pendingPhotoBase64.ifBlank { photoBase64 }
    return SwipeableGalleryItem(
        id = userId.ifBlank { id },
        title = fullName.ifBlank { email.substringBefore("@").ifBlank { "Member" } },
        subtitle = email,
        imageBase64OrUrl = resolvedPhoto,
        badgeText = statusBadge,
        details = detailsList
    )
}

fun BroadcastMessage.toSwipeableGalleryItem(): SwipeableGalleryItem {
    val sig = resolvedSignalType()
    val badge = buildString {
        if (sig.isNotBlank()) append("$sig • ")
        append(riskLevel.uppercase(Locale.ROOT) + " RISK")
    }
    val detailsList = buildList {
        add("Target Tier" to targetTierDisplayLabel())
        if (category.isNotBlank() && category != "NONE") add("Category" to category)
        if (authorName.isNotBlank()) add("Published By" to authorName)
        if (content.isNotBlank()) add("Signal Summary" to content.take(180))
    }
    return SwipeableGalleryItem(
        id = id,
        title = title,
        subtitle = targetTierDisplayLabel(),
        imageBase64OrUrl = markedPhotoBase64,
        badgeText = badge,
        details = detailsList
    )
}

@Composable
fun SwipeablePhotoAndProfileDialog(
    items: List<SwipeableGalleryItem>,
    initialIndex: Int = 0,
    dialogTitle: String = "Swipe Profiles & Photos",
    onDismiss: () -> Unit
) {
    if (items.isEmpty()) {
        onDismiss()
        return
    }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val safeInitial = initialIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
    val pagerState = rememberPagerState(
        initialPage = safeInitial,
        pageCount = { items.size }
    )

    LaunchedEffect(pagerState.currentPage) {
        SoundAndHapticFeedback.playClick(context)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF050811),
                            Color(0xFF0B1224),
                            Color(0xFF050811)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = dialogTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Swipe Left / Right or Pinch to Zoom • ${pagerState.currentPage + 1} of ${items.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    FilledIconButton(
                        onClick = {
                            SoundAndHapticFeedback.playClick(context)
                            onDismiss()
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color.White.copy(alpha = 0.14f),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.testTag("close_swipe_viewer_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Viewer"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Horizontal Pager for Swiping Profiles & Photos
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag("swipe_photo_profile_pager"),
                    pageSpacing = 16.dp
                ) { pageIndex ->
                    val item = items[pageIndex]
                    var zoomScale by remember(pageIndex) { mutableFloatStateOf(1f) }
                    var panOffset by remember(pageIndex) { mutableStateOf(Offset.Zero) }
                    val decodedBitmap = remember(item.imageBase64OrUrl) {
                        if (item.imageBase64OrUrl.isNotBlank() && !item.imageBase64OrUrl.startsWith("http")) {
                            ImageCodecHelper.decodeBase64ToBitmap(item.imageBase64OrUrl)
                        } else {
                            null
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF10192E)
                        ),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.28f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Interactive Zoomable Photo / Avatar Container
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(if (item.details.isEmpty()) 420.dp else 270.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFF070C18))
                                    .border(
                                        1.dp,
                                        Color.White.copy(alpha = 0.12f),
                                        RoundedCornerShape(20.dp)
                                    )
                                    .pointerInput(pageIndex) {
                                        detectTransformGestures { _, pan, zoom, _ ->
                                            val newScale = (zoomScale * zoom).coerceIn(1f, 3.5f)
                                            zoomScale = newScale
                                            panOffset = if (newScale > 1f) {
                                                Offset(
                                                    x = (panOffset.x + pan.x).coerceIn(-350f, 350f),
                                                    y = (panOffset.y + pan.y).coerceIn(-350f, 350f)
                                                )
                                            } else {
                                                Offset.Zero
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (decodedBitmap != null) {
                                    Image(
                                        bitmap = decodedBitmap.asImageBitmap(),
                                        contentDescription = item.title,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer(
                                                scaleX = zoomScale,
                                                scaleY = zoomScale,
                                                translationX = panOffset.x,
                                                translationY = panOffset.y
                                            )
                                    )
                                } else {
                                    DefaultProfileAvatarFallback(title = item.title)
                                }

                                // Zoom controls overlay
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(10.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(Color.Black.copy(alpha = 0.65f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = {
                                            SoundAndHapticFeedback.playClick(context)
                                            zoomScale = (zoomScale - 0.5f).coerceAtLeast(1f)
                                            if (zoomScale <= 1f) panOffset = Offset.Zero
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ZoomOut,
                                            contentDescription = "Zoom Out",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Text(
                                        text = "${(zoomScale * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    IconButton(
                                        onClick = {
                                            SoundAndHapticFeedback.playClick(context)
                                            zoomScale = (zoomScale + 0.5f).coerceAtMost(3.5f)
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ZoomIn,
                                            contentDescription = "Zoom In",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                if (item.badgeText.isNotBlank()) {
                                    Surface(
                                        color = Color(0xFF0284C7),
                                        shape = RoundedCornerShape(50),
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(10.dp)
                                    ) {
                                        Text(
                                            text = item.badgeText,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Title & Subtitle
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            if (item.subtitle.isNotBlank()) {
                                Text(
                                    text = item.subtitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF38BDF8),
                                    textAlign = TextAlign.Center
                                )
                            }

                            if (item.details.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color.White.copy(alpha = 0.04f))
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    item.details.forEach { (label, value) ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = Color(0xFF94A3B8),
                                                modifier = Modifier.weight(0.42f)
                                            )
                                            Text(
                                                text = value,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.White,
                                                textAlign = TextAlign.End,
                                                modifier = Modifier.weight(0.58f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Previous / Next Navigation Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            if (pagerState.currentPage > 0) {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                            }
                        },
                        enabled = pagerState.currentPage > 0,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E293B),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Previous"
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Prev", fontWeight = FontWeight.Bold)
                    }

                    Text(
                        text = "${pagerState.currentPage + 1} / ${items.size}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF38BDF8)
                    )

                    Button(
                        onClick = {
                            if (pagerState.currentPage < items.size - 1) {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            }
                        },
                        enabled = pagerState.currentPage < items.size - 1,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0284C7),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Next", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Next"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DefaultProfileAvatarFallback(title: String) {
    val initials = title.trim().split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.take(1).uppercase() }
        .ifBlank { "VIP" }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(116.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF0284C7), Color(0xFF6366F1))
                    )
                )
                .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Swipe Left / Right to Browse",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF94A3B8)
        )
    }
}

/**
 * Full-Screen Mandatory Firmware Update Gate pushed by Admin to all devices.
 */
@Composable
fun MandatoryFirmwareUpdateScreen(
    firmwareConfig: FirmwareUpdateConfig,
    isAdmin: Boolean,
    onApplyUpdateNow: () -> Unit,
    onAdminDisableGate: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var isInstalling by remember { mutableStateOf(false) }
    var progressValue by remember { mutableFloatStateOf(0f) }

    val pulseTransition = rememberInfiniteTransition(label = "fwPulse")
    val glowAlpha by pulseTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fwGlow"
    )

    LaunchedEffect(isInstalling) {
        if (isInstalling) {
            SoundAndHapticFeedback.playTradeSignal(context, "BUY")
            for (step in 1..20) {
                delay(75)
                progressValue = step / 20f
                if (step % 5 == 0) {
                    SoundAndHapticFeedback.playClick(context)
                }
            }
            SoundAndHapticFeedback.playTradeSignal(context, "TARGET HIT")
            onApplyUpdateNow()
        }
    }

    val formattedTime = remember(firmwareConfig.pushedAtMillis) {
        if (firmwareConfig.pushedAtMillis > 0L) {
            SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                .format(Date(firmwareConfig.pushedAtMillis))
        } else {
            "Just Now"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF040711),
                        Color(0xFF0A1428),
                        Color(0xFF040711)
                    )
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF0F172A)
            ),
            border = BorderStroke(
                2.dp,
                Color(0xFF38BDF8).copy(alpha = glowAlpha)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF0284C7), Color(0xFF2563EB))
                            )
                        )
                        .border(2.dp, Color(0xFF7DD3FC), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = "Mandatory Firmware Update",
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    color = Color(0xFFEF4444).copy(alpha = 0.18f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.55f))
                ) {
                    Text(
                        text = "⚡ MANDATORY ADMIN FIRMWARE UPDATE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFCA5A5),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "New Official Firmware ${firmwareConfig.versionName}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Pushed by Main Admin • $formattedTime",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF38BDF8),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF1E293B).copy(alpha = 0.7f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "What's Included in Firmware ${firmwareConfig.versionName}:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF7DD3FC)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = firmwareConfig.releaseNotes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFE2E8F0)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (isInstalling) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LinearProgressIndicator(
                            progress = { progressValue },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(50)),
                            color = Color(0xFF38BDF8),
                            trackColor = Color(0xFF1E293B)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Synchronizing Firmware Build ${firmwareConfig.versionName}... ${(progressValue * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                } else {
                    Button(
                        onClick = { isInstalling = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("apply_mandatory_firmware_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0284C7),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Update & Synchronize Firmware Now",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    if (isAdmin && onAdminDisableGate != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = onAdminDisableGate,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF334155),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Turn Off Mandatory Firmware Gate for All Devices",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
