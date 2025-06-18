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

@Serializable
data class OffSearchResponse(
    val count: Int,
    val page: Int,
    val products: List<OffSearchProductResponse>,

    @SerialName("page_count")
    val pageCount: Int,

    @SerialName("page_size")
    val pageSize: Int,
)

@Serializable
data class OffSearchProductResponse(
    val code: String,
    val url: String,

    @SerialName("schema_version")
    val schemaVersion: String,
)