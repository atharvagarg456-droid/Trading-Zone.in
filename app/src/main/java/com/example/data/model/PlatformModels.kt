package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil

const val PRIMARY_ADMIN_EMAIL = "atharvagarg456@gmail.com"
const val MAX_ADMINS_LIMIT = 3

val TELEGRAM_REACTION_EMOJIS = listOf("🔥", "🚀", "💰", "💎", "👏", "❤️", "👍")

const val COMMODITY_IDENTITY_WARNING =
    "WARNING: Do not disclose your identity, phone number, email, or personal ID to anybody. " +
        "That may lead to immediate suspension of your account with NO REFUND."

const val DEFAULT_TERMS_AND_CONDITIONS = """
1. PLATFORM OPERATOR & ACADEMIC SUPERVISION
Trading Zone is operated by Atharva Garg under the academic guidance of Mr. Vikash Chandra Garg.

2. EDUCATIONAL & INFORMATIONAL PURPOSE ONLY (NON-LIABILITY)
All broadcast messages, marked charts, risk levels, and Commodity discussions shared inside Trading Zone are strictly for educational and informational study. Neither Atharva Garg nor Mr. Vikash Chandra Garg is a SEBI-registered investment advisor, and neither shall be held liable for any financial profit or loss incurred from market decisions.

3. DEVICE BINDING & SINGLE-TRIAL POLICY
Each account is bound to your hardware Device ID and network telemetry. The 7-Day Free Trial can be claimed only once per physical device. Attempting to bypass device limits or share credentials will result in a permanent hardware ban.

4. STRICT NO-IDENTITY DISCLOSURE & ANTI-ABUSE POLICY (NO REFUND)
In the Commodity section, you must NEVER disclose your personal identity, phone number, email address, UPI ID, or social/personal handles to anybody. Attempting to send phone numbers or personal IDs is automatically blocked and reported to the Admin. Any violation or abusive behavior will result in immediate account suspension and permanent device ban with NO REFUND.
"""

enum class AppThemePreference(val title: String, val subtitle: String) {
    DARK("Obsidian Terminal", "Deep OLED Carbon & Emerald"),
    CYBER_EMERALD("Cyber Matrix", "Neon Bullish Emerald Glow"),
    ROYAL_GOLD("Royal Bullion", "Institutional Gold & Onyx"),
    MIDNIGHT_SAPPHIRE("Wall Street Pro", "Electric Sapphire & Navy"),
    LIGHT("Daylight Desk", "Crisp High-Contrast Light"),
    SYSTEM("System Auto", "Follow Device Appearance")
}

data class CustomPlanItem(
    val id: String = "",
    val name: String = "",
    val priceInr: Double = 0.0,
    val validityDays: Long = 30L,
    val enabled: Boolean = true,
    val qrOnlyMode: Boolean = false,
    val badge: String = "CUSTOM VIP",
    val badgeText: String = badge,
    val qrCodeBase64: String = ""
)

