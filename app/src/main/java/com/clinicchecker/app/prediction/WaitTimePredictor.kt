package com.clinicchecker.app.prediction

import com.clinicchecker.app.data.ConsultationRecord
import java.text.SimpleDateFormat
import java.util.*

class WaitTimePredictor {
    
    fun calculateAverageConsultationTime(records: List<ConsultationRecord>): Long {
        if (records.isEmpty()) return 5 // 初期値として5分を返す
        
        val completedRecords = records.filter { it.duration != null }
        if (completedRecords.isEmpty()) return 5 // 初期値として5分を返す
        
        // 3名分以上のデータがある場合のみ最大・最小値を除外
        if (completedRecords.size >= 3) {
            val sortedDurations = completedRecords.map { it.duration!! }.sorted()
            val durationsWithoutExtremes = sortedDurations.drop(1).dropLast(1) // 最大・最小値を除外
            val totalDuration = durationsWithoutExtremes.sum()
            return totalDuration / durationsWithoutExtremes.size
        } else {
            // 3名未満の場合は通常の平均
            val totalDuration = completedRecords.sumOf { it.duration!! }
            return totalDuration / completedRecords.size
        }
    }
    
    fun predictCallTime(
        currentNumber: Int,
        reservationNumber: Int,
        averageConsultationTime: Long,
        records: List<ConsultationRecord>
    ): Pair<String, Long> {
        if (averageConsultationTime <= 0 || currentNumber >= reservationNumber) {
            return Pair("", 0L)
        }
        
        val patientsAhead = reservationNumber - currentNumber
        val estimatedMinutes = patientsAhead * averageConsultationTime
        
        val estimatedTime = Calendar.getInstance().apply {
            add(Calendar.MINUTE, estimatedMinutes.toInt())
        }
        
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val timeString = timeFormat.format(estimatedTime.time)
        
        return Pair(timeString, estimatedMinutes)
    }
    
    fun updateConsultationRecords(
        currentRecords: List<ConsultationRecord>,
        currentNumber: Int,
        previousNumber: Int
    ): List<ConsultationRecord> {
        val now = System.currentTimeMillis()
        val updatedRecords = currentRecords.toMutableList()
        
        // Mark completed consultations
        for (i in 0 until updatedRecords.size) {
            val record = updatedRecords[i]
            if (record.endTime == null && record.patientNumber < currentNumber) {
                val duration = (now - record.startTime) / (1000 * 60) // Convert to minutes
                updatedRecords[i] = record.copy(
                    endTime = now,
                    duration = duration
                )
            }
        }
        
        // Add new consultation if number increased
        if (currentNumber > previousNumber) {
            for (number in (previousNumber + 1)..currentNumber) {
                if (!updatedRecords.any { it.patientNumber == number }) {
                    updatedRecords.add(
                        ConsultationRecord(
                            patientNumber = number,
                            startTime = now
                        )
                    )
                }
            }
        }
        
        return updatedRecords
    }
    
    fun shouldStartTracking(currentNumber: Int, reservationNumber: Int): Boolean {
        return currentNumber > 0 && reservationNumber > 0 && currentNumber < reservationNumber
    }
    
    fun getRelevantRecords(
        records: List<ConsultationRecord>,
        currentNumber: Int,
        maxRecords: Int = 10
    ): List<ConsultationRecord> {
        return records
            .filter { it.patientNumber >= currentNumber - maxRecords }
            .sortedBy { it.patientNumber }
    }
} 