package com.uader.ptah.data

/**
 * Frontera entre el Repository y el proveedor remoto concreto.
 *
 * La UI y el ViewModel solo conocen [PtahRepository]. Cuando PTAH publique su
 * contrato definitivo, una nueva implementación de esta interfaz podrá
 * reemplazar a Groq sin cambiar el flujo conversacional.
 */
fun interface QueryRemoteDataSource {
    suspend fun ask(query: String): String
}
