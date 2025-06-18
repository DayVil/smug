package com.github.smugapp.ui.components

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.github.smugapp.image.ImageAnalyzer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

private const val TAG = "CameraPreview"

@Composable
fun CameraPreview(
    onBarcodeDetected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            startCamera(ctx, lifecycleOwner, previewView, cameraExecutor, onBarcodeDetected)
            previewView
        },
        modifier = modifier.fillMaxSize()
    )
}

private fun startCamera(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    cameraExecutor: ExecutorService,
    onBarcodeDetected: (String) -> Unit
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

    cameraProviderFuture.addListener(
        {
            try {
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview
                    .Builder()
                    .build()
                    .also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                val imageAnalyzer = ImageAnalysis
                    .Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor, ImageAnalyzer(onBarcodeDetected))
                    }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalyzer
                )

                Log.d(TAG, "Camera started successfully")

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        },
        ContextCompat.getMainExecutor(context)
    )
} 