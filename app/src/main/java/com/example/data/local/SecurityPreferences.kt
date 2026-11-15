package com.example.data.local

import android.content.Context
import com.example.data.model.AppThemePreference
import java.security.MessageDigest

class SecurityPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "trading_zone_security_prefs",
        Context.MODE_PRIVATE
    )

    fun isPinConfigured(userId: String): Boolean {
        return !prefs.getString("pin_hash_$userId", null).isNullOrBlank()
    }

    fun saveAppPassword(userId: String, password: String) {
        val clean = password.trim()
        if (clean.isNotBlank()) {
            prefs.edit()
                .putString("app_pass_hash_$userId", sha256(clean))
                .putString("app_pass_plain_$userId", clean)
                .apply()
        }
    }

    fun getPlainAppPassword(userId: String): String {
        return prefs.getString("app_pass_plain_$userId", "") ?: ""
    }

    fun isAppPasswordConfigured(userId: String): Boolean {
        return !prefs.getString("app_pass_hash_$userId", null).isNullOrBlank()
    }

    fun verifyAppPassword(userId: String, candidatePassword: String): Boolean {
        val storedHash = prefs.getString("app_pass_hash_$userId", null)
        if (storedHash.isNullOrBlank()) return true
        return storedHash == sha256(candidatePassword.trim())
    }

    fun savePin(userId: String, pin: String) {
        val clean = pin.trim()
        prefs.edit()
            .putString("pin_hash_$userId", sha256(clean))
            .putString("pin_plain_$userId", clean)
            .apply()
    }

    fun getPlainPin(userId: String): String {
        return prefs.getString("pin_plain_$userId", "") ?: ""
    }

    fun verifyPin(userId: String, candidatePin: String): Boolean {
        val storedHash = prefs.getString("pin_hash_$userId", null) ?: return false
        return storedHash == sha256(candidatePin.trim())
    }

    fun isBiometricEnabled(userId: String): Boolean {
        return prefs.getBoolean("biometric_$userId", false)
    }

    fun setBiometricEnabled(userId: String, enabled: Boolean) {
        prefs.edit().putBoolean("biometric_$userId", enabled).apply()
    }

    fun saveSecurityQuestionAndAnswer(userId: String, question: String, answer: String) {
        prefs.edit()
            .putString("sec_q_$userId", question.trim())
            .putString("sec_a_hash_$userId", sha256(answer.trim().lowercase()))
            .apply()
    }

    fun getSecurityQuestion(userId: String): String {
        return prefs.getString("sec_q_$userId", "") ?: ""
    }

    fun verifySecurityAnswer(userId: String, candidateAnswer: String): Boolean {
        val storedHash = prefs.getString("sec_a_hash_$userId", null) ?: return false
        return storedHash == sha256(candidateAnswer.trim().lowercase())
    }

    fun getThemePreference(): AppThemePreference {
        val raw = prefs.getString("theme_pref", AppThemePreference.DARK.name)
        return try {
            AppThemePreference.valueOf(raw ?: AppThemePreference.DARK.name)
        } catch (_: Exception) {
            AppThemePreference.DARK
        }
    }

    fun setThemePreference(preference: AppThemePreference) {
        prefs.edit().putString("theme_pref", preference.name).apply()
    }

    // Customizable Notification Preferences
    fun isNotificationsEnabled(): Boolean {
        return prefs.getBoolean("notifications_enabled", true)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("notifications_enabled", enabled).apply()
    }

    fun isOfflineDataReminderEnabled(): Boolean {
        return prefs.getBoolean("offline_data_reminder_enabled", true)
    }

    fun setOfflineDataReminderEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("offline_data_reminder_enabled", enabled).apply()
    }

    fun getOfflineReminderIntervalMinutes(): Int {
        return prefs.getInt("offline_reminder_interval_mins", 60).coerceIn(15, 240)
    }

    fun setOfflineReminderIntervalMinutes(minutes: Int) {
        prefs.edit().putInt("offline_reminder_interval_mins", minutes.coerceIn(15, 240)).apply()
    }

    fun getOfflineSinceMillis(): Long {
        return prefs.getLong("offline_since_millis", 0L)
    }

    fun setOfflineSinceMillis(timestampMillis: Long) {
        prefs.edit().putLong("offline_since_millis", timestampMillis).apply()
    }

    fun getLastOfflineNotificationSentMillis(): Long {
        return prefs.getLong("last_offline_notif_sent_millis", 0L)
    }

    fun setLastOfflineNotificationSentMillis(timestampMillis: Long) {
        prefs.edit().putLong("last_offline_notif_sent_millis", timestampMillis).apply()
    }

    fun isCycleExpiryNotificationsEnabled(): Boolean {
        return prefs.getBoolean("cycle_expiry_notifications_enabled", true)
    }

    fun setCycleExpiryNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("cycle_expiry_notifications_enabled", enabled).apply()
    }

    fun isHighRiskVibrationEnabled(): Boolean {
        return prefs.getBoolean("high_risk_vibration", true)
    }

    fun setHighRiskVibrationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("high_risk_vibration", enabled).apply()
    }

    fun isCompactFeedMode(): Boolean {
        return prefs.getBoolean("compact_feed_mode", false)
    }

    fun setCompactFeedMode(enabled: Boolean) {
        prefs.edit().putBoolean("compact_feed_mode", enabled).apply()
    }

    fun isLargeTextMode(): Boolean {
        return prefs.getBoolean("large_text_mode", false)
    }

    fun setLargeTextMode(enabled: Boolean) {
        prefs.edit().putBoolean("large_text_mode", enabled).apply()
    }

    fun getOpenedMessageIds(userId: String): Set<String> {
        return prefs.getStringSet("opened_msgs_$userId", emptySet())?.toSet() ?: emptySet()
    }

    fun markMessageOpened(userId: String, messageId: String) {
        if (messageId.isBlank()) return
        val current = getOpenedMessageIds(userId).toMutableSet()
        current.add(messageId)
        prefs.edit().putStringSet("opened_msgs_$userId", current).apply()
    }

    fun getAppleWebAppUrl(): String {
        val defaultUrl = "https://raw.githack.com/atharvagarg456-droid/Trading-Zone.in/main/docs/index.html"
        val raw = prefs.getString("apple_web_app_url", defaultUrl)
            ?.ifBlank { defaultUrl }
            ?: defaultUrl
        return when {
            raw.contains("TradingCoLTD") || raw == "https://atharvagarg456-droid.github.io/Trading-Zone.in/" -> {
                prefs.edit().putString("apple_web_app_url", defaultUrl).apply()
                defaultUrl
            }
            else -> raw
        }
    }

    fun setAppleWebAppUrl(url: String) {
        val clean = url.trim().ifBlank { "https://raw.githack.com/atharvagarg456-droid/Trading-Zone.in/main/docs/index.html" }
        prefs.edit().putString("apple_web_app_url", clean).apply()
    }

    fun getAndroidApkShareUrl(): String {
        val defaultUrl = "https://github.com/atharvagarg456-droid/Trading-Zone.in/raw/main/Trading-Zone.apk"
        val raw = prefs.getString("android_apk_share_url", defaultUrl)
            ?.ifBlank { defaultUrl }
            ?: defaultUrl
        return if (raw.contains("TradingCoLTD")) {
            val migrated = raw.replace("TradingCoLTD", "Trading-Zone.in")
            prefs.edit().putString("android_apk_share_url", migrated).apply()
            migrated
        } else {
            raw
        }
    }

    fun setAndroidApkShareUrl(url: String) {
        val clean = url.trim().ifBlank {
            "https://github.com/atharvagarg456-droid/Trading-Zone.in/raw/main/Trading-Zone.apk"
        }
        prefs.edit().putString("android_apk_share_url", clean).apply()
    }

    fun getInstalledFirmwareStamp(): String {
        return prefs.getString("installed_firmware_stamp", "") ?: ""
    }

    fun setInstalledFirmwareStamp(stamp: String) {
        prefs.edit().putString("installed_firmware_stamp", stamp.trim()).apply()
    }

    fun isDeviceTrustedForQuickLogin(userId: String, deviceId: String): Boolean {
        if (userId.isBlank() || deviceId.isBlank()) return false
        return prefs.getBoolean("trusted_device_login_${userId}_${deviceId}", false)
    }

    fun setDeviceTrustedForQuickLogin(userId: String, deviceId: String, trusted: Boolean) {
        if (userId.isBlank() || deviceId.isBlank()) return
        prefs.edit().putBoolean("trusted_device_login_${userId}_${deviceId}", trusted).apply()
    }

    fun getScreenshotStrikeCount(userId: String): Int {
        if (userId.isBlank()) return 0
        return prefs.getInt("screenshot_strikes_$userId", 0).coerceAtLeast(0)
    }

    fun incrementScreenshotStrikeCount(userId: String): Int {
        if (userId.isBlank()) return 1
        val next = getScreenshotStrikeCount(userId) + 1
        prefs.edit().putInt("screenshot_strikes_$userId", next).apply()
        return next
    }

    fun resetScreenshotStrikeCount(userId: String) {
        if (userId.isBlank()) return
        prefs.edit().putInt("screenshot_strikes_$userId", 0).apply()
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
