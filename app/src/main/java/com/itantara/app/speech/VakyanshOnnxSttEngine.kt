package com.itantara.app.speech

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.AssetManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.OrtSession.SessionOptions
import ai.onnxruntime.TensorInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.FileNotFoundException
import java.nio.FloatBuffer
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max

class VakyanshOnnxSttEngine(
    private val context: Context
) : SpeechToTextEngine {

    private val TAG = "ITANTRA_STT_VAKYANSH"
    private val assetManager: AssetManager = context.assets

    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private var audioRecord: AudioRecord? = null
    private val isRecording = AtomicBoolean(false)
    private val recordedSamples = mutableListOf<ShortArray>()

    private var ortEnvironment: OrtEnvironment? = null
    private var ortSession: OrtSession? = null
    private var tokenizer: Wav2Vec2HindiTokenizer? = null
    private var initException: Exception? = null

    val isAvailable: Boolean
        get() = ortSession != null && tokenizer != null && initException == null

    init {
        initializeModel()
    }

    private fun initializeModel() {
        Log.d(TAG, "ITANTRA_STT_VAKYANSH: ONNX_INIT_START")
        val modelAssetPath = "onnx/vakyansh_hindi.onnx"
        val modelDataAssetPath = "onnx/vakyansh_hindi.onnx.data"
        val vocabPath = "vakyansh-hindi/vocab.json"
        val expectedExternalDataSize = 377_749_504L

        try {
            val assetFiles = assetManager.list("onnx") ?: emptyArray()
            val modelAssetExists = assetFiles.contains("vakyansh_hindi.onnx")
            val modelDataAssetExists = assetFiles.contains("vakyansh_hindi.onnx.data")
            Log.d(TAG, "ITANTRA_STT_VAKYANSH: MODEL_ASSET_EXISTS=${modelAssetExists}")
            Log.d(TAG, "ITANTRA_STT_VAKYANSH: MODEL_HAS_EXTERNAL_DATA=${modelDataAssetExists}")
            Log.d(TAG, "model asset path=$modelAssetPath data_asset_path=$modelDataAssetPath asset_dir_files=${assetFiles.joinToString()} asset_found=${modelAssetExists}")

            if (!modelAssetExists) {
                throw FileNotFoundException("Missing model asset: $modelAssetPath, available files=${assetFiles.joinToString()}")
            }
            if (!modelDataAssetExists) {
                throw FileNotFoundException("Missing model data asset: $modelDataAssetPath, available files=${assetFiles.joinToString()}")
            }

            ortEnvironment = OrtEnvironment.getEnvironment()
            val modelDir = File(context.filesDir, "onnx")
            if (!modelDir.exists()) {
                val created = modelDir.mkdirs()
                Log.d(TAG, "ITANTRA_STT_VAKYANSH: RUNTIME_DIR_CREATED=${created} path=${modelDir.absolutePath}")
            }

            val copiedModelFile = File(modelDir, "vakyansh_hindi.onnx")
            val copiedModelDataFile = File(modelDir, "vakyansh_hindi.onnx.data")

            fun copyAssetToFile(assetPath: String, targetFile: File, expectedSize: Long? = null): Boolean {
                if (targetFile.exists() && expectedSize != null && targetFile.length() == expectedSize) {
                    Log.d(TAG, "ITANTRA_STT_VAKYANSH: SKIP_COPY_EXISTING target=${targetFile.absolutePath} size=${targetFile.length()}")
                    return true
                }
                if (targetFile.exists() && targetFile.delete()) {
                    Log.d(TAG, "ITANTRA_STT_VAKYANSH: REPLACE_EXISTING target=${targetFile.absolutePath} size=${targetFile.length()}")
                }

                assetManager.open(assetPath).use { input ->
                    BufferedInputStream(input).use { bufferedInput ->
                        FileOutputStream(targetFile).use { output ->
                            BufferedOutputStream(output).use { bufferedOutput ->
                                val buffer = ByteArray(64 * 1024)
                                var bytesRead: Int
                                while (bufferedInput.read(buffer).also { bytesRead = it } != -1) {
                                    bufferedOutput.write(buffer, 0, bytesRead)
                                }
                                bufferedOutput.flush()
                            }
                        }
                    }
                }

                val finalSize = targetFile.length()
                if (expectedSize != null && finalSize != expectedSize) {
                    throw IllegalStateException("Copied asset $assetPath size mismatch: expected=${expectedSize}, actual=${finalSize}, target=${targetFile.absolutePath}")
                }
                Log.d(TAG, "ITANTRA_STT_VAKYANSH: COPIED asset=$assetPath target=${targetFile.absolutePath} size=${finalSize}")
                return targetFile.exists()
            }

            val modelCopied = copyAssetToFile(modelAssetPath, copiedModelFile)
            val modelDataCopied = copyAssetToFile(modelDataAssetPath, copiedModelDataFile, expectedExternalDataSize)

            val actualModelPath = copiedModelFile.absolutePath
            val actualModelDataPath = copiedModelDataFile.absolutePath
            Log.d(TAG, "ITANTRA_STT_VAKYANSH: ONNX_MODEL_PATH=$actualModelPath")
            Log.d(TAG, "ITANTRA_STT_VAKYANSH: ONNX_MODEL_DATA_PATH=$actualModelDataPath")
            Log.d(TAG, "ITANTRA_STT_VAKYANSH: MODEL_FILE_EXISTS=${copiedModelFile.exists()}")
            Log.d(TAG, "ITANTRA_STT_VAKYANSH: MODEL_DATA_FILE_EXISTS=${copiedModelDataFile.exists()}")
            Log.d(TAG, "ITANTRA_STT_VAKYANSH: MODEL_FILE_SIZE=${copiedModelFile.length()}")
            Log.d(TAG, "ITANTRA_STT_VAKYANSH: MODEL_DATA_FILE_SIZE=${copiedModelDataFile.length()}")

            if (!modelCopied || !copiedModelFile.exists()) {
                throw FileNotFoundException("vakyansh_hindi.onnx is required but was not copied successfully.")
            }
            if (!modelDataCopied || !copiedModelDataFile.exists()) {
                throw FileNotFoundException("vakyansh_hindi.onnx.data is required but was not copied successfully.")
            }
            if (copiedModelDataFile.length() != expectedExternalDataSize) {
                throw IllegalStateException("External data size mismatch: expected=${expectedExternalDataSize}, actual=${copiedModelDataFile.length()} path=${copiedModelDataFile.absolutePath}")
            }

            val sessionOptions = SessionOptions()
            sessionOptions.setIntraOpNumThreads(2)
            sessionOptions.setInterOpNumThreads(1)

            val environment = ortEnvironment ?: throw IllegalStateException("OrtEnvironment unavailable for model=$actualModelPath")
            Log.d(TAG, "ITANTRA_STT_VAKYANSH: CREATING_ORT_SESSION path=$actualModelPath data_path=$actualModelDataPath")
            ortSession = environment.createSession(actualModelPath, sessionOptions)
            Log.d(TAG, "ITANTRA_STT_VAKYANSH: ORT_SESSION_CREATED path=$actualModelPath")
            tokenizer = Wav2Vec2HindiTokenizer(assetManager, vocabPath)

            Log.d(TAG, "ITANTRA_STT_VAKYANSH: ONNX_INIT_SUCCESS")
        } catch (e: Exception) {
            initException = e
            ortSession = null
            ortEnvironment = null
            tokenizer = null
            Log.e(TAG, "ITANTRA_STT_VAKYANSH: ONNX_INIT_FAILED", e)
            throw e
        }
    }

    @SuppressLint("MissingPermission")
    override fun startRecording() {
        Log.d(TAG, "ITANTRA_STT: START_AUDIO_RECORD")
        if (isRecording.getAndSet(true)) return

        try {
            val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            val minBuf = max(2 * 16000, bufferSize)
            Log.d(TAG, "ITANTRA_STT: AUDIO_RECORD_CONFIG sampleRate=$sampleRate channels=mono format=PCM16 bufferSize=$bufferSize minBuf=$minBuf")
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                minBuf
            )
            audioRecord?.startRecording()
            Log.d(TAG, "ITANTRA_STT: AUDIO_RECORD_STARTED")
            Log.d(TAG, "ITANTRA_STT: RECORDING_STARTED")
            recordedSamples.clear()

            Thread {
                val audioBuffer = ShortArray(minBuf)
                while (isRecording.get()) {
                    val readResult = audioRecord?.read(audioBuffer, 0, audioBuffer.size) ?: 0
                    if (readResult > 0) {
                        val slice = ShortArray(readResult)
                        System.arraycopy(audioBuffer, 0, slice, 0, readResult)
                        synchronized(recordedSamples) {
                            recordedSamples.add(slice)
                        }
                    }
                }
                Log.d(TAG, "ITANTRA_STT: STOP_AUDIO_RECORD")
                audioRecord?.stop()
                Log.d(TAG, "ITANTRA_STT: AUDIO_RECORD_STOPPED")
                audioRecord?.release()
                audioRecord = null
            }.start()

            Log.d(TAG, "Recording started samples_target=${minBuf}")
        } catch (e: Exception) {
            isRecording.set(false)
            audioRecord?.release()
            audioRecord = null
            Log.e(TAG, "ITANTRA_STT: AUDIO_RECORD_ERROR: ${e.message ?: "Unknown AudioRecord error"}", e)
        }
    }

    override suspend fun stopRecordingAndTranscribe(): String? = withContext(Dispatchers.Default) {
        if (!isRecording.getAndSet(false)) return@withContext null

        val session = ortSession ?: run {
            Log.e(TAG, "ONNX session unavailable")
            return@withContext null
        }

        val tokenizerInstance = tokenizer ?: run {
            Log.e(TAG, "Tokenizer unavailable")
            return@withContext null
        }

        try {
            val allSamples = synchronized(recordedSamples) {
                val total = recordedSamples.sumOf { it.size }
                val merged = ShortArray(total)
                var offset = 0
                for (chunk in recordedSamples) {
                    System.arraycopy(chunk, 0, merged, offset, chunk.size)
                    offset += chunk.size
                }
                recordedSamples.clear()
                merged
            }

            if (allSamples.isEmpty()) {
                Log.d(TAG, "No audio samples captured")
                return@withContext null
            }

            val floatSamples = FloatArray(allSamples.size)
            for (i in allSamples.indices) {
                floatSamples[i] = allSamples[i].toFloat() / 32768.0f
            }

            val sampleMean = if (floatSamples.isNotEmpty()) floatSamples.average().toFloat() else 0f
            for (i in floatSamples.indices) {
                floatSamples[i] -= sampleMean
            }

            val sampleMin = floatSamples.minOrNull() ?: 0f
            val sampleMax = floatSamples.maxOrNull() ?: 0f
            val rms = if (floatSamples.isNotEmpty()) {
                kotlin.math.sqrt(floatSamples.fold(0.0) { acc, value -> acc + (value * value).toDouble() } / floatSamples.size)
            } else {
                0.0f
            }
            val durationSeconds = floatSamples.size.toFloat() / sampleRate.toFloat()

            Log.d(TAG, "PREPROCESS sample_count=${floatSamples.size} duration_s=${durationSeconds} min=${sampleMin} max=${sampleMax} mean=${sampleMean} rms=${rms}")

            val inputBuffer = FloatBuffer.allocate(floatSamples.size)
            inputBuffer.put(floatSamples)
            inputBuffer.rewind()

            val environment = ortEnvironment ?: run {
                Log.e(TAG, "ONNX session unavailable")
                return@withContext null
            }

            val input = OnnxTensor.createTensor(
                environment,
                inputBuffer,
                longArrayOf(1, floatSamples.size.toLong())
            )

            val start = System.currentTimeMillis()
            val outputs = session.run(mapOf("input_values" to input))
            val elapsed = System.currentTimeMillis() - start

            val logitsValue = outputs[0].value
            val logitsShape = (outputs[0].info as TensorInfo).getShape()
            Log.d(TAG, "inference_ms=$elapsed logits_shape=${logitsShape.contentToString()}")

            @Suppress("UNCHECKED_CAST")
            val batchLogits = logitsValue as Array<Array<FloatArray>>
            val timeLogits = batchLogits.firstOrNull()
                ?: throw IllegalStateException("ONNX output batch was empty for logits shape=${logitsShape.contentToString()}")
            val logitsArray = timeLogits
            val tokenCounts = logitsArray.size
            val predIds = IntArray(tokenCounts)

            for (t in 0 until tokenCounts) {
                var bestIndex = 0
                var bestValue = logitsArray[t][0]
                for (token in 1 until logitsArray[t].size) {
                    if (logitsArray[t][token] > bestValue) {
                        bestValue = logitsArray[t][token]
                        bestIndex = token
                    }
                }
                predIds[t] = bestIndex
            }

            val text = tokenizerInstance.decode(predIds)
            Log.d(TAG, "text=$text")
            return@withContext text
        } catch (e: Exception) {
            Log.e(TAG, "Inference failed", e)
            return@withContext null
        }
    }

    override fun release() {
        isRecording.set(false)
        audioRecord?.release()
        audioRecord = null
        ortSession?.close()
        ortSession = null
        ortEnvironment?.close()
        ortEnvironment = null
        tokenizer = null
    }

    private class Wav2Vec2HindiTokenizer(
        private val assetManager: AssetManager,
        private val vocabAssetPath: String
    ) {
        private val vocab: Map<String, Int> = loadVocab()
        private val reverse: Map<Int, String> = vocab.entries.associate { it.value to it.key }

        private fun loadVocab(): Map<String, Int> {
            val text = assetManager.open(vocabAssetPath).bufferedReader().use { it.readText() }
            val raw = org.json.JSONObject(text)
            val map = mutableMapOf<String, Int>()
            val keys = raw.keys()
            while (keys.hasNext()) {
                val token = keys.next()
                map[token] = raw.getInt(token)
            }
            return map
        }

        fun decode(predIds: IntArray): String {
            val chars = StringBuilder()
            var previous: Int? = null

            for (id in predIds) {
                if (id == 0 || id == 1 || id == 2 || id == 3) {
                    previous = id
                    continue
                }
                if (id == previous) {
                    continue
                }
                previous = id
                val token = reverse[id] ?: continue
                if (token == "|") {
                    chars.append(' ')
                    continue
                }
                chars.append(token)
            }

            return chars.toString().replace(Regex("\\s+"), " ").trim()
        }
    }
}
