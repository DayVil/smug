package com.github.smugapp.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Entity(tableName = "drink_products")
@Serializable
data class DrinkProduct(
    @PrimaryKey
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

    @Transient
    val createdAt: Long = System.currentTimeMillis(),
) {
    fun getSensibleName(): String {
        return germanName ?: englishName ?: frenchName ?: defaultName
    }
}
