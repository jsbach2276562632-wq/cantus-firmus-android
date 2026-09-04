package de.orgeljahr.demo.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Navy = Color(0xFF0E223D)
val Gold = Color(0xFFBE8F42)
val Ivory = Color(0xFFF8F5EE)
val Sage = Color(0xFF546F5B)
val LiturgicalGreen = Color(0xFF3A6948)

private val LightColors = lightColorScheme(
    primary = Navy,
    onPrimary = Color.White,
    secondary = Gold,
    onSecondary = Navy,
    tertiary = Sage,
    background = Ivory,
    surface = Color.White,
    surfaceVariant = Color(0xFFF0ECE3),
    onBackground = Color(0xFF1B1B1D),
    onSurface = Color(0xFF1B1B1D),
    onSurfaceVariant = Color(0xFF444446),
    outline = Color(0xFF68686B)
)

@Composable
fun OrgeljahrTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        // The organ-loft reader deliberately stays in a high-contrast light theme.
        // Several screens use a fixed ivory paper surface, so following the system
        // dark theme would place light text on that light background.
        colorScheme = LightColors,
        content = content
    )
}
