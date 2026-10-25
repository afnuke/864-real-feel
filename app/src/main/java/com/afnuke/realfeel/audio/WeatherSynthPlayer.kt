package com.afnuke.realfeel.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin

/**
 * Generates and plays gentle, slow, smooth elevator-style background music
 * reminiscent of The Weather Channel's "Local on the 8s".
 *
 * Generated 100% IN CODE with synthesized Rhodes/piano-like tones.
 * Uses AudioTrack in MODE_STATIC for zero continuous CPU overhead and seamless looping.
 */
class WeatherSynthPlayer(private val context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _isMuted = MutableStateFlow(prefs.getBoolean(KEY_MUTED, false))
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private var audioTrack: AudioTrack? = null
    private var isAppOnScreen = false
    private var isInitialized = false

    private val scope = CoroutineScope(Dispatchers.Default)

    init {
        scope.launch {
            initAudioTrack()
        }
    }

    private fun initAudioTrack() {
        try {
            val sampleRate = 22050
            val pcmData = generateWeatherMusicPcm(sampleRate)
            val bufferSize = pcmData.size * 2 // 16-bit PCM = 2 bytes per sample

            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            val format = AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .build()

            val track = AudioTrack(
                attributes,
                format,
                bufferSize,
                AudioTrack.MODE_STATIC,
                AudioManager.AUDIO_SESSION_ID_GENERATE
            )

            track.write(pcmData, 0, pcmData.size)
            track.setLoopPoints(0, pcmData.size, -1) // Loop indefinitely
            track.setVolume(VOLUME_LEVEL)

            synchronized(this) {
                audioTrack = track
                isInitialized = true
                updatePlaybackState()
            }
        } catch (_: Exception) {
            // AudioTrack creation error fallback
        }
    }

    fun onResume() {
        synchronized(this) {
            isAppOnScreen = true
            updatePlaybackState()
        }
    }

    fun onPause() {
        synchronized(this) {
            isAppOnScreen = false
            updatePlaybackState()
        }
    }

    fun toggleMute() {
        val newMuted = !_isMuted.value
        _isMuted.value = newMuted
        prefs.edit().putBoolean(KEY_MUTED, newMuted).apply()
        synchronized(this) {
            updatePlaybackState()
        }
    }

    private fun updatePlaybackState() {
        val track = audioTrack ?: return
        if (isAppOnScreen && !_isMuted.value) {
            try {
                if (track.playState != AudioTrack.PLAYSTATE_PLAYING) {
                    track.play()
                }
            } catch (_: Exception) {}
        } else {
            try {
                if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    track.pause()
                }
            } catch (_: Exception) {}
        }
    }

    fun release() {
        synchronized(this) {
            isAppOnScreen = false
            try {
                audioTrack?.stop()
                audioTrack?.release()
            } catch (_: Exception) {}
            audioTrack = null
            isInitialized = false
        }
    }

    /**
     * Synthesizes an 8-measure gentle, slow Rhodes-style electric piano chord progression
     * with mellow harmonics and soft chime accents (~72 BPM).
     */
    private fun generateWeatherMusicPcm(sampleRate: Int): ShortArray {
        val bpm = 70.0
        val beatDurationSec = 60.0 / bpm
        val measureDurationSec = beatDurationSec * 4.0
        val totalMeasures = 8
        val totalDurationSec = measureDurationSec * totalMeasures
        val totalSamples = (totalDurationSec * sampleRate).toInt()

        val mixBuffer = FloatArray(totalSamples)

        // Musical frequencies (Hz)
        val A2 = 110.00
        val C3 = 130.81
        val D3 = 146.83
        val E3 = 164.81
        val F3 = 174.61
        val G2 = 98.00
        val G3 = 196.00
        val A3 = 220.00
        val B3 = 246.94
        val C4 = 261.63
        val Cs4 = 277.18
        val D4 = 293.66
        val E4 = 329.63
        val F4 = 349.23
        val G4 = 392.00
        val A4 = 440.00
        val B4 = 493.88
        val C5 = 523.25
        val D5 = 587.33
        val E5 = 659.25
        val G5 = 783.99

        data class SynthNote(
            val freq: Double,
            val startBeat: Double,
            val durationBeats: Double,
            val velocity: Float
        )

        val notes = mutableListOf<SynthNote>()

        // Measure 1: Cmaj9 (warm, sunny morning vibe)
        // Bass
        notes.add(SynthNote(C3, 0.0, 3.8, 0.50f))
        // Chord pads
        notes.add(SynthNote(E4, 0.05, 3.6, 0.35f))
        notes.add(SynthNote(G4, 0.08, 3.6, 0.32f))
        notes.add(SynthNote(B4, 0.12, 3.5, 0.30f))
        notes.add(SynthNote(D5, 0.15, 3.4, 0.28f))
        // Gentle melodic chime
        notes.add(SynthNote(E5, 2.5, 1.8, 0.26f))

        // Measure 2: Am9 (reflective, peaceful)
        notes.add(SynthNote(A2, 4.0, 3.8, 0.50f))
        notes.add(SynthNote(C4, 4.05, 3.6, 0.35f))
        notes.add(SynthNote(E4, 4.08, 3.6, 0.32f))
        notes.add(SynthNote(G4, 4.12, 3.5, 0.30f))
        notes.add(SynthNote(B4, 4.15, 3.4, 0.28f))
        notes.add(SynthNote(C5, 6.0, 1.8, 0.25f))

        // Measure 3: Dm9 (smooth local forecast transition)
        notes.add(SynthNote(D3, 8.0, 3.8, 0.50f))
        notes.add(SynthNote(F4, 8.05, 3.6, 0.35f))
        notes.add(SynthNote(A4, 8.08, 3.6, 0.32f))
        notes.add(SynthNote(C5, 8.12, 3.5, 0.30f))
        notes.add(SynthNote(E5, 8.15, 3.4, 0.28f))
        notes.add(SynthNote(D5, 10.5, 1.8, 0.24f))

        // Measure 4: G13 / G9sus (classic Weather Channel resolving lift)
        notes.add(SynthNote(G2, 12.0, 3.8, 0.52f))
        notes.add(SynthNote(F4, 12.05, 3.6, 0.32f))
        notes.add(SynthNote(A4, 12.08, 3.6, 0.32f))
        notes.add(SynthNote(B4, 12.12, 3.5, 0.30f))
        notes.add(SynthNote(E5, 12.15, 3.4, 0.26f))
        notes.add(SynthNote(G5, 14.5, 1.8, 0.22f))

        // Measure 5: Em7 (gentle breeze)
        notes.add(SynthNote(E3, 16.0, 3.8, 0.50f))
        notes.add(SynthNote(G4, 16.05, 3.6, 0.34f))
        notes.add(SynthNote(B4, 16.08, 3.6, 0.32f))
        notes.add(SynthNote(D5, 16.12, 3.5, 0.30f))
        notes.add(SynthNote(E5, 18.0, 1.8, 0.25f))

        // Measure 6: A7b13 / A7 (mellow jazz color)
        notes.add(SynthNote(A2, 20.0, 3.8, 0.50f))
        notes.add(SynthNote(G4, 20.05, 3.6, 0.32f))
        notes.add(SynthNote(Cs4, 20.08, 3.6, 0.32f))
        notes.add(SynthNote(F4, 20.12, 3.5, 0.30f))
        notes.add(SynthNote(A4, 22.5, 1.8, 0.25f))

        // Measure 7: Dm9
        notes.add(SynthNote(D3, 24.0, 3.8, 0.50f))
        notes.add(SynthNote(F4, 24.05, 3.6, 0.35f))
        notes.add(SynthNote(A4, 24.08, 3.6, 0.32f))
        notes.add(SynthNote(C5, 24.12, 3.5, 0.30f))
        notes.add(SynthNote(E5, 26.0, 1.8, 0.26f))

        // Measure 8: G7sus4 -> G7 (gentle cadence back to C)
        notes.add(SynthNote(G2, 28.0, 3.8, 0.52f))
        notes.add(SynthNote(F4, 28.05, 3.6, 0.32f))
        notes.add(SynthNote(C5, 28.08, 1.8, 0.30f)) // sus4
        notes.add(SynthNote(B4, 30.0, 1.8, 0.28f))  // resolves to 3rd
        notes.add(SynthNote(D5, 30.5, 1.5, 0.22f))

        // Render notes into buffer with electric piano tone synthesis
        for (note in notes) {
            val startSample = (note.startBeat * beatDurationSec * sampleRate).toInt()
            val noteDurationSec = note.durationBeats * beatDurationSec
            val noteSamples = (noteDurationSec * sampleRate).toInt()
            val endSample = min(totalSamples, startSample + noteSamples)

            val freq = note.freq
            val vel = note.velocity

            for (i in startSample until endSample) {
                val t = (i - startSample).toDouble() / sampleRate
                // Attack: 15ms smooth ramp to prevent any transient clicking
                val attack = if (t < 0.015) (t / 0.015).toFloat() else 1.0f
                // Mellow exponential decay (Rhodes bell decay)
                val decay = exp(-t / 1.8).toFloat()
                val env = attack * decay * vel

                // Harmonics:
                // Fundamental sine
                val h1 = sin(2.0 * PI * freq * t).toFloat()
                // 2nd harmonic (warmth)
                val h2 = (0.28 * sin(4.0 * PI * freq * t) * exp(-t / 0.8)).toFloat()
                // 3rd harmonic (tine chime)
                val h3 = (0.09 * sin(6.0 * PI * freq * t) * exp(-t / 0.4)).toFloat()
                // 4th harmonic
                val h4 = (0.03 * sin(8.0 * PI * freq * t) * exp(-t / 0.2)).toFloat()

                mixBuffer[i] += (h1 + h2 + h3 + h4) * env
            }
        }

        // Apply a gentle 0.3s crossfade at the very end to seamlessly loop into sample 0
        val fadeSamples = (0.35 * sampleRate).toInt()
        for (i in 0 until fadeSamples) {
            val progress = i.toFloat() / fadeSamples
            val endIdx = totalSamples - fadeSamples + i
            if (endIdx in 0 until totalSamples) {
                // Crossfade tail into head
                mixBuffer[endIdx] = mixBuffer[endIdx] * (1.0f - progress) + mixBuffer[i] * progress
            }
        }

        // Find peak for normalization
        var maxVal = 0.001f
        for (sample in mixBuffer) {
            val abs = kotlin.math.abs(sample)
            if (abs > maxVal) {
                maxVal = abs
            }
        }

        // Convert to 16-bit PCM ShortArray with soft ceiling
        val pcm = ShortArray(totalSamples)
        val targetPeak = 14000.0f // Leave plenty of headroom for soft, calm listening
        val scale = targetPeak / maxVal

        for (i in 0 until totalSamples) {
            val scaled = mixBuffer[i] * scale
            val clamped = scaled.coerceIn(Short.MIN_VALUE.toFloat(), Short.MAX_VALUE.toFloat())
            pcm[i] = clamped.toInt().toShort()
        }

        return pcm
    }

    companion object {
        private const val PREFS_NAME = "real_feel_prefs"
        private const val KEY_MUTED = "is_music_muted"
        private const val VOLUME_LEVEL = 0.18f // Soft, ambient background level
    }
}
