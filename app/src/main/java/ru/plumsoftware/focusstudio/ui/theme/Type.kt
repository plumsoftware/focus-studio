package ru.plumsoftware.focusstudio.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import ru.plumsoftware.focusstudio.R

val SFPro = FontFamily(
    Font(R.font.sf_pro_regular, FontWeight.Normal),
    Font(R.font.sf_pro_medium, FontWeight.Medium),
    Font(R.font.sf_pro_bold, FontWeight.Bold)
)

val FocusTypography = Typography(
    // Заголовок "Фокус студио"
    displayMedium = TextStyle(
        fontFamily = SFPro,
        fontWeight = FontWeight.Bold,
        fontSize = 42.sp,
        letterSpacing = (-1).sp
    ),
    // Заголовки в карточках "ФОТО"
    headlineSmall = TextStyle(
        fontFamily = SFPro,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        letterSpacing = 0.5.sp
    ),
    // Текст описания
    bodyMedium = TextStyle(
        fontFamily = SFPro,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        color = AppleGray // См. ниже в Color
    ),
    // Badge "ПРОФЕССИОНАЛЬНАЯ СТУДИЯ"
    labelSmall = TextStyle(
        fontFamily = SFPro,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        letterSpacing = 1.sp
    ),
    // Технические характеристики снизу
    labelMedium = TextStyle(
        fontFamily = SFPro,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        letterSpacing = 0.5.sp
    )
)