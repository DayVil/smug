package com.github.smugapp.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Entity(tableName = "measurements")
@Serializable
data class Measurement(
    @PrimaryKey(autoGenerate = true)
    @Transient
    val id: Long = 0,

    val value: Double,
    val unit: MeasurementUnit,

    @Transient
    val timestamp: Long = System.currentTimeMillis(),
)

@Serializable
enum class MeasurementUnit {
    kg,
    g
}
