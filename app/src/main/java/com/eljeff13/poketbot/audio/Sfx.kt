package com.eljeff13.poketbot.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

enum class SfxCue { TAP, HIT, CRIT, GUARD, SPECIAL, HEAL, WIN, LOSE, UNLOCK }

/**
 * Tiny PCM synthesiser. Generating the waveforms on the fly keeps the APK free
 * of audio assets — and of their licensing — while still giving the game some
 * arcade feedback. Every failure is swallowed: sound is never worth a crash.
 */
class Sfx {

    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "poketbot-sfx").apply { isDaemon = true }
    }

    @Volatile
    var enabled: Boolean = true

    fun play(cue: SfxCue) {
        if (!enabled) return
        try {
            executor.execute { render(cue) }
        } catch (e: RejectedExecutionException) {
            Log.d(TAG, "sfx rejected", e)
        }
    }

    fun release() {
        executor.shutdownNow()
    }

    private fun render(cue: SfxCue) {
        val samples = when (cue) {
            SfxCue.TAP -> tone(660f, 660f, 55, volume = 0.25f)
            SfxCue.GUARD -> tone(300f, 220f, 120, volume = 0.3f)
            SfxCue.HIT -> tone(320f, 110f, 150, volume = 0.42f, noise = 0.35f)
            SfxCue.CRIT -> tone(900f, 1400f, 90, volume = 0.45f) + tone(400f, 140f, 140, volume = 0.4f, noise = 0.3f)
            SfxCue.SPECIAL -> tone(430f, 980f, 220, volume = 0.4f)
            SfxCue.HEAL -> tone(520f, 790f, 190, volume = 0.32f)
            SfxCue.UNLOCK -> tone(523f, 523f, 90, 0.32f) + tone(659f, 659f, 90, 0.32f) + tone(880f, 880f, 160, 0.34f)
            SfxCue.WIN -> tone(523f, 523f, 100, 0.34f) + tone(659f, 659f, 100, 0.34f) +
                tone(784f, 784f, 100, 0.34f) + tone(1047f, 1047f, 240, 0.36f)
            SfxCue.LOSE -> tone(392f, 392f, 140, 0.32f) + tone(311f, 311f, 160, 0.32f) + tone(233f, 180f, 320, 0.32f)
        }
        writeToTrack(samples)
    }

    /** Builds one enveloped tone, optionally sweeping in pitch and mixed with noise. */
    private fun tone(
        startHz: Float,
        endHz: Float,
        durationMs: Int,
        volume: Float,
        noise: Float = 0f,
    ): ShortArray {
        val count = (SAMPLE_RATE * durationMs / 1000f).toInt().coerceAtLeast(1)
        val out = ShortArray(count)
        val attack = (count * 0.06f).toInt().coerceAtLeast(1)
        val release = (count * 0.35f).toInt().coerceAtLeast(1)
        var phase = 0.0

        for (i in 0 until count) {
            val progress = i.toFloat() / count
            val hz = startHz + (endHz - startHz) * progress
            phase += 2.0 * PI * hz / SAMPLE_RATE

            // Square-ish wave: warmer than a sine, cheaper than a wavetable.
            val wave = sin(phase).let { if (it >= 0) 0.7 * it + 0.3 else 0.7 * it - 0.3 }
            val noisy = if (noise > 0f) wave * (1 - noise) + (random.nextDouble() * 2 - 1) * noise else wave

            val envelope = when {
                i < attack -> i.toFloat() / attack
                i > count - release -> (count - i).toFloat() / release
                else -> 1f
            }

            out[i] = (noisy * envelope * volume * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        }
        return out
    }

    private operator fun ShortArray.plus(other: ShortArray): ShortArray {
        val merged = ShortArray(size + other.size)
        copyInto(merged)
        other.copyInto(merged, size)
        return merged
    }

    private fun writeToTrack(samples: ShortArray) {
        var track: AudioTrack? = null
        try {
            val minBuffer = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
            )
            if (minBuffer <= 0) return
            val bufferBytes = maxOf(minBuffer, samples.size * 2)

            track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                )
                .setBufferSizeInBytes(bufferBytes)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            track.play()
            // Blocking write: returns once every sample has been queued.
            track.write(samples, 0, samples.size)

            // Let the queued tail drain before tearing the track down.
            val tailMs = samples.size * 1000L / SAMPLE_RATE + 80L
            Thread.sleep(min(2_000L, tailMs))
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        } catch (e: Exception) {
            Log.d(TAG, "sfx playback failed", e)
        } finally {
            runCatching {
                track?.stop()
                track?.release()
            }
        }
    }

    private val random = java.util.Random()

    private companion object {
        const val TAG = "PoketbotSfx"
        const val SAMPLE_RATE = 22050
    }
}
