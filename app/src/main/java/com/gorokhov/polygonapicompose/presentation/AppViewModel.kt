package com.gorokhov.polygonapicompose.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gorokhov.polygonapicompose.data.ApiFactory
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AppViewModel : ViewModel() {

    private val apiService = ApiFactory.apiService

    private val _screenState = MutableStateFlow<ScreenState>(ScreenState.Initial)
    val screenState: StateFlow<ScreenState> = _screenState.asStateFlow()

    private var _cachedState: ScreenState = ScreenState.Initial

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        _screenState.value = ScreenState.Loading // тут нужно обработать ошибку загрузки данных
    }

    init {
        loadBarList()
    }

    fun loadBarList(timeFrame: TimeFrame = TimeFrame.HOUR_1) {
        _cachedState = _screenState.value
        _screenState.value = ScreenState.Loading
        viewModelScope.launch(exceptionHandler) {
            val bars = apiService.loadBars(timeFrame = timeFrame.value).barList
            _screenState.value = ScreenState.Content(barList = bars, timeFrame = timeFrame)
        }
    }
}