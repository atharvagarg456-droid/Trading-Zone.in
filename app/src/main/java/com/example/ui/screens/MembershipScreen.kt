package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AccessRequest
import com.example.data.model.DeviceRecord
import com.example.data.model.PricingConfig
import com.example.data.model.SubscriptionPlanOption
import com.example.data.model.UserProfile
import com.example.util.ImageBase64Util
import com.example.util.SoundAndHapticFeedback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MembershipScreen(
    profile: UserProfile?,
    deviceRecord: DeviceRecord?,
    pricingConfig: PricingConfig,
    myRequests: List<AccessRequest>,
    isBusy: Boolean,
    isAdmin: Boolean = false,
    onRequestTrial: () -> Unit,
    onSubmitUpiPayment: (planName: String, amountInr: Double, validityDays: Long, utrReference: String, screenshotBase64: String) -> Unit,
    onTogglePlanEnabled: (SubscriptionPlanOption, Boolean) -> Unit = { _, _ -> },
    onUpsertPlanInStudio: (
        slotIndex: Int,
        planId: String,
        name: String,
        priceInr: Double,
        validityDays: Long,
        badge: String,
        qrCodeBase64: String,
        qrOnlyMode: Boolean,
        enabled: Boolean
    ) -> Unit = { _, _, _, _, _, _, _, _, _ -> },
    onDeletePlanInStudio: (SubscriptionPlanOption) -> Unit = {},
    onBulkExtendSubscribers: (Int) -> Unit = {},
    onApplyDailyMessageLimit: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showBuyerPreviewForAdmin by rememberSaveable { mutableStateOf(false) }
    var editingPlanOption by remember { mutableStateOf<SubscriptionPlanOption?>(null) }
    var isAddingNewCustomPlan by remember { mutableStateOf(false) }
    var applyPlansLiveToggle by rememberSaveable { mutableStateOf(true) }
    var applyDailyLimitToggle by rememberSaveable { mutableStateOf(true) }
    var dailyLimitInput by rememberSaveable(pricingConfig.dailyReviewLimitPerUser) {
        mutableStateOf(pricingConfig.dailyReviewLimitPerUser.toString())
    }
    var localAppliedBanner by remember { mutableStateOf<String?>(null) }

    val availablePlans = remember(pricingConfig) { pricingConfig.availablePlans() }
    val allAdminPlans = remember(pricingConfig) { pricingConfig.allPlansForAdmin() }

    var selectedPlanId by rememberSaveable {
        mutableStateOf(availablePlans.firstOrNull()?.id ?: "plan_1")
    }
    LaunchedEffect(availablePlans) {
        if (availablePlans.isNotEmpty() && availablePlans.none { it.id == selectedPlanId }) {
            selectedPlanId = availablePlans.first().id
        }
    }
    val selectedPlan: SubscriptionPlanOption? =
        availablePlans.firstOrNull { it.id == selectedPlanId } ?: availablePlans.firstOrNull()

    var utrReference by rememberSaveable { mutableStateOf("") }
    var screenshotBase64 by rememberSaveable { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    val qrBitmap = remember(selectedPlan?.qrCodeBase64) {
        ImageBase64Util.decodeBase64ToBitmap(selectedPlan?.qrCodeBase64.orEmpty())
    }
    val proofBitmap = remember(screenshotBase64) {
        ImageBase64Util.decodeBase64ToBitmap(screenshotBase64)
    }

    val screenshotPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val encoded = withContext(Dispatchers.IO) {
                    ImageBase64Util.encodeUriToBase64(context, uri, maxDimension = 720, quality = 70)
                }
                if (encoded != null) {
                    screenshotBase64 = encoded
                    localError = null
                } else {
                    localError = "Unable to process payment screenshot."
                }
            }
        }
    }

    val trialAlreadyUsed = (profile?.trialUsed == true) || (deviceRecord?.trialClaimed == true)
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize()
                .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isAdmin) {
                // 1. OWNER LIFETIME MASTER STATUS BANNER (Admin never pays for their own app!)
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_lifetime_master_card"),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        ),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.10f),
                                            Color.Transparent
                                        )
                                    )
                                )
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Shield,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(26.dp)
                                            )
                                        }
                                    }
                                    Column {
                                        Text(
                                            text = "OWNER LIFETIME VIP MASTER",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Platform Owner • No Subscription Payment Needed",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                AssistChip(
                                    onClick = { showBuyerPreviewForAdmin = !showBuyerPreviewForAdmin },
                                    label = {
                                        Text(
                                            if (showBuyerPreviewForAdmin) "Hide Buyer View" else "Preview Buyer View",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Visibility,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                            }

                            Text(
                                text = "As the Platform Owner, your account has permanent, unrestricted Lifetime Master Access. Use the interactive Plan Studio below to add custom plans, edit prices, delete plans, or switch any plan ON/OFF in real time.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { isAddingNewCustomPlan = true },
                                    modifier = Modifier.testTag("admin_add_custom_plan_button"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Add Custom Plan", fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = { onBulkExtendSubscribers(7) },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Gift +7 Days to All Active")
                                }
                                OutlinedButton(
                                    onClick = { onBulkExtendSubscribers(30) },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Gift +30 Days to All Active")
                                }
                            }
                        }
                    }
                }

                // 2. INTERACTIVE ADMIN PLAN STUDIO (Add / Edit / Delete / Toggle ON & OFF)
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_plan_studio_card"),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Column {
                                        Text(
                                            text = "Interactive Plan Studio (${availablePlans.size} Live / ${allAdminPlans.size} Total)",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Text(
                                            text = "Turn any plan ON or OFF, edit pricing & validity, or add/delete custom plans",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            allAdminPlans.forEach { plan ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("studio_plan_row_${plan.id}"),
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (plan.enabled) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
                                    },
                                    border = BorderStroke(
                                        width = if (plan.enabled) 1.5.dp else 1.dp,
                                        color = if (plan.enabled) {
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                        } else {
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                        }
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // Top Row: Badge + Live Status Pill + Toggle Switch (Never wraps unevenly)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = if (plan.enabled) {
                                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                                    } else {
                                                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                                    }
                                                ) {
                                                    Text(
                                                        text = if (plan.enabled) "ON • LIVE" else "OFF • HIDDEN",
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        maxLines = 1,
                                                        softWrap = false,
                                                        color = if (plan.enabled) {
                                                            MaterialTheme.colorScheme.primary
                                                        } else {
                                                            MaterialTheme.colorScheme.error
                                                        }
                                                    )
                                                }
                                                if (plan.badge.isNotBlank()) {
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                                                    ) {
                                                        Text(
                                                            text = plan.badge,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                                        )
                                                    }
                                                }
                                            }
                                            Switch(
                                                checked = plan.enabled,
                                                onCheckedChange = { enabled ->
                                                    SoundAndHapticFeedback.playClick(context)
                                                    onTogglePlanEnabled(plan, enabled)
                                                },
                                                modifier = Modifier.testTag("toggle_plan_${plan.id}")
                                            )
                                        }

                                        // Middle Row: Plan Name on left, Tabular Price & Validity on right
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .padding(end = 10.dp)
                                            ) {
                                                Text(
                                                    text = plan.name,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${plan.validityDays} Days Validity",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontFamily = FontFamily.Monospace,
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Text(
                                                text = "₹${plan.priceInr.toInt()}",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Black,
                                                maxLines = 1,
                                                softWrap = false,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        // Bottom Row: Equal-width aligned action buttons
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedButton(
                                                onClick = {
                                                    SoundAndHapticFeedback.playClick(context)
                                                    editingPlanOption = plan
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(40.dp)
                                                    .testTag("edit_plan_${plan.id}"),
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Edit Plan",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                            OutlinedButton(
                                                onClick = {
                                                    SoundAndHapticFeedback.playClick(context)
                                                    onDeletePlanInStudio(plan)
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(40.dp)
                                                    .testTag("delete_plan_${plan.id}"),
                                                shape = RoundedCornerShape(10.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    contentColor = MaterialTheme.colorScheme.error
                                                ),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(15.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (plan.isCustom) "Delete Plan" else "Turn Off",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                            // EXPLICIT APPLY TOGGLE & BUTTON FOR PLANS + DAILY MESSAGE LIMIT
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.20f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "⚡ Apply Plan & Daily Limit Controls",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                            Text(
                                                text = "Toggle ON & click Apply to broadcast updated Plans and Daily Message Limit to all devices",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = applyPlansLiveToggle,
                                            onCheckedChange = {
                                                SoundAndHapticFeedback.playClick(context)
                                                applyPlansLiveToggle = it
                                            },
                                            modifier = Modifier.testTag("apply_plans_live_toggle")
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = dailyLimitInput,
                                            onValueChange = { dailyLimitInput = it.filter { c -> c.isDigit() } },
                                            label = { Text("Daily Msg Limit / User") },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("membership_daily_limit_input"),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "Apply Limit",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Switch(
                                                checked = applyDailyLimitToggle,
                                                onCheckedChange = {
                                                    SoundAndHapticFeedback.playClick(context)
                                                    applyDailyLimitToggle = it
                                                },
                                                modifier = Modifier.testTag("apply_daily_limit_toggle")
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            SoundAndHapticFeedback.playTradeSignal(context, "TARGET HIT")
                                            val parsedLimit = (dailyLimitInput.toLongOrNull() ?: pricingConfig.dailyReviewLimitPerUser)
                                                .coerceIn(1L, 100L)
                                            onApplyDailyMessageLimit(parsedLimit)
                                            localAppliedBanner = "Applied ${availablePlans.size} Live Plans & $parsedLimit Messages/Day Limit across all devices!"
                                        },
                                        enabled = (applyPlansLiveToggle || applyDailyLimitToggle) && !isBusy,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("apply_plans_and_limit_button"),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Apply Plans & Daily Message Limit Now",
                                            fontWeight = FontWeight.ExtraBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    if (localAppliedBanner != null) {
                                        Text(
                                            text = "✅ ${localAppliedBanner!!}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (!isAdmin || showBuyerPreviewForAdmin) {
                // Current Subscription Status Card (for normal users or when Admin toggles buyer preview)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Verified,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    Column {
                                        Text(
                                            text = profile?.activePlanName?.ifBlank {
                                                stringResource(R.string.membership_title)
                                            } ?: stringResource(R.string.membership_title),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Status: ${profile?.entitlementStatus ?: "NONE"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                val daysLeft = profile?.daysRemaining() ?: 0
                                AssistChip(
                                    onClick = {},
                                    label = {
                                        Text(
                                            text = if (daysLeft > 0) "$daysLeft Days Left" else "Inactive",
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = if (daysLeft > 0) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.errorContainer
                                        }
                                    )
                                )
                            }

                            if ((profile?.validUntilMillis ?: 0L) > System.currentTimeMillis()) {
                                Text(
                                    text = "Valid until: ${dateFormat.format(Date(profile!!.validUntilMillis))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // 1-Time 7-Day Free Trial Card (Only for non-admin buyers)
                if (!isAdmin) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                    Text(
                                        text = "1-Time ${pricingConfig.trialDays}-Day Free Trial",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "Strictly limited to 1 trial per user and hardware device ID (${deviceRecord?.deviceId ?: profile?.deviceId ?: "bound"}). Admin approval is required to activate.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Button(
                                    onClick = onRequestTrial,
                                    enabled = !trialAlreadyUsed && !isBusy,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("claim_free_trial_button"),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text(
                                        text = if (trialAlreadyUsed) {
                                            stringResource(R.string.membership_trial_used)
                                        } else {
                                            stringResource(R.string.membership_claim_trial)
                                        },
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Multi-Plan Selector Cards + UPI QR Payment Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "Choose Membership Plan",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )

                            if (availablePlans.isEmpty()) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ) {
                                    Text(
                                        text = "All paid plans are currently paused by the Admin. Please check back shortly.",
                                        modifier = Modifier.padding(16.dp),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    availablePlans.forEach { planOption ->
                                        val isSelected = planOption.id == selectedPlan?.id
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(16.dp))
                                                .clickable {
                                                    SoundAndHapticFeedback.playClick(context)
                                                    selectedPlanId = planOption.id
                                                }
                                                .testTag("select_${planOption.id}"),
                                            shape = RoundedCornerShape(16.dp),
                                            color = if (isSelected) {
                                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                            } else {
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                            },
                                            border = BorderStroke(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                                }
                                            )
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(14.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                // Top Row: Badge Pill + Selected Indicator (Never wraps unevenly)
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                                    ) {
                                                        Text(
                                                            text = planOption.badge.ifBlank { "VIP Plan" },
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.primary,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                    Text(
                                                        text = if (isSelected) "● SELECTED" else "TAP TO SELECT",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        maxLines = 1,
                                                        softWrap = false,
                                                        color = if (isSelected) {
                                                            MaterialTheme.colorScheme.primary
                                                        } else {
                                                            MaterialTheme.colorScheme.onSurfaceVariant
                                                        }
                                                    )
                                                }

                                                // Bottom Row: Plan Name & Validity on Left, Price on Right
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .padding(end = 10.dp)
                                                    ) {
                                                        Text(
                                                            text = planOption.name,
                                                            style = MaterialTheme.typography.titleMedium,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        Text(
                                                            text = "${planOption.validityDays} Days Full Access",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            fontFamily = FontFamily.Monospace,
                                                            maxLines = 1,
                                                            softWrap = false,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    Text(
                                                        text = "₹${planOption.priceInr.toInt()}",
                                                        style = MaterialTheme.typography.titleLarge,
                                                        fontFamily = FontFamily.Monospace,
                                                        fontWeight = FontWeight.Black,
                                                        maxLines = 1,
                                                        softWrap = false,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            val displayBenefits = pricingConfig.displayBenefits()
                            if (displayBenefits.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    displayBenefits.forEach { benefit ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = benefit,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                }
                            }

                            if (selectedPlan != null && !isAdmin) {
                                HorizontalDivider()

                                // QR Code Display for Selected Plan
                                if (qrBitmap != null) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "Scan Official QR Code to Pay ₹${selectedPlan.priceInr.toInt()} (${selectedPlan.name})",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                                            modifier = Modifier.size(220.dp)
                                        ) {
                                            Image(
                                                bitmap = qrBitmap.asImageBitmap(),
                                                contentDescription = "UPI QR Code for ${selectedPlan.name}",
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(12.dp),
                                                contentScale = ContentScale.Fit
                                            )
                                        }
                                    }
                                } else {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.QrCode2,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "Pay via UPI below or ask Admin to upload the QR code for ${selectedPlan.name}.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                // Hide UPI ID & Number when QR-Only mode is enabled for the selected plan
                                if (!selectedPlan.qrOnlyMode) {
                                    if (pricingConfig.upiId.isNotBlank()) {
                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "Official UPI ID",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        text = pricingConfig.upiId,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontFamily = FontFamily.Monospace,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                IconButton(
                                                    onClick = {
                                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                        clipboard.setPrimaryClip(ClipData.newPlainText("UPI ID", pricingConfig.upiId))
                                                    }
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.ContentCopy,
                                                        contentDescription = stringResource(R.string.cd_copy_upi)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    if (pricingConfig.upiMobileNumber.isNotBlank()) {
                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "UPI Mobile Number",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        text = pricingConfig.upiMobileNumber,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontFamily = FontFamily.Monospace,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                IconButton(
                                                    onClick = {
                                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                        clipboard.setPrimaryClip(ClipData.newPlainText("UPI Mobile", pricingConfig.upiMobileNumber))
                                                    }
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.ContentCopy,
                                                        contentDescription = "Copy UPI Number"
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = utrReference,
                                    onValueChange = {
                                        utrReference = it
                                        localError = null
                                    },
                                    label = { Text(stringResource(R.string.membership_utr_label)) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("upi_utr_input"),
                                    shape = RoundedCornerShape(14.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            screenshotPicker.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("upload_payment_proof_button"),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            if (screenshotBase64.isBlank()) {
                                                stringResource(R.string.membership_upload_proof)
                                            } else {
                                                "Change Screenshot"
                                            }
                                        )
                                    }

                                    if (proofBitmap != null) {
                                        Image(
                                            bitmap = proofBitmap.asImageBitmap(),
                                            contentDescription = "Payment Proof Preview",
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(RoundedCornerShape(10.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }

                                if (localError != null) {
                                    Text(
                                        text = localError!!,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                Button(
                                    onClick = {
                                        if (utrReference.trim().length < 6) {
                                            localError = "Please enter a valid 12-digit UPI UTR / Reference number."
                                            return@Button
                                        }
                                        onSubmitUpiPayment(
                                            selectedPlan.name,
                                            selectedPlan.priceInr,
                                            selectedPlan.validityDays,
                                            utrReference.trim(),
                                            screenshotBase64
                                        )
                                        utrReference = ""
                                        screenshotBase64 = ""
                                    },
                                    enabled = !isBusy,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("submit_upi_payment_button"),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.Default.Payments, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Submit ₹${selectedPlan.priceInr.toInt()} Payment for ${selectedPlan.name}",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Recent Access / Payment Requests History
            if (myRequests.isNotEmpty() && !isAdmin) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Your Access & Payment Requests",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                items(myRequests, key = { it.id }) { req ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${req.planName} (${if (req.requestType == "FREE_TRIAL_7D") "Free Trial" else "₹${req.amountInr.toInt()}"})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Ref: ${req.utrReference} • ${req.validityDays} Days",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            AssistChip(
                                onClick = {},
                                label = {
                                    Text(
                                        text = req.status,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (editingPlanOption != null || isAddingNewCustomPlan) {
        val initialPlan = editingPlanOption
        PlanStudioEditorDialog(
            initialPlan = initialPlan,
            onDismiss = {
                editingPlanOption = null
                isAddingNewCustomPlan = false
            },
            onSave = { slotIndex, planId, name, priceInr, validityDays, badge, qrCodeBase64, qrOnlyMode, enabled ->
                onUpsertPlanInStudio(
                    slotIndex,
                    planId,
                    name,
                    priceInr,
                    validityDays,
                    badge,
                    qrCodeBase64,
                    qrOnlyMode,
                    enabled
                )
                editingPlanOption = null
                isAddingNewCustomPlan = false
            }
        )
    }
}

@Composable
private fun PlanStudioEditorDialog(
    initialPlan: SubscriptionPlanOption?,
    onDismiss: () -> Unit,
    onSave: (
        slotIndex: Int,
        planId: String,
        name: String,
        priceInr: Double,
        validityDays: Long,
        badge: String,
        qrCodeBase64: String,
        qrOnlyMode: Boolean,
        enabled: Boolean
    ) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by rememberSaveable { mutableStateOf(initialPlan?.name ?: "") }
    var priceText by rememberSaveable { mutableStateOf(initialPlan?.priceInr?.toInt()?.toString() ?: "1499") }
    var validityText by rememberSaveable { mutableStateOf(initialPlan?.validityDays?.toString() ?: "45") }
    var badge by rememberSaveable { mutableStateOf(initialPlan?.badge ?: "VIP Custom") }
    var qrCodeBase64 by rememberSaveable { mutableStateOf(initialPlan?.qrCodeBase64 ?: "") }
    var qrOnlyMode by rememberSaveable { mutableStateOf(initialPlan?.qrOnlyMode ?: false) }
    var enabled by rememberSaveable { mutableStateOf(initialPlan?.enabled ?: true) }

    val qrPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val encoded = withContext(Dispatchers.IO) {
                    ImageBase64Util.encodeUriToBase64(context, uri, maxDimension = 640, quality = 72)
                }
                if (encoded != null) {
                    qrCodeBase64 = encoded
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialPlan == null) "Add Custom Membership Plan" else "Edit ${initialPlan.name}",
                fontWeight = FontWeight.ExtraBold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Plan Name (e.g. Weekly Gold Sniper)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Price (₹ INR)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = validityText,
                        onValueChange = { validityText = it.filter { c -> c.isDigit() } },
                        label = { Text("Validity (Days)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = badge,
                    onValueChange = { badge = it },
                    label = { Text("Highlight Badge (e.g. Best Value / VIP)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Plan Active (ON / Visible to Buyers)", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                }
                if ((initialPlan?.slotIndex ?: 0) in 1..3) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("QR-Only Mode (Hide UPI ID)", style = MaterialTheme.typography.bodySmall)
                        Switch(checked = qrOnlyMode, onCheckedChange = { qrOnlyMode = it })
                    }
                    OutlinedButton(
                        onClick = {
                            qrPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.QrCode2, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (qrCodeBase64.isBlank()) "Upload Custom QR Image" else "Change QR Image")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        initialPlan?.slotIndex ?: 0,
                        initialPlan?.id ?: "",
                        name.trim().ifBlank { "Custom VIP Plan" },
                        priceText.toDoubleOrNull() ?: 999.0,
                        validityText.toLongOrNull() ?: 30L,
                        badge.trim().ifBlank { "Custom Tier" },
                        qrCodeBase64,
                        qrOnlyMode,
                        enabled
                    )
                }
            ) {
                Text("Save Plan", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
