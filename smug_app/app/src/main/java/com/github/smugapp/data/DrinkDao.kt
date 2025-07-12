package com.github.smugapp.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.github.smugapp.model.DrinkProduct
import kotlinx.coroutines.flow.Flow

@Dao
interface DrinkDao {
    @Query(
        "SELECT * FROM drink_products " +
                "WHERE createdAt >= :timestampThreshold " +
                "ORDER BY createdAt DESC"
    )
    fun getAllDrinkProducts(timestampThreshold: Long): Flow<List<DrinkProduct>>
    @Query(
        "SELECT * FROM drink_products " +
                "WHERE date(createdAt / 1000, 'unixepoch') = date('now') " +
                "ORDER BY createdAt DESC"
    )
    fun getTodayDrinkProducts(): Flow<List<DrinkProduct>>

    @Query(
        "SELECT * FROM drink_products " +
                "WHERE datetime(createdAt / 1000, 'unixepoch') >= datetime('now', '-7 days') " +
                "ORDER BY createdAt DESC"
    )
    fun getWeeklyDrinkProducts(): Flow<List<DrinkProduct>>

    @Query(
        "SELECT * FROM drink_products " +
                "WHERE id = :id AND createdAt >= :timestampThreshold " +
                "ORDER BY createdAt DESC"
    )

    suspend fun getDrinkProductById(id: String, timestampThreshold: Long): DrinkProduct?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDrinkProduct(drinkProduct: DrinkProduct)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDrinkProducts(drinkProducts: List<DrinkProduct>)

    @Update
    suspend fun updateDrinkProduct(drinkProduct: DrinkProduct)

    @Delete
    suspend fun deleteDrinkProduct(drinkProduct: DrinkProduct)

    @Query("DELETE FROM drink_products WHERE id = :id")
    suspend fun deleteDrinkProductById(id: String)

    @Query("DELETE FROM drink_products")
    suspend fun deleteAllDrinkProducts()

    @Query(
        "SELECT * FROM drink_products " +
                "WHERE defaultName LIKE '%' || :searchQuery || '%' " +
                "OR germanName LIKE '%' || :searchQuery || '%' " +
                "OR englishName LIKE '%' || :searchQuery || '%' " +
                "OR frenchName LIKE '%' || :searchQuery || '%'"
    )
    fun searchDrinkProducts(searchQuery: String): Flow<List<DrinkProduct>>
}