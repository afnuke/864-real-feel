package com.afnuke.realfeel.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.NightlightRound
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.afnuke.realfeel.data.WeatherData
import com.afnuke.realfeel.ui.theme.DarkBackground
import com.afnuke.realfeel.ui.theme.DarkSurface
import com.afnuke.realfeel.ui.theme.DarkSurfaceVariant
import com.afnuke.realfeel.ui.theme.TextMuted
import com.afnuke.realfeel.ui.theme.TextPrimary
import com.afnuke.realfeel.ui.theme.TextSecondary
import com.afnuke.realfeel.ui.theme.WarmAmber

@Composable
fun RealFeelScreen(
    uiState: RealFeelUiState,
    isMusicMuted: Boolean,
    onToggleMusic: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("real_feel_scaffold"),
        containerColor = DarkBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            when (uiState) {
                is RealFeelUiState.Loading -> {
                    LoadingView(
                        isMusicMuted = isMusicMuted,
                        onToggleMusic = onToggleMusic
                    )
                }
                is RealFeelUiState.Error -> {
                    ErrorView(
                        message = uiState.message,
                        isMusicMuted = isMusicMuted,
                        onToggleMusic = onToggleMusic,
                        onRetry = onRefresh
                    )
                }
                is RealFeelUiState.Success -> {
                    WeatherContentView(
                        weather = uiState.weather,
                        isRefreshing = uiState.isRefreshing,
                        isStale = uiState.isStale,
                        staleNotice = uiState.staleNotice,
                        isMusicMuted = isMusicMuted,
                        onToggleMusic = onToggleMusic,
                        onRefresh = onRefresh
                    )
                }
            }
        }
    }
}

@Composable
private fun TopLocationBar(
    isRefreshing: Boolean,
    isMusicMuted: Boolean,
    onToggleMusic: () -> Unit,
    onRefresh: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "spin_refresh")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "864 Real Feel",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp,
                    color = TextPrimary
                ),
                modifier = Modifier.testTag("app_title_text")
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.LocationOn,
                    contentDescription = null,
                    tint = WarmAmber,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Greenville County, SC",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onToggleMusic,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("mute_toggle_button")
            ) {
                Icon(
                    imageVector = if (isMusicMuted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                    contentDescription = if (isMusicMuted) "Unmute background music" else "Mute background music",
                    tint = if (isMusicMuted) TextMuted else WarmAmber,
                    modifier = Modifier.size(22.dp)
                )
            }

            IconButton(
                onClick = onRefresh,
                enabled = !isRefreshing,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("refresh_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = "Refresh weather",
                    tint = if (isRefreshing) TextMuted else TextSecondary,
                    modifier = Modifier
                        .size(22.dp)
                        .then(if (isRefreshing) Modifier.rotate(rotation) else Modifier)
                )
            }
        }
    }
}

@Composable
private fun WeatherContentView(
    weather: WeatherData,
    isRefreshing: Boolean,
    isStale: Boolean,
    staleNotice: String,
    isMusicMuted: Boolean,
    onToggleMusic: () -> Unit,
    onRefresh: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .widthIn(max = 600.dp)
            .padding(bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopLocationBar(
            isRefreshing = isRefreshing,
            isMusicMuted = isMusicMuted,
            onToggleMusic = onToggleMusic,
            onRefresh = onRefresh
        )

        if (isStale && staleNotice.isNotBlank()) {
            StaleNoticeBanner(
                notice = staleNotice,
                onRefresh = onRefresh
            )
        }

        // 1) Heat Line: Prominently shown if real feel >= 95°F, hidden otherwise
        if (weather.heatLine != null) {
            HeatLineCard(message = weather.heatLine)
            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 1. Actual temperature, huge: "64°"
        Text(
            text = "${weather.actualTemp}°",
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 86.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            ),
            modifier = Modifier.testTag("actual_temp_text")
        )

        // 2. Real feel, huge: "Feels like 57°"
        Text(
            text = "Feels like ${weather.realFeel}°",
            style = MaterialTheme.typography.displayMedium.copy(
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            ),
            modifier = Modifier
                .padding(top = 2.dp)
                .testTag("real_feel_text")
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Verdict line: real feel below temp = "Feels cooler than the thermometer says."
        // Above = "Feels warmer than the thermometer says." Within 1° = "Feels just like it reads."
        VerdictLine(verdict = weather.verdict)

        Spacer(modifier = Modifier.height(20.dp))

        // 4. COAT LINE (prominent — the heart of the app)
        // Consistent card style, no cyan/amber/green tints
        CoatLineCard(weather = weather)

        Spacer(modifier = Modifier.height(14.dp))

        // 5. Tonight line: "Tonight after dark: [X]°"
        TonightLineCard(tonightLine = weather.tonightLine)

        Spacer(modifier = Modifier.height(14.dp))

        // 6. Rain line: "Rain likely after [time] — umbrella weather." or "Dry all day."
        RainLineCard(rainLine = weather.rainLine)

        Spacer(modifier = Modifier.height(14.dp))

        // 7. Atmospheric strip (wind speed, humidity, condition)
        AtmosphericStrip(weather = weather)

        Spacer(modifier = Modifier.height(24.dp))

        // 8. Bottom line: "Updated [time] · Data: Open-Meteo"
        Text(
            text = "Updated ${weather.updatedTimeFormatted} · Data: Open-Meteo",
            style = MaterialTheme.typography.labelMedium.copy(
                color = TextMuted,
                fontSize = 12.sp
            ),
            modifier = Modifier.testTag("bottom_updated_line")
        )
    }
}

/**
 * 1) Dangerously hot warning: shown only when real feel >= 95°F
 */
@Composable
private fun HeatLineCard(message: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = DarkSurface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .testTag("heat_line_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Warning,
                contentDescription = null,
                tint = WarmAmber,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = "HEAT ADVISORY",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = WarmAmber,
                        letterSpacing = 1.2.sp
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )
                )
            }
        }
    }
}

