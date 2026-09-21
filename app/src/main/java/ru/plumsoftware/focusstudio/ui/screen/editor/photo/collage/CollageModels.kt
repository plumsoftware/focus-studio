package ru.plumsoftware.focusstudio.ui.screen.editor.photo.collage

import android.net.Uri
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import ru.plumsoftware.focusstudio.R

/**
 * Одна ячейка коллажа.
 * [zoom] — дополнительный зум поверх cover-fill (>= 1f), [panX]/[panY] — сдвиг в долях
 * размера ячейки. Такая нормализация позволяет одинаково рисовать и в превью (Compose),
 * и при сборке итогового bitmap (Canvas), не завися от реального размера холста.
 */
data class CollageCell(
    val uri: Uri? = null,
    val zoom: Float = 1f,
    val panX: Float = 0f,
    val panY: Float = 0f
)

/** Соотношение сторон всего коллажа (ширина / высота). */
enum class CollageAspect(val labelRes: Int, val ratio: Float) {
    SQUARE(R.string.ratio_1_1, 1f),
    PORTRAIT_3_4(R.string.ratio_3_4, 3f / 4f),
    PORTRAIT_9_16(R.string.ratio_9_16, 9f / 16f),
    LANDSCAPE_4_3(R.string.ratio_4_3, 4f / 3f),
    LANDSCAPE_16_9(R.string.ratio_16_9, 16f / 9f)
}

/**
 * Шаблон раскладки. [cells] — нормализованные прямоугольники (0..1) без учёта отступов.
 * [count] равен размеру [cells].
 */
data class CollageTemplate(
    val id: String,
    val count: Int,
    val cells: List<Rect>
)

/** Полное состояние конструктора коллажа. Размер [cells] всегда равен template.count. */
data class CollageState(
    val template: CollageTemplate,
    val cells: List<CollageCell>,
    val aspect: CollageAspect = CollageAspect.SQUARE,
    val gap: Float = 0.28f,      // 0..1, доля -> реальный отступ считается в рендере
    val corner: Float = 0.25f,   // 0..1, доля -> радиус скругления
    val background: Color = Color.White
)

/** Каталог шаблонов, сгруппированный по количеству фото. */
object CollageTemplates {

    private fun r(l: Float, t: Float, rr: Float, b: Float) = Rect(l, t, rr, b)

    val forTwo: List<CollageTemplate> = listOf(
        CollageTemplate("2_cols", 2, listOf(r(0f, 0f, 0.5f, 1f), r(0.5f, 0f, 1f, 1f))),
        CollageTemplate("2_rows", 2, listOf(r(0f, 0f, 1f, 0.5f), r(0f, 0.5f, 1f, 1f))),
        CollageTemplate("2_big_left", 2, listOf(r(0f, 0f, 0.64f, 1f), r(0.64f, 0f, 1f, 1f))),
        CollageTemplate("2_big_top", 2, listOf(r(0f, 0f, 1f, 0.64f), r(0f, 0.64f, 1f, 1f)))
    )

    val forThree: List<CollageTemplate> = listOf(
        CollageTemplate(
            "3_left_two", 3,
            listOf(r(0f, 0f, 0.6f, 1f), r(0.6f, 0f, 1f, 0.5f), r(0.6f, 0.5f, 1f, 1f))
        ),
        CollageTemplate(
            "3_top_two", 3,
            listOf(r(0f, 0f, 1f, 0.6f), r(0f, 0.6f, 0.5f, 1f), r(0.5f, 0.6f, 1f, 1f))
        ),
        CollageTemplate(
            "3_cols", 3,
            listOf(r(0f, 0f, 0.333f, 1f), r(0.333f, 0f, 0.666f, 1f), r(0.666f, 0f, 1f, 1f))
        ),
        CollageTemplate(
            "3_rows", 3,
            listOf(r(0f, 0f, 1f, 0.333f), r(0f, 0.333f, 1f, 0.666f), r(0f, 0.666f, 1f, 1f))
        )
    )

    val forFour: List<CollageTemplate> = listOf(
        CollageTemplate(
            "4_grid", 4,
            listOf(
                r(0f, 0f, 0.5f, 0.5f), r(0.5f, 0f, 1f, 0.5f),
                r(0f, 0.5f, 0.5f, 1f), r(0.5f, 0.5f, 1f, 1f)
            )
        ),
        CollageTemplate(
            "4_left_three", 4,
            listOf(
                r(0f, 0f, 0.6f, 1f),
                r(0.6f, 0f, 1f, 0.333f), r(0.6f, 0.333f, 1f, 0.666f), r(0.6f, 0.666f, 1f, 1f)
            )
        ),
        CollageTemplate(
            "4_top_three", 4,
            listOf(
                r(0f, 0f, 1f, 0.6f),
                r(0f, 0.6f, 0.333f, 1f), r(0.333f, 0.6f, 0.666f, 1f), r(0.666f, 0.6f, 1f, 1f)
            )
        ),
        CollageTemplate(
            "4_cols", 4,
            listOf(
                r(0f, 0f, 0.25f, 1f), r(0.25f, 0f, 0.5f, 1f),
                r(0.5f, 0f, 0.75f, 1f), r(0.75f, 0f, 1f, 1f)
            )
        )
    )

    fun byCount(count: Int): List<CollageTemplate> = when (count) {
        2 -> forTwo
        3 -> forThree
        else -> forFour
    }

    val availableCounts = listOf(2, 3, 4)

    /**
     * Меняет шаблон, сохраняя уже выбранные фото по индексам ячеек.
     * Лишние отбрасываются, недостающие становятся пустыми.
     */
    fun applyTemplate(state: CollageState, template: CollageTemplate): CollageState {
        val newCells = List(template.count) { index ->
            state.cells.getOrNull(index) ?: CollageCell()
        }
        return state.copy(template = template, cells = newCells)
    }
}
