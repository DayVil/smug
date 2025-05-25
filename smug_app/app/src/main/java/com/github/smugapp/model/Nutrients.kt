package com.github.smugapp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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