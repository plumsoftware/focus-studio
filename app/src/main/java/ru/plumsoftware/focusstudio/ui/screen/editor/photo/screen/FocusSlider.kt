package ru.plumsoftware.focusstudio.ui.screen.editor.photo.screen

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.math.abs
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.plumsoftware.focusstudio.ui.theme.AccentBlue
import ru.plumsoftware.focusstudio.ui.theme.AccentGreen
import ru.plumsoftware.focusstudio.ui.theme.AppleGray
import ru.plumsoftware.focusstudio.ui.theme.FocusDesign
import ru.plumsoftware.focusstudio.ui.theme.iOSBlue

@Composable
fun FocusSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float> = -100f..100f,
    onValueChange: (Float) -> Unit
) {
    val view = LocalView.current
    var lastValue by remember { mutableFloatStateOf(value) }
    LaunchedEffect(value) {
        if (abs(lastValue - value) > 0.5f) {
            lastValue = value
        }
    }
    val displayValue = if (value > 0) "+${value.toInt()}" else "${value.toInt()}"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = FocusDesign.paddingSmall)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = AppleGray
            )
            Text(
                text = displayValue,
                style = MaterialTheme.typography.labelSmall,
                color = iOSBlue,
                fontWeight = FontWeight.Bold
            )
        }

        CenterZeroSlider(
            value = value,
            valueRange = valueRange,
            onValueChange = { newValue ->
                val crossedZero =
                    (lastValue < 0f && newValue >= 0f) || (lastValue > 0f && newValue <= 0f)
                if (crossedZero) {
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                }
                lastValue = newValue
                onValueChange(newValue)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(FocusDesign.sliderHeight)
        )
    }
}

@Composable
private fun CenterZeroSlider(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val trackHeight = with(density) { FocusDesign.sliderTrackHeight.toPx() }
    val thumbRadius = with(density) { FocusDesign.sliderThumbSize.toPx() / 2f }
    val tickWidth = with(density) { 2.dp.toPx() }
    val tickHeight = with(density) { 16.dp.toPx() }
    val zero = 0f
    val rangeSpan = valueRange.endInclusive - valueRange.start

    BoxWithConstraints(modifier = modifier) {
        val trackWidth = constraints.maxWidth.toFloat()

        fun valueToFraction(v: Float): Float =
            ((v - valueRange.start) / rangeSpan).coerceIn(0f, 1f)

        fun fractionToValue(f: Float): Float =
            (valueRange.start + f * rangeSpan).coerceIn(valueRange.start, valueRange.endInclusive)

        val zeroFraction = valueToFraction(zero)
        val thumbFraction = valueToFraction(value)
        val thumbX = thumbFraction * trackWidth

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(FocusDesign.sliderHeight)
                .pointerInput(valueRange, trackWidth) {
                    detectTapGestures { offset ->
                        val x = offset.x.coerceIn(0f, trackWidth)
                        onValueChange(fractionToValue(x / trackWidth))
                    }
                }
                .pointerInput(valueRange, trackWidth) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val x = change.position.x.coerceIn(0f, trackWidth)
                        onValueChange(fractionToValue(x / trackWidth))
                    }
                }
        ) {
            Canvas(modifier = Modifier
                .fillMaxWidth()
                .height(FocusDesign.sliderHeight)
                .align(Alignment.Center)) {
                val centerY = size.height / 2f
                val trackTop = centerY - trackHeight / 2f
                val zeroX = zeroFraction * size.width

                drawRoundRect(
                    color = Color.DarkGray.copy(alpha = 0.3f),
                    topLeft = Offset(0f, trackTop),
                    size = Size(size.width, trackHeight),
                    cornerRadius = CornerRadius(trackHeight / 2f)
                )

                if (value < zero) {
                    val leftStart = thumbX
                    val leftWidth = (zeroX - thumbX).coerceAtLeast(0f)
                    drawRoundRect(
                        color = AccentBlue,
                        topLeft = Offset(leftStart, trackTop),
                        size = Size(leftWidth, trackHeight),
                        cornerRadius = CornerRadius(trackHeight / 2f)
                    )
                } else if (value > zero) {
                    val rightWidth = (thumbX - zeroX).coerceAtLeast(0f)
                    drawRoundRect(
                        color = AccentGreen,
                        topLeft = Offset(zeroX, trackTop),
                        size = Size(rightWidth, trackHeight),
                        cornerRadius = CornerRadius(trackHeight / 2f)
                    )
                }

                drawRoundRect(
                    color = Color.White.copy(alpha = 0.6f),
                    topLeft = Offset(zeroX - tickWidth / 2f, centerY - tickHeight / 2f),
                    size = Size(tickWidth, tickHeight),
                    cornerRadius = CornerRadius(1f)
                )

                drawCircle(
                    color = Color.White,
                    radius = thumbRadius,
                    center = Offset(thumbX, centerY)
                )
            }
        }
    }
}
