package com.gorokhov.polygonapicompose.presentation

import com.gorokhov.polygonapicompose.data.Bar

sealed class ScreenState {

    data object Initial : ScreenState()

    class Content(
        val barList: List<Bar>
    ) : ScreenState()
}