package com.clinicchecker.app.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.net.CookieManager
import java.net.CookiePolicy

class ClinicRepository {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val baseUrl = "https://ssc10.doctorqube.com/miyatanaika-clinic/input.cgi"

    suspend fun login(credentials: ClinicCredentials): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val formBody = FormBody.Builder()
                .add("login_id", credentials.clinicId)
                .add("password", credentials.password)
                .build()

            val request = Request.Builder()
                .url("$baseUrl?vMode=mode_bookConf&Stamp=154822")
                .post(formBody)
                .build()

            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (body != null && body.contains("ログイン")) {
                    Log.d("ClinicRepository", "Login successful")
                    Result.success(true)
                } else {
                    Log.e("ClinicRepository", "Login failed: Invalid response")
                    Result.failure(Exception("Invalid login response"))
                }
            } else {
                Log.e("ClinicRepository", "Login failed: ${response.code}")
                Result.failure(Exception("Login failed with code: ${response.code}"))
            }
        } catch (e: Exception) {
            Log.e("ClinicRepository", "Login error", e)
            Result.failure(e)
        }
    }

    suspend fun fetchClinicData(): Result<ClinicData> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl?vMode=mode_bookConf&Stamp=154822")
                .get()
                .build()

            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (body != null) {
                    val doc = Jsoup.parse(body)
                    val clinicData = parseClinicData(doc)
                    Result.success(clinicData)
                } else {
                    Result.failure(Exception("Empty response body"))
                }
            } else {
                Result.failure(Exception("Failed to fetch data: ${response.code}"))
            }
        } catch (e: Exception) {
            Log.e("ClinicRepository", "Fetch error", e)
            Result.failure(e)
        }
    }

    private fun parseClinicData(doc: Document): ClinicData {
        var currentNumber = 0
        var reservationNumber = 0

        // Parse current consultation number
        val currentNumberText = doc.select("text:contains(現在診察中の番号)").firstOrNull()?.text()
        if (currentNumberText != null) {
            val regex = Regex("現在(\\d+)番の方まで診察中")
            val match = regex.find(currentNumberText)
            currentNumber = match?.groupValues?.get(1)?.toIntOrNull() ?: 0
        }

        // Parse reservation number (this might need adjustment based on actual HTML structure)
        val reservationElements = doc.select("text:contains(自分の予約番号)")
        if (reservationElements.isNotEmpty()) {
            // This is a placeholder - actual parsing logic depends on the real HTML structure
            reservationNumber = 0
        }

        return ClinicData(
            currentNumber = currentNumber,
            reservationNumber = reservationNumber,
            lastUpdateTime = System.currentTimeMillis()
        )
    }

    suspend fun retryWithBackoff(
        maxRetries: Int = 3,
        initialDelay: Long = 10000,
        operation: suspend () -> Result<ClinicData>
    ): Result<ClinicData> {
        var lastException: Exception? = null
        var delay = initialDelay

        repeat(maxRetries) { attempt ->
            try {
                val result = operation()
                if (result.isSuccess) {
                    return result
                } else {
                    lastException = result.exceptionOrNull() as? Exception
                }
            } catch (e: Exception) {
                lastException = e
            }
            
            if (attempt < maxRetries - 1) {
                kotlinx.coroutines.delay(delay)
                delay *= 2 // Exponential backoff
            }
        }

        return Result.failure(lastException ?: Exception("Max retries exceeded"))
    }
} 