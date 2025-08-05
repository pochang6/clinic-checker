package com.clinicchecker.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clinicchecker.app.R
import com.clinicchecker.app.data.NotificationPolicy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onClinicIdChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onPollingIntervalChange: (Int) -> Unit,
    onNotificationOffsetChange: (Int) -> Unit,
    onEnableVoiceChange: (Boolean) -> Unit,
    onEnableVibrationChange: (Boolean) -> Unit,
    onEnableSystemNotificationChange: (Boolean) -> Unit,
    onNotificationPolicyChange: (NotificationPolicy) -> Unit,
    onDeveloperModeChange: (Boolean) -> Unit,
    onManualReservationNumberChange: (Int) -> Unit,
    onMockHasReservationChange: (Boolean) -> Unit,
    onAdsRemovedChange: (Boolean) -> Unit,
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onSaveClick) {
                        Icon(Icons.Default.Save, contentDescription = "Save")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Clinic Settings
            SettingsSection(title = stringResource(R.string.clinic_settings)) {
                OutlinedTextField(
                    value = uiState.credentials.clinicId,
                    onValueChange = onClinicIdChange,
                    label = { Text(stringResource(R.string.clinic_id)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = uiState.credentials.password,
                    onValueChange = onPasswordChange,
                    label = { Text(stringResource(R.string.password)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            // Polling Settings
            SettingsSection(title = stringResource(R.string.polling_interval)) {
                OutlinedTextField(
                    value = uiState.pollingInterval.toString(),
                    onValueChange = { value ->
                        value.toIntOrNull()?.let { onPollingIntervalChange(it) }
                    },
                    label = { Text(stringResource(R.string.polling_interval)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            // Notification Settings
            SettingsSection(title = stringResource(R.string.notification_settings)) {
                OutlinedTextField(
                    value = uiState.notificationSettings.offset.toString(),
                    onValueChange = { value ->
                        value.toIntOrNull()?.let { onNotificationOffsetChange(it) }
                    },
                    label = { Text(stringResource(R.string.notification_offset)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = uiState.notificationSettings.enableVoice,
                        onCheckedChange = onEnableVoiceChange
                    )
                    Text(stringResource(R.string.enable_voice_notification))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = uiState.notificationSettings.enableVibration,
                        onCheckedChange = onEnableVibrationChange
                    )
                    Text(stringResource(R.string.enable_vibration))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = uiState.notificationSettings.enableSystemNotification,
                        onCheckedChange = onEnableSystemNotificationChange
                    )
                    Text(stringResource(R.string.enable_system_notification))
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.notification_policy),
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                NotificationPolicy.values().forEach { policy ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = uiState.notificationSettings.policy == policy,
                            onClick = { onNotificationPolicyChange(policy) }
                        )
                        Text(
                            text = when (policy) {
                                NotificationPolicy.NO_NOTIFICATION -> stringResource(R.string.no_notification)
                                NotificationPolicy.ALWAYS_NOTIFY -> stringResource(R.string.always_notify)
                                NotificationPolicy.NOTIFY_ON_INCREMENT -> stringResource(R.string.notify_on_increment)
                            }
                        )
                    }
                }
            }

            // Developer Mode
            SettingsSection(title = stringResource(R.string.developer_mode)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = uiState.developerMode,
                        onCheckedChange = onDeveloperModeChange
                    )
                    Text(stringResource(R.string.enable_developer_mode))
                }

                if (uiState.developerMode) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = stringResource(R.string.mock_data_settings),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = uiState.manualReservationNumber.toString(),
                        onValueChange = { value ->
                            value.toIntOrNull()?.let { onManualReservationNumberChange(it) }
                        },
                        label = { Text(stringResource(R.string.manual_reservation_number)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = uiState.mockHasReservation,
                            onCheckedChange = onMockHasReservationChange
                        )
                        Text(stringResource(R.string.mock_has_reservation))
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = stringResource(R.string.developer_mode_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Ads Settings
            SettingsSection(title = stringResource(R.string.remove_ads)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = uiState.adsRemoved,
                        onCheckedChange = onAdsRemovedChange,
                        enabled = false // This should be controlled by in-app purchase
                    )
                    Text(
                        text = if (uiState.adsRemoved) {
                            stringResource(R.string.ads_removed)
                        } else {
                            stringResource(R.string.purchase_ads_removal)
                        }
                    )
                }
            }

            // Save Button
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = onSaveClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.save_settings),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
} 