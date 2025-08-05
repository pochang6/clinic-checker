package com.clinicchecker.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.clinicchecker.app.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    
    private val dataStore = ClinicDataStore(application)
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    init {
        viewModelScope.launch {
            dataStore.clinicCredentials.collect { credentials ->
                _uiState.value = _uiState.value.copy(credentials = credentials)
            }
        }
        viewModelScope.launch {
            dataStore.pollingInterval.collect { interval ->
                _uiState.value = _uiState.value.copy(pollingInterval = interval)
            }
        }
        viewModelScope.launch {
            dataStore.notificationSettings.collect { settings ->
                _uiState.value = _uiState.value.copy(notificationSettings = settings)
            }
        }
        viewModelScope.launch {
            dataStore.developerMode.collect { devMode ->
                _uiState.value = _uiState.value.copy(developerMode = devMode)
            }
        }
        viewModelScope.launch {
            dataStore.manualReservationNumber.collect { number ->
                _uiState.value = _uiState.value.copy(manualReservationNumber = number)
            }
        }
        viewModelScope.launch {
            dataStore.adsRemoved.collect { removed ->
                _uiState.value = _uiState.value.copy(adsRemoved = removed)
            }
        }
        viewModelScope.launch {
            dataStore.mockHasReservation.collect { hasReservation ->
                _uiState.value = _uiState.value.copy(mockHasReservation = hasReservation)
            }
        }
    }
    
    fun updateClinicId(clinicId: String) {
        val currentCredentials = _uiState.value.credentials
        val updatedCredentials = currentCredentials.copy(clinicId = clinicId)
        
        viewModelScope.launch {
            dataStore.saveClinicCredentials(updatedCredentials)
        }
    }
    
    fun updatePassword(password: String) {
        val currentCredentials = _uiState.value.credentials
        val updatedCredentials = currentCredentials.copy(password = password)
        
        viewModelScope.launch {
            dataStore.saveClinicCredentials(updatedCredentials)
        }
    }
    
    fun updatePollingInterval(interval: Int) {
        viewModelScope.launch {
            dataStore.savePollingInterval(interval)
        }
    }
    
    fun updateNotificationOffset(offset: Int) {
        val currentSettings = _uiState.value.notificationSettings
        val updatedSettings = currentSettings.copy(offset = offset)
        
        viewModelScope.launch {
            dataStore.saveNotificationSettings(updatedSettings)
        }
    }
    
    fun updateEnableVoice(enabled: Boolean) {
        val currentSettings = _uiState.value.notificationSettings
        val updatedSettings = currentSettings.copy(enableVoice = enabled)
        
        viewModelScope.launch {
            dataStore.saveNotificationSettings(updatedSettings)
        }
    }
    
    fun updateEnableVibration(enabled: Boolean) {
        val currentSettings = _uiState.value.notificationSettings
        val updatedSettings = currentSettings.copy(enableVibration = enabled)
        
        viewModelScope.launch {
            dataStore.saveNotificationSettings(updatedSettings)
        }
    }
    
    fun updateEnableSystemNotification(enabled: Boolean) {
        val currentSettings = _uiState.value.notificationSettings
        val updatedSettings = currentSettings.copy(enableSystemNotification = enabled)
        
        viewModelScope.launch {
            dataStore.saveNotificationSettings(updatedSettings)
        }
    }
    
    fun updateNotificationPolicy(policy: NotificationPolicy) {
        val currentSettings = _uiState.value.notificationSettings
        val updatedSettings = currentSettings.copy(policy = policy)
        
        viewModelScope.launch {
            dataStore.saveNotificationSettings(updatedSettings)
        }
    }
    
    fun updateDeveloperMode(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.saveDeveloperMode(enabled)
        }
    }
    
    fun updateManualReservationNumber(number: Int) {
        viewModelScope.launch {
            dataStore.saveManualReservationNumber(number)
        }
    }
    
    fun updateAdsRemoved(removed: Boolean) {
        viewModelScope.launch {
            dataStore.saveAdsRemoved(removed)
        }
    }
    
    fun updateMockHasReservation(hasReservation: Boolean) {
        viewModelScope.launch {
            dataStore.saveMockHasReservation(hasReservation)
        }
    }
}

data class SettingsUiState(
    val credentials: ClinicCredentials = ClinicCredentials(),
    val pollingInterval: Int = 60,
    val notificationSettings: NotificationSettings = NotificationSettings(),
    val developerMode: Boolean = false,
    val manualReservationNumber: Int = 0,
    val mockHasReservation: Boolean = true,
    val adsRemoved: Boolean = false
) 