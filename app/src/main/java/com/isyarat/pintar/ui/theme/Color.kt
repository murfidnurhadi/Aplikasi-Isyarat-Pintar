package com.isyarat.pintar.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Happy Hues Palette Themes
 */
sealed class ThemeColors(
    val background: Color,
    val headline: Color,
    val secondary: Color,
    val button: Color,
    val buttonText: Color,
    val stroke: Color,
    val navIndicator: Color
) {
    object PaletteLight : ThemeColors(
        background = Color(0xFFE3F2FD), // Biru langit ceria
        headline = Color(0xFF0D47A1), // Biru tua kontras
        secondary = Color(0xFFFFF9C4), // Kuning cerah pastel
        button = Color(0xFFFF9800), // Oranye mencolok
        buttonText = Color(0xFFFFFFFF), // Putih
        stroke = Color.Transparent, // Border dihilangkan (modern)
        navIndicator = Color(0xFFFF9800) // Oranye untuk Light Mode
    )

    object PaletteDark : ThemeColors(
        background = Color(0xFF1E1E1E), // Hitam lembut (Soft Black / Dark Grey)
        headline = Color(0xFFF5F5F5), // Putih lembut
        secondary = Color(0xFF2C2C2C), // Abu-abu gelap untuk card
        button = Color(0xFFFF9800), // Oranye ceria
        buttonText = Color(0xFF1E1E1E), // Teks gelap di tombol
        stroke = Color.Transparent, // Border dihilangkan
        navIndicator = Color(0xFF0D47A1) // Biru Tua untuk Dark Mode
    )

    companion object {
        fun getAll() = listOf(PaletteLight, PaletteDark)
    }
}

var HH_Background = ThemeColors.PaletteLight.background
var HH_Headline = ThemeColors.PaletteLight.headline
var HH_Paragraph = ThemeColors.PaletteLight.headline
var HH_Button = ThemeColors.PaletteLight.button
var HH_ButtonText = ThemeColors.PaletteLight.buttonText
var HH_Secondary = ThemeColors.PaletteLight.secondary
var HH_Tertiary = ThemeColors.PaletteLight.secondary
var HH_Stroke = ThemeColors.PaletteLight.stroke
var HH_Highlight = ThemeColors.PaletteLight.button
var HH_NavIndicator = ThemeColors.PaletteLight.navIndicator

fun updateThemeColors(theme: ThemeColors) {
    HH_Background = theme.background
    HH_Headline = theme.headline
    HH_Paragraph = theme.headline
    HH_Button = theme.button
    HH_ButtonText = theme.buttonText
    HH_Secondary = theme.secondary
    HH_Tertiary = theme.secondary
    HH_Stroke = theme.stroke
    HH_Highlight = theme.button
    HH_NavIndicator = theme.navIndicator
}
