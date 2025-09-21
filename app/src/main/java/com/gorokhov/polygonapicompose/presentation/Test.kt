package com.gorokhov.polygonapicompose.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.gorokhov.polygonapicompose.data.Bar



@Composable
fun TestCanvas(bars: List<Bar>) {
    val padding = 16.dp
    val candleWidth = 8.dp

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(padding)
    ) {
        if (bars.isEmpty()) return@Canvas

        val canvasWidth = size.width
        val canvasHeight = size.height

        // Находим минимальное и максимальное значение цен для масштабирования
        val minPrice = bars.minOf { it.low }
        val maxPrice = bars.maxOf { it.high }
        val priceRange = maxPrice - minPrice

        // Рассчитываем ширину свечи и промежутки
        val candleSpacing = canvasWidth / (bars.size * 2f)
        val actualCandleWidth = minOf(candleWidth.toPx(), candleSpacing * 0.8f)

        // Функция для преобразования цены в координату Y
        fun priceToY(price: Float): Float {
            return canvasHeight - ((price - minPrice) / priceRange * canvasHeight)
        }

        // Рисуем сетку и оси
        drawLine(
            color = Color.Gray.copy(alpha = 0.5f),
            start = Offset(0f, 0f),
            end = Offset(0f, canvasHeight),
            strokeWidth = 1f
        )

        drawLine(
            color = Color.Gray.copy(alpha = 0.5f),
            start = Offset(0f, canvasHeight),
            end = Offset(canvasWidth, canvasHeight),
            strokeWidth = 1f
        )

        // Рисуем свечи
        bars.forEachIndexed { index, bar ->
            val x = candleSpacing * (index * 2 + 1)
            val openY = priceToY(bar.open)
            val closeY = priceToY(bar.close)
            val highY = priceToY(bar.high)
            val lowY = priceToY(bar.low)

            // Определяем цвет свечи (зеленая - рост, красная - падение)
            val isGrowing = bar.close >= bar.open
            val candleColor = if (isGrowing) Color.Green else Color.Red

            // Рисуем вертикальную линию (тень свечи)
            drawLine(
                color = candleColor,
                start = Offset(x, highY),
                end = Offset(x, lowY),
                strokeWidth = 1f
            )

            // Рисуем тело свечи
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
                // Для doji (open == close) рисуем горизонтальную линию
                drawLine(
                    color = candleColor,
                    start = Offset(x - actualCandleWidth / 2, openY),
                    end = Offset(x + actualCandleWidth / 2, openY),
                    strokeWidth = 2f
                )
            }
        }

        // Подписи цен
        drawContext.canvas.nativeCanvas.apply {
            drawText(
                "%.2f".format(maxPrice),
                0f,
                20f,
                android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 24f
                }
            )

            drawText(
                "%.2f".format(minPrice),
                0f,
                canvasHeight - 10f,
                android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 24f
                }
            )
        }
    }
}


