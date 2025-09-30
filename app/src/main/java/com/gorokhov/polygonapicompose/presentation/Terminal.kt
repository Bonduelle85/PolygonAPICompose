package com.gorokhov.polygonapicompose.presentation

import android.os.Parcelable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gorokhov.polygonapicompose.data.Bar
import kotlinx.parcelize.Parcelize
import kotlin.math.roundToInt

private const val MIN_VISIBLE_BARS_COUNT = 20

@Composable
fun Terminal(
    modifier: Modifier = Modifier,
    bars: List<Bar>
) {

    var terminalState by rememberTerminalState(bars)

    Chart(
        modifier = modifier,
        terminalState = terminalState,
        onTerminalStateChanged = {
            terminalState = it // about callback in 10.14 lesson
        }
    )

    bars.firstOrNull()?.let {
        Prices(
            modifier = modifier,
            max = terminalState.max,
            min = terminalState.min,
            pxPerPoint = terminalState.pixelPerPoint,
            lastPrice = it.close
        )
    }
}

@Composable
private fun Chart(
    modifier: Modifier = Modifier,
    terminalState: TerminalState,
    onTerminalStateChanged: (TerminalState) -> Unit
) {
    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        // Обновляем количество видимых баров с ограничением
        val visibleBarsCount = (terminalState.visibleBarsCount / zoomChange)
            .roundToInt()
            .coerceIn(MIN_VISIBLE_BARS_COUNT, terminalState.bars.size)

        // Скролл
        val scrolledBy = (terminalState.scrolledBy + panChange.x)
            .coerceAtLeast(0f)
            .coerceAtMost(terminalState.bars.size * terminalState.barWidth - terminalState.terminalWidth)

        onTerminalStateChanged(
            terminalState.copy(
                visibleBarsCount = visibleBarsCount,
                scrolledBy = scrolledBy
            )
        )

    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clipToBounds()
            .padding(vertical = 32.dp)
            .transformable(transformableState)
            .onSizeChanged {
                onTerminalStateChanged(
                    terminalState.copy(
                        terminalWidth = it.width.toFloat(),
                        terminalHeight = it.height.toFloat()
                    )
                )
            }
    ) {
        val min = terminalState.min
        val pixelPerPoint = terminalState.pixelPerPoint

        // В translate() мы передаём смещение, на которое нужно сместить график
        translate(left = terminalState.scrolledBy) {
            terminalState.bars.forEachIndexed { index: Int, bar: Bar ->
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

@Composable
private fun Prices(
    modifier: Modifier = Modifier,
    max: Float,
    min: Float,
    pxPerPoint: Float,
    lastPrice: Float
) {
    val textMeasurer: TextMeasurer = rememberTextMeasurer()

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .padding(vertical = 32.dp)
    ) {
        drawPrices(max, min, pxPerPoint, lastPrice, textMeasurer)
    }
}

private fun DrawScope.drawPrices(
    max: Float,
    min: Float,
    pxPerPoint: Float,
    lastPrice: Float,
    textMeasurer: TextMeasurer
) {

    // max
    drawDashedLine(
        start = Offset(x = 0f, y = 0f),
        end = Offset(x = size.width, y = 0f),
    )
    val textLayoutResultMax: TextLayoutResult = textMeasurer.measure(
        text = max.toString(),
        style = TextStyle(color = Color.White, fontSize = 12.sp)
    )
    drawText(
        textLayoutResult = textLayoutResultMax,
        topLeft = Offset(
            size.width - textLayoutResultMax.size.width - 4.dp.toPx(),
            0f
        )
    )

    // last price
    drawDashedLine(
        start = Offset(x = 0f, y = size.height - (lastPrice - min) * pxPerPoint),
        end = Offset(x = size.width, y = size.height - (lastPrice - min) * pxPerPoint),
    )
    val textLayoutResultLast: TextLayoutResult = textMeasurer.measure(
        text = lastPrice.toString(),
        style = TextStyle(color = Color.White, fontSize = 12.sp)
    )
    drawText(
        textLayoutResult = textLayoutResultLast,
        topLeft = Offset(
            size.width - textLayoutResultMax.size.width - 4.dp.toPx(),
            (size.height - (lastPrice - min) * pxPerPoint)
        )
    )

    // min
    drawDashedLine(
        start = Offset(x = 0f, y = size.height),
        end = Offset(x = size.width, y = size.height),
    )
    val textLayoutResultMin: TextLayoutResult = textMeasurer.measure(
        text = min.toString(),
        style = TextStyle(color = Color.White, fontSize = 12.sp)
    )
    drawText(
        textLayoutResult = textLayoutResultMin,
        topLeft = Offset(
            size.width - textLayoutResultMax.size.width - 4.dp.toPx(),
            size.height
        )
    )
}

private fun DrawScope.drawDashedLine(
    start: Offset,
    end: Offset,
    color: Color = Color.White,
    strokeWidth: Float = 2f,
    pathEffect: PathEffect = PathEffect.dashPathEffect(
        intervals = floatArrayOf(4.dp.toPx(), 4.dp.toPx()) // пунктир - полоска и пропуск
    )
) {
    drawLine(
        color = color,
        start = start,
        end = end,
        strokeWidth = strokeWidth,
        pathEffect = pathEffect
    )
}


// State для хранения состояния (должен лежать в отдельном файле)
@Parcelize
data class TerminalState(
    val bars: List<Bar>,
    val visibleBarsCount: Int = 100,
    val terminalWidth: Float = 1f,
    val terminalHeight: Float = 1f,
    val scrolledBy: Float = 0f
) : Parcelable {

    val barWidth: Float
        get() = terminalWidth / visibleBarsCount

    private val visibleBars: List<Bar>
        get() {
            val startIndex = (scrolledBy / barWidth).roundToInt().coerceAtLeast(0)
            val endIndex = (startIndex + visibleBarsCount).coerceAtMost(bars.size)
            return bars.subList(fromIndex = startIndex, toIndex = endIndex)
        }

    val max: Float
        get() = visibleBars.maxOf { it.high }
    val min: Float
        get() = visibleBars.minOf { it.low }
    val pixelPerPoint: Float
        get() = terminalHeight / (max - min)
}

@Composable
fun rememberTerminalState(bars: List<Bar>): MutableState<TerminalState> {
    return rememberSaveable {
        mutableStateOf(TerminalState(bars = bars))
    }
}


