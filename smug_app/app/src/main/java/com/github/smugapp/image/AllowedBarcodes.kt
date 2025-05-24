package com.github.smugapp.image

import com.google.mlkit.vision.barcode.common.Barcode

sealed interface AllowedBarcodes {
    fun isValidProductCode(value: String): Boolean
    fun getFormat(): Int
    fun getFormatName(): String

    data object UpcA : AllowedBarcodes {
        override fun isValidProductCode(value: String): Boolean {
            return value.length == 12 && value.all { it.isDigit() }
        }

        override fun getFormat() = Barcode.FORMAT_UPC_A

        override fun getFormatName() = "UPC-A"
    }

    data object UpcE : AllowedBarcodes {
        override fun isValidProductCode(value: String): Boolean {
            return value.length == 8 && value.all { it.isDigit() }
        }

        override fun getFormat() = Barcode.FORMAT_UPC_E

        override fun getFormatName() = "UPC-E"
    }

    data object EAN13 : AllowedBarcodes {
        override fun isValidProductCode(value: String): Boolean {
            return value.length == 13 && value.all { it.isDigit() }
        }

        override fun getFormat() = Barcode.FORMAT_EAN_13

        override fun getFormatName() = "EAN-13"
    }

    data object QRCode : AllowedBarcodes {
        override fun isValidProductCode(value: String): Boolean {
            return value.isNotBlank()
        }

        override fun getFormat() = Barcode.FORMAT_QR_CODE

        override fun getFormatName() = "QR Code"
    }

    data object DataMatrix : AllowedBarcodes {
        override fun isValidProductCode(value: String): Boolean {
            return value.isNotBlank()
        }

        override fun getFormat() = Barcode.FORMAT_DATA_MATRIX

        override fun getFormatName() = "Data Matrix"
    }

    data object Pdf417 : AllowedBarcodes {
        override fun isValidProductCode(value: String): Boolean {
            return value.isNotBlank()
        }

        override fun getFormat() = Barcode.FORMAT_PDF417

        override fun getFormatName() = "PDF417"
    }
}

fun allowedBarcodeFrom(format: Int): AllowedBarcodes? {
    return when (format) {
        Barcode.FORMAT_UPC_A -> AllowedBarcodes.UpcA
        Barcode.FORMAT_UPC_E -> AllowedBarcodes.UpcE
        Barcode.FORMAT_EAN_13 -> AllowedBarcodes.EAN13
        Barcode.FORMAT_QR_CODE -> AllowedBarcodes.QRCode
        Barcode.FORMAT_DATA_MATRIX -> AllowedBarcodes.DataMatrix
        Barcode.FORMAT_PDF417 -> AllowedBarcodes.Pdf417
        else -> null
    }
}

fun allowedBarcodes() = setOf(
    AllowedBarcodes.UpcA,
    AllowedBarcodes.UpcE,
    AllowedBarcodes.EAN13,
    AllowedBarcodes.QRCode,
    AllowedBarcodes.DataMatrix,
    AllowedBarcodes.Pdf417
)