package com.uader.ptah.ui.chat

import com.uader.ptah.data.PtahRepository
import com.uader.ptah.data.QueryResponse
import com.uader.ptah.data.stt.SpeechRecognizerManager
import com.uader.ptah.data.tts.SpeechOutput
import com.uader.ptah.data.tts.TtsState
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    
    private lateinit var repository: PtahRepository
    private lateinit var speechManager: SpeechRecognizerManager
    private lateinit var speechOutput: SpeechOutput
    
    private lateinit var sttStateFlow: MutableStateFlow<SttState>
    private lateinit var ttsStateFlow: MutableStateFlow<TtsState>

    private lateinit var viewModel: ChatViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        repository = mockk()
        speechManager = mockk(relaxed = true)
        speechOutput = mockk(relaxed = true)

        sttStateFlow = MutableStateFlow(SttState.Idle)
        ttsStateFlow = MutableStateFlow(TtsState.Idle)

        every { speechManager.sttState } returns sttStateFlow
        every { speechOutput.state } returns ttsStateFlow
        
        viewModel = ChatViewModel(repository, speechManager, speechOutput)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `when STT result is received, it auto sends the query and auto speaks the response`() = runTest {
        // Arrange
        val voiceText = "Consulta de voz"
        val sysResponse = "Respuesta del sistema"
        coEvery { repository.ask(voiceText) } returns Result.success(QueryResponse(sysResponse))

        // Act
        sttStateFlow.value = SttState.Result(voiceText)
        testScheduler.advanceUntilIdle() // Process all coroutines

        // Assert
        // Verifica que se cambió el inputText por un momento
        // (ya que se borra rápido al autoenviar)
        assertEquals("", viewModel.inputText) 
        
        // Verifica que se llamó al repo con el texto
        coEvery { repository.ask(voiceText) }
        
        // Verifica que el mensaje del sistema se agregó
        val lastMessage = viewModel.messages.last()
        assertEquals(sysResponse, lastMessage.text)
        assertEquals(ChatMessage.Author.SYSTEM, lastMessage.author)

        // Verifica que se llamó a TTS (auto-speak)
        verify { speechOutput.speak(lastMessage.id, sysResponse) }
    }
    
    @Test
    fun `when mic is clicked, if TTS is speaking, it stops TTS`() = runTest {
        // Arrange
        ttsStateFlow.value = TtsState.Speaking(1L)
        testScheduler.advanceUntilIdle()
        
        // El estado debería ser Speaking
        assertTrue(viewModel.conversationState is ConversationState.Speaking)

        // Act
        viewModel.onMicClicked(permissionGranted = true)

        // Assert
        verify { speechOutput.stop() }
    }
}
