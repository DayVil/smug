package com.github.smugapp.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
// @Transient was removed from createdAt
// PrimaryKey was moved from id to createdAt

@Entity(tableName = "drink_products")
@Serializable
data class DrinkProduct(
    // The product's API ID is now a regular field, not the primary key.
    @SerialName("_id")
    val id: String,

    @SerialName("product_name")
    val defaultName: String,

    @SerialName("product_name_de")
    val germanName: String? = null,

    @SerialName("product_name_fr")
    val frenchName: String? = null,

    @SerialName("product_name_en")
    val englishName: String? = null,

    @SerialName("brands")
    val brands: String? = null,

    @Embedded
    @SerialName("nutriments")
    val nutrients: Nutrients? = null,

    // createdAt is now the PrimaryKey to ensure each log entry is unique.
    @PrimaryKey
    val createdAt: Long = System.currentTimeMillis(),

    // New field to store the amount consumed in grams or ml.
    val consumedAmount: Int? = 100
) {
    fun getSensibleName(): String {
        return germanName ?: englishName ?: frenchName ?: defaultName
    }
}
