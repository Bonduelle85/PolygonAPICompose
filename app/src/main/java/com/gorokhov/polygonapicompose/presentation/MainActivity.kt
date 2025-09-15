package com.gorokhov.polygonapicompose.presentation

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gorokhov.polygonapicompose.ui.theme.PolygonAPIComposeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PolygonAPIComposeTheme {
                val viewModel: AppViewModel = viewModel()
                val screenState = viewModel.screenState.collectAsState()
                when(val currentState = screenState.value) {
                    is ScreenState.Content -> {
                        Log.d("MainActivity", "${currentState.barList}")
                    }
                    is ScreenState.Initial -> {

                    }
                }
            }
        }
    }
}
