package com.clinicchecker.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clinicchecker.app.R
import com.clinicchecker.app.data.ClinicData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    uiState: MainUiState,
    onStartMonitoring: () -> Unit,
    onStopMonitoring: () -> Unit,
    onRefresh: () -> Unit,
    onSettingsClick: () -> Unit,
    onClearError: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
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
            // Error dialog
            uiState.error?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onClearError) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                }
            }

            // Main content
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    // Check if user has reservation
                    if (!uiState.clinicData.hasReservation) {
                        // No reservation state
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = stringResource(R.string.no_reservation_message),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    } else {
                        // Has reservation - show normal information
                        // Current consultation number
                        InfoRow(
                            label = stringResource(R.string.current_consultation_number),
                            value = uiState.clinicData.currentNumber.toString(),
                            icon = Icons.Default.Person
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Reservation number
                        InfoRow(
                            label = stringResource(R.string.your_reservation_number),
                            value = uiState.clinicData.reservationNumber.toString(),
                            icon = Icons.Default.Assignment
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Wait time prediction (only show if user has reservation)
                    if (uiState.clinicData.hasReservation && uiState.clinicData.averageConsultationTime > 0) {
                        InfoRow(
                            label = stringResource(R.string.average_consultation_time),
                            value = "${uiState.clinicData.averageConsultationTime}${stringResource(R.string.minutes)}",
                            icon = Icons.Default.Timer
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (uiState.clinicData.estimatedCallTime.isNotEmpty()) {
                            InfoRow(
                                label = stringResource(R.string.estimated_call_time),
                                value = uiState.clinicData.estimatedCallTime,
                                icon = Icons.Default.Schedule
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            InfoRow(
                                label = stringResource(R.string.time_remaining),
                                value = "${uiState.clinicData.timeRemaining}${stringResource(R.string.minutes)}",
                                icon = Icons.Default.AccessTime
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Next refresh time and predicted consultation time
                    if (uiState.isMonitoring) {
                        InfoRow(
                            label = stringResource(R.string.next_refresh_time),
                            value = uiState.nextRefreshTime,
                            icon = Icons.Default.Refresh
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        if (uiState.clinicData.averageConsultationTime > 0) {
                            InfoRow(
                                label = stringResource(R.string.predicted_consultation_time),
                                value = "${uiState.clinicData.averageConsultationTime}${stringResource(R.string.minutes)}",
                                icon = Icons.Default.Timer
                            )
                        }
                    }
                }
            }

            // Action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (uiState.isMonitoring) {
                    Button(
                        onClick = onStopMonitoring,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            stringResource(R.string.stop_monitoring),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Button(
                        onClick = onRefresh,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.refresh))
                    }
                } else {
                    Button(
                        onClick = onStartMonitoring,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState.credentials.clinicId.isNotBlank() && uiState.credentials.password.isNotBlank()
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.start_monitoring))
                    }
                }
            }

            // Status indicator
            if (uiState.isMonitoring) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Circle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.status_monitoring),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
} 