package com.clinicchecker.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "clinic_checker_prefs")

class ClinicDataStore(private val context: Context) {

    // Clinic credentials
    private val CLINIC_ID = stringPreferencesKey("clinic_id")
    private val PASSWORD = stringPreferencesKey("password")

    // Polling settings
    private val POLLING_INTERVAL = intPreferencesKey("polling_interval")

    // Notification settings
    private val NOTIFICATION_OFFSET = intPreferencesKey("notification_offset")
    private val ENABLE_VOICE = booleanPreferencesKey("enable_voice")
    private val ENABLE_VIBRATION = booleanPreferencesKey("enable_vibration")
    private val ENABLE_SYSTEM_NOTIFICATION = booleanPreferencesKey("enable_system_notification")
    private val NOTIFICATION_POLICY = stringPreferencesKey("notification_policy")

    // Developer mode
    private val DEVELOPER_MODE = booleanPreferencesKey("developer_mode")
    private val MANUAL_RESERVATION_NUMBER = intPreferencesKey("manual_reservation_number")
    private val MOCK_HAS_RESERVATION = booleanPreferencesKey("mock_has_reservation")

    // Ads
    private val ADS_REMOVED = booleanPreferencesKey("ads_removed")

    // Consultation records
    private val CONSULTATION_RECORDS = stringPreferencesKey("consultation_records")

    val clinicCredentials: Flow<ClinicCredentials> = context.dataStore.data.map { preferences ->
        ClinicCredentials(
            clinicId = preferences[CLINIC_ID] ?: "",
            password = preferences[PASSWORD] ?: ""
        )
    }

    val pollingInterval: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[POLLING_INTERVAL] ?: 60
    }

    val notificationSettings: Flow<NotificationSettings> = context.dataStore.data.map { preferences ->
        NotificationSettings(
            offset = preferences[NOTIFICATION_OFFSET] ?: 3,
            enableVoice = preferences[ENABLE_VOICE] ?: true,
            enableVibration = preferences[ENABLE_VIBRATION] ?: true,
            enableSystemNotification = preferences[ENABLE_SYSTEM_NOTIFICATION] ?: true,
            policy = try {
                NotificationPolicy.valueOf(preferences[NOTIFICATION_POLICY] ?: NotificationPolicy.ALWAYS_NOTIFY.name)
            } catch (e: IllegalArgumentException) {
                NotificationPolicy.ALWAYS_NOTIFY
            }
        )
    }

    val developerMode: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[DEVELOPER_MODE] ?: false
    }

    val manualReservationNumber: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[MANUAL_RESERVATION_NUMBER] ?: 0
    }

    val mockHasReservation: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[MOCK_HAS_RESERVATION] ?: true
    }

    val adsRemoved: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[ADS_REMOVED] ?: false
    }

    suspend fun saveClinicCredentials(credentials: ClinicCredentials) {
        context.dataStore.edit { preferences ->
            preferences[CLINIC_ID] = credentials.clinicId
            preferences[PASSWORD] = credentials.password
        }
    }

    suspend fun savePollingInterval(interval: Int) {
        context.dataStore.edit { preferences ->
            preferences[POLLING_INTERVAL] = interval
        }
    }

    suspend fun saveNotificationSettings(settings: NotificationSettings) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATION_OFFSET] = settings.offset
            preferences[ENABLE_VOICE] = settings.enableVoice
            preferences[ENABLE_VIBRATION] = settings.enableVibration
            preferences[ENABLE_SYSTEM_NOTIFICATION] = settings.enableSystemNotification
            preferences[NOTIFICATION_POLICY] = settings.policy.name
        }
    }

    suspend fun saveDeveloperMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DEVELOPER_MODE] = enabled
        }
    }

    suspend fun saveManualReservationNumber(number: Int) {
        context.dataStore.edit { preferences ->
            preferences[MANUAL_RESERVATION_NUMBER] = number
        }
    }

    suspend fun saveMockHasReservation(hasReservation: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[MOCK_HAS_RESERVATION] = hasReservation
        }
    }

    suspend fun saveAdsRemoved(removed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ADS_REMOVED] = removed
        }
    }

    suspend fun saveConsultationRecords(records: List<ConsultationRecord>) {
        val json = records.joinToString("|") { record ->
            "${record.patientNumber},${record.startTime},${record.endTime ?: ""},${record.duration ?: ""}"
        }
        context.dataStore.edit { preferences ->
            preferences[CONSULTATION_RECORDS] = json
        }
    }

    val consultationRecords: Flow<List<ConsultationRecord>> = context.dataStore.data.map { preferences ->
        val json = preferences[CONSULTATION_RECORDS] ?: ""
        if (json.isEmpty()) {
            emptyList()
        } else {
            json.split("|").mapNotNull { recordStr ->
                val parts = recordStr.split(",")
                if (parts.size >= 4) {
                    ConsultationRecord(
                        patientNumber = parts[0].toIntOrNull() ?: return@mapNotNull null,
                        startTime = parts[1].toLongOrNull() ?: return@mapNotNull null,
                        endTime = parts[2].toLongOrNull(),
                        duration = parts[3].toLongOrNull()
                    )
                } else null
            }
        }
    }
} 