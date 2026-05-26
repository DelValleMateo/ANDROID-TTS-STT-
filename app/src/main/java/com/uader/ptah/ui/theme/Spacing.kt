package com.uader.ptah.ui.theme

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Tokens de espaciado de la aplicación PTAH.
 *
 * Centralizar estos valores aquí garantiza consistencia visual en toda la app:
 * si se necesita ajustar el margen general, se cambia un solo lugar.
 * Seguimos el sistema de 4dp-grid recomendado por Material Design.
 */
object PtahSpacing {
    /** Margen lateral estándar de pantalla (16dp). Material Design recomienda 16dp mínimo. */
    val screenHorizontal = 16.dp

    /** Margen vertical estándar entre secciones de pantalla. */
    val screenVertical = 12.dp

    /** Espacio entre elementos hermanos dentro de una misma fila/columna. */
    val itemGap = 8.dp

    /** Padding interno de burbujas de mensaje y badges. */
    val bubbleInner = 12.dp

    /** Padding interno compacto (chips, etiquetas, íconos). */
    val compact = 4.dp
}

/**
 * Modifier de extensión que aplica el padding de pantalla estándar de PTAH.
 *
 * Uso:
 * ```kotlin
 * Column(modifier = Modifier.defaultScreenPadding()) { ... }
 * ```
 *
 * Esto garantiza que TODOS los contenedores de pantalla tengan exactamente
 * los mismos márgenes, sin hardcodear valores de dp en cada Composable.
 */
fun Modifier.defaultScreenPadding(): Modifier =
    this.padding(
        horizontal = PtahSpacing.screenHorizontal,
        vertical   = PtahSpacing.screenVertical
    )

/**
 * Modifier de extensión para el padding horizontal únicamente.
 * Útil cuando el padding vertical lo maneja el Scaffold (innerPadding).
 *
 * Uso:
 * ```kotlin
 * Column(modifier = Modifier.padding(paddingValues).screenHorizontalPadding()) { ... }
 * ```
 */
fun Modifier.screenHorizontalPadding(): Modifier =
    this.padding(horizontal = PtahSpacing.screenHorizontal)
