package com.github.smugapp.image

import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

const val TAG = "BarCodeScanner"

class ImageAnalyzer(
    private val onBarcodeDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {
    @OptIn(ExperimentalGetImage::class)
    override fun analyze(proxyImage: ImageProxy) {
        val mediaImage = proxyImage.image

        if (mediaImage == null) {
            proxyImage.close()
            return
        }

        val image = InputImage.fromMediaImage(mediaImage, proxyImage.imageInfo.rotationDegrees)
        val options = buildBarcodeScannerOptions(allowedBarcodes().map { it.getFormat() })

        val scanner = BarcodeScanning.getClient(options)
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    val rawValue = barcode.rawValue
                    val format = allowedBarcodeFrom(barcode.format)
                    if (rawValue.isNullOrBlank() || format == null) {
                        Log.d(TAG, "Empty barcode detected")
                        continue
                    }

                    Log.d(TAG, "Found barcode: ${format}, format: ${format.getFormatName()}")

                    if (format.isValidProductCode(rawValue)) {
                        Log.d(TAG, "Valid product barcode detected: $rawValue")
                        onBarcodeDetected(rawValue)
                        break
                    } else {
                        Log.d(TAG, "Barcode format allowed but content invalid: $rawValue")
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Barcode scanning failed", exception)
            }
            .addOnCompleteListener {
                proxyImage.close()
            }
    }

    private fun buildBarcodeScannerOptions(formats: Collection<Int>): BarcodeScannerOptions {
        val firstFormat = formats.firstOrNull()!!
        val rest = formats.drop(1)

        return BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                firstFormat,
                *rest.toIntArray()
            )
            .enableAllPotentialBarcodes()
            .build()

    }
}