package com.github.smugapp.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.smugapp.off.OffService
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

private const val TAG = "BarCodeScanner"

@Serializable
object BarCodeScannerRoute

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val exception: Throwable) : UiState<Nothing>
}

@Composable
fun BarCodeScannerContent() {
    var uiState by remember { mutableStateOf<UiState<Any>>(UiState.Success("Unanswered")) }
    val scope = rememberCoroutineScope()
    val client = remember { OffService() }

    DisposableEffect(Unit) {
        onDispose {
            client.close()
            Log.d(TAG, "Client closed")
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(start = 20.dp, top = 16.dp, end = 20.dp)
        ) {
            Button(
                onClick = {
                    Log.d(TAG, "Running request")
                    scope.launch {
                        val product = client.fetchProduct("54491472")
                        uiState = product.fold(
                            onSuccess = {
                                Log.d(TAG, "Success: $it")
                                UiState.Success(it)
                            },
                            onFailure = {
                                Log.d(TAG, "Failure: $it")
                                UiState.Error(it)
                            }
                        )
                    }
                },
                enabled = uiState !is UiState.Loading
            ) {
                if (uiState is UiState.Loading) {
                    Text(text = "Loading...")
                } else {
                    Text(text = "Request")
                }
            }

            when (val state = uiState) {
                is UiState.Loading -> {
                    Text(text = "Loading...")
                }

                is UiState.Success -> {
                    Text(text = "Success: ${state.data}")
                    Log.d(TAG, "Success: ${state.data}")
                }

                is UiState.Error -> {
                    Text(text = "Error: ${state.exception}")
                    Log.d(TAG, "Error: ${state.exception}")
                }
            }
        }
    }
}

