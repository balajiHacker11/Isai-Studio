package com.example.core.player

import android.content.Context
import android.net.Uri
import android.util.LruCache
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import com.example.core.model.TrackType
import com.example.features.catalog.data.CatalogRepository
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tanh

@UnstableApi
class StudioDataSourceFactory(
    context: Context,
    private val catalogRepository: CatalogRepository
) : DataSource.Factory {
    private val defaultFactory = DefaultDataSource.Factory(context.applicationContext)

    override fun createDataSource(): DataSource {
        return StudioRoutingDataSource(
            delegate = defaultFactory.createDataSource(),
            catalogRepository = catalogRepository
        )
    }
}

@UnstableApi
private class StudioRoutingDataSource(
    private val delegate: DataSource,
    private val catalogRepository: CatalogRepository
) : BaseDataSource(/* isNetwork = */ false) {

    private var activeDelegate: DataSource? = null
    private var studioData: ByteArray? = null
    private var readPosition: Int = 0
    private var openedUri: Uri? = null

    override fun open(dataSpec: DataSpec): Long {
        val uri = dataSpec.uri
        openedUri = uri
        return if (uri.scheme == StudioAudioSynth.SCHEME) {
            transferInitializing(dataSpec)
            val trackId = uri.lastPathSegment?.toIntOrNull() ?: 1
            val wav = StudioAudioSynth.getOrRenderTrackWav(trackId, catalogRepository)
            studioData = wav
            val startPos = dataSpec.position.toInt().coerceIn(0, wav.size)
            readPosition = startPos
            transferStarted(dataSpec)
            (wav.size - startPos).toLong()
        } else {
            activeDelegate = delegate
            delegate.open(dataSpec)
        }
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        val delegateSource = activeDelegate
        if (delegateSource != null) {
            return delegateSource.read(buffer, offset, length)
        }
        if (length == 0) return 0
        val data = studioData ?: return C.RESULT_END_OF_INPUT
        val remaining = data.size - readPosition
        if (remaining <= 0) return C.RESULT_END_OF_INPUT
        val toRead = min(length, remaining)
        System.arraycopy(data, readPosition, buffer, offset, toRead)
        readPosition += toRead
        bytesTransferred(toRead)
        return toRead
    }

    override fun getUri(): Uri? = activeDelegate?.uri ?: openedUri

    override fun close() {
        val delegateSource = activeDelegate
        if (delegateSource != null) {
            activeDelegate = null
            delegateSource.close()
        }
        if (studioData != null) {
            studioData = null
            transferEnded()
        }
        openedUri = null
    }
}

object StudioAudioSynth {
    const val SCHEME = "studio"
    private const val SAMPLE_RATE = 22050
    private const val DURATION_SECONDS = 32
    private const val TOTAL_SAMPLES = SAMPLE_RATE * DURATION_SECONDS
    private const val WAV_HEADER_SIZE = 44

    private val wavCache = LruCache<Int, ByteArray>(4)

    fun uriForTrack(trackId: Int): String = "$SCHEME://track/$trackId"

    fun getOrRenderTrackWav(trackId: Int, catalogRepository: CatalogRepository): ByteArray {
        wavCache.get(trackId)?.let { return it }
        val track = catalogRepository.trackById(trackId)
        val rendered = renderWavBytes(
            seedId = trackId,
            trackType = track?.trackType ?: TrackType.SONG
        )
        wavCache.put(trackId, rendered)
        return rendered
    }