@Composable
private fun VerdictLine(verdict: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = DarkSurfaceVariant.copy(alpha = 0.65f),
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .testTag("verdict_line_text")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Thermostat,
                contentDescription = null,
                tint = WarmAmber,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = verdict,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
            )
        }
    }
}

/**
 * 4) Coat Line: One consistent card style; amber is for accent text only.
 * No status color tints anywhere.
 */
@Composable
private fun CoatLineCard(weather: WeatherData) {
    val advice = weather.coatAdvice

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = DarkSurface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .testTag("coat_line_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "COAT CHECK",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = WarmAmber,
                    letterSpacing = 1.2.sp
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = advice.headline,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = TextPrimary
                )
            )

            if (advice.detail.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = advice.detail,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextSecondary,
                        fontSize = 15.sp
                    )
                )
            }
        }
    }
}

/**
 * 2) Tonight Line: standalone line/card reading "Tonight after dark: [X]°"
 */
@Composable
private fun TonightLineCard(tonightLine: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = DarkSurface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .testTag("tonight_line_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.NightlightRound,
                contentDescription = null,
                tint = WarmAmber,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = "TONIGHT",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = WarmAmber,
                        letterSpacing = 1.1.sp
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = tonightLine,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp,
                        color = TextPrimary
                    )
                )
            }
        }
    }
}

/**
 * 5) Rain Line: "Rain likely after [time] — umbrella weather." or "Dry all day."
 */
@Composable
private fun RainLineCard(rainLine: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = DarkSurface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .testTag("rain_line_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.WaterDrop,
                contentDescription = null,
                tint = WarmAmber,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = "RAIN OUTLOOK",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = WarmAmber,
                        letterSpacing = 1.1.sp
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = rainLine,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp,
                        color = TextPrimary
                    )
                )
            }
        }
    }
}

@Composable
private fun AtmosphericStrip(weather: WeatherData) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AtmosphericItem(
            icon = Icons.Rounded.Air,
            label = "Wind",
            value = "${weather.windSpeedMph} mph",
            modifier = Modifier.weight(1f)
        )
        AtmosphericItem(
            icon = Icons.Rounded.WaterDrop,
            label = "Humidity",
            value = "${weather.humidity}%",
            modifier = Modifier.weight(1f)
        )
        AtmosphericItem(
            icon = Icons.Rounded.Cloud,
            label = "Sky",
            value = weather.weatherCondition,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AtmosphericItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = DarkSurface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = WarmAmber,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                ),
                maxLines = 1
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 11.sp,
                    color = TextMuted
                )
            )
        }
    }
}

@Composable
private fun StaleNoticeBanner(
    notice: String,
    onRefresh: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = DarkSurface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clickable(onClick = onRefresh)
            .testTag("stale_notice_banner")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.WarningAmber,
                contentDescription = null,
                tint = WarmAmber,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = notice,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                )
            )
        }
    }
}

@Composable
private fun LoadingView(
    isMusicMuted: Boolean,
    onToggleMusic: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "864 Real Feel",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        color = TextPrimary
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Greenville County, SC",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                )
            }
            IconButton(
                onClick = onToggleMusic,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("mute_toggle_button")
            ) {
                Icon(
                    imageVector = if (isMusicMuted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                    contentDescription = if (isMusicMuted) "Unmute background music" else "Mute background music",
                    tint = if (isMusicMuted) TextMuted else WarmAmber
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = WarmAmber,
                strokeWidth = 3.dp,
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "Checking conditions...",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            )
        }

        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
private fun ErrorView(
    message: String,
    isMusicMuted: Boolean,
    onToggleMusic: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Rounded.ErrorOutline,
            contentDescription = null,
            tint = WarmAmber,
            modifier = Modifier.size(38.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Couldn't reach the weather — tap to retry",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = TextPrimary
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = TextSecondary,
                fontSize = 14.sp
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = WarmAmber,
                contentColor = DarkBackground
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .height(48.dp)
                .testTag("retry_button")
        ) {
            Icon(
                imageVector = Icons.Rounded.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Retry",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}
