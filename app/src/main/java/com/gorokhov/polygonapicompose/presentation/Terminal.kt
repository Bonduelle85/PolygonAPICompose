package com.gorokhov.polygonapicompose.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.TransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import com.gorokhov.polygonapicompose.data.Bar
import kotlin.math.roundToInt

private const val MIN_VISIBLE_BARS_COUNT = 20

@Composable
fun Terminal(bars: List<Bar>) {

    var visibleBarsCount by remember { mutableStateOf(100) }

    var scrolledBy by remember { mutableStateOf(0f) }

    var terminalWidth by remember { mutableStateOf(0f) }

    val barWidth by remember { // Стейт зависит от двух других стейтов
        derivedStateOf {
            terminalWidth / visibleBarsCount
        }
    }

    val visibleBars by remember {
        derivedStateOf {
            val startIndex = (scrolledBy / barWidth).roundToInt().coerceAtLeast(0)
            val endIndex = (startIndex + visibleBarsCount).coerceAtMost(bars.size)
            bars.subList(fromIndex = startIndex, toIndex = endIndex)
        }
    }

    val transformableState = TransformableState { zoomChange, panChange, _ ->
        // Обновляем количество видимых баров с ограничением
        visibleBarsCount = (visibleBarsCount / zoomChange)
            .roundToInt()
            .coerceIn(MIN_VISIBLE_BARS_COUNT, bars.size)

        // Скролл
        scrolledBy = (scrolledBy + panChange.x)
            .coerceAtLeast(0f)
            .coerceAtMost(bars.size * barWidth - terminalWidth)
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .transformable(transformableState)
    ) {
        terminalWidth = size.width
        val max = visibleBars.maxOf { it.high }
        val min = visibleBars.minOf { it.low }
        val pixelPerPoint = size.height / (max - min)

        // В translate() мы передаём смещение, на которое нужно сместить график
        translate(left = scrolledBy) {
            bars.forEachIndexed { index: Int, bar: Bar ->
                val offsetX = size.width - index * barWidth
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
                    strokeWidth = barWidth / 2
                )
            }
        }
    }
}