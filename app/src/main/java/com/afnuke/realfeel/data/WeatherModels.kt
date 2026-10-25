package com.afnuke.realfeel.data

enum class VerdictType {
    COOLER,
    WARMER,
    SAME
}

enum class CoatAdviceType {
    COAT_NOW,
    COAT_TONIGHT,
    NO_COAT
}

data class CoatAdvice(
    val type: CoatAdviceType,
    val headline: String,
    val detail: String = ""
)

data class WeatherData(
    val actualTemp: Int,
    val realFeel: Int,
    val heatLine: String?,
    val verdict: String,
    val verdictType: VerdictType,
    val coatAdvice: CoatAdvice,
    val tonightLine: String,
    val tonightLow: Int?,
    val rainLine: String,
    val isRainLikely: Boolean,
    val humidity: Int,
    val windSpeedMph: Int,
    val weatherCondition: String,
    val weatherCode: Int,
    val isDay: Boolean,
    val fetchedAtEpochMs: Long,
    val updatedTimeFormatted: String
)
