package edu.cuhk.csci3310.liftlog.ui.speech

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineWhisperModelConfig
import edu.cuhk.csci3310.liftlog.ui.speech.SpeechManager.Companion.CHUNK_SECONDS
import edu.cuhk.csci3310.liftlog.ui.speech.SpeechManager.Companion.COMPLETE_PHRASES
import edu.cuhk.csci3310.liftlog.ui.speech.SpeechManager.Companion.SAMPLE_RATE
import edu.cuhk.csci3310.liftlog.ui.speech.SpeechManager.Companion.WORD_TO_NUMBER
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Wraps sherpa-onnx's [OfflineRecognizer] (Whisper "tiny.en", int8) to provide continuous,
 * fully offline speech recognition throughout the spotter screen session.
 *
 * Usage:
 *  1. Create an instance (any thread).
 *  2. Call [startListening] to begin a recognition session.
 *  3. Call [stopListening] to pause (e.g., while rest timer is active).
 *  4. Call [release] when the screen is disposed to free all resources.
 *
 * Two kinds of voice events are detected:
 *  - **Rep count**: a number word or digit →  [onNumberDetected] is called with the integer.
 *    Numbers set (not increment) the rep counter, even if smaller than the current count.
 *  - **Complete set**: a completion phrase (e.g. "done", "complete", "next set") →
 *    [onCompleteSet] is called. Complete-set detection takes priority; if a completion
 *    phrase is found in a chunk the number callback is suppressed for that chunk.
 *
 * The recognizer records continuous 2-second audio chunks at 16 kHz and runs
 * Whisper inference on each chunk in a background coroutine.
 *
 * @param context          Application or Activity context (used for AssetManager).
 * @param onNumberDetected Called on the main thread with the detected rep number (1–99).
 * @param onCompleteSet    Called on the main thread when a completion phrase is detected.
 */
