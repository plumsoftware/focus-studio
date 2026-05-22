package ru.plumsoftware.focusstudio.ui.screen.editor.photo

import android.content.Context
import android.graphics.Typeface
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.core.content.res.ResourcesCompat
import ru.plumsoftware.focusstudio.R

fun Color.toHex(): String {
    val r = (this.red * 255).toInt()
    val g = (this.green * 255).toInt()
    val b = (this.blue * 255).toInt()
    return String.format("#%02X%02X%02X", r, g, b)
}

fun String.parseColor(): Color? {
    return try {
        val hex = if (!this.startsWith("#")) "#$this" else this
        Color(hex.toColorInt())
    } catch (e: Exception) {
        null
    }
}

fun getFontFamily(name: String): FontFamily {
    return when (name.lowercase()) {
        "serif" -> FontFamily.Serif
        "sans serif" -> FontFamily.SansSerif
        "monospace" -> FontFamily.Monospace
        "cursive" -> FontFamily.Cursive
        "sf pro" -> FontFamily(Font(ru.plumsoftware.focusstudio.R.font.sf_pro_regular))
        "google sans" -> FontFamily(Font(ru.plumsoftware.focusstudio.R.font.google_sans))
        "passions conflict" -> FontFamily(Font(ru.plumsoftware.focusstudio.R.font.passions_conflict))
        "ruthless sketch" -> FontFamily(Font(ru.plumsoftware.focusstudio.R.font.ruthless_sketch))
        "montserrat underline" -> FontFamily(Font(ru.plumsoftware.focusstudio.R.font.montserrat_underline))
        "old soviet" -> FontFamily(Font(ru.plumsoftware.focusstudio.R.font.old_soviet))
        "aa stetica" -> FontFamily(Font(ru.plumsoftware.focusstudio.R.font.aa_stetica))
        "accidental presidency" -> FontFamily(Font(ru.plumsoftware.focusstudio.R.font.accid))
        else -> FontFamily.Default
    }
}

fun getAndroidTypeface(context: Context, name: String): Typeface {
    return when (name.lowercase()) {
        "serif" -> Typeface.SERIF
        "sans serif" -> Typeface.SANS_SERIF
        "monospace" -> Typeface.MONOSPACE
        "cursive" -> Typeface.create("cursive", Typeface.NORMAL)

        // Кастомные шрифты из ресурсов
        "sf pro" -> ResourcesCompat.getFont(context, R.font.sf_pro_regular)
        "google sans" -> ResourcesCompat.getFont(context, R.font.google_sans)
        "passions conflict" -> ResourcesCompat.getFont(context, R.font.passions_conflict)
        "ruthless sketch" -> ResourcesCompat.getFont(context, R.font.ruthless_sketch)
        "montserrat underline" -> ResourcesCompat.getFont(context, R.font.montserrat_underline)
        "old soviet" -> ResourcesCompat.getFont(context, R.font.old_soviet)

        else -> Typeface.DEFAULT
    } ?: Typeface.DEFAULT
}