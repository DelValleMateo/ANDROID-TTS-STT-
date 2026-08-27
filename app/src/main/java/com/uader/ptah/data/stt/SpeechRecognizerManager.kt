package com.uader.ptah.data.stt

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.uader.ptah.ui.chat.SttState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Wrapper del [SpeechRecognizer] nativo de Android.
 *
 * Responsabilidades:
 *  - Inicializar y liberar el reconocedor de forma segura.
 *  - Exponer [sttState] como [StateFlow] para que el ViewModel observe reactivamente.
 *  - Manejar inicio, parada y cancelación.
 *  - Detectar inicio y fin de escucha con callbacks del [RecognitionListener].
 *  - Traducir los códigos de error al español para mensajes de UI amigables.
 *
 * Limitaciones documentadas:
 *  - Requiere conexión a Internet en la mayoría de los dispositivos Android (motor Google).
 *  - En el emulador puede comportarse diferente al dispositivo físico.
 *  - Solo procesa un idioma a la vez (configurado como es-AR).
 *  - El motor no siempre detecta fin de habla correctamente en ambientes ruidosos.
 */
import android.os.Handler
import android.os.Looper
import java.util.Locale

class SpeechRecognizerManager(private val context: Context) {

    private val _sttState = MutableStateFlow<SttState>(SttState.Idle)
    val sttState: StateFlow<SttState> = _sttState.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    /** Inicia el ciclo de escucha en el hilo principal. */
    fun startListening() {
        mainHandler.post {
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                Log.w(TAG, "SpeechRecognizer no disponible en este dispositivo.")
                _sttState.value = SttState.Error(
                    "El reconocimiento de voz no está disponible en este dispositivo."
                )
                return@post
            }

            try {
                // Destruir instancia previa de forma segura
                speechRecognizer?.destroy()
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(PtahRecognitionListener())
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    )
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "es-AR")
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                }

                Log.d(TAG, "Iniciando escucha STT en MainLooper.")
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Excepción al iniciar SpeechRecognizer: ${e.message}", e)
                _sttState.value = SttState.Error("Error al iniciar el micrófono: ${e.localizedMessage}")
            }
        }
    }

    /** Detiene la escucha en el hilo principal. */
    fun stopListening() {
        mainHandler.post {
            Log.d(TAG, "Deteniendo escucha STT.")
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {
                Log.e(TAG, "Error en stopListening", e)
            }
            _sttState.value = SttState.Processing
        }
    }

    /** Cancela el reconocimiento en curso y vuelve a [SttState.Idle]. */
    fun cancel() {
        mainHandler.post {
            Log.d(TAG, "Cancelando reconocimiento STT.")
            try {
                speechRecognizer?.cancel()
            } catch (e: Exception) {
                Log.e(TAG, "Error en cancel", e)
            }
            _sttState.value = SttState.Idle
        }
    }

    /** Resetea el estado a [SttState.Idle]. */
    fun resetToIdle() {
        _sttState.value = SttState.Idle
    }

    /** Libera los recursos del [SpeechRecognizer]. */
    fun destroy() {
        mainHandler.post {
            Log.d(TAG, "Destruyendo SpeechRecognizer.")
            try {
                speechRecognizer?.destroy()
            } catch (e: Exception) {
                Log.e(TAG, "Error al destruir recognizer", e)
            }
            speechRecognizer = null
        }
    }

    // ─── RecognitionListener ────────────────────────────────────────────────

    private inner class PtahRecognitionListener : RecognitionListener {

        override fun onReadyForSpeech(params: Bundle?) {
            Log.d(TAG, "onReadyForSpeech → Listening")
            _sttState.value = SttState.Listening
        }

        override fun onBeginningOfSpeech() {
            Log.d(TAG, "onBeginningOfSpeech → sigue en Listening")
            _sttState.value = SttState.Listening
        }

        override fun onEndOfSpeech() {
            Log.d(TAG, "onEndOfSpeech → Processing")
            _sttState.value = SttState.Processing
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull().orEmpty().trim()
            Log.d(TAG, "onResults: \"$text\"")

            _sttState.value = if (text.isBlank()) {
                SttState.Error("No se detectó ninguna palabra. Intentá de nuevo.")
            } else {
                SttState.Result(text)
            }
        }

        override fun onError(error: Int) {
            val msg = mapError(error)
            Log.e(TAG, "onError código=$error → $msg")
            _sttState.value = SttState.Error(msg)
        }

        // Callbacks no utilizados en esta implementación.
        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onPartialResults(partialResults: Bundle?) = Unit
        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    // ─── Mapeo de errores ───────────────────────────────────────────────────

    private fun mapError(errorCode: Int): String = when (errorCode) {
        SpeechRecognizer.ERROR_AUDIO ->
            "Error de audio. Verificá que el micrófono esté disponible."
        SpeechRecognizer.ERROR_CLIENT ->
            "Error interno del reconocedor. Intentá de nuevo."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
            "Permiso de micrófono denegado. Habilitalo en Configuración."
        SpeechRecognizer.ERROR_NETWORK ->
            "Error de red. Verificá tu conexión a Internet."
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
            "Tiempo de espera agotado. Verificá tu conexión."
        SpeechRecognizer.ERROR_NO_MATCH ->
            "No se reconoció ninguna palabra. Hablá más claro e intentá de nuevo."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
            "El reconocedor está ocupado. Esperá un momento."
        SpeechRecognizer.ERROR_SERVER ->
            "Error del servidor de reconocimiento. Intentá más tarde."
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
            "No se detectó voz. Intentá hablar más fuerte."
        else ->
            "Error desconocido en el reconocimiento de voz (código $errorCode)."
    }

    private companion object {
        const val TAG = "SpeechRecognizerMgr"
    }
}
