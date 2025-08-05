package com.clinicchecker.app.notification

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.clinicchecker.app.R
import com.clinicchecker.app.data.NotificationSettings
import java.util.*

class ClinicNotificationManager(private val context: Context) {
    private var textToSpeech: TextToSpeech? = null
    private val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    private val notificationManager = NotificationManagerCompat.from(context)

    init {
        initializeTextToSpeech()
    }

    private fun initializeTextToSpeech() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech?.setLanguage(Locale.JAPANESE)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    // Fallback to default language
                    textToSpeech?.setLanguage(Locale.getDefault())
                }
            }
        }

        textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {}
            override fun onError(utteranceId: String?) {}
        })
    }

    fun notifyConsultationApproaching(
        settings: NotificationSettings,
        currentNumber: Int,
        reservationNumber: Int,
        estimatedTime: String,
        minutesRemaining: Int
    ) {
        val message = context.getString(
            R.string.predicted_call_time,
            estimatedTime,
            minutesRemaining
        )

        if (settings.enableVoice) {
            speakNotification(message)
        }

        if (settings.enableVibration) {
            vibrate()
        }

        if (settings.enableSystemNotification) {
            showSystemNotification(message)
        }
    }

    private fun speakNotification(message: String) {
        textToSpeech?.let { tts ->
            val fullMessage = "${context.getString(R.string.consultation_approaching)}。$message"
            
            // 開発者モード用の特別なメッセージ
            val specialMessage = if (context.getSharedPreferences("clinic_checker_prefs", Context.MODE_PRIVATE)
                .getBoolean("developer_mode", false)) {
                "何番、つまり今診察中の人の番号まで呼ばれています。$message"
            } else {
                fullMessage
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                tts.speak(specialMessage, TextToSpeech.QUEUE_FLUSH, null, "clinic_notification")
            } else {
                @Suppress("DEPRECATION")
                tts.speak(specialMessage, TextToSpeech.QUEUE_FLUSH, null)
            }
        }
    }

    private fun vibrate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE)
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(1000)
        }
    }

    private fun showSystemNotification(message: String) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.consultation_approaching))
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        try {
            notificationManager.notify(NOTIFICATION_ID, builder.build())
        } catch (e: SecurityException) {
            // Handle notification permission not granted
        }
    }

    fun shouldNotify(
        settings: NotificationSettings,
        currentNumber: Int,
        reservationNumber: Int,
        previousCurrentNumber: Int
    ): Boolean {
        return when (settings.policy) {
            com.clinicchecker.app.data.NotificationPolicy.NO_NOTIFICATION -> false
            com.clinicchecker.app.data.NotificationPolicy.ALWAYS_NOTIFY -> 
                currentNumber >= (reservationNumber - settings.offset)
            com.clinicchecker.app.data.NotificationPolicy.NOTIFY_ON_INCREMENT -> 
                currentNumber > previousCurrentNumber && currentNumber >= (reservationNumber - settings.offset)
        }
    }

    fun cleanup() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
    }

    companion object {
        private const val CHANNEL_ID = "clinic_checker_channel"
        private const val NOTIFICATION_ID = 1
    }
} 