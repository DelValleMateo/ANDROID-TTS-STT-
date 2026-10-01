package com.uader.ptah.ui.chat

import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.uader.ptah.data.PtahRepository
import com.uader.ptah.data.stt.SpeechRecognizerManager
import com.uader.ptah.data.tts.SpeechOutput
import com.uader.ptah.data.tts.TtsState
import com.uader.ptah.di.ServiceLocator
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: PtahRepository,
    private val speechManager: SpeechRecognizerManager,
    private val naturalTtsManager: SpeechOutput
) : ViewModel() {
    private val speechOutput: SpeechOutput get() = naturalTtsManager

    // ─── Estado de Conversación (Unificado) ──────────────────────────────────
    
    var conversationState by mutableStateOf<ConversationState>(ConversationState.Idle)
        private set

    var autoSpeakEnabled by mutableStateOf(true) // Por defecto encendido
        private set

    var metrics by mutableStateOf(ConversationMetrics())
        private set

    // ─── Estado del chat ────────────────────────────────────────────────────

    private val _messages = mutableStateListOf<ChatMessage>()
    val messages: List<ChatMessage> = _messages

    var inputText by mutableStateOf("")
        private set

    var inputOrigin by mutableStateOf(InputOrigin.KEYBOARD)
        private set

    private var lastQuery: String? = null
    private var nextMessageId = 0L

    val ttsState: StateFlow<TtsState> = speechOutput.state
    val sttState: StateFlow<SttState> = speechManager.sttState

    // ─── Tiempos para métricas ──────────────────────────────────────────────
    private var sttStartTime: Long? = null
    private var apiStartTime: Long? = null
    private var ttsStartTime: Long? = null
    private var flowStartTime: Long? = null // Para la latencia total voz->respuesta

    // ─── Eventos de UI (one-shot) ────────────────────────────────────────────

    private val _userEvents = Channel<UserEvent>(capacity = Channel.BUFFERED)
    val userEvents = _userEvents.receiveAsFlow()

    init {
        viewModelScope.launch {
            speechManager.sttState.collect { state ->
                when (state) {
                    is SttState.Idle -> {
                        if (conversationState is ConversationState.Listening || conversationState is ConversationState.ProcessingVoice) {
                            conversationState = ConversationState.Idle
                        }
                    }
                    is SttState.Listening -> conversationState = ConversationState.Listening
                    is SttState.Processing -> conversationState = ConversationState.ProcessingVoice
                    is SttState.Result -> {
                        val latency = sttStartTime?.let { SystemClock.elapsedRealtime() - it }
                        metrics = metrics.copy(sttLatencyMs = latency)
                        
                        onSttResultReceived(state.text)
                        speechManager.resetToIdle()
                    }
                    is SttState.Error -> {
                        Log.e(TAG, "STT Error: ${state.message}")
                        conversationState = ConversationState.Error(state.message, canRetry = false)
                        _userEvents.send(UserEvent.ShowSttError(state.message))
                    }
                    is SttState.PermissionDenied -> {
                        _userEvents.send(
                            UserEvent.ShowSttError("Permiso de micrófono denegado. Habilitalo en Configuración.")
                        )
                        conversationState = ConversationState.Idle
                    }
                }
            }
        }
        viewModelScope.launch {
            naturalTtsManager.state.collect { state ->
                when (state) {
                    is TtsState.Idle -> {
                        if (conversationState is ConversationState.Speaking) {
                            val latency = ttsStartTime?.let { SystemClock.elapsedRealtime() - it }
                            metrics = metrics.copy(
                                ttsLatencyMs = latency,
                                totalLatencyMs = flowStartTime?.let { SystemClock.elapsedRealtime() - it }
                            )
                            conversationState = ConversationState.Idle
                        }
                    }
                    is TtsState.Generating -> {
                        if (ttsStartTime == null) ttsStartTime = SystemClock.elapsedRealtime()
                    }
                    is TtsState.Speaking -> {
                        if (ttsStartTime == null) ttsStartTime = SystemClock.elapsedRealtime()
                        conversationState = ConversationState.Speaking(state.messageId)
                    }
                    is TtsState.Error -> {
                        _userEvents.send(UserEvent.ShowTtsError(state.message))
                        if (conversationState is ConversationState.Speaking) {
                            conversationState = ConversationState.Idle
                        }
                    }
                    is TtsState.Initializing -> Unit
                }
            }
        }
    }

    // ─── Acciones del chat ──────────────────────────────────────────────────

    fun toggleAutoSpeak() {
        autoSpeakEnabled = !autoSpeakEnabled
    }

    fun onSttResultReceived(text: String) {
        inputText = text
        inputOrigin = InputOrigin.VOICE
        Log.d(TAG, "Resultado STT recibido; origen: VOICE")
        
        // Sprint 10: Auto-send for Voice queries
        onSendClicked()
    }

    fun onTextChanged(text: String) {
        val wasVoiceInput = inputOrigin == InputOrigin.VOICE
        inputText = text
        if (wasVoiceInput) {
            inputOrigin = InputOrigin.KEYBOARD
        }
    }

    fun onSendClicked() {
        val clean = inputText.trim()
        if (clean.isEmpty()) return
        if (conversationState is ConversationState.Consulting) return

        val origin = inputOrigin
        _messages.add(ChatMessage(nextMessageId++, ChatMessage.Author.USER, clean, origin))
        inputText = ""
        inputOrigin = InputOrigin.KEYBOARD
        
        executeQuery(clean, origin)
    }

    fun retryLastQuery() {
        lastQuery?.let {
            Log.d(TAG, "Reintentando la última consulta.")
            // Asumimos origen teclado para reintentos por defecto
            executeQuery(it, InputOrigin.KEYBOARD)
        }
    }

    /** Alias para reintentar la última consulta fallida sin volver a escribir. */
    fun reintentarUltimaConsulta() = retryLastQuery()

    private fun executeQuery(query: String, origin: InputOrigin) {
        lastQuery = query
        conversationState = ConversationState.Consulting(query)

        viewModelScope.launch {
            apiStartTime = SystemClock.elapsedRealtime()
            if (origin != InputOrigin.VOICE) {
                // Si es texto, reiniciamos el tiempo total
                flowStartTime = apiStartTime 
            }
            Log.d(TAG, "Inicio de consulta al servicio remoto.")

            repository.ask(query)
                .onSuccess { response ->
                    val latencyMs = SystemClock.elapsedRealtime() - apiStartTime!!
                    metrics = metrics.copy(apiLatencyMs = latencyMs)
                    Log.d(TAG, "Fin de consulta remota. Latencia: ${latencyMs}ms")
                    
                    speechOutput.stop()
                    val sysMessage = ChatMessage(nextMessageId++, ChatMessage.Author.SYSTEM, response.answer)
                    _messages.add(sysMessage)
                    
                    conversationState = ConversationState.Idle

                    // Reproducir automáticamente si el origen es voz o si autoSpeakEnabled
                    if (autoSpeakEnabled || origin == InputOrigin.VOICE) {
                        onSpeakClicked(sysMessage)
                    } else {
                        // Flujo finalizado si no hay TTS
                        metrics = metrics.copy(
                            totalLatencyMs = flowStartTime?.let { SystemClock.elapsedRealtime() - it }
                        )
                    }
                }
                .onFailure { throwable ->
                    handleFailure(throwable, apiStartTime!!)
                }
        }
    }

    private fun handleFailure(throwable: Throwable, startedAt: Long) {
        val latencyMs = SystemClock.elapsedRealtime() - startedAt
        val message = throwable.message
            ?.takeIf { it.isNotBlank() }
            ?: "Fallo inesperado. Intenta nuevamente."

        Log.e(TAG, "Error en consulta remota tras ${latencyMs}ms: $message", throwable)
        conversationState = ConversationState.Error(message, canRetry = true)

        viewModelScope.launch {
            _userEvents.send(UserEvent.ShowError(message))
        }
    }

    // ─── Acciones de STT / TTS ───────────────────────────────────────────────

    fun onMicClicked(permissionGranted: Boolean) {
        if (!permissionGranted) {
            viewModelScope.launch {
                _userEvents.send(UserEvent.RequestMicPermission)
            }
            return
        }

        // Si está hablando, callar al asistente
        if (conversationState is ConversationState.Speaking) {
            speechOutput.stop()
        }

        when (speechManager.sttState.value) {
            is SttState.Idle, is SttState.Error, is SttState.Result,
            is SttState.PermissionDenied -> {
                sttStartTime = SystemClock.elapsedRealtime()
                flowStartTime = sttStartTime // Inicia el flujo completo Voz->Respuesta
                speechManager.startListening()
            }
            is SttState.Listening -> speechManager.cancel()
            is SttState.Processing -> Unit
        }
    }

    fun onSpeakClicked(message: ChatMessage) {
        if (message.author == ChatMessage.Author.SYSTEM) {
            ttsStartTime = null // reset antes de inicializar
            naturalTtsManager.speak(message.id, message.text)
        }
    }

    fun onStopSpeakingClicked() = naturalTtsManager.stop()

    // ─── Ciclo de vida ───────────────────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        speechManager.destroy()
        naturalTtsManager.shutdown()
    }

    class Factory(
        private val context: Context,
        private val repository: PtahRepository = ServiceLocator.ptahRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ChatViewModel::class.java)) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            return ChatViewModel(
                repository = repository,
                speechManager = ServiceLocator.createSpeechManager(context),
                naturalTtsManager = ServiceLocator.createSpeechOutput(context)
            ) as T
        }
    }

    private companion object {
        const val TAG = "ChatViewModel"
    }
}
