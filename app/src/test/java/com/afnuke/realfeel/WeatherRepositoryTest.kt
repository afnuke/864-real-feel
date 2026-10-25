package com.afnuke.realfeel

import com.afnuke.realfeel.data.CoatAdviceType
import com.afnuke.realfeel.data.VerdictType
import com.afnuke.realfeel.data.WeatherRepository
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.ZonedDateTime

@RunWith(RobolectricTestRunner::class)
class WeatherRepositoryTest {

    private val repository = WeatherRepository()

    private fun createMockJson(
        actualTemp: Double,
        realFeel: Double,
        sunsetIso: String? = null,
        hourlyTemps: List<Double>? = null,
        hourlyProbs: List<Int>? = null
    ): JSONObject {
        val root = JSONObject()

        val current = JSONObject().apply {
            put("temperature_2m", actualTemp)
            put("apparent_temperature", realFeel)
            put("relative_humidity_2m", 65.0)
            put("wind_speed_10m", 8.0)
            put("weather_code", 0)
            put("is_day", 1)
        }
        root.put("current", current)

        val hourly = JSONObject()
        val times = JSONArray()
        val apparentTemps = JSONArray()
        val probs = JSONArray()

        val now = ZonedDateTime.now(WeatherRepository.GREENVILLE_ZONE)
        for (h in 0..24) {
            val hourTime = now.plusHours(h.toLong())
            times.put(hourTime.toLocalDateTime().toString())
            val temp = hourlyTemps?.getOrNull(h) ?: realFeel
            apparentTemps.put(temp)
            val prob = hourlyProbs?.getOrNull(h) ?: 0
            probs.put(prob)
        }
        hourly.put("time", times)
        hourly.put("apparent_temperature", apparentTemps)
        hourly.put("precipitation_probability", probs)
        root.put("hourly", hourly)

        val daily = JSONObject()
        val sunsets = JSONArray().apply {
            put(sunsetIso ?: (now.toLocalDate().toString() + "T18:30"))
        }
        daily.put("sunset", sunsets)
        root.put("daily", daily)

        return root
    }

    @Test
    fun testHeatLineWhen95OrAbove() {
        // Real feel 95°F -> Dangerously hot
        val hotJson = createMockJson(actualTemp = 90.0, realFeel = 96.0)
        val hotData = repository.parseWeatherJson(hotJson, System.currentTimeMillis())
        assertNotNull(hotData.heatLine)
        assertEquals("Dangerously hot — shade, water, and breaks.", hotData.heatLine)

        // Real feel 94°F -> null (hidden)
        val normalJson = createMockJson(actualTemp = 90.0, realFeel = 94.0)
        val normalData = repository.parseWeatherJson(normalJson, System.currentTimeMillis())
        assertNull(normalData.heatLine)
    }

    @Test
    fun testTonightLineAndSunsetAnchoredWindow() {
        val now = ZonedDateTime.now(WeatherRepository.GREENVILLE_ZONE)
        val sunsetIso = now.toLocalDate().toString() + "T19:00"

        // Hourly: before sunset is 65°, after sunset drops to 48°, tomorrow 8 AM goes to 60°
        // h=0 is now (assume afternoon, e.g. before sunset)
        val json = createMockJson(
            actualTemp = 68.0,
            realFeel = 65.0,
            sunsetIso = sunsetIso,
            hourlyTemps = List(25) { h -> if (h in 5..14) 48.0 else 65.0 }
        )
        val data = repository.parseWeatherJson(json, System.currentTimeMillis())
        assertEquals("Tonight after dark: 48°", data.tonightLine)
        assertEquals(48, data.tonightLow)
    }

    @Test
    fun testCoatTonightHeadlineExactWording() {
        // Warm now (68°), but drops below 55° tonight (49°)
        val json = createMockJson(
            actualTemp = 70.0,
            realFeel = 68.0,
            hourlyTemps = List(25) { h -> if (h in 4..12) 49.0 else 68.0 }
        )
        val data = repository.parseWeatherJson(json, System.currentTimeMillis())
        assertEquals(CoatAdviceType.COAT_TONIGHT, data.coatAdvice.type)
        assertEquals("Coat weather after dark.", data.coatAdvice.headline)
    }

    @Test
    fun testRainLineFiftyPercentThreshold() {
        // Probability 40% -> Dry all day
        val dryJson = createMockJson(
            actualTemp = 65.0,
            realFeel = 65.0,
            hourlyProbs = List(25) { 40 }
        )
        val dryData = repository.parseWeatherJson(dryJson, System.currentTimeMillis())
        assertEquals("Dry all day.", dryData.rainLine)

        // Probability 50% or above -> Rain likely after [time] — umbrella weather.
        val rainJson = createMockJson(
            actualTemp = 65.0,
            realFeel = 65.0,
            hourlyProbs = List(25) { h -> if (h == 3) 55 else 10 }
        )
        val rainData = repository.parseWeatherJson(rainJson, System.currentTimeMillis())
        assertTrue(rainData.rainLine.startsWith("Rain likely after "))
        assertTrue(rainData.rainLine.endsWith(" — umbrella weather."))
    }

    @Test
    fun testVerdictLineCalculations() {
        val coolerJson = createMockJson(actualTemp = 64.0, realFeel = 57.0)
        val coolerData = repository.parseWeatherJson(coolerJson, System.currentTimeMillis())
        assertEquals(VerdictType.COOLER, coolerData.verdictType)
        assertEquals("Feels cooler than the thermometer says.", coolerData.verdict)

        val warmerJson = createMockJson(actualTemp = 60.0, realFeel = 66.0)
        val warmerData = repository.parseWeatherJson(warmerJson, System.currentTimeMillis())
        assertEquals(VerdictType.WARMER, warmerData.verdictType)
        assertEquals("Feels warmer than the thermometer says.", warmerData.verdict)

        val sameJson = createMockJson(actualTemp = 62.0, realFeel = 62.4)
        val sameData = repository.parseWeatherJson(sameJson, System.currentTimeMillis())
        assertEquals(VerdictType.SAME, sameData.verdictType)
        assertEquals("Feels just like it reads.", sameData.verdict)
    }
}
