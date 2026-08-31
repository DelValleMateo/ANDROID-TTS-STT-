package com.uader.ptah.data.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/** Adaptador del motor TextToSpeech nativo. Su ciclo de vida pertenece al ViewModel. */
class AndroidTextToSpeechManager(context: Context) : SpeechOutput {
    private val _state = MutableStateFlow<TtsState>(TtsState.Initializing)
    override val state: StateFlow<TtsState> = _state.asStateFlow()

    private var engine: TextToSpeech? = null
    private var ready = false
    private var released = false
    private var pendingRequest: SpeechRequest? = null
    private var activeMessageId: Long? = null

    init {
        engine = TextToSpeech(context.applicationContext) { status -> onInitialized(status) }
    }

    private fun onInitialized(status: Int) {
        if (released) return
        val tts = engine ?: return fail("No se pudo crear el motor de voz.")
        if (status != TextToSpeech.SUCCESS) return fail("No se pudo inicializar Text to Speech.")

        val languageResult = tts.setLanguage(Locale.forLanguageTag("es-AR"))
        if (languageResult == TextToSpeech.LANG_MISSING_DATA ||
            languageResult == TextToSpeech.LANG_NOT_SUPPORTED
        ) {
            val fallbackResult = tts.setLanguage(Locale.forLanguageTag("es-ES"))
            if (fallbackResult == TextToSpeech.LANG_MISSING_DATA ||
                fallbackResult == TextToSpeech.LANG_NOT_SUPPORTED
            ) return fail("El dispositivo no tiene una voz en español instalada.")
        }

        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) = finishIfLast(utteranceId)
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) = onSpeechError(utteranceId)
            override fun onError(utteranceId: String?, errorCode: Int) = onSpeechError(utteranceId)
        })
        ready = true
        _state.value = TtsState.Idle
        pendingRequest?.also {
            pendingRequest = null
            speak(it.messageId, it.text)
        }
    }

    override fun speak(messageId: Long, text: String) {
        if (released || text.isBlank()) return
        if (!ready) {
            pendingRequest = SpeechRequest(messageId, text)
            _state.value = TtsState.Initializing
            return
        }

        val tts = engine ?: return fail("El motor de voz no está disponible.")
        val chunks = splitForEngine(text)
        activeMessageId = messageId
        _state.value = TtsState.Speaking(messageId)
        chunks.forEachIndexed { index, chunk ->
            val utteranceId = utteranceId(messageId, index, chunks.lastIndex)
            val queueMode = if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            if (tts.speak(chunk, queueMode, null, utteranceId) == TextToSpeech.ERROR) {
                fail("No se pudo reproducir la respuesta.")
                return
            }
        }
    }

    override fun stop() {
        pendingRequest = null
        activeMessageId = null
        if (ready) engine?.stop()
        if (!released) _state.value = if (ready) TtsState.Idle else TtsState.Initializing
    }

    override fun shutdown() {
        if (released) return
        released = true
        pendingRequest = null
        activeMessageId = null
        engine?.stop()
        engine?.shutdown()
        engine = null
        ready = false
        _state.value = TtsState.Idle
    }

    private fun finishIfLast(utteranceId: String?) {
        val id = utteranceId ?: return
        if (!id.endsWith(LAST_SUFFIX)) return
        val messageId = id.substringBefore(':').toLongOrNull() ?: return
        if (activeMessageId == messageId) {
            activeMessageId = null
            _state.value = TtsState.Idle
        }
    }

    private fun onSpeechError(utteranceId: String?) {
        val messageId = utteranceId?.substringBefore(':')?.toLongOrNull()
        if (messageId == null || activeMessageId == messageId) fail("Ocurrió un error al reproducir la respuesta.")
    }

    private fun fail(message: String) {
        Log.e(TAG, message)
        activeMessageId = null
        _state.value = TtsState.Error(message)
    }

    private data class SpeechRequest(val messageId: Long, val text: String)

    companion object {
        private const val TAG = "PtahTts"
        private const val LAST_SUFFIX = ":last"

        internal fun splitForEngine(text: String, maxLength: Int = TextToSpeech.getMaxSpeechInputLength()): List<String> {
            val clean = text.trim()
            if (clean.length <= maxLength) return listOf(clean)
            return clean.chunked(maxLength)
        }

        private fun utteranceId(messageId: Long, index: Int, lastIndex: Int): String =
            "$messageId:$index${if (index == lastIndex) LAST_SUFFIX else ""}"
    }
}
