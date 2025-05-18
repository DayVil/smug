package com.github.smugapp.ui.screens

import androidx.compose.runtime.Composable
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import kotlinx.serialization.Serializable

private val TAG = "BarCodeScanner"

@Serializable
object BarCodeScannerRoute

@Composable
fun BarCodeScannerContent() {
    val client = HttpClient(CIO)
}