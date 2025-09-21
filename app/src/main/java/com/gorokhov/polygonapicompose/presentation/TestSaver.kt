package com.gorokhov.polygonapicompose.presentation

import android.os.Parcelable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.parcelize.Parcelize


@Composable
fun TestSaver() {

    var testData by rememberSaveable(saver = TestDataSaverMany.Saver) {
        mutableStateOf(
            // TestDataParcelable(0)
            // TestDataSaverOne(0)
            TestDataSaverMany(0, "some text")
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable {
                testData = testData.copy(number = testData.number + 1)
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            color = Color.White,
            text = "Test ${testData.number}, ${testData.text}"
        )
    }
}

// 1. Parcelable - один из вариантов, если есть доступ к классу и можем "навесить" @Parcelize
@Parcelize
data class TestDataParcelable(val number: Int): Parcelable

// 2. Custom Saver с одним параметром
data class TestDataSaverOne(val number: Int) {

    companion object {
        val Saver: Saver<MutableState<TestDataSaverOne>, Int> = Saver(
            save = {
                it.value.number
            },
            restore = {
                mutableStateOf(TestDataSaverOne(it))
            }
        )
    }
}

// 3. Custom Saver с множеством параметров
data class TestDataSaverMany(val number: Int, val text: String) {

    companion object {
        val Saver: Saver<MutableState<TestDataSaverMany>, Any> = listSaver(
            save = {
                val state = it.value
                listOf(state.number, state.text)
            },
            restore = {
                val testData = TestDataSaverMany(
                    number = it[0] as Int,
                    text = it[1] as String
                )
                mutableStateOf(testData)
            }
        )
    }
}
