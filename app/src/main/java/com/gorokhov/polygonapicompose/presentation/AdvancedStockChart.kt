package com.gorokhov.polygonapicompose.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.gorokhov.polygonapicompose.data.Bar

@Composable
fun AdvancedStockChart(bars: List<Bar>) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(0f) }
    var visibleBars by remember { mutableStateOf(50) } // Количество видимых свечей

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.5f, 5f)
                    offset += pan.x
                    visibleBars = (50 / scale).toInt().coerceIn(10, bars.size)
                }
            }
    ) {
        val visibleBarsList = bars.takeLast(visibleBars)

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Аналогичная логика рисования, но для visibleBarsList
            // Добавляем обработку offset для прокрутки
            if (visibleBarsList.isEmpty()) return@Canvas

            val canvasWidth = size.width
            val canvasHeight = size.height

            val minPrice = visibleBarsList.minOf { it.low }
            val maxPrice = visibleBarsList.maxOf { it.high }
            val priceRange = maxPrice - minPrice

            val candleSpacing = canvasWidth / (visibleBarsList.size * 2f)
            val actualCandleWidth = minOf(8f, candleSpacing * 0.8f)

            fun priceToY(price: Float): Float {
                return canvasHeight - ((price - minPrice) / priceRange * canvasHeight)
            }

            // Остальная логика рисования аналогична предыдущему примеру
            visibleBarsList.forEachIndexed { index, bar ->
                val x = candleSpacing * (index * 2 + 1) + offset
                // Пропускаем свечи за пределами экрана
                if (x in -actualCandleWidth..canvasWidth + actualCandleWidth) {
                    val openY = priceToY(bar.open)
                    val closeY = priceToY(bar.close)
                    val highY = priceToY(bar.high)
                    val lowY = priceToY(bar.low)

                    val isGrowing = bar.close >= bar.open
                    val candleColor = if (isGrowing) Color.Green else Color.Red

                    drawLine(
                        color = candleColor,
                        start = Offset(x, highY),
                        end = Offset(x, lowY),
                        strokeWidth = 1f
                    )

                    val bodyTop = minOf(openY, closeY)
                    val bodyBottom = maxOf(openY, closeY)
                    val bodyHeight = bodyBottom - bodyTop

                    if (bodyHeight > 0) {
                        drawRect(
                            color = candleColor,
                            topLeft = Offset(x - actualCandleWidth / 2, bodyTop),
                            size = Size(actualCandleWidth, bodyHeight)
                        )
                    } else {
                        drawLine(
                            color = candleColor,
                            start = Offset(x - actualCandleWidth / 2, openY),
                            end = Offset(x + actualCandleWidth / 2, openY),
                            strokeWidth = 2f
                        )
                    }
                }
            }
        }
    }
}