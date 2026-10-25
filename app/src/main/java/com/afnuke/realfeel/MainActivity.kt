package com.afnuke.realfeel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.afnuke.realfeel.audio.WeatherSynthPlayer
import com.afnuke.realfeel.ui.RealFeelScreen
import com.afnuke.realfeel.ui.RealFeelViewModel
import com.afnuke.realfeel.ui.theme.RealFeelTheme

class MainActivity : ComponentActivity() {

    private val viewModel: RealFeelViewModel by viewModels()
    private lateinit var synthPlayer: WeatherSynthPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        synthPlayer = WeatherSynthPlayer(this)

        setContent {
            RealFeelTheme {
                val uiState by viewModel.uiState.collectAsState()
                val isMusicMuted by synthPlayer.isMuted.collectAsState()

                RealFeelScreen(
                    uiState = uiState,
                    isMusicMuted = isMusicMuted,
                    onToggleMusic = { synthPlayer.toggleMute() },
                    onRefresh = { viewModel.refresh() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        synthPlayer.onResume()
    }

    override fun onPause() {
        super.onPause()
        synthPlayer.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        synthPlayer.release()
    }
}
