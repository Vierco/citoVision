package dev.lovelace.citovision.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import citovision.shared.generated.resources.Res
import citovision.shared.generated.resources.Dongle_Bold
import citovision.shared.generated.resources.Dongle_Light
import citovision.shared.generated.resources.Dongle_Regular
import org.jetbrains.compose.resources.Font

@Composable
fun getTypography(): Typography {
    val dongleFontFamily = FontFamily(
        Font(Res.font.Dongle_Regular, FontWeight.Normal),
        Font(Res.font.Dongle_Bold, FontWeight.Bold),
        Font(Res.font.Dongle_Light, FontWeight.Light)
    )

    return Typography(
        displayLarge = TextStyle(
            fontFamily = dongleFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 42.sp
        ),
        headlineLarge = TextStyle(
            fontFamily = dongleFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 34.sp
        ),
        headlineMedium = TextStyle(
            fontFamily = dongleFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 30.sp
        ),
        titleLarge = TextStyle(
            fontFamily = dongleFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 24.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = dongleFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 20.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = dongleFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 18.sp
        ),
        labelLarge = TextStyle(
            fontFamily = dongleFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 18.sp
        )
    )
}
