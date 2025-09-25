package com.gorokhov.polygonapicompose.presentation

import android.os.Parcelable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.TransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.onSizeChanged
import com.gorokhov.polygonapicompose.data.Bar
import kotlinx.parcelize.Parcelize
import kotlin.math.roundToInt

private const val MIN_VISIBLE_BARS_COUNT = 20

@Composable
fun Terminal(bars: List<Bar>) {

    var terminalState by rememberSaveable {
        mutableStateOf(TerminalState(bars = bars))
    }

    val transformableState = TransformableState { zoomChange, panChange, _ ->
        // Обновляем количество видимых баров с ограничением
        val visibleBarsCount = (terminalState.visibleBarsCount / zoomChange)
            .roundToInt()
            .coerceIn(MIN_VISIBLE_BARS_COUNT, bars.size)

        // Скролл
        val scrolledBy = (terminalState.scrolledBy + panChange.x)
            .coerceAtLeast(0f)
            .coerceAtMost(bars.size * terminalState.barWidth - terminalState.terminalWidth)

        terminalState = terminalState.copy(visibleBarsCount = visibleBarsCount, scrolledBy = scrolledBy)
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .transformable(transformableState)
            .onSizeChanged {
                terminalState = terminalState.copy(terminalWidth = it.width.toFloat())
            }
    ) {
        val max = terminalState.visibleBars.maxOf { it.high }
        val min = terminalState.visibleBars.minOf { it.low }
        val pixelPerPoint = size.height / (max - min)

        // В translate() мы передаём смещение, на которое нужно сместить график
        translate(left = terminalState.scrolledBy) {
            bars.forEachIndexed { index: Int, bar: Bar ->
                val offsetX = size.width - index * terminalState.barWidth
                drawLine(
                    color = Color.White,
                    start = Offset(x = offsetX, y = size.height - (bar.low - min) * pixelPerPoint),
                    end = Offset(x = offsetX, y = size.height - (bar.high - min) * pixelPerPoint),
                    strokeWidth = 2f
                )
                drawLine(
                    color = if (bar.open > bar.close) Color.Red else Color.Green,
                    start = Offset(x = offsetX, y = size.height - (bar.open - min) * pixelPerPoint),
                    end = Offset(x = offsetX, y = size.height - (bar.close - min) * pixelPerPoint),
                    strokeWidth = terminalState.barWidth / 2
                )
            }
        }
    }
}

// State для хранения состояния
@Parcelize
data class TerminalState(
    val bars: List<Bar>,
    val visibleBarsCount: Int = 100,
    val terminalWidth: Float = 0f,
    val scrolledBy: Float = 0f
): Parcelable {

    val barWidth: Float
        get() = terminalWidth / visibleBarsCount

    val visibleBars: List<Bar>
        get() {
            val startIndex = (scrolledBy / barWidth).roundToInt().coerceAtLeast(0)
            val endIndex = (startIndex + visibleBarsCount).coerceAtMost(bars.size)
            return bars.subList(fromIndex = startIndex, toIndex = endIndex)
        }
}

