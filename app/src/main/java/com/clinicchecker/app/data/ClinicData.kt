package com.clinicchecker.app.data

import kotlinx.parcelize.Parcelize
import android.os.Parcelable

@Parcelize
data class ClinicData(
    val currentNumber: Int = 0,
    val reservationNumber: Int = 0,
    val averageConsultationTime: Long = 0, // in minutes
    val estimatedCallTime: String = "",
    val timeRemaining: Long = 0, // in minutes
    val lastUpdateTime: Long = 0,
    val isMonitoring: Boolean = false,
    val hasReservation: Boolean = false
) : Parcelable

@Parcelize
data class ConsultationRecord(
    val patientNumber: Int,
    val startTime: Long,
    val endTime: Long? = null,
    val duration: Long? = null // in minutes
) : Parcelable

@Parcelize
data class NotificationSettings(
    val offset: Int = 3,
    val enableVoice: Boolean = true,
    val enableVibration: Boolean = true,
    val enableSystemNotification: Boolean = true,
    val policy: NotificationPolicy = NotificationPolicy.ALWAYS_NOTIFY
) : Parcelable

enum class NotificationPolicy {
    NO_NOTIFICATION,
    ALWAYS_NOTIFY,
    NOTIFY_ON_INCREMENT
}

@Parcelize
data class ClinicCredentials(
    val clinicId: String = "",
    val password: String = ""
) : Parcelable 