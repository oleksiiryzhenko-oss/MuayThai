package com.muaythai.arena.ui

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Sound effects and haptics. Both respect the toggles in Settings.
 * Tones are synthesised by the system, so the app ships no audio files.
 */
class Feedback(context: Context) {
    @Volatile var soundEnabled: Boolean = true
    @Volatile var vibrationEnabled: Boolean = true

    private val vibrator: Vibrator? = context.getSystemService(Vibrator::class.java)
    private val tone: ToneGenerator? = try {
        ToneGenerator(AudioManager.STREAM_MUSIC, 55)
    } catch (e: RuntimeException) {
        null
    }

    private fun beep(type: Int, ms: Int) {
        if (!soundEnabled) return
        try {
            tone?.startTone(type, ms)
        } catch (e: RuntimeException) {
            // Audio focus or the tone generator is unavailable; feedback is optional.
        }
    }

    private fun buzz(ms: Long) {
        if (!vibrationEnabled) return
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(ms)
            }
        } catch (e: RuntimeException) {
            // Some devices reject vibration while in battery saver.
        }
    }

    fun tap() = buzz(12)
    fun strike() { beep(ToneGenerator.TONE_PROP_BEEP, 40); buzz(18) }
    fun success() { beep(ToneGenerator.TONE_PROP_ACK, 140); buzz(30) }
    fun error() { beep(ToneGenerator.TONE_PROP_NACK, 180); buzz(80) }
    fun bell() { beep(ToneGenerator.TONE_PROP_BEEP2, 420); buzz(160) }
    fun reward() { beep(ToneGenerator.TONE_PROP_PROMPT, 220); buzz(60) }
}

val LocalFeedback = staticCompositionLocalOf<Feedback> { error("Feedback not provided") }
