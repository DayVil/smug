package com.github.smugapp.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import com.github.smugapp.data.SmugRepo
import com.github.smugapp.model.DrinkProduct
import com.github.smugapp.network.off.OffService
import com.github.smugapp.network.off.parseInput
import com.github.smugapp.ui.components.CameraPreview
import com.github.smugapp.ui.components.OffCard
import com.github.smugapp.ui.components.ScannerSearchBox
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

private const val TAG = "BarCodeScanner"

@Serializable
object BarCodeScannerRoute

sealed interface UiState {
    data object Init : UiState
    data object Loading : UiState
    data class Success(val data: List<DrinkProduct>) : UiState
    data class Error(val exception: Throwable) : UiState
}

@Composable
fun BarCodeScannerContent(repo: SmugRepo) {
    var uiState by remember { mutableStateOf<UiState>(UiState.Init) }
    var productId by remember { mutableStateOf("") }
    var isScanning by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val client = remember { OffService(repo) }
    val keyboardController = LocalSoftwareKeyboardController.current

    DisposableEffect(Unit) {
        onDispose {
            client.close()
            Log.d(TAG, "Client closed")
        }
    }

    fun searchAction() {
        if (productId.isBlank()) {
            return
        }

        if (uiState is UiState.Loading) {
            Log.d(TAG, "Already loading, ignoring request")
            return
        }
        uiState = UiState.Loading
        val searchTerm = parseInput(productId)
        scope.launch {
            val product = client.fetchProduct(searchTerm)
            uiState = if (product.isNotEmpty()) {
                UiState.Success(product)
            } else {
                UiState.Error(Exception("No product found"))
            }
        }
        keyboardController?.hide()
    }

    if (isScanning) {
        ScanRunning(
            onBarcodeDetected = { barcode ->
                Log.d(TAG, "Barcode detected: $barcode")
                productId = barcode
                isScanning = false
                scope.launch {
                    delay(100)
                    searchAction()
                }
            },
            setScanning = { isScanning = false }
        )
    } else {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(start = 20.dp, top = 16.dp, end = 20.dp)
                    .fillMaxWidth()
            ) {
                ScannerSearchBox(
                    productId = productId,
                    setProductId = { productId = it },
                    uiState = uiState,
                    onScanAction = { isScanning = true },
                    searchAction = { searchAction() }
                )

                when (val state = uiState) {
                    is UiState.Loading -> {
                        Text(text = "Loading...", modifier = Modifier.padding(top = 16.dp))
                    }

                    is UiState.Success -> {
                        OffCard(state.data[0])
                    }

                    is UiState.Error -> {
                        Text(
                            text = "Error: ${state.exception}",
                            modifier = Modifier.padding(top = 16.dp)
                        )
                        Log.d(TAG, "Error: ${state.exception}")
                    }

                    UiState.Init -> {
                        Text(
                            text = "Enter a product ID to search or tap camera to scan",
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ScanRunning(
    onBarcodeDetected: (String) -> Unit,
    setScanning: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        CameraPreview(
            onBarcodeDetected = onBarcodeDetected,
            modifier = Modifier.fillMaxSize()
        )

        IconButton(
            onClick = setScanning,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back to manual input",
                tint = Color.White
            )
        }

        Text(
            text = "Point camera at barcode to scan",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            color = Color.White
        )
    }
}