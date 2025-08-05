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
import kotlin.random.Random

class ClinicRepository {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val baseUrl = "https://ssc10.doctorqube.com/miyatanaika-clinic/input.cgi"
    
    // モック用の状態管理
    private var mockCurrentNumber = 15
    private var mockReservationNumber = 25
    private var mockIncrementCounter = 0
    private var mockHasReservation = true // 開発者モードで予約の有無を制御
    private var mockLastUpdateTime = System.currentTimeMillis()

    suspend fun login(credentials: ClinicCredentials, isDeveloperMode: Boolean = false): Result<Boolean> = withContext(Dispatchers.IO) {
        if (isDeveloperMode) {
            Log.d("ClinicRepository", "Developer mode: Mock login successful")
            return@withContext Result.success(true)
        }
        
        try {
            val formBody = FormBody.Builder()
                .add("Pno", credentials.clinicId)
                .add("Ppass", credentials.password)
                .add("vMode", "mode_bookConf")
                .add("eLang", "")
                .add("vSimple", "")
                .add("Step", "1")
                .add("gMailReg", "1")
                .add("bOK", "OK")
                .build()

            val request = Request.Builder()
                .url("https://ssc10.doctorqube.com/miyatanaika-clinic/dqw.cgi")
                .post(formBody)
                .build()

            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (body != null && (body.contains("現在のご予約状況") || body.contains("予約メニュー") || body.contains("ID:"))) {
                    Log.d("ClinicRepository", "Login successful")
                    Result.success(true)
                } else if (body != null && body.contains("通信できません")) {
                    Log.e("ClinicRepository", "Login failed: Communication error")
                    Result.failure(Exception("通信エラーが発生しました。しばらくしてから再試行してください。"))
                } else {
                    Log.e("ClinicRepository", "Login failed: Invalid response")
                    Result.failure(Exception("ログインに失敗しました。IDとパスワードを確認してください。"))
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

    suspend fun fetchClinicData(isDeveloperMode: Boolean = false): Result<ClinicData> = withContext(Dispatchers.IO) {
        if (isDeveloperMode) {
            return@withContext generateMockData()
        }
        
        try {
            // First, get the main page to check reservation status
            val mainRequest = Request.Builder()
                .url("https://ssc10.doctorqube.com/miyatanaika-clinic/dqw.cgi?vMode=mode_menu&ssc=788433556&Stamp=2414")
                .get()
                .build()

            val mainResponse = client.newCall(mainRequest).execute()
            
            if (mainResponse.isSuccessful) {
                val mainBody = mainResponse.body?.string()
                if (mainBody != null) {
                    val mainDoc = Jsoup.parse(mainBody)
                    
                    // Check if user has any reservation
                    val noReservationText = mainDoc.select(".booklist.sub_section").firstOrNull()?.text()
                    if (noReservationText?.contains("現在ご予約はありません") == true) {
                        // No reservation - return default state
                        return@withContext Result.success(ClinicData(
                            currentNumber = 0,
                            reservationNumber = 0,
                            lastUpdateTime = System.currentTimeMillis(),
                            hasReservation = false
                        ))
                    }
                    
                    // If user has reservation, parse the same page for consultation data
                    // The consultation information might be on the same page or we need to navigate
                    val clinicData = parseConsultationData(mainDoc)
                    Result.success(clinicData)
                } else {
                    Result.failure(Exception("Empty main response body"))
                }
            } else {
                Result.failure(Exception("Failed to fetch main data: ${mainResponse.code}"))
            }
        } catch (e: Exception) {
            Log.e("ClinicRepository", "Fetch error", e)
            Result.failure(e)
        }
    }

    private fun generateMockData(): Result<ClinicData> {
        // モックデータを生成（現実的なシナリオ）
        mockIncrementCounter++
        val currentTime = System.currentTimeMillis()
        
        if (!mockHasReservation) {
            // 予約なし状態
            return Result.success(ClinicData(
                currentNumber = 0,
                reservationNumber = 0,
                lastUpdateTime = currentTime,
                hasReservation = false
            ))
        }
        
        // 予約あり状態 - 20秒に1人診察が終わるシミュレーション
        val timeDiff = currentTime - mockLastUpdateTime
        val secondsPassed = timeDiff / 1000
        
        // 20秒ごとに1人診察が終わる
        if (secondsPassed >= 20) {
            val patientsCompleted = (secondsPassed / 20).toInt()
            mockCurrentNumber += patientsCompleted
            mockLastUpdateTime = currentTime - (secondsPassed % 20) * 1000
            Log.d("ClinicRepository", "Mock: $patientsCompleted patients completed, current number: $mockCurrentNumber")
        }
        
        // 予約番号は固定（設定画面で変更可能）
        
        val clinicData = ClinicData(
            currentNumber = mockCurrentNumber,
            reservationNumber = mockReservationNumber,
            lastUpdateTime = currentTime,
            hasReservation = true
        )
        
        Log.d("ClinicRepository", "Generated mock data: current=$mockCurrentNumber, reservation=$mockReservationNumber, hasReservation=$mockHasReservation")
        return Result.success(clinicData)
    }
    
    // 開発者モード用の設定関数
    fun setMockReservationNumber(number: Int) {
        mockReservationNumber = number
        Log.d("ClinicRepository", "Mock reservation number set to: $number")
    }
    
    fun setMockCurrentNumber(number: Int) {
        mockCurrentNumber = number
        Log.d("ClinicRepository", "Mock current number set to: $number")
    }
    
    fun setMockHasReservation(hasReservation: Boolean) {
        mockHasReservation = hasReservation
        Log.d("ClinicRepository", "Mock hasReservation set to: $hasReservation")
    }

    private fun parseConsultationData(doc: Document): ClinicData {
        var currentNumber = 0
        var reservationNumber = 0

        // Log the entire document text for debugging
        Log.d("ClinicRepository", "Document text: ${doc.text()}")

        // Parse current consultation number
        // Look for text containing "現在" and "番の方まで診察中"
        val currentNumberElements = doc.select("*:contains(現在), *:contains(番の方まで診察中)")
        for (element in currentNumberElements) {
            val text = element.text()
            Log.d("ClinicRepository", "Checking element for current number: $text")
            val regex = Regex("現在(\\d+)番の方まで診察中")
            val match = regex.find(text)
            if (match != null) {
                currentNumber = match.groupValues[1].toIntOrNull() ?: 0
                Log.d("ClinicRepository", "Found current number: $currentNumber")
                break
            }
        }

        // Parse reservation number
        // Look for text containing "あなたの予約番号" or similar
        val reservationElements = doc.select("*:contains(あなたの予約番号), *:contains(予約番号)")
        for (element in reservationElements) {
            val text = element.text()
            Log.d("ClinicRepository", "Checking element for reservation number: $text")
            val regex = Regex("(\\d+)番")
            val match = regex.find(text)
            if (match != null) {
                reservationNumber = match.groupValues[1].toIntOrNull() ?: 0
                Log.d("ClinicRepository", "Found reservation number: $reservationNumber")
                break
            }
        }

        // If we couldn't find the numbers, try alternative selectors
        if (currentNumber == 0) {
            // Try to find any number that might be the current consultation number
            val allText = doc.text()
            val currentRegex = Regex("現在.*?(\\d+).*?番.*?診察")
            val currentMatch = currentRegex.find(allText)
            if (currentMatch != null) {
                currentNumber = currentMatch.groupValues[1].toIntOrNull() ?: 0
                Log.d("ClinicRepository", "Found current number (alternative): $currentNumber")
            }
        }

        if (reservationNumber == 0) {
            // Try to find any number that might be the reservation number
            val allText = doc.text()
            val reservationRegex = Regex("予約.*?(\\d+).*?番")
            val reservationMatch = reservationRegex.find(allText)
            if (reservationMatch != null) {
                reservationNumber = reservationMatch.groupValues[1].toIntOrNull() ?: 0
                Log.d("ClinicRepository", "Found reservation number (alternative): $reservationNumber")
            }
        }

        // If still no numbers found, try more generic patterns
        if (currentNumber == 0) {
            val allText = doc.text()
            val genericCurrentRegex = Regex("(\\d+)番.*?診察")
            val genericCurrentMatch = genericCurrentRegex.find(allText)
            if (genericCurrentMatch != null) {
                currentNumber = genericCurrentMatch.groupValues[1].toIntOrNull() ?: 0
                Log.d("ClinicRepository", "Found current number (generic): $currentNumber")
            }
        }

        if (reservationNumber == 0) {
            val allText = doc.text()
            val genericReservationRegex = Regex("(\\d+)番.*?予約")
            val genericReservationMatch = genericReservationRegex.find(allText)
            if (genericReservationMatch != null) {
                reservationNumber = genericReservationMatch.groupValues[1].toIntOrNull() ?: 0
                Log.d("ClinicRepository", "Found reservation number (generic): $reservationNumber")
            }
        }

        return ClinicData(
            currentNumber = currentNumber,
            reservationNumber = reservationNumber,
            lastUpdateTime = System.currentTimeMillis(),
            hasReservation = true
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