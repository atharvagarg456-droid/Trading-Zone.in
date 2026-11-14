package com.example.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View

object SoundAndHapticFeedback {

    private const val PREFS_NAME = "tz_sound_haptic_prefs"
    private const val KEY_SOUND_ENABLED = "sound_enabled"
    private const val KEY_HAPTIC_ENABLED = "haptic_enabled"

    fun isSoundEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_SOUND_ENABLED, true)
    }

    fun setSoundEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SOUND_ENABLED, enabled)
            .apply()
    }

    fun isHapticEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_HAPTIC_ENABLED, true)
    }

    fun setHapticEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_HAPTIC_ENABLED, enabled)
            .apply()
    }

    fun playTap(context: Context) {
        if (isHapticEnabled(context)) {
            vibratePattern(context, longArrayOf(0, 12))
        }
        if (isSoundEnabled(context)) {
            playToneSafe(ToneGenerator.TONE_PROP_BEEP, 28, 45)
        }
    }

    fun playTap(view: View) {
        val ctx = view.context
        if (isHapticEnabled(ctx)) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
        if (isSoundEnabled(ctx)) {
            playToneSafe(ToneGenerator.TONE_PROP_BEEP, 28, 45)
        }
    }

    fun playClick(context: Context) = playTap(context)

    fun playClick(view: View) = playTap(view)

    fun playSuccess(context: Context) {
        if (isHapticEnabled(context)) {
            vibratePattern(context, longArrayOf(0, 22, 40, 35))
        }
        if (isSoundEnabled(context)) {
            playToneSafe(ToneGenerator.TONE_PROP_ACK, 75, 65)
        }
    }

    fun playSuccess(view: View) {
        val ctx = view.context
        if (isHapticEnabled(ctx)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            } else {
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            }
            vibratePattern(ctx, longArrayOf(0, 22, 40, 35))
        }
        if (isSoundEnabled(ctx)) {
            playToneSafe(ToneGenerator.TONE_PROP_ACK, 75, 65)
        }
    }

    fun playAlertOrError(context: Context) {
        if (isHapticEnabled(context)) {
            vibratePattern(context, longArrayOf(0, 45, 50, 65))
        }
        if (isSoundEnabled(context)) {
            playToneSafe(ToneGenerator.TONE_PROP_NACK, 110, 75)
        }
    }

    fun playAlertOrError(view: View) {
        val ctx = view.context
        if (isHapticEnabled(ctx)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                view.performHapticFeedback(HapticFeedbackConstants.REJECT)
            } else {
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            }
            vibratePattern(ctx, longArrayOf(0, 45, 50, 65))
        }
        if (isSoundEnabled(ctx)) {
            playToneSafe(ToneGenerator.TONE_PROP_NACK, 110, 75)
        }
    }

    /**
     * Triggers specialized haptic vibration patterns & synthesizer tones for Trade Signals:
     * - "BUY": Rapid double ascending pulse [0, 30, 45, 75] + rising dual-tone
     * - "SELL": Sharp descending warning pulse [0, 70, 40, 35, 40, 70] + warning tone
     * - "TARGET HIT": Celebratory triple-burst jackpot fanfare [0, 25, 35, 25, 35, 50, 45, 130]
     */
    fun playTradeSignal(context: Context, signalType: String) {
        val norm = signalType.trim().uppercase()
        when {
            norm.contains("TARGET") || norm.contains("PROFIT") || norm.contains("HIT") -> {
                if (isHapticEnabled(context)) {
                    vibratePattern(context, longArrayOf(0, 25, 35, 25, 35, 50, 45, 130))
                }
                if (isSoundEnabled(context)) {
                    playToneSafe(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 190, 85)
                }
            }
            norm.contains("SELL") || norm.contains("SHORT") || norm.contains("PUT") -> {
                if (isHapticEnabled(context)) {
                    vibratePattern(context, longArrayOf(0, 70, 40, 35, 40, 70))
                }
                if (isSoundEnabled(context)) {
                    playToneSafe(ToneGenerator.TONE_PROP_BEEP2, 135, 80)
                }
            }
            else -> {
                // Default to BUY signal pattern
                if (isHapticEnabled(context)) {
                    vibratePattern(context, longArrayOf(0, 30, 45, 75))
                }
                if (isSoundEnabled(context)) {
                    playToneSafe(ToneGenerator.TONE_PROP_ACK, 115, 80)
                }
            }
        }
    }

    fun detectSignalType(title: String, content: String = ""): String {
        val combined = "$title $content".uppercase()
        return when {
            combined.contains("TARGET HIT") ||
                combined.contains("TGT HIT") ||
                combined.contains("TARGET ACHIEVED") ||
                combined.contains("ALL TARGETS") ||
                combined.contains("BOOK PROFIT") -> "TARGET HIT"
            combined.contains("SELL") ||
                combined.contains("SHORT") ||
                combined.contains("BEARISH") -> "SELL"
            combined.contains("BUY") ||
                combined.contains("LONG") ||
                combined.contains("BULLISH") -> "BUY"
            else -> "BUY"
        }
    }

    private fun vibratePattern(context: Context, pattern: LongArray) {
        try {
            val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(40L)
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun playToneSafe(toneType: Int, durationMs: Int, volume: Int) {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, volume.coerceIn(10, 100))
            toneGen.startTone(toneType, durationMs)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try {
                    toneGen.release()
                } catch (_: Exception) {
                }
            }, (durationMs + 60).toLong())
        } catch (_: Exception) {
        }
    }
}
