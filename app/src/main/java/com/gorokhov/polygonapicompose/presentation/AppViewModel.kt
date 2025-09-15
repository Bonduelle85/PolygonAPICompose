package com.gorokhov.polygonapicompose.presentation

import android.util.Log
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

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.d(
            "AppViewModel",
            "Exception caught by CoroutineExceptionHandler in AppViewModel: ${throwable.message}"
        )
    }

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch(exceptionHandler) {
            val bars = apiService.loadBars().barList
            _screenState.value = ScreenState.Content(bars)
        }
    }
}