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
import com.uader.ptah.data.network.ApiException
import com.uader.ptah.data.stt.SpeechRecognizerManager
import com.uader.ptah.data.tts.SpeechOutput
import com.uader.ptah.data.tts.TtsState
import com.uader.ptah.di.ServiceLocator
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.io.IOException

class ChatViewModel(
    private val repository: PtahRepository,
    private val speechManager: SpeechRecognizerManager,
    private val speechOutput: SpeechOutput
) : ViewModel() {

    // ─── Estado del chat ────────────────────────────────────────────────────

    private val _messages = mutableStateListOf<ChatMessage>()
    val messages: List<ChatMessage> = _messages

    var uiState by mutableStateOf<ChatUiState>(ChatUiState.Idle)
        private set

    var inputText by mutableStateOf("")
        private set

    /**
     * Indica el origen del texto actualmente en [inputText].
     *
     * Se actualiza cada vez que el texto cambia por teclado ([KEYBOARD]) o
     * llega un resultado STT ([VOICE]). Se incluye en [ChatMessage] al enviar,
     * para que la UI dibuje el badge correcto en la burbuja del usuario.
     */
    var inputOrigin by mutableStateOf(InputOrigin.KEYBOARD)
        private set

    private var lastQuery: String? = null
    private var nextMessageId = 0L

    val ttsState: StateFlow<TtsState> = speechOutput.state

    // ─── Estado de STT ──────────────────────────────────────────────────────

    /** Estado del reconocimiento de voz. La UI observa este Flow para actualizar los indicadores. */
    val sttState: StateFlow<SttState> = speechManager.sttState

    // ─── Eventos de UI (one-shot) ────────────────────────────────────────────

    private val _userEvents = Channel<UserEvent>(capacity = Channel.BUFFERED)
    val userEvents = _userEvents.receiveAsFlow()

    // ─── Init: observar cambios de SttState ─────────────────────────────────

    init {
        viewModelScope.launch {
            speechManager.sttState.collect { state ->
                when (state) {
                    is SttState.Result -> {
                        // Delegar en la función pública para que sea testeable
                        // y para documentar claramente el punto de unificación.
                        onSttResultReceived(state.text)
                        speechManager.resetToIdle()
                    }
                    is SttState.Error -> {
                        Log.e(TAG, "STT Error: ${state.message}")
                        _userEvents.send(UserEvent.ShowSttError(state.message))
                    }
                    is SttState.PermissionDenied -> {
                        _userEvents.send(
                            UserEvent.ShowSttError(
                                "Permiso de micrófono denegado. Habilitalo en Configuración."
                            )
                        )
                    }
                    else -> Unit
                }
            }
        }
        viewModelScope.launch {
            speechOutput.state.collect { state ->
                if (state is TtsState.Error) _userEvents.send(UserEvent.ShowTtsError(state.message))
            }
        }
    }

    // ─── Punto de entrada unificado para resultado STT ───────────────────────

    /**
     * Recibe el texto reconocido por el motor STT y lo carga en el campo de entrada.
     *
     * Este es el **único punto de unión** entre la tubería de voz y la tubería de texto:
     * después de esta función, el texto dictado sigue exactamente el mismo camino que el
     * texto escrito a mano → [inputText] → [onSendClicked] → [executeQuery].
     *
     * La UI mantiene el campo editable para que el usuario pueda corregir antes de enviar.
     *
     * @param text Texto reconocido por [SpeechRecognizerManager]. Nunca vacío (el manager
     *             emite [SttState.Error] si el resultado es blank).
     */
    fun onSttResultReceived(text: String) {
        inputText = text
        inputOrigin = InputOrigin.VOICE
        Log.d(TAG, "STT Result → inputText: \"$text\" | origin: VOICE")
    }

    // ─── Acciones del chat ──────────────────────────────────────────────────

    /**
     * Llamado por la UI cada vez que el usuario modifica el campo de texto manualmente.
     * Resetea el origen a [InputOrigin.KEYBOARD] para no etiquetar como voz un texto editado.
     */
    fun onTextChanged(text: String) {
        val wasVoiceInput = inputOrigin == InputOrigin.VOICE
        inputText = text
        // Si el usuario edita el campo después de un dictado, el origen pasa a KEYBOARD.
        // Así la burbuja no mostrará el badge de voz para texto modificado por el usuario.
        if (wasVoiceInput) {
            inputOrigin = InputOrigin.KEYBOARD
        }
    }

    fun onSendClicked() {
        val clean = inputText.trim()
        if (clean.isEmpty()) return
        if (uiState is ChatUiState.Loading) return

        val origin = inputOrigin          // capturar antes de limpiar
        _messages.add(ChatMessage(nextMessageId++, ChatMessage.Author.USER, clean, origin))
        inputText = ""
        inputOrigin = InputOrigin.KEYBOARD   // resetear para la próxima consulta
        executeQuery(clean)
    }

    fun retryLastQuery() {
        lastQuery?.let {
            Log.d(TAG, "Reintentando ultima consulta: $it")
            executeQuery(it)
        }
    }

    private fun executeQuery(query: String) {
        lastQuery = query
        uiState = ChatUiState.Loading

        viewModelScope.launch {
            val startedAt = SystemClock.elapsedRealtime()
            Log.d(TAG, "Inicio de consulta a Groq: $query")

            repository.ask(query)
                .onSuccess { response ->
                    val latencyMs = SystemClock.elapsedRealtime() - startedAt
                    Log.d(TAG, "Fin de consulta a Groq. Latencia: ${latencyMs}ms")
                    speechOutput.stop()
                    _messages.add(ChatMessage(nextMessageId++, ChatMessage.Author.SYSTEM, response.answer))
                    uiState = ChatUiState.Success(latencyMs)
                }
                .onFailure { throwable ->
                    handleFailure(throwable, startedAt)
                }
        }
    }

    private fun handleFailure(throwable: Throwable, startedAt: Long) {
        val latencyMs = SystemClock.elapsedRealtime() - startedAt
        val message = when (throwable) {
            is ApiException -> throwable.message
                ?: "El servicio respondio con un error. Intenta nuevamente."
            is IOException -> throwable.message
                ?: "No se pudo conectar con el servidor. Verifica tu conexion."
            else -> throwable.message ?: "Fallo inesperado. Intenta nuevamente."
        }

        Log.e(TAG, "Error en consulta a Groq tras ${latencyMs}ms: $message", throwable)
        uiState = ChatUiState.Error(message, latencyMs)

        viewModelScope.launch {
            _userEvents.send(UserEvent.ShowError(message))
        }
    }

    // ─── Acciones de STT ────────────────────────────────────────────────────

    /**
     * Alterna el reconocimiento de voz.
     * - Si está en [SttState.Idle]: inicia la escucha.
     * - Si está en [SttState.Listening] o [SttState.Processing]: cancela.
     *
     * La UI es responsable de verificar el permiso RECORD_AUDIO antes de llamar
     * a este método. Si el permiso no fue otorgado, emite [UserEvent.RequestMicPermission].
     */
    fun onMicClicked(permissionGranted: Boolean) {
        if (!permissionGranted) {
            viewModelScope.launch {
                _userEvents.send(UserEvent.RequestMicPermission)
            }
            return
        }

        when (speechManager.sttState.value) {
            is SttState.Idle, is SttState.Error, is SttState.Result,
            is SttState.PermissionDenied -> speechManager.startListening()
            is SttState.Listening -> speechManager.cancel()
            is SttState.Processing -> Unit // No interrumpir el procesamiento.
        }
    }

    fun onSpeakClicked(message: ChatMessage) {
        if (message.author == ChatMessage.Author.SYSTEM) speechOutput.speak(message.id, message.text)
    }

    fun onStopSpeakingClicked() = speechOutput.stop()

    // ─── Ciclo de vida ───────────────────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        speechManager.destroy()
        speechOutput.shutdown()
    }

    // ─── Factory ─────────────────────────────────────────────────────────────

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
                speechOutput = ServiceLocator.createSpeechOutput(context)
            ) as T
        }
    }

    private companion object {
        const val TAG = "ChatViewModel"
    }
}
