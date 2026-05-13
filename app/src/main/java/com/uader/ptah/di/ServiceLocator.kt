package com.uader.ptah.di

import com.uader.ptah.data.PtahRepository
import com.uader.ptah.data.PtahRepositoryImpl
import com.uader.ptah.data.network.RetrofitProvider

/**
 * DI manual mínima.
 *
 * Centraliza la creación del cliente HTTP y del Repository para que el ViewModel
 * no instancie dependencias por su cuenta y los Composables queden libres
 * de lógica pesada. Cuando se incorpore Hilt o Koin, este objeto se reemplaza
 * sin tocar la UI ni el ViewModel.
 */
object ServiceLocator {

    val ptahRepository: PtahRepository by lazy {
        PtahRepositoryImpl(
            apiService = RetrofitProvider.apiService,
            useFakeBackend = true
        )
    }
}
