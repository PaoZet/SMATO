package com.example.smato.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// Esquema de colores personalizado para el modo oscuro de la aplicación SMATO,
// configurando la paleta cromática principal en base a tonos púrpura neón, cian y superficies oscuras.
private val DarkColorScheme = darkColorScheme(
    primary = NeonPurple,
    secondary = AquaCyan,
    background = DarkBackground,
    surface = SurfaceDark,
    onPrimary = TextWhite,
    onSecondary = DarkBackground,
    onBackground = TextWhite,
    onSurface = TextWhite
)

// Componente contenedor de tema que aplica el esquema de colores personalizado
// y los estilos visuales de Material Design 3 a toda la jerarquía de interfaces.
@Composable
fun SMATOTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}