data class TodayProfitBanner(
    val enabled: Boolean = true,
    val isActive: Boolean = enabled,
    val amountText: String = "+₹48,500",
    val profitAmountText: String = amountText,
    val instrumentText: String = "MCX GOLD & SILVER",
    val instrumentName: String = instrumentText,
    val roiPercent: String = "+18.4% Intraday",
    val roiText: String = roiPercent,
    val noteText: String = "Institutional breakout targets achieved today",
    val isProfit: Boolean = true,
    val updatedAtMillis: Long = 0L
)

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val fullName: String = "",
    val phoneNumber: String = "",
    val city: String = "",
    val photoBase64: String = "",
    val deviceId: String = "",
    val deviceModel: String = "",
    val ipAddress: String = "",
    val accountStatus: String = "PENDING_APPROVAL", // PENDING_APPROVAL, APPROVED, BANNED
    val entitlementStatus: String = "NONE", // NONE, TRIAL_PENDING, TRIAL_ACTIVE, PAID_ACTIVE, EXPIRED
    val trialUsed: Boolean = false,
    val validUntilMillis: Long = 0L,
    val activePlanName: String = "",
    val appLockPin: String = "",
    val securityQuestion: String = "",
    val securityAnswer: String = "",
    val termsAccepted: Boolean = false,
    val isCommodityMuted: Boolean = false,
    val hasPendingProfileEdit: Boolean = false,
    val pendingFullName: String = "",
    val pendingPhoneNumber: String = "",
    val pendingCity: String = "",
    val pendingPhotoBase64: String = "",
    val dailyReviewCount: Long = 0L,
    val lastReviewDate: String = "",
    val adminEmails: List<String> = listOf(PRIMARY_ADMIN_EMAIL),
    @ServerTimestamp val lastActiveAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun isEntitledNow(nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (accountStatus != "APPROVED") return false
        if (entitlementStatus != "TRIAL_ACTIVE" && entitlementStatus != "PAID_ACTIVE") return false
        return validUntilMillis > nowMillis
    }

    fun daysRemaining(nowMillis: Long = System.currentTimeMillis()): Int {
        if (validUntilMillis <= nowMillis) return 0
        val diff = validUntilMillis - nowMillis
        return ceil(diff.toDouble() / 86_400_000.0).toInt().coerceAtLeast(0)
    }

    fun usedReviewsToday(): Int {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        return if (lastReviewDate == today) dailyReviewCount.toInt().coerceAtLeast(0) else 0
    }

    fun remainingReviewsToday(dailyLimit: Int): Int {
        return (dailyLimit - usedReviewsToday()).coerceAtLeast(0)
    }
}

