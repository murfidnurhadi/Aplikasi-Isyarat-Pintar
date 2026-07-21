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
    val stroke: Color
) {
    object Palette1 : ThemeColors(
        background = Color(0xFFF6E2C3),
        headline = Color(0xFF002D54),
        secondary = Color(0xFFD0D5C1),
        button = Color(0xFF8B5E3C),
        buttonText = Color(0xFFF6E2C3),
        stroke = Color(0xFF002D54)
    )

    object Palette2 : ThemeColors(
        background = Color(0xFF332941),
        headline = Color(0xFFF8E559),
        secondary = Color(0xFF3B3486),
        button = Color(0xFF864AF9),
        buttonText = Color(0xFFF8E559),
        stroke = Color(0xFFF8E559)
    )

    object Palette3 : ThemeColors(
        background = Color(0xFFFFFFFF),
        headline = Color(0xFF000000),
        secondary = Color(0xFFE0E0E0),
        button = Color(0xFF000000),
        buttonText = Color(0xFFFFFFFF),
        stroke = Color(0xFF000000)
    )

    object Palette4 : ThemeColors(
        background = Color(0xFF001C30),
        headline = Color(0xFFF7C04A),
        secondary = Color(0xFF176B87),
        button = Color(0xFF64CCC5),
        buttonText = Color(0xFF001C30),
        stroke = Color(0xFFF7C04A)
    )

    companion object {
        fun getAll() = listOf(Palette3, Palette1, Palette2, Palette4)
    }
}

var HH_Background = ThemeColors.Palette3.background
var HH_Headline = ThemeColors.Palette3.headline
var HH_Paragraph = ThemeColors.Palette3.headline
var HH_Button = ThemeColors.Palette3.button
var HH_ButtonText = ThemeColors.Palette3.buttonText
var HH_Secondary = ThemeColors.Palette3.secondary
var HH_Tertiary = ThemeColors.Palette3.secondary
var HH_Stroke = ThemeColors.Palette3.stroke
var HH_Highlight = ThemeColors.Palette3.button

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
}