    private fun renderWavBytes(seedId: Int, trackType: TrackType): ByteArray {
        val dataByteSize = TOTAL_SAMPLES * 2
        val output = ByteArray(WAV_HEADER_SIZE + dataByteSize)
        writeWavHeader(output, dataByteSize)

        val rng = java.util.Random((seedId.toLong() * 2654435761L) xor trackType.ordinal.toLong())

        val bpm = when (trackType) {
            TrackType.SONG -> 108 + (seedId % 16)
            TrackType.THEME -> 126 + (seedId % 10)
            TrackType.BGM -> 88 + (seedId % 14)
            TrackType.VERSION -> 82 + (seedId % 10)
        }
        val stepSeconds = (60.0 / bpm) / 2.0 // 8th-note steps
        val stepSamples = (stepSeconds * SAMPLE_RATE).toInt().coerceAtLeast(1)

        // Root MIDI notes around C3..A3 (48..57)
        val rootMidi = 48 + ((seedId * 5) % 10)
        val scaleIntervals = when (trackType) {
            TrackType.SONG -> intArrayOf(0, 2, 4, 7, 9, 12, 14, 16) // Major pentatonic + octaves
            TrackType.THEME -> intArrayOf(0, 2, 3, 5, 7, 8, 10, 12) // Natural minor heroic
            TrackType.BGM -> intArrayOf(0, 1, 3, 5, 7, 8, 11, 12) // Cinematic dark
            TrackType.VERSION -> intArrayOf(0, 2, 4, 5, 7, 9, 11, 12) // Warm major
        }
        val progression = when (seedId % 4) {
            0 -> intArrayOf(0, 4, 5, 3)
            1 -> intArrayOf(0, 5, 3, 4)
            2 -> intArrayOf(0, 3, 4, 4)
            else -> intArrayOf(5, 3, 0, 4)
        }

        // Precompute 16-step melody motif
        val motifOffsets = IntArray(16) { step ->
            if (step % 2 == 0 || rng.nextFloat() > 0.35f) {
                scaleIntervals[rng.nextInt(scaleIntervals.size)]
            } else {
                -1
            }
        }

        val twoPi = 2.0 * PI
        val invSampleRate = 1.0 / SAMPLE_RATE
        var noiseState = (seedId * 1103515245 + 12345).toInt()
        var prevNoise = 0.0

        val fadeInSamples = (SAMPLE_RATE * 0.04).toInt()
        val fadeOutStart = TOTAL_SAMPLES - (SAMPLE_RATE * 0.6).toInt()

        for (i in 0 until TOTAL_SAMPLES) {
            val t = i * invSampleRate
            val stepIndex = i / stepSamples
            val stepOffset = i % stepSamples
            val stepTime = stepOffset * invSampleRate

            val barIndex = (stepIndex / 8) % 4
            val chordDegree = progression[barIndex]
            val chordBaseMidi = rootMidi + scaleIntervals[chordDegree % scaleIntervals.size]

            // 1. Warm Chord Pad
            val f1 = midiToHz(chordBaseMidi + 12)
            val f2 = midiToHz(chordBaseMidi + 16)
            val f3 = midiToHz(chordBaseMidi + 19)
            val padSwell = 0.65 + 0.35 * sin(twoPi * t / (stepSeconds * 8.0))
            val pad = (
                sin(twoPi * f1 * t) +
                    0.8 * sin(twoPi * f2 * t) +
                    0.7 * sin(twoPi * f3 * t) +
                    0.2 * sin(twoPi * f1 * 2.0 * t)
                ) * 0.08 * padSwell

            // 2. Sub-Bass groove
            val stepInBar = stepIndex % 8
            val bassActive = when (trackType) {
                TrackType.THEME, TrackType.BGM -> true
                else -> stepInBar == 0 || stepInBar == 3 || stepInBar == 4 || stepInBar == 6
            }
            val bassEnv = if (bassActive) exp(-stepTime * 5.2) else 0.0
            val bassFreq = midiToHz(chordBaseMidi - 12)
            val bass = (sin(twoPi * bassFreq * t) + 0.25 * sin(twoPi * bassFreq * 2.0 * t)) * 0.26 * bassEnv

            // 3. Lead Pluck Melody
            val motifNote = motifOffsets[stepIndex % 16]
            val lead = if (motifNote >= 0) {
                val leadFreq = midiToHz(rootMidi + 24 + motifNote)
                val leadEnv = exp(-stepTime * if (trackType == TrackType.VERSION) 4.0 else 6.5)
                val fundamental = sin(twoPi * leadFreq * t)
                val overtone = 0.32 * sin(twoPi * leadFreq * 2.0 * t) * exp(-stepTime * 12.0)
                (fundamental + overtone) * 0.22 * leadEnv
            } else {
                0.0
            }

            // 4. Percussion
            noiseState = noiseState * 1664525 + 1013904223
            val rawWhite = ((noiseState ushr 16) and 0x7FFF) / 16384.0 - 1.0
            val highPassedNoise = rawWhite - prevNoise
            prevNoise = rawWhite

            val hasKick = (stepInBar == 0 || stepInBar == 4 || (trackType == TrackType.THEME && stepInBar == 6))
            val kick = if (hasKick && stepTime < 0.16) {
                val kickFreq = 46.0 + 95.0 * exp(-stepTime * 34.0)
                sin(twoPi * kickFreq * stepTime) * exp(-stepTime * 18.0) * 0.42
            } else 0.0

            val hasSnare = (stepInBar == 2 || stepInBar == 6) && trackType != TrackType.VERSION
            val snare = if (hasSnare && stepTime < 0.11) {
                (sin(twoPi * 185.0 * stepTime) * 0.35 + rawWhite * 0.65) * exp(-stepTime * 32.0) * 0.18
            } else 0.0

            val hiHat = if (stepTime < 0.035) {
                highPassedNoise * exp(-stepTime * 95.0) * if (trackType == TrackType.VERSION) 0.04 else 0.08
            } else 0.0

            var mixed = pad + bass + lead + kick + snare + hiHat
            if (i < fadeInSamples) {
                mixed *= (i.toDouble() / fadeInSamples)
            } else if (i > fadeOutStart) {
                val remainingRatio = ((TOTAL_SAMPLES - i).toDouble() / (TOTAL_SAMPLES - fadeOutStart)).coerceIn(0.0, 1.0)
                mixed *= remainingRatio
            }

            val sampleShort = (tanh(mixed * 1.15) * 26500.0).toInt().coerceIn(-32767, 32767)
            val byteIdx = WAV_HEADER_SIZE + (i * 2)
            output[byteIdx] = (sampleShort and 0xFF).toByte()
            output[byteIdx + 1] = ((sampleShort ushr 8) and 0xFF).toByte()
        }

        return output
    }