class SpeechManager(
    private val context: Context,
    private val onNumberDetected: (Int) -> Unit,
    private val onCompleteSet: () -> Unit = {},
) {
    companion object {
        private const val TAG = "SpeechManager"

        private const val SAMPLE_RATE = 16_000
        private const val CHUNK_SECONDS = 2
        private const val CHUNK_SAMPLES = SAMPLE_RATE * CHUNK_SECONDS

        /** asset-relative paths (inside app/src/main/assets/whisper/) */
        private const val ASSET_MODEL_DIR = "whisper"
        private const val ASSET_ENCODER_PATH = "$ASSET_MODEL_DIR/tiny.en-encoder.onnx"
        private const val ASSET_DECODER_PATH = "$ASSET_MODEL_DIR/tiny.en-decoder.onnx"
        private const val ASSSET_TOKENS_PATH = "$ASSET_MODEL_DIR/tiny.en-tokens.txt"

        val WORD_TO_NUMBER = mapOf(
            "one" to 1, "won" to 1,
            "two" to 2, "to" to 2, "too" to 2,
            "three" to 3, "free" to 3,
            "four" to 4, "for" to 4, "fore" to 4,
            "five" to 5,
            "six" to 6,
            "seven" to 7,
            "eight" to 8, "ate" to 8,
            "nine" to 9,
            "ten" to 10,
            "eleven" to 11,
            "twelve" to 12,
            "thirteen" to 13,
            "fourteen" to 14,
            "fifteen" to 15,
            "sixteen" to 16,
            "seventeen" to 17,
            "eighteen" to 18,
            "nineteen" to 19,
            "twenty" to 20,
            "twenty-one" to 21, "twenty one" to 21,
            "twenty-two" to 22, "twenty two" to 22,
            "twenty-three" to 23, "twenty three" to 23,
            "twenty-four" to 24, "twenty four" to 24,
            "twenty-five" to 25, "twenty five" to 25,
            "twenty-six" to 26, "twenty six" to 26,
            "twenty-seven" to 27, "twenty seven" to 27,
            "twenty-eight" to 28, "twenty eight" to 28,
            "twenty-nine" to 29, "twenty nine" to 29,
            "thirty" to 30,
        )

        val COMPLETE_PHRASES = setOf(
            "complete", "complete set", "complete the set",
            "done", "i'm done", "i am done", "all done",
            "finish", "finished",
            "next", "next set",
            "rack", "rack it", "rack it up",
        )

        /**
         * Returns true if the transcribed [text] contains a completion trigger.
         *
         * Matching is case-insensitive. Each phrase in [COMPLETE_PHRASES] is checked as a
         * whole-word / whole-phrase substring so that, e.g., "done" inside "donegal" does
         * not fire —  the phrase must be surrounded by word boundaries (start/end of string,
         * space, or punctuation).
         */
        fun parseCompleteSet(text: String): Boolean {
            val normalised = text.trim().lowercase()
                .replace(Regex("[.,!?]"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
            for (phrase in COMPLETE_PHRASES) {
                val pattern = Regex("(?<![a-z])${Regex.escape(phrase)}(?![a-z])")
                if (pattern.containsMatchIn(normalised)) return true
            }
            return false
        }

        /**
         * Parses a transcription string (case-insensitive) and returns the **last** number
         * found in the text, or null if none is detected.
         *
         * Scanning is left-to-right through whitespace-delimited tokens. At each position:
         *  1. A two-token compound phrase (hyohenated or not) is tried first (e.g., "twenty-one").
         *     On a compound match the two tokens are consumed and the scan advances by 2.
         *  2. Otherwise, a single token is checked against [WORD_TO_NUMBER] for word forms
         *     (e.g., "five") and as a bare integer string (e.g., "5" or "12").
         */
        fun parseNumber(text: String): Int? {
            val trimmed = text.trim().lowercase()
            val tokens = trimmed.split(Regex("\\s+")).filter { it.isNotEmpty() }

            var match: Int? = null
            var i = 0
            while (i < tokens.size) {
                val token = tokens[i].trimEnd('.', ',', '!', '?')

                // try two-token compound first ("twenty one", "twenty-one")
                if (i + 1 < tokens.size) {
                    val value = WORD_TO_NUMBER["$token ${tokens[i + 1]}"]
                        ?: WORD_TO_NUMBER["$token-${tokens[i + 1]}"]
                    if (value != null) {
                        match = value
                        i += 2
                        continue
                    }
                }

                // single token: word form or bare digit
                val value = WORD_TO_NUMBER[token] ?: token.toIntOrNull().takeIf { it in 1..30 }
                if (value != null) match = value

                i++
            }

            return match
        }
    }

    /** True while the caller wants audio to be processed. */
    @Volatile
    var isActive = false
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Lazy delegate so we can check isInitialized before releasing. */
    private val recognizerDelegate = lazy {
        val config = OfflineRecognizerConfig(
            featConfig = FeatureConfig(sampleRate = SAMPLE_RATE, featureDim = 80),
            modelConfig = OfflineModelConfig(
                whisper = OfflineWhisperModelConfig(
                    encoder = ASSET_ENCODER_PATH,
                    decoder = ASSET_DECODER_PATH,
                ),
                tokens = ASSSET_TOKENS_PATH,
                modelType = "whisper",
                numThreads = 2,
            ),
        )
        Log.d(TAG, "Initialising OfflineRecognizer...")
        OfflineRecognizer(assetManager = context.assets, config = config)
    }
    private val recognizer: OfflineRecognizer by recognizerDelegate

    /** Begin continuous listening. Safe to call multiple times; no-op if already active. */
    fun startListening() {
        if (isActive) return
        isActive = true
        scope.launch { recordLoop() }
    }

    /** Pause listening (e.g., while rest timer is running). Releases the mic immediately. */
    fun stopListening() {
        isActive = false
        // (the recordLoop checks isActive each iteration and exits cleanly)
    }

    /** Release all resources. Must be called when the screen is disposed. */
    fun release() {
        isActive = false
        scope.cancel()
        // release the recognizer only if it was ever initialized
        if (recognizerDelegate.isInitialized()) {
            recognizer.release()
        }
    }

    /**
     * Runs on an IO thread. Continuously records [CHUNK_SECONDS]-second PCM audio
     * buffers and submits each one to Whisper for transcription. Exits when [isActive]
     * becomes false.
     */
    private suspend fun recordLoop() {
        val minBufSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        // buffer: at least one chunk, rounded up to the nearest min buffer boundary
        val bufSize = maxOf(minBufSize, CHUNK_SAMPLES * 2 /* 2 bytes per sample */)

        val audioRecord = try {
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufSize,
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission RECORD_AUDIO not granted", e)
            isActive = false
            return
        }

        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "Failed to initialise AudioRecord")
            audioRecord.release()
            isActive = false
            return
        }

        audioRecord.startRecording()
        Log.d(TAG, "Recording started")

        try {
            val shortBuf = ShortArray(CHUNK_SAMPLES)

            while (isActive && scope.isActive) {
                // read exactly CHUNK_SAMPLES shorts (blocking read, fills completely)
                var totalRead = 0
                while (totalRead < CHUNK_SAMPLES && isActive) {
                    val read = audioRecord.read(shortBuf, totalRead, CHUNK_SAMPLES - totalRead)
                    if (read <= 0) break
                    totalRead += read
                }

                if (totalRead == 0 || !isActive) continue

                // convert PCM-16 shorts →  float32 normalized to [-1, 1]
                val floatBuf = FloatArray(totalRead) { shortBuf[it] / 32768.0f }

                // Whisper ASR inference (CPU-bound, already on IO dispatcher)
                val text = transcribe(floatBuf)
                if (text.isNotBlank()) {
                    Log.d(TAG, text)
                    if (parseCompleteSet(text)) {
                        withContext(Dispatchers.Main) { onCompleteSet() }
                    } else {
                        val number = parseNumber(text)
                        if (number != null) withContext(Dispatchers.Main) { onNumberDetected(number) }
                    }
                }
            }
        } finally {
            audioRecord.stop()
            audioRecord.release()
            Log.d(TAG, "Recording stopped")
        }
    }

    /**
     * Runs Whisper inference on a float32 PCM buffer sampled at [SAMPLE_RATE].
     * Returns the transcribed text (maybe empty if nothing was recognized).
     */
    private fun transcribe(samples: FloatArray): String {
        val stream = recognizer.createStream()
        return try {
            stream.acceptWaveform(samples, sampleRate = SAMPLE_RATE)
            recognizer.decode(stream)
            recognizer.getResult(stream).text.trim()
        } finally {
            stream.release()
        }
    }
}
