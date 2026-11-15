package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import com.example.data.local.MarketInstrumentQuote
import com.example.data.model.BroadcastMessage
import com.example.data.model.ChatBubbleStylePreference
import com.example.data.model.DeviceRecord
import com.example.data.model.MessageReadReceipt
import com.example.data.model.PricingConfig
import com.example.data.model.UserProfile
import com.example.data.local.LiveMarketEngine
import com.example.ui.components.AttachedLiveMarketSetupCard
import com.example.ui.components.ImageCodecHelper
import com.example.ui.components.LiveMarketWatchboardCard
import com.example.ui.components.SwipeableGalleryItem
import com.example.ui.components.SwipeablePhotoAndProfileDialog
import com.example.ui.components.toSwipeableGalleryItem
import com.example.util.SoundAndHapticFeedback
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun TradeSignalTypeBadge(
    signalType: String,
    onClick: () -> Unit = {}
) {
    val upper = signalType.uppercase(Locale.ROOT).trim()
    val (bg, fg, label) = when (upper) {
        "BUY" -> Triple(Color(0xFF10B981).copy(alpha = 0.20f), Color(0xFF34D399), "🟢 BUY SIGNAL")
        "SELL" -> Triple(Color(0xFFEF4444).copy(alpha = 0.20f), Color(0xFFF87171), "🔴 SELL SIGNAL")
        "TARGET HIT", "TARGET_HIT", "TARGET" -> Triple(
            Color(0xFFF59E0B).copy(alpha = 0.22f),
            Color(0xFFFBBF24),
            "🎯 TARGET HIT"
        )
        else -> return
    }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bg,
        border = BorderStroke(1.dp, fg.copy(alpha = 0.45f)),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun TradeRiskBadge(riskLevel: String) {
    val upper = riskLevel.uppercase(Locale.ROOT)
    val (bg, fg, label) = when (upper) {
        "HIGH" -> Triple(Color(0xFFEF4444).copy(alpha = 0.18f), Color(0xFFEF4444), "HIGH RISK")
        "LOW" -> Triple(Color(0xFF10B981).copy(alpha = 0.18f), Color(0xFF10B981), "LOW RISK")
        else -> Triple(Color(0xFFF59E0B).copy(alpha = 0.18f), Color(0xFFF59E0B), "MEDIUM RISK")
    }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bg
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun BroadcastStreamScreen(
    messages: List<BroadcastMessage>,
    openedMessageIds: Set<String> = emptySet(),
    allMessageReadReceipts: List<MessageReadReceipt> = emptyList(),
    userProfile: UserProfile?,
    deviceRecord: DeviceRecord?,
    pricingConfig: PricingConfig,
    onlinePeopleCount: Int,
    compactFeedMode: Boolean = false,
    largeTextMode: Boolean = false,
    isAdmin: Boolean,
    emojiReactionsByTarget: Map<String, Map<String, Int>> = emptyMap(),
    myReactedKeys: Set<String> = emptySet(),
    onMarkMessageOpened: (BroadcastMessage) -> Unit = {},
    onToggleReaction: (targetId: String, emoji: String) -> Unit = { _, _ -> },
    onRequestFreeTrial: () -> Unit,
    onNavigateToMembership: () -> Unit,
    onNavigateToReviews: () -> Unit = {},
    onReplyToMessageRequest: (BroadcastMessage) -> Unit = {},
    onEditMessageRequest: (BroadcastMessage) -> Unit,
    onDeleteMessageRequest: (String) -> Unit,
    chatBubbleStyle: ChatBubbleStylePreference = ChatBubbleStylePreference.MODERN_GLASS,
    liveMarketQuotes: List<MarketInstrumentQuote> = emptyList(),
    onOpenAdminMarketSearch: () -> Unit = {},
    onSelectMarketQuote: (MarketInstrumentQuote) -> Unit = {},
    onOpenTradeSetupForQuote: (MarketInstrumentQuote) -> Unit = {},
    onQuickPublishBroadcast: (
        title: String,
        content: String,
        targetTier: String,
        signalType: String,
        markedPhotoBase64: String
    ) -> Unit = { _, _, _, _, _ -> }
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isEntitled = isAdmin || (userProfile?.isEntitledNow() == true)
    var swipeGalleryItems by remember { mutableStateOf<List<SwipeableGalleryItem>>(emptyList()) }
    var swipeInitialIndex by remember { mutableStateOf(0) }
    var inspectSeenByMessage by remember { mutableStateOf<BroadcastMessage?>(null) }
    var previewQuotedMessage by remember { mutableStateOf<BroadcastMessage?>(null) }
    var showQuickComposeDialog by remember { mutableStateOf(false) }

    val eligibleChartGallery = remember(messages, userProfile, isAdmin) {
        messages.filter {
            it.markedPhotoBase64.isNotBlank() && it.isUserEligibleForTier(userProfile, isAdmin)
        }.map { it.toSwipeableGalleryItem() }
    }

    val receiptsByMessageId = remember(allMessageReadReceipts) {
        allMessageReadReceipts.groupBy { it.messageId }
    }

    val unopenedCount = remember(messages, openedMessageIds, isAdmin) {
        if (isAdmin) 0 else messages.count { it.id.isNotBlank() && it.id !in openedMessageIds }
    }

    if (swipeGalleryItems.isNotEmpty()) {
        SwipeablePhotoAndProfileDialog(
            items = swipeGalleryItems,
            initialIndex = swipeInitialIndex,
            dialogTitle = "Swipe Marked Trade Charts",
            onDismiss = { swipeGalleryItems = emptyList() }
        )
    }

    if (inspectSeenByMessage != null) {
        val msg = inspectSeenByMessage!!
        val seenList = receiptsByMessageId[msg.id].orEmpty()
        SeenByMembersDialog(
            messageTitle = msg.title,
            receipts = seenList,
            onDismiss = { inspectSeenByMessage = null }
        )
    }

    if (previewQuotedMessage != null) {
        val quoted = previewQuotedMessage!!
        AlertDialog(
            onDismissRequest = { previewQuotedMessage = null },
            title = { Text(quoted.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TradeRiskBadge(riskLevel = quoted.riskLevel)
                    Text(
                        text = quoted.content,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { previewQuotedMessage = null }) {
                    Text("Close")
                }
            }
        )
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        if (!isEntitled) {
            val canClaimTrial = userProfile?.trialUsed == false && deviceRecord?.trialClaimed != true
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 460.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Verified Trade Broadcasts Locked",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Activate your 1-time 7-Day Free Trial or a UPI QR Membership Plan (${pricingConfig.planName} • ₹${pricingConfig.priceInr.toInt()}) to unlock live trade broadcasts and marked chart setups.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        if (canClaimTrial) {
                            Button(
                                onClick = onRequestFreeTrial,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("claim_free_trial_button")
                            ) {
                                Text("Activate 1-Time 7-Day Free Trial")
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                        OutlinedButton(
                            onClick = onNavigateToMembership,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("View UPI QR Membership Plans")
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp)
                    .testTag("broadcast_stream_list"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(if (compactFeedMode) 10.dp else 14.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                            MaterialTheme.colorScheme.surface
                                        )
                                    )
                                )
                                .padding(18.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Official Trade Broadcasts",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = Color(0xFF10B981).copy(alpha = 0.18f)
                                    ) {
                                        Text(
                                            text = "● $onlinePeopleCount Online",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color(0xFF10B981),
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                val ticker = pricingConfig.announcementTicker()
                                if (ticker.isNotBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "⚡ $ticker",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                                val pnlBanner = pricingConfig.todayProfitBanner()
                                if (pnlBanner.enabled) {
                                    val pColor = if (pnlBanner.isProfit) Color(0xFF00E676) else Color(0xFFFF5252)
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = pColor.copy(alpha = 0.14f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onNavigateToReviews() }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = if (pnlBanner.isProfit) {
                                                        "TRADE SIGNAL PROFIT (AS PER CALL) • ${pnlBanner.instrumentText}"
                                                    } else {
                                                        "TRADE SIGNAL P&L (AS PER CALL) • ${pnlBanner.instrumentText}"
                                                    },
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = pColor,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                                if (pnlBanner.linkedBroadcastTitle.isNotBlank()) {
                                                    Text(
                                                        text = "Call: ${pnlBanner.linkedBroadcastTitle}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                                Text(
                                                    text = "${pnlBanner.amountText} (${pnlBanner.roiPercent}) — Tap to react in Commodity 🔥",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                                Text(
                                    text = if (unopenedCount > 0) {
                                        "You have $unopenedCount new unopened message(s). Tap the envelope button to open."
                                    } else {
                                        "All trade broadcasts opened • Tap Commodity to discuss setups."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = onNavigateToReviews,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = "Commodity & Live P&L",
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (eligibleChartGallery.isNotEmpty()) {
                                        Button(
                                            onClick = {
                                                swipeInitialIndex = 0
                                                swipeGalleryItems = eligibleChartGallery
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.testTag("swipe_all_charts_button")
                                        ) {
                                            Text("📸 Swipe Charts (${eligibleChartGallery.size})")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Investing.com-Style Live Market Desk & Watchboard (With Admin Search & Pin)
                if (liveMarketQuotes.isNotEmpty()) {
                    item(key = "broadcast_live_market_desk") {
                        LiveMarketWatchboardCard(
                            quotes = liveMarketQuotes,
                            isAdmin = isAdmin,
                            onOpenAdminSearchDialog = onOpenAdminMarketSearch,
                            onSelectQuote = onSelectMarketQuote,
                            onOpenSetMarketWithMessage = onOpenTradeSetupForQuote
                        )
                    }
                }

                if (messages.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Text(
                                text = "No broadcast messages published yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(24.dp)
                            )
                        }
                    }
                } else {
                    items(messages, key = { it.id.ifBlank { it.title + it.createdAt } }) { message ->
                        val isOpened = isAdmin || message.id in openedMessageIds
                        val isEligibleForTier = message.isUserEligibleForTier(userProfile, isAdmin)
                        val messageReceipts = receiptsByMessageId[message.id].orEmpty()
                        val msgTargetId = "msg_${message.id}"

                        Box(
                            modifier = Modifier.animateItem(
                                fadeInSpec = androidx.compose.animation.core.tween(160),
                                fadeOutSpec = androidx.compose.animation.core.tween(120),
                                placementSpec = spring(
                                    dampingRatio = 0.85f,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            )
                        ) {
                            BroadcastMessageCard(
                                message = message,
                                isOpened = isOpened,
                                isEligibleForTier = isEligibleForTier,
                                seenByCount = messageReceipts.size,
                                largeTextMode = largeTextMode,
                                chatBubbleStyle = chatBubbleStyle,
                                isAdmin = isAdmin,
                                targetId = msgTargetId,
                                reactions = emojiReactionsByTarget[msgTargetId].orEmpty(),
                                myReactedKeys = myReactedKeys,
                                onReact = { emoji -> onToggleReaction(msgTargetId, emoji) },
                                onOpenEnvelope = { onMarkMessageOpened(message) },
                                onUpgradePlanRequest = onNavigateToMembership,
                                onInspectSeenBy = { inspectSeenByMessage = message },
                                onClickQuotedReply = {
                                    val target = messages.find { it.id == message.replyToMessageId }
                                    if (target != null) {
                                        previewQuotedMessage = target
                                    }
                                },
                                onExpandPhoto = {
                                    val idx = eligibleChartGallery.indexOfFirst { it.id == message.id }
                                    if (idx >= 0) {
                                        swipeInitialIndex = idx
                                        swipeGalleryItems = eligibleChartGallery
                                    } else {
                                        swipeInitialIndex = 0
                                        swipeGalleryItems = listOf(message.toSwipeableGalleryItem())
                                    }
                                },
                                onReply = { onReplyToMessageRequest(message) },
                                onEdit = { onEditMessageRequest(message) },
                                onDelete = { onDeleteMessageRequest(message.id) },
                                liveMarketQuotes = liveMarketQuotes,
                                onSelectMarketQuote = onSelectMarketQuote
                            )
                        }
                    }
                }
            }

            // WhatsApp-style Floating Pencil Compose Button directly inside the Messages Tab for Admin
            if (isAdmin) {
                ExtendedFloatingActionButton(
                    onClick = {
                        SoundAndHapticFeedback.playClick(context)
                        showQuickComposeDialog = true
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Write Broadcast Message"
                        )
                    },
                    text = {
                        Text(
                            text = "Write Message",
                            fontWeight = FontWeight.ExtraBold
                        )
                    },
                    containerColor = Color(0xFF10B981),
                    contentColor = Color.Black,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(20.dp)
                        .testTag("whatsapp_pencil_compose_fab")
                )
            }
        }
    }

    if (showQuickComposeDialog && isAdmin) {
        QuickBroadcastComposeDialog(
            pricingConfig = pricingConfig,
            liveMarketQuotes = liveMarketQuotes,
            onOpenFullMarketSetup = { quote ->
                showQuickComposeDialog = false
                onOpenTradeSetupForQuote(quote)
            },
            onDismiss = { showQuickComposeDialog = false },
            onPublish = { title, content, tier, sig, photo ->
                onQuickPublishBroadcast(title, content, tier, sig, photo)
                showQuickComposeDialog = false
            }
        )
    }
}

@Composable
private fun QuickBroadcastComposeDialog(
    pricingConfig: PricingConfig,
    liveMarketQuotes: List<MarketInstrumentQuote> = emptyList(),
    onOpenFullMarketSetup: (MarketInstrumentQuote) -> Unit = {},
    onDismiss: () -> Unit,
    onPublish: (title: String, content: String, targetTier: String, signalType: String, markedPhotoBase64: String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var selectedTier by remember { mutableStateOf("ALL") }
    var selectedSignal by remember { mutableStateOf("AUTO") }
    var photoBase64 by remember { mutableStateOf("") }

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
                photoBase64 = encoded
            }
        }
    }

    val tierOptions = remember(pricingConfig) {
        val dynamicPlans = pricingConfig.availablePlans().map { it.name to "🎯 ${it.name}" }
        listOf(
            "ALL" to "🌐 Same to All Members",
            "PAID_ONLY" to "💎 Paid Members Only"
        ) + dynamicPlans
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF10B981))
                Text("Quick Broadcast Message", fontWeight = FontWeight.ExtraBold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (liveMarketQuotes.isNotEmpty()) {
                    Button(
                        onClick = {
                            SoundAndHapticFeedback.playTap(context)
                            val firstPinned = liveMarketQuotes.firstOrNull { it.isPinned } ?: liveMarketQuotes.first()
                            onOpenFullMarketSetup(firstPinned)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFC400),
                            contentColor = Color.Black
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_full_market_target_setup_button")
                    ) {
                        Text(
                            text = "🎯 Set Live Market & Targets with Message (Gold/Silver/Nifty)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Text(
                    text = "⚡ 1-Tap Broadcast Quick Templates",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        Triple("🟢 BUY CALL", "BUY", "BUY MCX GOLD @ CMP") to "Entry: CMP\nStop Loss: Strict Intraday SL\nTarget 1: +250 pts\nTarget 2: +480 pts",
                        Triple("🔴 SELL CALL", "SELL", "SELL MCX SILVER @ CMP") to "Entry: CMP\nStop Loss: Strict Intraday SL\nTarget 1: -450 pts\nTarget 2: -900 pts",
                        Triple("🎯 TARGET 1 HIT", "TARGET HIT", "🎯 TARGET 1 ACHIEVED — BOOK 50%") to "First target achieved cleanly as per our broadcast call! Book 50% profit and trail Stop Loss to cost.",
                        Triple("🔥 FULL TARGET HIT", "TARGET HIT", "🔥 ALL TARGETS ACHIEVED — FULL PROFIT") to "Boom! All intraday targets achieved with high precision. Lock in full profits now!",
                        Triple("🛡️ TRAIL SL TO COST", "AUTO", "🛡️ RISK UPDATE — TRAIL STOP LOSS TO COST") to "Trade is moving strongly in our favor. Trail your Stop Loss to Entry Cost for a zero-risk trade."
                    ).forEach { (meta, bodyTemplate) ->
                        val (chipLabel, sigType, titleTemplate) = meta
                        FilterChip(
                            selected = title == titleTemplate,
                            onClick = {
                                SoundAndHapticFeedback.playTap(context)
                                title = titleTemplate
                                content = bodyTemplate
                                selectedSignal = sigType
                            },
                            label = { Text(chipLabel, style = MaterialTheme.typography.labelSmall, maxLines = 1) }
                        )
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Headline (e.g. BUY MCX GOLD 76500)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quick_compose_title_input")
                )

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Trade Levels, Stop Loss & Targets") },
                    minLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quick_compose_content_input")
                )

                Text(
                    text = "Target Membership Audience",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    tierOptions.forEach { (tierKey, tierLabel) ->
                        FilterChip(
                            selected = selectedTier == tierKey,
                            onClick = {
                                SoundAndHapticFeedback.playTap(context)
                                selectedTier = tierKey
                            },
                            label = { Text(tierLabel, maxLines = 1, softWrap = false) }
                        )
                    }
                }

                Text(
                    text = "Signal Haptic Vibration",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "AUTO" to "⚡ Auto",
                        "BUY" to "🟢 BUY",
                        "SELL" to "🔴 SELL",
                        "TARGET HIT" to "🎯 TARGET HIT"
                    ).forEach { (sigKey, sigLabel) ->
                        FilterChip(
                            selected = selectedSignal == sigKey,
                            onClick = {
                                SoundAndHapticFeedback.playTap(context)
                                selectedSignal = sigKey
                            },
                            label = { Text(sigLabel, maxLines = 1, softWrap = false) }
                        )
                    }
                }

                OutlinedButton(
                    onClick = {
                        imagePicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (photoBase64.isBlank()) "Attach Chart Photo (Optional)" else "✅ Chart Photo Attached (Tap to Change)")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank() || content.isBlank()) return@Button
                    val resolvedSig = if (selectedSignal == "AUTO") {
                        SoundAndHapticFeedback.detectSignalType(title, content)
                    } else {
                        selectedSignal
                    }
                    if (resolvedSig.isNotBlank()) {
                        SoundAndHapticFeedback.playTradeSignal(context, resolvedSig)
                    } else {
                        SoundAndHapticFeedback.playSuccess(context)
                    }
                    onPublish(title.trim(), content.trim(), selectedTier, selectedSignal, photoBase64)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF10B981),
                    contentColor = Color.Black
                ),
                modifier = Modifier.testTag("quick_compose_send_button")
            ) {
                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Send Now", fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BroadcastMessageCard(
    message: BroadcastMessage,
    isOpened: Boolean,
    isEligibleForTier: Boolean,
    seenByCount: Int,
    largeTextMode: Boolean,
    chatBubbleStyle: ChatBubbleStylePreference = ChatBubbleStylePreference.MODERN_GLASS,
    isAdmin: Boolean,
    targetId: String,
    reactions: Map<String, Int>,
    myReactedKeys: Set<String>,
    onReact: (String) -> Unit,
    onOpenEnvelope: () -> Unit,
    onUpgradePlanRequest: () -> Unit,
    onInspectSeenBy: () -> Unit,
    onClickQuotedReply: () -> Unit,
    onExpandPhoto: (String) -> Unit,
    onReply: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    liveMarketQuotes: List<MarketInstrumentQuote> = emptyList(),
    onSelectMarketQuote: (MarketInstrumentQuote) -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val uriHandler = LocalUriHandler.current
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy • hh:mm a", Locale.getDefault()) }
    val dateStr = remember(message.createdAt) {
        message.createdAt?.toDate()?.let { dateFormat.format(it) } ?: "Just now"
    }
    val resolvedSig = remember(message.title, message.content, message.linkTitle) {
        message.resolvedSignalType()
    }
    val tierLabel = remember(message.linkTitle) {
        message.targetTierDisplayLabel()
    }
    val isRestrictedTier = remember(message.linkTitle) {
        val t = message.targetMembershipTier()
        t.isNotBlank() && !t.equals("ALL", ignoreCase = true)
    }
    val cornerRadius = chatBubbleStyle.cornerDp.dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (!isEligibleForTier) {
                    Modifier.border(1.5.dp, Color(0xFFF59E0B).copy(alpha = 0.65f), RoundedCornerShape(cornerRadius))
                } else if (message.isPinned) {
                    Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(cornerRadius))
                } else {
                    Modifier
                }
            )
            .testTag("broadcast_card_${message.id}"),
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            if (!isEligibleForTier) {
                // Locked VIP Broadcast Card for lower-tier or free-trial members
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFFF59E0B).copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "👑 VIP TIER EXCLUSIVE • ${tierLabel.uppercase(Locale.ROOT)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFBBF24),
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFF59E0B).copy(alpha = 0.16f),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Locked VIP Broadcast",
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "VIP Trade Signal Locked",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Published $dateStr by ${message.authorName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This high-conviction signal is targeted exclusively to $tierLabel members. Upgrade your membership plan to unlock this broadcast and marked chart setup immediately.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            SoundAndHapticFeedback.playClick(context)
                            onUpgradePlanRequest()
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("upgrade_vip_broadcast_${message.id}")
                    ) {
                        Text(
                            text = "⚡ Upgrade to $tierLabel to Unlock",
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            } else {
                // Unopened Envelope State for Eligible Members
                AnimatedVisibility(
                    visible = !isOpened,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.MarkEmailUnread,
                                    contentDescription = "New Unopened Message",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Trading Zone • 1 New Message",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (isRestrictedTier) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "👑 $tierLabel Broadcast",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFBBF24),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Posted $dateStr by ${message.authorName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                if (resolvedSig.isNotBlank()) {
                                    SoundAndHapticFeedback.playTradeSignal(context, resolvedSig)
                                } else {
                                    SoundAndHapticFeedback.playSuccess(context)
                                }
                                onOpenEnvelope()
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("open_message_envelope_${message.id}")
                        ) {
                            Text("1 New Message — Tap to Open", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Opened Message State with Smooth Spring Reveal
                AnimatedVisibility(
                    visible = isOpened,
                    enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                        expandVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy))
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                if (message.isPinned) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = "📌 PINNED",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                                if (resolvedSig.isNotBlank()) {
                                    TradeSignalTypeBadge(
                                        signalType = resolvedSig,
                                        onClick = {
                                            SoundAndHapticFeedback.playTradeSignal(context, resolvedSig)
                                        }
                                    )
                                }
                                if (isRestrictedTier || isAdmin) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isRestrictedTier) {
                                            Color(0xFFF59E0B).copy(alpha = 0.18f)
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    ) {
                                        Text(
                                            text = if (isRestrictedTier) "👑 $tierLabel" else "👥 All Members",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isRestrictedTier) {
                                                Color(0xFFFBBF24)
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                                if (message.hasDisplayCategory()) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer
                                    ) {
                                        Text(
                                            text = message.category,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                                if (message.hasDisplayRiskLevel()) {
                                    TradeRiskBadge(riskLevel = message.riskLevel)
                                }
                            }

                        if (isAdmin) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedButton(
                                    onClick = onInspectSeenBy,
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .height(32.dp)
                                        .testTag("seen_by_button_${message.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Visibility,
                                        contentDescription = "Seen By",
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Seen ($seenByCount)",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                                IconButton(
                                    onClick = onReply,
                                    modifier = Modifier.testTag("reply_message_${message.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Reply,
                                        contentDescription = "Reply to Message",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(onClick = onEdit) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Message"
                                    )
                                }
                                IconButton(onClick = onDelete) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Message",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }

                    // WhatsApp-style Quoted Reply Bar if replying to a previous message
                    if (message.replyToMessageId.isNotBlank() || message.replyToTitle.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onClickQuotedReply() }
                                .testTag("quoted_reply_bar_${message.id}")
                        ) {
                            Row(modifier = Modifier.padding(10.dp)) {
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .height(40.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "↩ Replying to: ${message.replyToTitle.ifBlank { "Previous Trade Message" }}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (message.replyToSnippet.isNotBlank()) {
                                        Text(
                                            text = message.replyToSnippet,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = message.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "By ${message.authorName} • $dateStr" + if (message.isEdited) " • (Edited)" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(10.dp))

                    val (attachedSetup, cleanMessageText) = remember(message.content) {
                        LiveMarketEngine.parseMessageWithMarketSetup(message.content)
                    }

                    if (attachedSetup != null) {
                        val matchedQuote = remember(liveMarketQuotes, attachedSetup.symbolId) {
                            liveMarketQuotes.firstOrNull { it.symbolId.equals(attachedSetup.symbolId, ignoreCase = true) }
                        }
                        AttachedLiveMarketSetupCard(
                            setup = attachedSetup,
                            liveQuote = matchedQuote,
                            onOpenLiveChart = if (matchedQuote != null) {
                                { onSelectMarketQuote(matchedQuote) }
                            } else {
                                null
                            }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    if (cleanMessageText.isNotBlank()) {
                        Text(
                            text = cleanMessageText,
                            style = if (largeTextMode) {
                                MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp, lineHeight = 26.sp)
                            } else {
                                MaterialTheme.typography.bodyLarge
                            },
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (message.markedPhotoBase64.isNotBlank()) {
                        val chartBmp = remember(message.markedPhotoBase64) {
                            ImageCodecHelper.decodeBase64ToBitmap(message.markedPhotoBase64)
                        }
                        if (chartBmp != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Image(
                                bitmap = chartBmp.asImageBitmap(),
                                contentDescription = "Marked Chart Photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 180.dp, max = 280.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onExpandPhoto(message.markedPhotoBase64) }
                            )
                        }
                    }

                    if (message.linkUrl.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = {
                                try {
                                    uriHandler.openUri(message.linkUrl)
                                } catch (_: Exception) {
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(message.displayLinkTitle().ifBlank { "Open Reference Link" })
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    TelegramEmojiReactionRow(
                        targetId = targetId,
                        reactions = reactions,
                        myReactedKeys = myReactedKeys,
                        onReact = onReact
                    )
                }
            }
            }
        }
    }
}

@Composable
private fun SeenByMembersDialog(
    messageTitle: String,
    receipts: List<MessageReadReceipt>,
    onDismiss: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM • hh:mm a", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Seen By (${receipts.size} Members)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = messageTitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            if (receipts.isEmpty()) {
                Text(
                    text = "No members have clicked to open this message yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(receipts, key = { it.id.ifBlank { it.userId } }) { receipt ->
                        val avatarBmp = remember(receipt.userPhotoBase64) {
                            ImageCodecHelper.decodeBase64ToBitmap(receipt.userPhotoBase64)
                        }
                        val timeStr = remember(receipt.viewedAt) {
                            receipt.viewedAt?.toDate()?.let { dateFormat.format(it) } ?: "Recently"
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (avatarBmp != null) {
                                Image(
                                    bitmap = avatarBmp.asImageBitmap(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = null)
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = receipt.userName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${receipt.userEmail} • Opened $timeStr",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