@IgnoreExtraProperties
data class DeviceRecord(
    @DocumentId val id: String = "",
    val deviceId: String = "",
    val ipAddress: String = "",
    val deviceModel: String = "",
    val boundUserId: String = "",
    val trialClaimed: Boolean = false,
    val isBanned: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class BroadcastMessage(
    @DocumentId val id: String = "",
    val title: String = "",
    val content: String = "",
    val category: String = "NONE",
    val riskLevel: String = "MEDIUM", // LOW, MEDIUM, HIGH
    val markedPhotoBase64: String = "",
    val linkUrl: String = "",
    val linkTitle: String = "",
    val replyToMessageId: String = "",
    val replyToTitle: String = "",
    val replyToSnippet: String = "",
    val replyToRiskLevel: String = "",
    val isPinned: Boolean = false,
    val isEdited: Boolean = false,
    val authorId: String = "",
    val authorName: String = "Atharva Garg",
    val visibility: String = "VERIFIED_MEMBERS",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun hasDisplayCategory(): Boolean {
        val clean = category.trim().uppercase(Locale.ROOT)
        return clean.isNotBlank() && clean != "NONE" && clean != "OPTIONAL" && clean != "GENERAL"
    }

    fun hasDisplayRiskLevel(): Boolean {
        if (replyToRiskLevel.equals("NONE", ignoreCase = true)) return false
        val clean = riskLevel.trim().uppercase(Locale.ROOT)
        return clean in listOf("LOW", "MEDIUM", "HIGH")
    }
}

@IgnoreExtraProperties
data class MessageReadReceipt(
    @DocumentId val id: String = "",
    val messageId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val userPhotoBase64: String = "",
    val adminEmails: List<String> = listOf(PRIMARY_ADMIN_EMAIL),
    @ServerTimestamp val viewedAt: Timestamp? = null
)

data class SubscriptionPlanOption(
    val id: String = "plan_1",
    val slotIndex: Int = 1,
    val name: String = "Monthly Pro Plan",
    val priceInr: Double = 2499.0,
    val validityDays: Long = 30L,
    val qrCodeBase64: String = "",
    val qrOnlyMode: Boolean = false,
    val enabled: Boolean = true,
    val badge: String = "CORE PLAN",
    val isCustom: Boolean = false
) {
    val tierIndex: Int get() = slotIndex
    val customPlanId: String get() = if (isCustom) id else ""
    val planName: String get() = name
    val badgeText: String get() = badge
    val isCustomPlan: Boolean get() = isCustom
}

typealias SubscriptionPlanTier = SubscriptionPlanOption

@IgnoreExtraProperties
data class PricingConfig(
    @DocumentId val id: String = "",
    val configType: String = "PRICING",
    val planName: String = "Monthly Pro Plan",
    val priceInr: Double = 2499.0,
    val validityDays: Long = 30L,
    val upiId: String = "atharvagarg@upi",
    val upiMobileNumber: String = "+91 9876543210",
    val qrCodeBase64: String = "",
    val qrOnlyMode: Boolean = false,
    val plan2Enabled: Boolean = true,
    val plan2Name: String = "Quarterly Elite Plan",
    val plan2PriceInr: Double = 5999.0,
    val plan2ValidityDays: Long = 90L,
    val plan2QrCodeBase64: String = "",
    val plan2QrOnlyMode: Boolean = false,
    val plan3Enabled: Boolean = true,
    val plan3Name: String = "Half-Yearly Institutional Plan",
    val plan3PriceInr: Double = 9999.0,
    val plan3ValidityDays: Long = 180L,
    val plan3QrCodeBase64: String = "",
    val plan3QrOnlyMode: Boolean = false,
    val dailyReviewLimitPerUser: Long = 10L,
    val commodityChatLocked: Boolean = false,
    val trialDays: Long = 7L,
    val benefits: List<String> = listOf(
        "Real-time verified trade & commodity broadcasts",
        "Marked chart setups with support & resistance zones",
        "Access to the moderated Commodity discussion feed",
        "Direct WhatsApp-style threaded trade updates"
    ),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun isPlan1Enabled(): Boolean {
        val meta = benefits.firstOrNull {
            it.startsWith("__META_P1_EN__:") || it.startsWith("__META_PLAN1_ENABLED__:")
        }
        return meta?.substringAfter(":")?.trim()?.toBooleanStrictOrNull() ?: true
    }

    fun customPlans(): List<CustomPlanItem> {
        val fromMulti = benefits.filter { it.startsWith("__META_PLAN__:") }.mapNotNull { entry ->
            val parts = entry.removePrefix("__META_PLAN__:").split("|")
            if (parts.size >= 5) {
                val badgeVal = parts.getOrNull(5)?.ifBlank { "CUSTOM VIP" } ?: "CUSTOM VIP"
                CustomPlanItem(
                    id = parts[0],
                    name = parts[1],
                    priceInr = parts[2].toDoubleOrNull() ?: 1999.0,
                    validityDays = parts[3].toLongOrNull() ?: 30L,
                    enabled = parts[4].toBooleanStrictOrNull() ?: (parts[4] != "0"),
                    qrOnlyMode = parts.getOrNull(6)?.let { it == "true" || it == "1" } ?: false,
                    badge = badgeVal,
                    badgeText = badgeVal,
                    qrCodeBase64 = parts.getOrNull(7).orEmpty()
                )
            } else {
                null
            }
        }
        return fromMulti
    }

    fun todayProfitBanner(): TodayProfitBanner {
        val meta = benefits.firstOrNull { it.startsWith("__META_PNL__:") }
            ?: return TodayProfitBanner()
        val parts = meta.removePrefix("__META_PNL__:").split("|")
        if (parts.size < 6) return TodayProfitBanner()
        val active = parts[0].toBooleanStrictOrNull() ?: true
        val amt = parts[1].ifBlank { "+₹48,500" }
        val inst = parts[2].ifBlank { "MCX GOLD & SILVER" }
        val roi = parts[3].ifBlank { "+18.4% Today" }
        val note = parts[4].ifBlank { "Institutional breakout targets achieved" }
        val isProf = parts[5].toBooleanStrictOrNull() ?: true
        val ts = parts.getOrNull(6)?.toLongOrNull() ?: 0L
        return TodayProfitBanner(
            enabled = active,
            isActive = active,
            amountText = amt,
            profitAmountText = amt,
            instrumentText = inst,
            instrumentName = inst,
            roiPercent = roi,
            roiText = roi,
            noteText = note,
            isProfit = isProf,
            updatedAtMillis = ts
        )
    }

    fun announcementTicker(): String {
        val meta = benefits.firstOrNull { it.startsWith("__META_TICKER__:") }
        return meta?.removePrefix("__META_TICKER__:")?.trim()
            ?: "LIVE MARKET DESK • Instant Trade Broadcasts & Commodity P&L Stream Active"
    }

    fun displayBenefits(): List<String> = publicBenefits()

    fun publicBenefits(): List<String> {
        val filtered = benefits.filterNot { it.startsWith("__META_") }
        return if (filtered.isEmpty()) {
            listOf(
                "Real-time verified trade & commodity broadcasts",
                "Marked chart setups with support & resistance zones"
            )
        } else {
            filtered
        }
    }

    fun allPlansForAdmin(): List<SubscriptionPlanOption> {
        val list = mutableListOf<SubscriptionPlanOption>()
        if (planName.isNotBlank() && planName != "__DELETED__") {
            list.add(
                SubscriptionPlanOption(
                    id = "plan_1",
                    slotIndex = 1,
                    name = planName,
                    priceInr = priceInr,
                    validityDays = validityDays.coerceAtLeast(1L),
                    qrCodeBase64 = qrCodeBase64,
                    qrOnlyMode = qrOnlyMode,
                    enabled = isPlan1Enabled(),
                    badge = "CORE PLAN",
                    isCustom = false
                )
            )
        }
        if (plan2Name.isNotBlank() && plan2Name != "__DELETED__") {
            list.add(
                SubscriptionPlanOption(
                    id = "plan_2",
                    slotIndex = 2,
                    name = plan2Name,
                    priceInr = plan2PriceInr,
                    validityDays = plan2ValidityDays.coerceAtLeast(1L),
                    qrCodeBase64 = plan2QrCodeBase64.ifBlank { qrCodeBase64 },
                    qrOnlyMode = plan2QrOnlyMode,
                    enabled = plan2Enabled,
                    badge = "MOST POPULAR",
                    isCustom = false
                )
            )
        }
        if (plan3Name.isNotBlank() && plan3Name != "__DELETED__") {
            list.add(
                SubscriptionPlanOption(
                    id = "plan_3",
                    slotIndex = 3,
                    name = plan3Name,
                    priceInr = plan3PriceInr,
                    validityDays = plan3ValidityDays.coerceAtLeast(1L),
                    qrCodeBase64 = plan3QrCodeBase64.ifBlank { qrCodeBase64 },
                    qrOnlyMode = plan3QrOnlyMode,
                    enabled = plan3Enabled,
                    badge = "INSTITUTIONAL",
                    isCustom = false
                )
            )
        }
        customPlans().forEachIndexed { idx, custom ->
            list.add(
                SubscriptionPlanOption(
                    id = custom.id.ifBlank { "custom_${idx + 1}" },
                    slotIndex = 4 + idx,
                    name = custom.name,
                    priceInr = custom.priceInr,
                    validityDays = custom.validityDays.coerceAtLeast(1L),
                    qrCodeBase64 = custom.qrCodeBase64.ifBlank { qrCodeBase64 },
                    qrOnlyMode = custom.qrOnlyMode || qrOnlyMode,
                    enabled = custom.enabled,
                    badge = custom.badge.ifBlank { custom.badgeText },
                    isCustom = true
                )
            )
        }
        return list
    }

    fun availablePlans(): List<SubscriptionPlanOption> {
        val enabledList = allPlansForAdmin().filter { it.enabled }
        return if (enabledList.isNotEmpty()) {
            enabledList
        } else {
            listOf(
                SubscriptionPlanOption(
                    id = "plan_1",
                    slotIndex = 1,
                    name = planName.takeUnless { it == "__DELETED__" || it.isBlank() } ?: "Monthly Pro Plan",
                    priceInr = priceInr,
                    validityDays = validityDays.coerceAtLeast(1L),
                    qrCodeBase64 = qrCodeBase64,
                    qrOnlyMode = qrOnlyMode,
                    enabled = true,
                    badge = "ACTIVE",
                    isCustom = false
                )
            )
        }
    }

    companion object {
        fun buildBenefitsWithMeta(
            displayBenefits: List<String>,
            plan1Enabled: Boolean,
            customPlans: List<CustomPlanItem>,
            todayProfit: TodayProfitBanner,
            announcementTicker: String
        ): List<String> {
            val result = mutableListOf<String>()
            result.add("__META_P1_EN__:$plan1Enabled")
            val cleanPnlAmount = todayProfit.amountText.replace("|", " ").take(28)
            val cleanInst = todayProfit.instrumentText.replace("|", " ").take(32)
            val cleanRoi = todayProfit.roiPercent.replace("|", " ").take(24)
            val cleanNote = todayProfit.noteText.replace("|", " ").take(55)
            result.add(
                "__META_PNL__:${todayProfit.enabled}|$cleanPnlAmount|$cleanInst|$cleanRoi|$cleanNote|${todayProfit.isProfit}|${todayProfit.updatedAtMillis}".take(195)
            )
            if (announcementTicker.isNotBlank()) {
                result.add("__META_TICKER__:${announcementTicker.replace("\n", " ").take(160)}")
            }
            customPlans.take(5).forEach { cp ->
                val safeId = cp.id.ifBlank { "cp_${System.currentTimeMillis()}" }.replace("|", "").take(20)
                val safeName = cp.name.replace("|", " ").take(45)
                val safeBadge = cp.badge.ifBlank { cp.badgeText }.replace("|", " ").take(20)
                result.add(
                    "__META_PLAN__:$safeId|$safeName|${cp.priceInr.toInt()}|${cp.validityDays}|${cp.enabled}|$safeBadge|${cp.qrOnlyMode}".take(195)
                )
            }
            val remainingSlots = (10 - result.size).coerceAtLeast(0)
            displayBenefits
                .filterNot { it.startsWith("__META_") }
                .map { it.trim().take(190) }
                .filter { it.isNotBlank() }
                .take(remainingSlots)
                .forEach { result.add(it) }
            return result.take(10)
        }
    }
}

@IgnoreExtraProperties
data class AdminSettings(
    @DocumentId val id: String = "",
    val configType: String = "ADMIN_SETTINGS",
    val primaryAdminEmail: String = PRIMARY_ADMIN_EMAIL,
    val adminEmails: List<String> = listOf(PRIMARY_ADMIN_EMAIL),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AccessRequest(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val userName: String = "",
    val userPhotoBase64: String = "",
    val deviceId: String = "",
    val ipAddress: String = "",
    val requestType: String = "FREE_TRIAL_7D", // FREE_TRIAL_7D, PAID_UPI
    val planName: String = "7-Day Free Trial",
    val amountInr: Double = 0.0,
    val validityDays: Long = 7L,
    val utrReference: String = "",
    val paymentScreenshotBase64: String = "",
    val status: String = "PENDING", // PENDING, APPROVED, REJECTED
    val adminEmails: List<String> = listOf(PRIMARY_ADMIN_EMAIL),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CommunityReview(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userPhotoBase64: String = "",
    val tradeMessageId: String = "",
    val tradeMessageTitle: String = "",
    val tradeRiskLevel: String = "MEDIUM",
    val topicTag: String = "Commodity",
    val publicContent: String = "",
    val rating: Int = 5,
    val hasBlurredWords: Boolean = false,
    val isPinned: Boolean = false,
    val visibility: String = "VERIFIED_MEMBERS",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun isEmojiReactionDoc(): Boolean = topicTag == "__REACTION__"

    fun isOwnerProfitCard(): Boolean = topicTag.startsWith("PROFIT_CARD|")

    fun parseOwnerProfitCard(): TodayProfitBanner? {
        if (!isOwnerProfitCard()) return null
        val parts = topicTag.removePrefix("PROFIT_CARD|").split("|")
        val amount = parts.getOrNull(0)?.ifBlank { "+₹0" } ?: "+₹0"
        val instrument = parts.getOrNull(1)?.ifBlank { "COMMODITY TRADE" } ?: "COMMODITY TRADE"
        val roi = parts.getOrNull(2)?.ifBlank { "LIVE P&L" } ?: "LIVE P&L"
        val isProfit = !amount.trim().startsWith("-")
        return TodayProfitBanner(
            isActive = true,
            profitAmountText = amount,
            instrumentName = instrument,
            roiText = roi,
            noteText = publicContent,
            isProfit = isProfit
        )
    }
}

@IgnoreExtraProperties
data class ModerationAlert(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val userName: String = "",
    val userPhotoBase64: String = "",
    val deviceId: String = "",
    val ipAddress: String = "",
    val tradeMessageTitle: String = "",
    val originalUneditedText: String = "",
    val blurredText: String = "",
    val detectedWords: String = "",
    val status: String = "OPEN", // OPEN, RESOLVED, BANNED
    val adminEmails: List<String> = listOf(PRIMARY_ADMIN_EMAIL),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PresenceRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val visibility: String = "PUBLIC_PRESENCE",
    @ServerTimestamp val lastActiveAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

object AbusiveWordModerator {
    private val prohibitedWords = setOf(
        "idiot", "stupid", "scam", "scammer", "fraud", "cheat", "cheater",
        "loser", "dumb", "bastard", "fool", "moron", "trash", "garbage",
        "fake", "liar", "chutiya", "madarchod", "bhenchod", "gandu",
        "harami", "sala", "kutte", "kamina", "wtf", "fuck", "shit", "bitch", "asshole"
    )

    private val phoneRegex = Regex("""(?:\+?\d[\d\-\s().]{6,14}\d)""")
    private val emailOrUpiRegex = Regex("""[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+""")
    private val handleOrIdKeywordRegex = Regex(
        """(?i)\b(?:whatsapp|telegram|insta|instagram|contact\s*me|call\s*me|dm\s*me|my\s*id|user\s*id|upi|phone\s*no|mobile\s*no)\b"""
    )
    private val spacedDigitsRegex = Regex("""(?:\d\s*){8,13}""")

    data class ModerationResult(
        val publicText: String,
        val hasBlurredWords: Boolean,
        val detectedWords: List<String>
    )

    data class ProhibitedContactCheckResult(
        val isBlocked: Boolean,
        val reasonSummary: String
    )

    fun checkProhibitedContactOrId(rawText: String): ProhibitedContactCheckResult {
        val detected = mutableListOf<String>()
        phoneRegex.find(rawText)?.value?.trim()?.let {
            detected.add("Phone Number ($it)")
        }
        if (detected.isEmpty()) {
            spacedDigitsRegex.find(rawText)?.value?.trim()?.let {
                detected.add("Digits/Phone ($it)")
            }
        }
        emailOrUpiRegex.find(rawText)?.value?.trim()?.let {
            detected.add("Email/UPI ID ($it)")
        }
        handleOrIdKeywordRegex.find(rawText)?.value?.trim()?.let {
            detected.add("Personal ID/Contact Keyword ($it)")
        }
        return if (detected.isNotEmpty()) {
            ProhibitedContactCheckResult(
                isBlocked = true,
                reasonSummary = detected.joinToString(", ")
            )
        } else {
            ProhibitedContactCheckResult(isBlocked = false, reasonSummary = "")
        }
    }

    fun moderateText(rawText: String): ModerationResult {
        val detected = mutableSetOf<String>()
        val tokenRegex = Regex("""\b[A-Za-z]+\b""")
        val moderated = tokenRegex.replace(rawText) { match ->
            val word = match.value
            if (prohibitedWords.contains(word.lowercase(Locale.ROOT))) {
                detected.add(word.lowercase(Locale.ROOT))
                "▓".repeat(word.length.coerceAtLeast(4))
            } else {
                word
            }
        }
        return ModerationResult(
            publicText = moderated,
            hasBlurredWords = detected.isNotEmpty(),
            detectedWords = detected.toList()
        )
    }
}
