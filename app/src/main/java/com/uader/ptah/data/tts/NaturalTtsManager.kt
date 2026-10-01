package com.uader.ptah.data.tts

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Sintetizador de voz natural de alta fidelidad para Android basado en Sherpa-ONNX
 * y el modelo neural nativo en español argentino (Piper VITS: es_AR-daniela-high).
 *
 * Características:
 * - 100% offline y local en el dispositivo.
 * - Voz femenina nativa en español argentino, clara y ágil con cadencia conversacional.
 * - Preprocesamiento inteligente del texto sin cortes artificiales entre oraciones.
 * - Síntesis de bloques continuos con entonación natural sin pausas forzadas de silencio plano.
 * - Streaming en tiempo real vía [AudioTrack] con buffer flotante PCM.
 * - Inferencia fuera del Main Thread con corrutinas y cancelación inmediata de respuestas previas.
 * - Estados reactivos: [TtsState.Idle], [TtsState.Generating], [TtsState.Speaking], [TtsState.Error].
 */
class NaturalTtsManager(
    private val context: Context,
    var config: Config = Config()
) : SpeechOutput {

    /**
     * Parámetros de configuración del sintetizador de voz natural.
     * Centralizados en una única estructura para fácil personalización.
     *
     * @param speed Velocidad de habla (por defecto 1.12f para cadencia ágil y moderna).
     * @param silenceScale Escala de silencios al inicio y fin generados por VITS (0.15f por defecto).
     * @param pauseBetweenSentencesMs Pausa en ms entre bloques de texto grandes (80 ms por defecto).
     * @param speakerId ID del locutor (0 para Daniela mono-locutor).
     * @param numThreads Cantidad de hilos de inferencia CPU.
     * @param maxChunkLength Longitud máxima recomendada por bloque antes de particionar textos muy extensos.
     * @param noiseScale Parámetro de ruido de síntesis VITS (0.667f por defecto).
     * @param noiseScaleW Parámetro de ruido fonético VITS (0.8f por defecto).
     * @param lengthScale Factor de escala de duración VITS (1.0f).
     */
    data class Config(
        val speed: Float = 1.12f,
        val silenceScale: Float = 0.15f,
        val pauseBetweenSentencesMs: Long = 80L,
        val speakerId: Int = SPEAKER_ASSISTANT,
        val numThreads: Int = 2,
        val maxChunkLength: Int = 360,
        val noiseScale: Float = 0.667f,
        val noiseScaleW: Float = 0.8f,
        val lengthScale: Float = 1.0f
    ) {
        companion object {
            /** ID del locutor del asistente virtual (Daniela, VITS mono-locutor id 0) */
            const val SPEAKER_ASSISTANT = 0
            const val DEFAULT_SPEED = 1.12f
            const val DEFAULT_SILENCE_SCALE = 0.15f
            const val DEFAULT_PAUSE_MS = 80L
        }
    }

    private val _state = MutableStateFlow<TtsState>(TtsState.Initializing)
    override val state: StateFlow<TtsState> = _state.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    private var sherpaTts: OfflineTts? = null

    @Volatile
    private var isInitialized = false

    @Volatile
    private var isReleased = false

    private var activeJob: Job? = null
    private var currentAudioTrack: AudioTrack? = null
    private val lock = Any()

    init {
        scope.launch {
            initializeEngine()
        }
    }

    /**
     * Inicializa el motor Sherpa-ONNX Piper VITS en segundo plano.
     */
    private suspend fun initializeEngine() = withContext(Dispatchers.IO) {
        if (isReleased) return@withContext

        try {
            Log.i(TAG, "Iniciando verificación del modelo Piper VITS Daniela...")
            val modelFiles = PiperModelManager.prepareModel(context)

            if (modelFiles == null) {
                val errorMsg = "Archivos del modelo Piper VITS no encontrados en assets o almacenamiento interno."
                Log.w(TAG, errorMsg)
                _state.value = TtsState.Error(errorMsg)
                return@withContext
            }

            Log.i(TAG, "Configurando Sherpa-ONNX VITS con modelo: ${modelFiles.modelPath}")

            val vitsConfig = OfflineTtsVitsModelConfig(
                model = modelFiles.modelPath,
                lexicon = modelFiles.lexiconPath,
                tokens = modelFiles.tokensPath,
                dataDir = modelFiles.dataDir,
                dictDir = "",
                noiseScale = config.noiseScale,
                noiseScaleW = config.noiseScaleW,
                lengthScale = config.lengthScale
            )

            val modelConfig = OfflineTtsModelConfig(
                vits = vitsConfig,
                numThreads = config.numThreads,
                debug = false,
                provider = "cpu"
            )

            val ttsConfig = OfflineTtsConfig(
                model = modelConfig,
                maxNumSentences = 1,
                silenceScale = config.silenceScale
            )

            // Instanciar OfflineTts con rutas de sistema de archivos
            val tts = OfflineTts(
                assetManager = null,
                config = ttsConfig
            )

            synchronized(lock) {
                sherpaTts = tts
                isInitialized = true
            }

            Log.i(
                TAG,
                "Sherpa-ONNX Piper VITS inicializado exitosamente. SampleRate: ${tts.sampleRate()}"
            )
            _state.value = TtsState.Idle

        } catch (e: Throwable) {
            Log.e(TAG, "Fallo al inicializar Sherpa-ONNX Piper VITS: ${e.message}", e)
            _state.value = TtsState.Error(e.message ?: "Error al inicializar motor de voz natural")
        }
    }

    /**
     * Sintetiza y reproduce texto con un ID de mensaje explícito.
     * Cancela automáticamente cualquier reproducción previa.
     */
    override fun speak(messageId: Long, text: String) {
        if (isReleased || text.isBlank()) return

        stop()

        activeJob = scope.launch {
            executeSpeech(messageId, text)
        }
    }

    /**
     * API simplificada: reproduce una frase sin necesidad de manejar IDs externamente.
     * Ejemplo de uso: `naturalTtsManager.speak("Hola, ¿cómo estás?")`
     */
    fun speak(text: String) {
        speak(messageId = System.currentTimeMillis(), text = text)
    }

    private suspend fun executeSpeech(messageId: Long, rawText: String) {
        // Asegurar que el motor esté listo
        val tts = waitForInitialization()
        if (tts == null) {
            _state.value = TtsState.Error("El sintetizador de voz no está listo o faltan los modelos.")
            return
        }

        // 1. Agrupar texto en bloques naturales completos (evita fragmentar oraciones aisladas)
        val speechBlocks = SpanishTextNormalizer.splitIntoSpeechBlocks(rawText, config.maxChunkLength)
        if (speechBlocks.isEmpty()) {
            _state.value = TtsState.Idle
            return
        }

        val sampleRate = tts.sampleRate()
        val pauseSamples = if (config.pauseBetweenSentencesMs > 0) {
            (sampleRate * (config.pauseBetweenSentencesMs / 1000.0)).toInt()
        } else 0
        val silenceArray = if (pauseSamples > 0) FloatArray(pauseSamples) else null

        var audioTrack: AudioTrack? = null

        try {
            _state.value = TtsState.Generating(messageId)

            // Crear AudioTrack en modo STREAM para reproducción fluida sin latencia perceptible
            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_FLOAT
            )
            val bufferSize = (minBufferSize * 4).coerceAtLeast(sampleRate * 2)

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            synchronized(lock) {
                currentAudioTrack = track
            }
            audioTrack = track
            track.play()

            // Inferencia y reproducción bloque a bloque (continuo)
            for ((index, block) in speechBlocks.withIndex()) {
                if (!coroutineScopeActive()) break

                Log.d(TAG, "Sintetizando bloque [$index/${speechBlocks.size}]: '$block'")

                if (index == 0) {
                    _state.value = TtsState.Generating(messageId)
                }

                val audio = withContext(Dispatchers.Default) {
                    tts.generate(
                        text = block,
                        sid = config.speakerId,
                        speed = config.speed
                    )
                }

                if (!coroutineScopeActive()) break

                _state.value = TtsState.Speaking(messageId)

                val samples = audio.samples
                if (samples.isNotEmpty()) {
                    track.write(samples, 0, samples.size, AudioTrack.WRITE_BLOCKING)
                }

                // Pausa natural únicamente entre bloques largos separados (no después de cada punto o frase)
                if (index < speechBlocks.lastIndex && silenceArray != null && coroutineScopeActive()) {
                    track.write(silenceArray, 0, silenceArray.size, AudioTrack.WRITE_BLOCKING)
                }
            }

            // Esperar a que el buffer termine de emitir sonido
            if (coroutineScopeActive() && track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                val remainingMs = 120L
                kotlinx.coroutines.delay(remainingMs)
            }

            if (coroutineScopeActive()) {
                _state.value = TtsState.Idle
            }

        } catch (e: CancellationException) {
            Log.d(TAG, "Síntesis cancelada por el usuario o nueva solicitud.")
        } catch (e: Throwable) {
            Log.e(TAG, "Error durante la síntesis/reproducción: ${e.message}", e)
            _state.value = TtsState.Error(e.message ?: "Error en la reproducción de voz")
        } finally {
            cleanupAudioTrack(audioTrack)
            synchronized(lock) {
                if (currentAudioTrack == audioTrack) {
                    currentAudioTrack = null
                }
            }
            if (_state.value is TtsState.Speaking || _state.value is TtsState.Generating) {
                _state.value = TtsState.Idle
            }
        }
    }

    private suspend fun waitForInitialization(): OfflineTts? {
        if (isInitialized) return sherpaTts
        for (i in 0 until 50) {
            if (isInitialized) return sherpaTts
            kotlinx.coroutines.delay(100)
        }
        return sherpaTts
    }

    private fun coroutineScopeActive(): Boolean = !isReleased && (activeJob?.isActive == true)

    /**
     * Detiene inmediatamente la síntesis y el audio actual.
     */
    override fun stop() {
        activeJob?.cancel()
        activeJob = null

        synchronized(lock) {
            currentAudioTrack?.let { track ->
                try {
                    track.pause()
                    track.flush()
                } catch (e: Exception) {
                    Log.w(TAG, "Error pausando audio track: ${e.message}")
                }
            }
        }

        if (!isReleased) {
            _state.value = if (isInitialized) TtsState.Idle else TtsState.Initializing
        }
    }

    /**
     * Libera todos los recursos nativos y detiene corrutinas.
     */
    override fun shutdown() {
        release()
    }

    /**
     * Alias explícito para liberar recursos.
     */
    fun release() {
        if (isReleased) return
        isReleased = true

        stop()
        scope.cancel()

        synchronized(lock) {
            currentAudioTrack?.let { cleanupAudioTrack(it) }
            currentAudioTrack = null

            sherpaTts?.let {
                try {
                    it.release()
                } catch (e: Exception) {
                    Log.w(TAG, "Error liberando OfflineTts: ${e.message}")
                }
            }
            sherpaTts = null
            isInitialized = false
        }

        _state.value = TtsState.Idle
        Log.i(TAG, "NaturalTtsManager liberado completamente.")
    }

    private fun cleanupAudioTrack(track: AudioTrack?) {
        if (track == null) return
        try {
            if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                track.stop()
            }
            track.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error al limpiar AudioTrack", e)
        }
    }

    companion object {
        private const val TAG = "NaturalTtsManager"

        @Volatile
        private var instance: NaturalTtsManager? = null

        /**
         * Singleton opcional para reutilización global de la instancia pesada del modelo.
         */
        fun getInstance(context: Context): NaturalTtsManager {
            return instance ?: synchronized(this) {
                instance ?: NaturalTtsManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
