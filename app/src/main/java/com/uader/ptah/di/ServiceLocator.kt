package com.uader.ptah.di

import android.content.Context
import com.uader.ptah.data.PtahRepository
import com.uader.ptah.data.PtahRepositoryImpl
import com.uader.ptah.data.network.RetrofitProvider
import com.uader.ptah.data.stt.SpeechRecognizerManager
import com.uader.ptah.data.tts.NaturalTtsManager
import com.uader.ptah.data.tts.SpeechOutput

/**
 * DI manual mínima.
 *
 * Centraliza la creación del cliente HTTP, el Repository, SpeechRecognizerManager y NaturalTtsManager
 * para que el ViewModel no instancie dependencias por su cuenta y los Composables queden
 * libres de lógica pesada.
 */
object ServiceLocator {

    val ptahRepository: PtahRepository by lazy {
        PtahRepositoryImpl(
            remoteDataSource = RetrofitProvider.queryRemoteDataSource
        )
    }

    /**
     * Crea una nueva instancia de [SpeechRecognizerManager].
     */
    fun createSpeechManager(context: Context): SpeechRecognizerManager {
        return SpeechRecognizerManager(context.applicationContext)
    }

    /**
     * Retorna la instancia de [NaturalTtsManager] (Sherpa-ONNX + Piper VITS).
     */
    fun createSpeechOutput(context: Context): SpeechOutput {
        return NaturalTtsManager.getInstance(context.applicationContext)
    }
}
