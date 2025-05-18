package com.github.smugapp.off

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
data class Nutrients(
    @SerialName("energy-kcal_100g")
    val caloriesPer100g: Double? = null,

    @SerialName("fat_100g")
    val saturatedFatPer100g: Double? = null,

    @SerialName("sugars_100g")
    val sugarsPer100g: Double? = null,

    @SerialName("caffeine_100g")
    val caffeinePer100g: Double? = null,
)

@Serializable
data class DrinkProduct(
    @SerialName("product_name")
    val defaultName: String,

    @SerialName("product_name_de")
    val germanName: String? = null,

    @SerialName("product_name_fr")
    val frenchName: String? = null,

    @SerialName("product_name_en")
    val englishName: String? = null,

    @SerialName("nutriments")
    val nutrients: Nutrients? = null,
) {
    fun getSensibleName(): String {
        return germanName ?: englishName ?: frenchName ?: defaultName
    }
}
