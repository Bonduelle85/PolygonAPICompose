package com.gorokhov.polygonapicompose.presentation

import android.os.Parcelable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gorokhov.polygonapicompose.data.Bar
import kotlinx.parcelize.Parcelize
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

private const val MIN_VISIBLE_BARS_COUNT = 20

@Composable
fun Terminal(
    modifier: Modifier = Modifier,
) {
    val viewModel: AppViewModel = viewModel()
    val screenState = viewModel.screenState.collectAsState()

    when (val currentState = screenState.value) {
        is ScreenState.Initial -> {}
        is ScreenState.Loading -> {
            ShowProgress()
        }

        is ScreenState.Content -> {
            // Паттерн "Передача MutableState\State": этот паттерн идеально подходит для высокочастотных,
            // локальных обновлений, которые не должны затрагивать весь экран. MutableState
            // реализуется без Callback. В 95% остальных случаев производительности паттерна
            // "Состояние + Callback" (передача TerminalState через делегат с by) более чем достаточно.
            val terminalState: MutableState<TerminalState> =
                rememberTerminalState(bars = currentState.barList)

            Chart(
                modifier = modifier,
                terminalState = terminalState,
                timeFrame = currentState.timeFrame,
                onTerminalStateChanged = {
                    terminalState.value = it // about callback in 10.14 lesson
                }
            )

            currentState.barList.firstOrNull()?.let { bar ->
                Prices(
                    modifier = modifier,
                    terminalState = terminalState,
                    lastPrice = bar.close
                )
            }
            TimeFrames(
                selectedFrame = currentState.timeFrame,
                onTimeFrameSelected = {
                    viewModel.loadBarList(it)
                }
            )
        }
    }
}

@Composable
private fun TimeFrames(
    selectedFrame: TimeFrame,
    onTimeFrameSelected: (TimeFrame) -> Unit
) {
    Row(
        modifier = Modifier
            .wrapContentSize()
            .padding(32.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        TimeFrame.entries.forEach { timeFrame ->
            val isSelected = timeFrame == selectedFrame
            AssistChip(
                onClick = {
                    onTimeFrameSelected(timeFrame)
                },
                label = {
                    Text(timeFrame.label)
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (isSelected) Color.White else Color.Black,
                    labelColor = if (isSelected) Color.Black else Color.White,
                )
            )
        }
    }
}

@Composable
private fun Chart(
    modifier: Modifier = Modifier,
    terminalState: State<TerminalState>,
    timeFrame: TimeFrame,
    onTerminalStateChanged: (TerminalState) -> Unit
) {
    val currentState = terminalState.value

    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        // Обновляем количество видимых баров с ограничением
        val visibleBarsCount = (currentState.visibleBarsCount / zoomChange)
            .roundToInt()
            .coerceIn(MIN_VISIBLE_BARS_COUNT, currentState.bars.size)

        // Scroll
        val scrolledBy = (currentState.scrolledBy + panChange.x)
            .coerceAtLeast(0f)
            .coerceAtMost(currentState.bars.size * currentState.barWidth - currentState.terminalWidth)

        onTerminalStateChanged(
            currentState.copy(
                visibleBarsCount = visibleBarsCount,
                scrolledBy = scrolledBy
            )
        )

    }

    val textMeasurer = rememberTextMeasurer()

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clipToBounds()
            .padding(vertical = 32.dp)
            .transformable(transformableState)
            .onSizeChanged {
                onTerminalStateChanged(
                    currentState.copy(
                        terminalWidth = it.width.toFloat(),
                        terminalHeight = it.height.toFloat()
                    )
                )
            }
    ) {
        val min = currentState.min
        val pixelPerPoint = currentState.pixelPerPoint

        // В translate() мы передаём смещение, на которое нужно сместить график
        translate(left = currentState.scrolledBy) {
            currentState.bars.forEachIndexed { index: Int, bar: Bar ->
                val offsetX = size.width - index * currentState.barWidth

                drawTimeDelimiter(
                    bar = bar,
                    nextBar = if (index < currentState.bars.size - 1) {
                        currentState.bars[index + 1]
                    } else {
                        null
                    },
                    timeFrame = timeFrame,
                    offsetX = offsetX,
                    textMeasurer = textMeasurer
                )

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
                    strokeWidth = currentState.barWidth / 2
                )
            }
        }
    }
}

@Composable
private fun Prices(
    modifier: Modifier = Modifier,
    terminalState: State<TerminalState>,
    lastPrice: Float
) {
    val currentState = terminalState.value

    val max: Float = currentState.max
    val min: Float = currentState.min
    val pxPerPoint: Float = currentState.pixelPerPoint

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

private fun DrawScope.drawTimeDelimiter(
    bar: Bar,
    nextBar: Bar?,
    timeFrame: TimeFrame,
    offsetX: Float,
    textMeasurer: TextMeasurer
) {
    val calendar = bar.calendar

    val minutes = calendar.get(Calendar.MINUTE)
    val hours = calendar.get(Calendar.HOUR_OF_DAY)
    val day = calendar.get(Calendar.DAY_OF_MONTH)

    val shouldDrawDelimiter = when (timeFrame) {
        TimeFrame.MIN_5 -> {
            minutes == 0
        }

        TimeFrame.MIN_15 -> {
            (minutes == 0) && (hours % 2 == 0)
        }

        TimeFrame.MIN_30, TimeFrame.HOUR_1 -> {
            val nextBarDay = nextBar?.calendar?.get(Calendar.DAY_OF_MONTH)
            nextBarDay != day
        }
    }
    if (!shouldDrawDelimiter) return

    drawLine(
        color = Color.White.copy(alpha = 0.5f),
        start = Offset(x = offsetX, y = 0f),
        end = Offset(x = offsetX, y = size.height),
        strokeWidth = 1f,
        pathEffect = PathEffect.dashPathEffect(intervals = floatArrayOf(4.dp.toPx(), 4.dp.toPx()))
    )
    val locale = Locale.getDefault()
    val nameOfMonth = calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, locale)
    val text = when (timeFrame) {
        TimeFrame.MIN_5, TimeFrame.MIN_15 -> {
            String.format(locale, "%02d:00", hours)
        }

        TimeFrame.MIN_30, TimeFrame.HOUR_1 -> {
            String.format("%s %s", day, nameOfMonth)
        }
    }
    val textLayoutResult: TextLayoutResult = textMeasurer.measure(
        text = text,
        style = TextStyle(color = Color.White, fontSize = 12.sp)
    )
    drawText(
        textLayoutResult = textLayoutResult,
        topLeft = Offset(
            offsetX - textLayoutResult.size.width / 2,
            size.height
        )
    )
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
private fun ShowProgress() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = Color.White)
    }
}

@Composable
fun rememberTerminalState(bars: List<Bar>): MutableState<TerminalState> {
    return rememberSaveable {
        mutableStateOf(TerminalState(bars = bars))
    }
}


