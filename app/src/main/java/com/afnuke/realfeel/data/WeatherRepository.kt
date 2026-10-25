package com.afnuke.realfeel.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class WeatherRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()
) {

    suspend fun fetchWeather(): Result<WeatherData> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(API_URL)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("HTTP ${response.code}: ${response.message}")
                    )
                }
                val bodyString = response.body?.string()
                    ?: return@withContext Result.failure(Exception("Empty weather response"))

                val json = JSONObject(bodyString)
                val data = parseWeatherJson(json, System.currentTimeMillis())
                Result.success(data)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun parseWeatherJson(json: JSONObject, fetchTimeMs: Long): WeatherData {
        val currentObj = json.getJSONObject("current")
        val hourlyObj = json.optJSONObject("hourly")
        val dailyObj = json.optJSONObject("daily")

        val actualTemp = currentObj.getDouble("temperature_2m").roundToInt()
        val realFeel = currentObj.getDouble("apparent_temperature").roundToInt()
        val humidity = currentObj.optDouble("relative_humidity_2m", 0.0).roundToInt()
        val windSpeedMph = currentObj.optDouble("wind_speed_10m", 0.0).roundToInt()
        val weatherCode = currentObj.optInt("weather_code", 0)
        val isDay = currentObj.optInt("is_day", 1) == 1

        val now = ZonedDateTime.ofInstant(Instant.ofEpochMilli(fetchTimeMs), GREENVILLE_ZONE)

        // 1. Heat Line: if real feel is 95°F or above, show prominently
        val heatLine = if (realFeel >= 95) {
            "Dangerously hot — shade, water, and breaks."
        } else {
            null
        }

        // 2. Verdict line: real feel below temp = "Feels cooler than the thermometer says."
        // Above = "Feels warmer than the thermometer says."
        // Within 1° = "Feels just like it reads."
        val verdictType: VerdictType
        val verdict: String
        val diff = realFeel - actualTemp
        when {
            diff < -1 -> {
                verdictType = VerdictType.COOLER
                verdict = "Feels cooler than the thermometer says."
            }
            diff > 1 -> {
                verdictType = VerdictType.WARMER
                verdict = "Feels warmer than the thermometer says."
            }
            else -> {
                verdictType = VerdictType.SAME
                verdict = "Feels just like it reads."
            }
        }

        // 4. Tonight's low window: from today's SUNSET to 7:00 AM tomorrow
        val tonightLow = extractTonightLowRealFeel(hourlyObj, dailyObj, now)
        val tonightLine = if (tonightLow != null) {
            "Tonight after dark: $tonightLow°"
        } else {
            "Tonight after dark: --°"
        }

        // 3. Coat Advice: Below 55 is coat weather, judged by REAL FEEL
        // Coat-tonight headline must read exactly: "Coat weather after dark."
        val coatAdvice: CoatAdvice = when {
            realFeel < 55 -> {
                CoatAdvice(
                    type = CoatAdviceType.COAT_NOW,
                    headline = "Coat weather.",
                    detail = "Current real feel is $realFeel°"
                )
            }
            tonightLow != null && tonightLow < 55 -> {
                CoatAdvice(
                    type = CoatAdviceType.COAT_TONIGHT,
                    headline = "Coat weather after dark.",
                    detail = "Drops to $tonightLow° tonight"
                )
            }
            else -> {
                val detailStr = if (tonightLow != null) "Low of $tonightLow° tonight" else "Mild conditions ahead"
                CoatAdvice(
                    type = CoatAdviceType.NO_COAT,
                    headline = "No coat needed.",
                    detail = detailStr
                )
            }
        }

        // 5. Rain line: trigger only at 50% probability or higher
        // Exact wording — likely: "Rain likely after [time] — umbrella weather."
        // Not likely at all: "Dry all day."
        val (rainLine, isRainLikely) = evaluateRainLine(hourlyObj, weatherCode, now)

        val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
        val updatedFormatted = now.format(timeFormatter)

        val condition = wmoCodeToCondition(weatherCode)

        return WeatherData(
            actualTemp = actualTemp,
            realFeel = realFeel,
            heatLine = heatLine,
            verdict = verdict,
            verdictType = verdictType,
            coatAdvice = coatAdvice,
            tonightLine = tonightLine,
            tonightLow = tonightLow,
            rainLine = rainLine,
            isRainLikely = isRainLikely,
            humidity = humidity,
            windSpeedMph = windSpeedMph,
            weatherCondition = condition,
            weatherCode = weatherCode,
            isDay = isDay,
            fetchedAtEpochMs = fetchTimeMs,
            updatedTimeFormatted = updatedFormatted
        )
    }

    /**
     * Computes the lowest real feel between today's SUNSET and 7:00 AM tomorrow.
     */
    fun extractTonightLowRealFeel(
        hourlyObj: JSONObject?,
        dailyObj: JSONObject?,
        now: ZonedDateTime
    ): Int? {
        if (hourlyObj == null) return null
        val times = hourlyObj.optJSONArray("time") ?: return null
        val apparentTemps = hourlyObj.optJSONArray("apparent_temperature") ?: return null

        // Determine sunset time
        val sunsetTime = resolveSunsetTime(dailyObj, now)

        // Window end is 7:00 AM tomorrow (or 7:00 AM today if currently before 7 AM)
        val (windowStart, windowEnd) = if (now.hour < 7) {
            val prevSunset = sunsetTime.minusDays(1)
            val endToday7Am = now.toLocalDate().atTime(7, 0).atZone(GREENVILLE_ZONE)
            Pair(prevSunset, endToday7Am)
        } else {
            val endTomorrow7Am = now.toLocalDate().plusDays(1).atTime(7, 0).atZone(GREENVILLE_ZONE)
            Pair(sunsetTime, endTomorrow7Am)
        }

        var lowest: Double? = null

        for (i in 0 until times.length()) {
            val timeStr = times.optString(i)
            if (timeStr.isBlank()) continue
            try {
                val parsed = LocalDateTime.parse(timeStr).atZone(GREENVILLE_ZONE)
                // Window from sunset to 7:00 AM tomorrow
                if (!parsed.isBefore(windowStart) && !parsed.isAfter(windowEnd)) {
                    val temp = apparentTemps.optDouble(i)
                    if (!temp.isNaN()) {
                        if (lowest == null || temp < lowest) {
                            lowest = temp
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        return lowest?.roundToInt()
    }

    private fun resolveSunsetTime(dailyObj: JSONObject?, now: ZonedDateTime): ZonedDateTime {
        val defaultSunset = now.toLocalDate().atTime(18, 30).atZone(GREENVILLE_ZONE)
        if (dailyObj == null) return defaultSunset
        val sunsets = dailyObj.optJSONArray("sunset") ?: return defaultSunset
        if (sunsets.length() == 0) return defaultSunset
        val sunsetIso = sunsets.optString(0)
        if (sunsetIso.isBlank()) return defaultSunset
        return try {
            LocalDateTime.parse(sunsetIso).atZone(GREENVILLE_ZONE)
        } catch (_: Exception) {
            defaultSunset
        }
    }

    /**
     * Rain line: trigger only at 50% probability or higher.
     * Likely: "Rain likely after [time] — umbrella weather."
     * Not likely at all: "Dry all day."
     */
    fun evaluateRainLine(
        hourlyObj: JSONObject?,
        currentWeatherCode: Int,
        now: ZonedDateTime
    ): Pair<String, Boolean> {
        if (hourlyObj != null) {
            val times = hourlyObj.optJSONArray("time")
            val probs = hourlyObj.optJSONArray("precipitation_probability")
            if (times != null && probs != null) {
                val hourFormatter = DateTimeFormatter.ofPattern("h a", Locale.US)
                val lookaheadEnd = now.plusHours(24)

                for (i in 0 until times.length()) {
                    val timeStr = times.optString(i)
                    if (timeStr.isBlank()) continue
                    try {
                        val parsed = LocalDateTime.parse(timeStr).atZone(GREENVILLE_ZONE)
                        // Look at future hours within the next 24 hours
                        if (!parsed.isBefore(now.minusMinutes(30)) && !parsed.isAfter(lookaheadEnd)) {
                            val prob = probs.optInt(i, 0)
                            if (prob >= 50) {
                                val timeLabel = parsed.format(hourFormatter)
                                return Pair("Rain likely after $timeLabel — umbrella weather.", true)
                            }
                        }
                    } catch (_: Exception) {}
                }
            }
        }

        // If rain currently falling, fallback to likely
        if (isRainingWmoCode(currentWeatherCode)) {
            val timeLabel = now.format(DateTimeFormatter.ofPattern("h a", Locale.US))
            return Pair("Rain likely after $timeLabel — umbrella weather.", true)
        }

        return Pair("Dry all day.", false)
    }

    private fun isRainingWmoCode(code: Int): Boolean {
        return code in 51..67 || code in 80..82 || code in 95..99
    }

    fun wmoCodeToCondition(code: Int): String {
        return when (code) {
            0 -> "Clear Sky"
            1 -> "Mainly Clear"
            2 -> "Partly Cloudy"
            3 -> "Overcast"
            45, 48 -> "Foggy"
            51, 53, 55 -> "Drizzle"
            56, 57 -> "Freezing Drizzle"
            61, 63 -> "Rain"
            65 -> "Heavy Rain"
            66, 67 -> "Freezing Rain"
            71, 73, 75, 77 -> "Snow"
            80, 81, 82 -> "Rain Showers"
            85, 86 -> "Snow Showers"
            95, 96, 99 -> "Thunderstorm"
            else -> "Fair"
        }
    }

    companion object {
        const val GREENVILLE_LATITUDE = 34.8526
        const val GREENVILLE_LONGITUDE = -82.3940
        const val TIMEZONE_ID = "America/New_York"
        val GREENVILLE_ZONE: ZoneId = ZoneId.of(TIMEZONE_ID)

        const val API_URL =
            "https://api.open-meteo.com/v1/forecast?latitude=34.8526&longitude=-82.394&current=temperature_2m,apparent_temperature,relative_humidity_2m,wind_speed_10m,weather_code,is_day&hourly=apparent_temperature,precipitation_probability&daily=sunset&temperature_unit=fahrenheit&wind_speed_unit=mph&precipitation_unit=inch&timezone=America%2FNew_York"
    }
}
