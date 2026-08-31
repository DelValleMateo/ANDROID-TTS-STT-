package com.uader.ptah.di

import android.content.Context
import com.uader.ptah.data.PtahRepository
import com.uader.ptah.data.PtahRepositoryImpl
import com.uader.ptah.data.network.RetrofitProvider
import com.uader.ptah.data.stt.SpeechRecognizerManager
import com.uader.ptah.data.tts.AndroidTextToSpeechManager
import com.uader.ptah.data.tts.SpeechOutput

/**
 * DI manual mínima.
 *
 * Centraliza la creación del cliente HTTP, el Repository y el SpeechRecognizerManager
 * para que el ViewModel no instancie dependencias por su cuenta y los Composables queden
 * libres de lógica pesada.
 *
 * Cuando se incorpore Hilt o Koin, este objeto se reemplaza sin tocar la UI ni el ViewModel.
 */
object ServiceLocator {

    val ptahRepository: PtahRepository by lazy {
        PtahRepositoryImpl(
            apiService = RetrofitProvider.groqApiService
        )
    }

    /**
     * Crea una nueva instancia de [SpeechRecognizerManager].
     *
     * No se cachea como singleton porque su ciclo de vida está atado al ViewModel:
     * el ViewModel llama a [SpeechRecognizerManager.destroy] en [ViewModel.onCleared].
     */
    fun createSpeechManager(context: Context): SpeechRecognizerManager {
        return SpeechRecognizerManager(context.applicationContext)
    }

    fun createSpeechOutput(context: Context): SpeechOutput {
        return AndroidTextToSpeechManager(context.applicationContext)
    }
}
