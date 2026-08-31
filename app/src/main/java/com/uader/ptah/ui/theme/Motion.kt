package com.uader.ptah.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

object PtahMotion {
    // Animaciones para cambios de estado (cortas pero suaves)
    val stateTransition = tween<Float>(durationMillis = 250)
    val colorTransition = tween<androidx.compose.ui.graphics.Color>(durationMillis = 300)

    // Animaciones físicas (Spring) para elementos que el usuario toca o que aparecen
    fun <T> springBouncy() = spring<T>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )

    fun <T> springSmooth() = spring<T>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )
}
