package com.clinicchecker.app.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.clinicchecker.app.data.*
import com.clinicchecker.app.notification.ClinicNotificationManager
import com.clinicchecker.app.prediction.WaitTimePredictor
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    private val dataStore = ClinicDataStore(application)
    private val repository = ClinicRepository()
    private val notificationManager = ClinicNotificationManager(application)
    private val waitTimePredictor = WaitTimePredictor()
    
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    
    private var previousCurrentNumber = 0
    private var isMonitoring = false
    
    init {
        viewModelScope.launch {
            // Collect all data sources individually
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
            dataStore.consultationRecords.collect { records ->
                _uiState.value = _uiState.value.copy(consultationRecords = records)
            }
        }
        viewModelScope.launch {
            dataStore.mockHasReservation.collect { hasReservation ->
                _uiState.value = _uiState.value.copy(mockHasReservation = hasReservation)
                // Update repository mock settings
                repository.setMockHasReservation(hasReservation)
            }
        }
    }
    
    fun startMonitoring() {
        if (isMonitoring) return
        
        isMonitoring = true
        _uiState.value = _uiState.value.copy(isMonitoring = true)
        
        viewModelScope.launch {
            val credentials = _uiState.value.credentials
            if (credentials.clinicId.isBlank() || credentials.password.isBlank()) {
                _uiState.value = _uiState.value.copy(
                    error = "クリニックIDとパスワードを設定してください"
                )
                return@launch
            }
            
            // Initial login
            val loginResult = repository.login(credentials, _uiState.value.developerMode)
            if (loginResult.isSuccess) {
                fetchAndUpdateData()
                startPolling()
            } else {
                _uiState.value = _uiState.value.copy(
                    error = "ログインに失敗しました: ${loginResult.exceptionOrNull()?.message}"
                )
                isMonitoring = false
                _uiState.value = _uiState.value.copy(isMonitoring = false)
            }
        }
    }
    
    fun stopMonitoring() {
        isMonitoring = false
        _uiState.value = _uiState.value.copy(isMonitoring = false)
    }
    
    fun refreshData() {
        if (!isMonitoring) return
        
        viewModelScope.launch {
            fetchAndUpdateData()
        }
    }
    
    private suspend fun startPolling() {
        val interval = _uiState.value.pollingInterval * 1000L
        
        while (isMonitoring) {
            kotlinx.coroutines.delay(interval)
            if (isMonitoring) {
                fetchAndUpdateData()
            }
        }
    }
    
    private suspend fun fetchAndUpdateData() {
        val result = repository.retryWithBackoff {
            repository.fetchClinicData(_uiState.value.developerMode)
        }
        
        if (result.isSuccess) {
            val newData = result.getOrNull() ?: return
            updateClinicData(newData)
        } else {
            _uiState.value = _uiState.value.copy(
                error = "データの取得に失敗しました: ${result.exceptionOrNull()?.message}"
            )
        }
    }
    
    private fun updateClinicData(newData: ClinicData) {
        val currentState = _uiState.value
        val currentNumber = newData.currentNumber
        val reservationNumber = if (currentState.developerMode && currentState.manualReservationNumber > 0) {
            currentState.manualReservationNumber
        } else {
            newData.reservationNumber
        }
        
        // Update consultation records
        val updatedRecords = waitTimePredictor.updateConsultationRecords(
            currentState.consultationRecords,
            currentNumber,
            previousCurrentNumber
        )
        
        // Calculate predictions
        val averageTime = waitTimePredictor.calculateAverageConsultationTime(updatedRecords)
        val (estimatedTime, minutesRemaining) = waitTimePredictor.predictCallTime(
            currentNumber,
            reservationNumber,
            averageTime,
            updatedRecords
        )
        
        // Check for notifications
        val shouldNotify = if (currentState.developerMode) {
            // 開発者モードでは常に通知
            true
        } else {
            notificationManager.shouldNotify(
                currentState.notificationSettings,
                currentNumber,
                reservationNumber,
                previousCurrentNumber
            )
        }
        
        if (shouldNotify) {
            Log.d("MainViewModel", "Sending notification: current=$currentNumber, reservation=$reservationNumber")
            notificationManager.notifyConsultationApproaching(
                currentState.notificationSettings,
                currentNumber,
                reservationNumber,
                estimatedTime,
                minutesRemaining.toInt()
            )
        }
        
        // Auto-stop when called (current number >= reservation number)
        if (currentNumber >= reservationNumber && reservationNumber > 0) {
            Log.d("MainViewModel", "Patient called! Auto-stopping monitoring")
            stopMonitoring()
        }
        
        // Update state
        val updatedData = newData.copy(
            reservationNumber = reservationNumber,
            averageConsultationTime = averageTime,
            estimatedCallTime = estimatedTime,
            timeRemaining = minutesRemaining,
            isMonitoring = isMonitoring
        )
        
        _uiState.value = currentState.copy(
            clinicData = updatedData,
            consultationRecords = updatedRecords,
            error = null
        )
        
        // Save updated records
        viewModelScope.launch {
            dataStore.saveConsultationRecords(updatedRecords)
        }
        
        previousCurrentNumber = currentNumber
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun updateNextRefreshTime() {
        val currentTime = Calendar.getInstance()
        currentTime.add(Calendar.SECOND, _uiState.value.pollingInterval)
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val nextRefreshTime = timeFormat.format(currentTime.time)
        
        _uiState.value = _uiState.value.copy(nextRefreshTime = nextRefreshTime)
    }
    
    override fun onCleared() {
        super.onCleared()
        notificationManager.cleanup()
    }
}

data class MainUiState(
    val clinicData: ClinicData = ClinicData(averageConsultationTime = 5), // 初期値を5分に設定
    val credentials: ClinicCredentials = ClinicCredentials(),
    val pollingInterval: Int = 60,
    val notificationSettings: NotificationSettings = NotificationSettings(),
    val developerMode: Boolean = false,
    val manualReservationNumber: Int = 0,
    val mockHasReservation: Boolean = true,
    val adsRemoved: Boolean = false,
    val consultationRecords: List<ConsultationRecord> = emptyList(),
    val isMonitoring: Boolean = false,
    val nextRefreshTime: String = "",
    val error: String? = null
) 