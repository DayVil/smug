package com.github.smugapp.network.off

sealed interface InputParser {
    data class BarCodeSearchTerm(val value: String) : InputParser
    data class ProductSearchTerm(val value: String) : InputParser
    data class UnknownSearchTerm(val value: String) : InputParser
}

fun parseInput(input: String): InputParser {
    return when {
        input.all { it.isDigit() } -> InputParser.BarCodeSearchTerm(input)
        input.all { it.isLetterOrDigit() || it.isWhitespace() } -> InputParser.ProductSearchTerm(
            input
        )

        else -> InputParser.UnknownSearchTerm(input)
    }
}