    private fun midiToHz(midi: Int): Double = 440.0 * 2.0.pow((midi - 69) / 12.0)

    private fun writeWavHeader(out: ByteArray, dataSize: Int) {
        val totalDataLen = dataSize + 36
        val byteRate = SAMPLE_RATE * 2 // mono 16-bit

        // "RIFF"
        out[0] = 'R'.code.toByte(); out[1] = 'I'.code.toByte(); out[2] = 'F'.code.toByte(); out[3] = 'F'.code.toByte()
        writeIntLE(out, 4, totalDataLen)
        // "WAVE"
        out[8] = 'W'.code.toByte(); out[9] = 'A'.code.toByte(); out[10] = 'V'.code.toByte(); out[11] = 'E'.code.toByte()
        // "fmt "
        out[12] = 'f'.code.toByte(); out[13] = 'm'.code.toByte(); out[14] = 't'.code.toByte(); out[15] = ' '.code.toByte()
        writeIntLE(out, 16, 16) // PCM chunk size
        writeShortLE(out, 20, 1) // AudioFormat = 1 (PCM)
        writeShortLE(out, 22, 1) // NumChannels = 1 (Mono)
        writeIntLE(out, 24, SAMPLE_RATE)
        writeIntLE(out, 28, byteRate)
        writeShortLE(out, 32, 2) // BlockAlign
        writeShortLE(out, 34, 16) // BitsPerSample
        // "data"
        out[36] = 'd'.code.toByte(); out[37] = 'a'.code.toByte(); out[38] = 't'.code.toByte(); out[39] = 'a'.code.toByte()
        writeIntLE(out, 40, dataSize)
    }

    private fun writeIntLE(out: ByteArray, offset: Int, value: Int) {
        out[offset] = (value and 0xFF).toByte()
        out[offset + 1] = ((value ushr 8) and 0xFF).toByte()
        out[offset + 2] = ((value ushr 16) and 0xFF).toByte()
        out[offset + 3] = ((value ushr 24) and 0xFF).toByte()
    }

    private fun writeShortLE(out: ByteArray, offset: Int, value: Int) {
        out[offset] = (value and 0xFF).toByte()
        out[offset + 1] = ((value ushr 8) and 0xFF).toByte()
    }
}
