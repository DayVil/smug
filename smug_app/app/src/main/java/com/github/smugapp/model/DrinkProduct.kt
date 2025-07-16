package com.github.smugapp.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import io.ktor.http.Url
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

    @SerialName("categories_tags")
    val categoryTags: List<String>? = null,

    @SerialName("pnns_groups_2")
    val pnnsGroup: String? = null,

    @SerialName("food_groups_tags")
    val foodGroups: List<String>? = null,

    @SerialName("ingredients_tags")
    val ingredients: List<String>? = null,

    @SerialName("image_front_small_url")
    val image: Url? = null,

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

class DrinkProductConverter {

    // -- Url converters --
    @TypeConverter
    fun fromUrl(url: Url?): String? {
        return url?.toString()
    }

    @TypeConverter
    fun toUrl(url: String?): Url? {
        return url?.let { Url(it) }
    }

}

class ListConverter {

    @TypeConverter
    fun fromList(list: List<String>?): String? {
        return list?.joinToString(",")
    }

    @TypeConverter
    fun toList(data: String?): List<String>? {
        return data?.split(",")?.map { it.trim() }
    }
}

