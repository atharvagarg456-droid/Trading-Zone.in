package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddReaction
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.MarketInstrumentQuote
import com.example.data.model.AutoTopicClassifier
import com.example.data.model.BroadcastMessage
import com.example.data.model.ChatBubbleStylePreference
import com.example.data.model.CommunityReview
import com.example.data.model.PricingConfig
import com.example.data.model.TELEGRAM_REACTION_EMOJIS
import com.example.data.model.TodayProfitBanner
import com.example.data.model.UserProfile
import com.example.ui.components.LiveMarketWatchboardCard
import com.example.util.ImageBase64Util
import java.text.SimpleDateFormat
import java.util.Locale

private val COMMODITY_TOPICS = listOf(
    "All",
    "Gold / Silver",
    "Crude / NatGas",
    "Nifty / BankNifty",
    "Risk Control",
    "Trade Setup"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CommunityReviewsScreen(
    profile: UserProfile?,
    pricingConfig: PricingConfig,
    broadcastMessages: List<BroadcastMessage>,
    reviews: List<CommunityReview>,
    onlinePeopleCount: Int,
    currentUserId: String,
    isAdmin: Boolean,
    isBusy: Boolean,
    emojiReactionsByTarget: Map<String, Map<String, Int>> = emptyMap(),
    myReactedKeys: Set<String> = emptySet(),
    onSubmitReview: (tradeMessageId: String, tradeMessageTitle: String, tradeRiskLevel: String, topicTag: String, opinionText: String, rating: Int) -> Unit,
    onTogglePinReview: (String, Boolean) -> Unit,
    onDeleteReview: (String) -> Unit,
    onToggleCommodityChatLock: (Boolean) -> Unit,
    onToggleReaction: (targetId: String, emoji: String) -> Unit = { _, _ -> },
    onUpdateTodayProfitBannerAndCard: (TodayProfitBanner, Boolean) -> Unit = { _, _ -> },
    onClearUnpinnedCommodityMessages: () -> Unit = {},
    onApplyDailyMessageLimit: (Long) -> Unit = {},
    chatBubbleStyle: ChatBubbleStylePreference = ChatBubbleStylePreference.MODERN_GLASS,
    compactFeedMode: Boolean = false,
    liveMarketQuotes: List<MarketInstrumentQuote> = emptyList(),
    onOpenAdminMarketSearch: () -> Unit = {},
    onSelectMarketQuote: (MarketInstrumentQuote) -> Unit = {},
    onUpdateCommodityModerationRules: (slowModeSeconds: Int, blockLinks: Boolean, customWordsCsv: String) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val focusManager = LocalFocusManager.current
    var opinionText by rememberSaveable { mutableStateOf("") }
    var directInputText by rememberSaveable { mutableStateOf("") }
    var selectedTopicFilter by rememberSaveable { mutableStateOf("All") }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isSearchFocused by remember { mutableStateOf(false) }
    var showComposer by rememberSaveable { mutableStateOf(false) }
    var showAdminTools by rememberSaveable { mutableStateOf(false) }
    var showOwnerPnlDialog by remember { mutableStateOf(false) }
    var inlineLimitSelection by remember(pricingConfig.dailyReviewLimitPerUser) {
        mutableStateOf(pricingConfig.dailyReviewLimitPerUser.toInt().coerceAtLeast(1))
    }

    val modConfig = remember(pricingConfig) { pricingConfig.commodityModerationConfig() }
    var slowModeSec by remember(modConfig.slowModeSeconds) { mutableStateOf(modConfig.slowModeSeconds) }
    var blockLinksToggle by remember(modConfig.blockExternalLinks) { mutableStateOf(modConfig.blockExternalLinks) }
    var customWordsInput by remember(modConfig.customBlockedWordsCsv) { mutableStateOf(modConfig.customBlockedWordsCsv) }

    val dailyLimit = pricingConfig.dailyReviewLimitPerUser.toInt().coerceAtLeast(1)
    val usedToday = profile?.usedReviewsToday() ?: 0
    val remainingToday = (dailyLimit - usedToday).coerceAtLeast(0)
    val isUserMuted = !isAdmin && (profile?.isCommodityMuted == true)
    val isChatLockedForUser = !isAdmin && pricingConfig.commodityChatLocked
    val canPost = isAdmin || (!isUserMuted && !isChatLockedForUser && remainingToday > 0)

    val latestBroadcast = broadcastMessages.firstOrNull()
    val todayProfitBanner = remember(pricingConfig) { pricingConfig.todayProfitBanner() }
    val autoDetectedComposerTopic = remember(opinionText, selectedTopicFilter) {
        AutoTopicClassifier.classifyTopic(opinionText, selectedTopicFilter)
    }
    val autoDetectedDirectTopic = remember(directInputText, selectedTopicFilter) {
        AutoTopicClassifier.classifyTopic(directInputText, selectedTopicFilter)
    }

    val filteredReviews = remember(reviews, selectedTopicFilter, searchQuery) {
        reviews.filter { item ->
            val matchesTopic = selectedTopicFilter == "All" ||
                item.isOwnerProfitCard() ||
                item.topicTag.equals(selectedTopicFilter, ignoreCase = true) ||
                item.tradeMessageTitle.contains(selectedTopicFilter, ignoreCase = true)
            val q = searchQuery.trim()
            val matchesSearch = q.isEmpty() ||
                item.publicContent.contains(q, ignoreCase = true) ||
                item.userName.contains(q, ignoreCase = true) ||
                item.topicTag.contains(q, ignoreCase = true)
            matchesTopic && matchesSearch
        }
    }

    val collapseChromeForSearch = isSearchFocused || searchQuery.isNotBlank()

    Box(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 840.dp)
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Compact Top Header + Collapsible Admin Tools + Search & Topic Filter
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnimatedVisibility(
                        visible = !collapseChromeForSearch,
                        enter = expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(tween(150)),
                        exit = shrinkVertically(spring(stiffness = Spring.StiffnessMedium)) + fadeOut(tween(110))
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Forum,
                                                contentDescription = "Commodity Room",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Column {
                                        Text(
                                            text = "Commodity Desk",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (isAdmin) {
                                                "$onlinePeopleCount Online • Unlimited Admin"
                                            } else {
                                                "$onlinePeopleCount Online • $remainingToday/$dailyLimit posts left"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (isAdmin) {
                                        FilterChip(
                                            selected = showAdminTools,
                                            onClick = {
                                                com.example.util.SoundAndHapticFeedback.playTap(context)
                                                showAdminTools = !showAdminTools
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.Tune,
                                                    contentDescription = "Toggle Admin Controls",
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            },
                                            label = {
                                                Text(
                                                    text = if (showAdminTools) "Hide Tools" else "Controls",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1
                                                )
                                            },
                                            modifier = Modifier.testTag("commodity_admin_tools_toggle")
                                        )
                                    } else {
                                        AssistChip(
                                            onClick = { if (canPost) showComposer = true },
                                            label = {
                                                Text(
                                                    text = "$remainingToday/$dailyLimit",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            },
                                            colors = AssistChipDefaults.assistChipColors(
                                                containerColor = if (canPost) {
                                                    MaterialTheme.colorScheme.primaryContainer
                                                } else {
                                                    MaterialTheme.colorScheme.errorContainer
                                                }
                                            )
                                        )
                                    }
                                }
                            }

                            // Collapsible Admin Quick Controls Bar
                            AnimatedVisibility(
                                visible = isAdmin && showAdminTools,
                                enter = expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(tween(150)),
                                exit = shrinkVertically(spring(stiffness = Spring.StiffnessMedium)) + fadeOut(tween(110))
                            ) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                                    imageVector = if (pricingConfig.commodityChatLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                                    contentDescription = "Room Lock State",
                                                    tint = if (pricingConfig.commodityChatLocked) {
                                                        MaterialTheme.colorScheme.error
                                                    } else {
                                                        MaterialTheme.colorScheme.primary
                                                    },
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Text(
                                                    text = if (pricingConfig.commodityChatLocked) {
                                                        "Room Locked (Admin Only)"
                                                    } else {
                                                        "Room Open ($dailyLimit/day)"
                                                    },
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Switch(
                                                checked = pricingConfig.commodityChatLocked,
                                                onCheckedChange = onToggleCommodityChatLock,
                                                modifier = Modifier.testTag("admin_commodity_lock_switch")
                                            )
                                        }

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "Limit:",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            listOf(1, 2, 3, 5, 10).forEach { limitOpt ->
                                                FilterChip(
                                                    selected = inlineLimitSelection == limitOpt,
                                                    onClick = {
                                                        com.example.util.SoundAndHapticFeedback.playTap(context)
                                                        inlineLimitSelection = limitOpt
                                                        onApplyDailyMessageLimit(limitOpt.toLong())
                                                    },
                                                    label = {
                                                        Text(
                                                            text = "$limitOpt",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            maxLines = 1,
                                                            softWrap = false
                                                        )
                                                    }
                                                )
                                            }
                                            Button(
                                                onClick = {
                                                    com.example.util.SoundAndHapticFeedback.playSuccess(context)
                                                    onApplyDailyMessageLimit(inlineLimitSelection.toLong())
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier
                                                    .height(34.dp)
                                                    .testTag("commodity_inline_apply_limit_button")
                                            ) {
                                                Text(
                                                    text = "Apply",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = { showOwnerPnlDialog = true },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("open_owner_pnl_modal_button"),
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Post P&L", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                                            }
                                            Button(
                                                onClick = onOpenAdminMarketSearch,
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFF10B981),
                                                    contentColor = Color.Black
                                                ),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("commodity_admin_market_search_button"),
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(15.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Pin Live Market", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                                            }
                                            OutlinedButton(
                                                onClick = onClearUnpinnedCommodityMessages,
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(15.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Clear", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                                            }
                                        }

                                        // Slow-Mode Cooldown & Link / Custom Word Filter Controls
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "Slow-Mode:",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            listOf(0 to "Off", 10 to "10s", 30 to "30s", 60 to "60s").forEach { (sec, label) ->
                                                FilterChip(
                                                    selected = slowModeSec == sec,
                                                    onClick = {
                                                        com.example.util.SoundAndHapticFeedback.playTap(context)
                                                        slowModeSec = sec
                                                        onUpdateCommodityModerationRules(sec, blockLinksToggle, customWordsInput)
                                                    },
                                                    label = { Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1) }
                                                )
                                            }
                                            FilterChip(
                                                selected = blockLinksToggle,
                                                onClick = {
                                                    com.example.util.SoundAndHapticFeedback.playTap(context)
                                                    blockLinksToggle = !blockLinksToggle
                                                    onUpdateCommodityModerationRules(slowModeSec, blockLinksToggle, customWordsInput)
                                                },
                                                label = {
                                                    Text(
                                                        text = if (blockLinksToggle) "🔗 Links Blocked" else "🔗 Allow Links",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        maxLines = 1
                                                    )
                                                }
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedTextField(
                                                value = customWordsInput,
                                                onValueChange = { customWordsInput = it },
                                                placeholder = { Text("Blocked words (comma separated)", style = MaterialTheme.typography.labelSmall) },
                                                singleLine = true,
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            Button(
                                                onClick = {
                                                    com.example.util.SoundAndHapticFeedback.playSuccess(context)
                                                    onUpdateCommodityModerationRules(slowModeSec, blockLinksToggle, customWordsInput)
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Text("Save Rules", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Search Bar + Direct Post Modal Trigger (Always visible and keyboard-safe)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search messages or traders...", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Commodity") },
                            trailingIcon = {
                                if (searchQuery.isNotBlank() || isSearchFocused) {
                                    IconButton(
                                        onClick = {
                                            searchQuery = ""
                                            focusManager.clearFocus()
                                        }
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear search")
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .onFocusChanged { isSearchFocused = it.isFocused }
                                .testTag("commodity_search_input"),
                            shape = RoundedCornerShape(14.dp)
                        )

                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                showComposer = true
                            },
                            enabled = canPost,
                            modifier = Modifier
                                .height(52.dp)
                                .testTag("toggle_commodity_composer_button"),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Open Direct Post", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Post", fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }

                    // Horizontal Topic Filter Chips (Also sets default topic if user filters by a category)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        COMMODITY_TOPICS.forEach { topic ->
                            FilterChip(
                                selected = selectedTopicFilter == topic,
                                onClick = {
                                    com.example.util.SoundAndHapticFeedback.playTap(context)
                                    selectedTopicFilter = topic
                                },
                                label = { Text(topic, style = MaterialTheme.typography.labelSmall, maxLines = 1) }
                            )
                        }
                    }
                }
            }

            // Scrollable Stream (Includes Live Market Watchboard, P&L Banner, Status Notices, and Commodity Messages)
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(if (compactFeedMode) 8.dp else 10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                if (!collapseChromeForSearch && liveMarketQuotes.isNotEmpty()) {
                    item(key = "commodity_live_market_board") {
                        LiveMarketWatchboardCard(
                            quotes = liveMarketQuotes,
                            isAdmin = isAdmin,
                            onOpenAdminSearchDialog = onOpenAdminMarketSearch,
                            onSelectQuote = onSelectMarketQuote
                        )
                    }
                }

                if (!collapseChromeForSearch && (todayProfitBanner.enabled || isAdmin)) {
                    item(key = "owner_pnl_banner") {
                        OwnerTodayProfitBannerCard(
                            banner = todayProfitBanner,
                            isAdmin = isAdmin,
                            reactions = emojiReactionsByTarget["pnl_banner"].orEmpty(),
                            myReactedKeys = myReactedKeys,
                            onReact = { emoji -> onToggleReaction("pnl_banner", emoji) },
                            onEditPnl = { showOwnerPnlDialog = true }
                        )
                    }
                }

                if (isUserMuted) {
                    item(key = "muted_notice") {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Block, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Text(
                                    text = "Your account has been muted from posting in Commodity by the Admin.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                } else if (isChatLockedForUser) {
                    item(key = "locked_notice") {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                Text(
                                    text = "Commodity room is currently paused by the Admin for live market announcement.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                if (filteredReviews.isEmpty()) {
                    item(key = "empty_reviews") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "No Commodity messages match this filter yet.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (canPost) {
                                    Button(onClick = { showComposer = true }) {
                                        Text("Be the first to post", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    items(filteredReviews, key = { it.id }) { review ->
                        val canDelete = isAdmin
                        val targetId = review.id.ifBlank { "rev_${review.createdAt?.seconds ?: 0L}" }
                        val itemReactions = emojiReactionsByTarget[targetId].orEmpty()

                        Box(
                            modifier = Modifier.animateItem(
                                fadeInSpec = tween(160),
                                fadeOutSpec = tween(120),
                                placementSpec = spring(
                                    dampingRatio = 0.85f,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            )
                        ) {
                            if (review.isOwnerProfitCard()) {
                                OwnerInFeedProfitTradeCard(
                                    review = review,
                                    isAdmin = isAdmin,
                                    reactions = itemReactions,
                                    myReactedKeys = myReactedKeys,
                                    targetId = targetId,
                                    onReact = { emoji -> onToggleReaction(targetId, emoji) },
                                    onTogglePin = { onTogglePinReview(review.id, !review.isPinned) },
                                    onDelete = { onDeleteReview(review.id) }
                                )
                            } else {
                                CommodityReviewCard(
                                    review = review,
                                    isAdmin = isAdmin,
                                    canDelete = canDelete,
                                    targetId = targetId,
                                    reactions = itemReactions,
                                    myReactedKeys = myReactedKeys,
                                    chatBubbleStyle = chatBubbleStyle,
                                    compactMode = compactFeedMode,
                                    onReact = { emoji -> onToggleReaction(targetId, emoji) },
                                    onTogglePin = { onTogglePinReview(review.id, !review.isPinned) },
                                    onDelete = { onDeleteReview(review.id) }
                                )
                            }
                        }
                    }
                }
            }

            // Pinned WhatsApp-Style Direct 1-Tap Message Bar (No confusing topic selection required!)
            if (canPost) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    tonalElevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (directInputText.isNotBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "⚡ Auto-Tag: $autoDetectedDirectTopic",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = if (isAdmin) "Admin Direct Post" else "$remainingToday/$dailyLimit left today",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = directInputText,
                                onValueChange = { directInputText = it },
                                placeholder = {
                                    Text(
                                        text = "Write direct message (auto-tagged by words)...",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                maxLines = 3,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("commodity_direct_bottom_input"),
                                shape = RoundedCornerShape(14.dp)
                            )
                            Button(
                                onClick = {
                                    val trimmed = directInputText.trim()
                                    if (trimmed.isNotEmpty()) {
                                        com.example.util.SoundAndHapticFeedback.playSuccess(context)
                                        val autoTag = AutoTopicClassifier.classifyTopic(trimmed, selectedTopicFilter)
                                        onSubmitReview(
                                            latestBroadcast?.id ?: "commodity_general",
                                            latestBroadcast?.title ?: "Live Market Discussion",
                                            latestBroadcast?.riskLevel ?: "MEDIUM",
                                            autoTag,
                                            trimmed,
                                            5
                                        )
                                        directInputText = ""
                                        focusManager.clearFocus()
                                    }
                                },
                                enabled = !isBusy && directInputText.trim().isNotEmpty(),
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                                modifier = Modifier
                                    .height(50.dp)
                                    .testTag("commodity_direct_bottom_send_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Direct Post", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Send", fontWeight = FontWeight.ExtraBold, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }

    // Direct 1-Step Commodity Post Dialog (Zero manual topic selection — automatically classifies by words or active filter!)
    if (showComposer && canPost) {
        Dialog(
            onDismissRequest = { showComposer = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                    tonalElevation = 8.dp,
                    modifier = Modifier
                        .widthIn(max = 560.dp)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Direct Commodity Post",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = if (isAdmin) {
                                        "Admin Unlimited • Topic auto-detected from your words"
                                    } else {
                                        "$remainingToday/$dailyLimit remaining today • Topic auto-detected automatically"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { showComposer = false },
                                modifier = Modifier.testTag("close_commodity_composer_button")
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close Composer")
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "⚡ Auto-Assigned Topic:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = autoDetectedComposerTopic,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }

                        OutlinedTextField(
                            value = opinionText,
                            onValueChange = { opinionText = it },
                            label = {
                                Text("Write your message directly and tap Send...")
                            },
                            minLines = 3,
                            maxLines = 6,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("community_opinion_input"),
                            shape = RoundedCornerShape(14.dp)
                        )

                        // Pinned Bottom Send Action (Always visible above keyboard!)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { showComposer = false },
                                modifier = Modifier.height(48.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Cancel")
                            }
                            Button(
                                onClick = {
                                    val trimmed = opinionText.trim()
                                    if (trimmed.isNotEmpty()) {
                                        com.example.util.SoundAndHapticFeedback.playSuccess(context)
                                        val resolvedTopic = AutoTopicClassifier.classifyTopic(trimmed, selectedTopicFilter)
                                        onSubmitReview(
                                            latestBroadcast?.id ?: "commodity_general",
                                            latestBroadcast?.title ?: "Live Market Discussion",
                                            latestBroadcast?.riskLevel ?: "MEDIUM",
                                            resolvedTopic,
                                            trimmed,
                                            5
                                        )
                                        opinionText = ""
                                        showComposer = false
                                    }
                                },
                                enabled = canPost && !isBusy && opinionText.trim().isNotEmpty(),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("submit_community_review_button"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Post Directly",
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showOwnerPnlDialog) {
        OwnerProfitEditorDialog(
            initialBanner = todayProfitBanner,
            broadcastMessages = broadcastMessages,
            onDismiss = { showOwnerPnlDialog = false },
            onSave = { updatedBanner, alsoPostInFeed ->
                onUpdateTodayProfitBannerAndCard(updatedBanner, alsoPostInFeed)
                showOwnerPnlDialog = false
            }
        )
    }
}

@Composable
private fun OwnerTodayProfitBannerCard(
    banner: TodayProfitBanner,
    isAdmin: Boolean,
    reactions: Map<String, Int>,
    myReactedKeys: Set<String>,
    onReact: (String) -> Unit,
    onEditPnl: () -> Unit
) {
    val profitColor = if (banner.isProfit) Color(0xFF00E676) else Color(0xFFFF5252)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("owner_today_pnl_banner"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.5.dp, profitColor.copy(alpha = 0.75f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            profitColor.copy(alpha = 0.16f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                            Color.Transparent
                        )
                    )
                )
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = profitColor.copy(alpha = 0.2f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (banner.isProfit) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                contentDescription = null,
                                tint = profitColor
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (banner.isProfit) "TRADE SIGNAL PROFIT (AS PER CALL)" else "TRADE SIGNAL P&L (AS PER CALL)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = profitColor
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = banner.instrumentText,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        if (banner.linkedBroadcastTitle.isNotBlank()) {
                            Text(
                                text = "📢 Broadcast Trade: ${banner.linkedBroadcastTitle}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = banner.amountText,
                                style = MaterialTheme.typography.headlineSmall,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold,
                                color = profitColor
                            )
                            if (banner.roiPercent.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = profitColor.copy(alpha = 0.18f)
                                ) {
                                    Text(
                                        text = "ROI ${banner.roiPercent}",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = profitColor
                                    )
                                }
                            }
                        }
                    }
                }

                if (isAdmin) {
                    IconButton(
                        onClick = onEditPnl,
                        modifier = Modifier.testTag("edit_today_pnl_banner_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Trade Signal P&L",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (banner.noteText.isNotBlank()) {
                Text(
                    text = banner.noteText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Telegram Emoji Reactions Row for P&L Banner
            TelegramEmojiReactionRow(
                targetId = "pnl_banner",
                reactions = reactions,
                myReactedKeys = myReactedKeys,
                onReact = onReact
            )
        }
    }
}

@Composable
private fun OwnerInFeedProfitTradeCard(
    review: CommunityReview,
    isAdmin: Boolean,
    reactions: Map<String, Int>,
    myReactedKeys: Set<String>,
    targetId: String,
    onReact: (String) -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit
) {
    val parsed = remember(review.topicTag, review.publicContent) {
        review.parseOwnerProfitCard() ?: TodayProfitBanner()
    }
    val profitColor = if (parsed.isProfit) Color(0xFF00E676) else Color(0xFFFF5252)
    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("owner_profit_card_${review.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.5.dp, profitColor.copy(alpha = 0.7f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            profitColor.copy(alpha = 0.14f),
                            Color.Transparent
                        )
                    )
                )
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                        imageVector = Icons.Default.Verified,
                        contentDescription = null,
                        tint = profitColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "TRADE BROADCAST OUTCOME • ${parsed.instrumentText}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = profitColor
                        )
                        if (review.tradeMessageTitle.isNotBlank()) {
                            Text(
                                text = "Signal: ${review.tradeMessageTitle}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = review.createdAt?.toDate()?.let { dateFormat.format(it) } ?: "Just now",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isAdmin) {
                        IconButton(onClick = onTogglePin, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "Pin",
                                tint = if (review.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = parsed.amountText,
                    style = MaterialTheme.typography.headlineSmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    color = profitColor
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = profitColor.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = "${parsed.instrumentText} • ${parsed.roiPercent}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = profitColor
                    )
                }
            }

            Text(
                text = review.publicContent,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            TelegramEmojiReactionRow(
                targetId = targetId,
                reactions = reactions,
                myReactedKeys = myReactedKeys,
                onReact = onReact
            )
        }
    }
}

@Composable
private fun CommodityReviewCard(
    review: CommunityReview,
    isAdmin: Boolean,
    canDelete: Boolean,
    targetId: String,
    reactions: Map<String, Int>,
    myReactedKeys: Set<String>,
    chatBubbleStyle: ChatBubbleStylePreference = ChatBubbleStylePreference.MODERN_GLASS,
    compactMode: Boolean = false,
    onReact: (String) -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
    val avatarBitmap = remember(review.userPhotoBase64) {
        ImageBase64Util.decodeBase64ToBitmap(review.userPhotoBase64)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("community_review_card_${review.id}"),
        shape = RoundedCornerShape(chatBubbleStyle.cornerDp.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (review.isPinned) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            width = if (review.isPinned) 1.5.dp else 1.dp,
            color = if (review.isPinned) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (compactMode) 10.dp else 14.dp),
            verticalArrangement = Arrangement.spacedBy(if (compactMode) 6.dp else 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(38.dp)
                    ) {
                        if (avatarBitmap != null) {
                            Image(
                                bitmap = avatarBitmap.asImageBitmap(),
                                contentDescription = review.userName,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = review.userName.take(1).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = review.userName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            if (review.isPinned) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "PINNED",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                            if (review.topicTag.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                                ) {
                                    Text(
                                        text = review.topicTag,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                        Text(
                            text = "Re: ${review.tradeMessageTitle}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = review.createdAt?.toDate()?.let { dateFormat.format(it) } ?: "Just now",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isAdmin) {
                        IconButton(
                            onClick = onTogglePin,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "Pin or Unpin Commodity message",
                                tint = if (review.isPinned) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    // ONLY ADMIN CAN DELETE COMMODITY MESSAGES!
                    if (canDelete) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("delete_review_${review.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Commodity message (Admin Only)",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Text(
                text = review.publicContent,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (review.hasBlurredWords) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Automated Safety Filter: Inappropriate words were blurred and reported to Admin.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            // Telegram Emoji Reaction Bar on every Commodity message
            TelegramEmojiReactionRow(
                targetId = targetId,
                reactions = reactions,
                myReactedKeys = myReactedKeys,
                onReact = onReact
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TelegramEmojiReactionRow(
    targetId: String,
    reactions: Map<String, Int>,
    myReactedKeys: Set<String>,
    onReact: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        TELEGRAM_REACTION_EMOJIS.forEach { emoji ->
            val count = reactions[emoji] ?: 0
            val isMine = myReactedKeys.contains("$targetId|$emoji")
            val scale by animateFloatAsState(
                targetValue = if (isMine) 1.08f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "emojiScale"
            )
            val bgColor by animateColorAsState(
                targetValue = if (isMine) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                },
                label = "emojiBg"
            )

            Surface(
                modifier = Modifier
                    .scale(scale)
                    .clip(RoundedCornerShape(50))
                    .clickable {
                        com.example.util.SoundAndHapticFeedback.playTap(context)
                        onReact(emoji)
                    }
                    .testTag("react_${targetId}_$emoji"),
                shape = RoundedCornerShape(50),
                color = bgColor,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isMine) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
                    }
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = emoji,
                        style = MaterialTheme.typography.labelMedium
                    )
                    if (count > 0) {
                        Text(
                            text = count.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isMine) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OwnerProfitEditorDialog(
    initialBanner: TodayProfitBanner,
    broadcastMessages: List<BroadcastMessage> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (TodayProfitBanner, Boolean) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var enabled by rememberSaveable { mutableStateOf(initialBanner.enabled) }
    var isProfit by rememberSaveable { mutableStateOf(initialBanner.isProfit) }
    var amountText by rememberSaveable { mutableStateOf(initialBanner.amountText) }
    var instrumentText by rememberSaveable { mutableStateOf(initialBanner.instrumentText) }
    var roiPercent by rememberSaveable { mutableStateOf(initialBanner.roiPercent) }
    var noteText by rememberSaveable { mutableStateOf(initialBanner.noteText) }
    var linkedBroadcastTitle by rememberSaveable { mutableStateOf(initialBanner.linkedBroadcastTitle) }
    var alsoPostInFeed by rememberSaveable { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Trade Signal P&L (As Per Broadcast Call)",
                fontWeight = FontWeight.ExtraBold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState())
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Publish the Profit or Loss achieved on the Trade Broadcast Message given to members.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Show Pinned Trade P&L Banner", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                }

                if (broadcastMessages.isNotEmpty()) {
                    Text(
                        text = "Select Broadcast Trade Message:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        broadcastMessages.take(4).forEach { msg ->
                            FilterChip(
                                selected = linkedBroadcastTitle == msg.title,
                                onClick = {
                                    linkedBroadcastTitle = msg.title
                                    if (msg.hasDisplayCategory()) {
                                        instrumentText = msg.category
                                    }
                                },
                                label = {
                                    Text(
                                        text = msg.title.take(26),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = linkedBroadcastTitle,
                    onValueChange = { linkedBroadcastTitle = it },
                    label = { Text("Broadcast Trade Headline / Call Reference") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = isProfit,
                        onClick = {
                            isProfit = true
                            if (amountText.startsWith("-")) {
                                amountText = "+" + amountText.removePrefix("-")
                            }
                        },
                        label = { Text("Trade Profit (+)", fontWeight = FontWeight.Bold) }
                    )
                    FilterChip(
                        selected = !isProfit,
                        onClick = {
                            isProfit = false
                            if (amountText.startsWith("+")) {
                                amountText = "-" + amountText.removePrefix("+")
                            }
                        },
                        label = { Text("Trade Loss (-)", fontWeight = FontWeight.Bold) }
                    )
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Trade Profit/Loss Amount (e.g. +₹48,500)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = instrumentText,
                        onValueChange = { instrumentText = it },
                        label = { Text("Instrument (e.g. MCX GOLD)") },
                        singleLine = true,
                        modifier = Modifier.weight(1.3f)
                    )
                    OutlinedTextField(
                        value = roiPercent,
                        onValueChange = { roiPercent = it },
                        label = { Text("Move / ROI (e.g. +18.4%)") },
                        singleLine = true,
                        modifier = Modifier.weight(0.9f)
                    )
                }

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Trade Call Outcome Note") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { alsoPostInFeed = !alsoPostInFeed }
                ) {
                    Checkbox(
                        checked = alsoPostInFeed,
                        onCheckedChange = { alsoPostInFeed = it }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Also post as a Trade Signal Outcome Card in the Commodity feed with Telegram reactions",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    com.example.util.SoundAndHapticFeedback.playSuccess(context)
                    onSave(
                        TodayProfitBanner(
                            enabled = enabled,
                            amountText = amountText.trim().ifBlank { "+₹48,500" },
                            instrumentText = instrumentText.trim().ifBlank { "MCX GOLD & SILVER" },
                            roiPercent = roiPercent.trim().ifBlank { "+18.4%" },
                            isProfit = isProfit,
                            noteText = noteText.trim().ifBlank { "As per Trade Broadcast call — targets achieved" },
                            linkedBroadcastTitle = linkedBroadcastTitle.trim().ifBlank { instrumentText.trim() }
                        ),
                        alsoPostInFeed
                    )
                }
            ) {
                Text("Publish Trade P&L", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
