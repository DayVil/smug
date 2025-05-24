package com.github.smugapp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OffResponse(
    val code: String,
    val product: DrinkProduct,
    val status: Int,

    @SerialName("status_verbose")
    val statusVerbose: String,
)