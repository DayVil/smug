package com.github.smugapp.data

import android.util.Log
import com.github.smugapp.model.DrinkProduct
import kotlinx.coroutines.flow.Flow

const val TAG = "DrinkRepo"

class DrinkRepo(private val drinkDao: DrinkDao) {
    fun getAllDrinkProducts(): Flow<List<DrinkProduct>> {
        val threshold = pastOffset(3)
        return drinkDao.getAllDrinkProducts(threshold)
    }

    fun getTodayDrinkProducts(): Flow<List<DrinkProduct>> {
        return drinkDao.getTodayDrinkProducts()
    }

    suspend fun getDrinkProductById(id: String): DrinkProduct? {
        val threshold = pastOffset(3)
        Log.d(TAG, "Getting drink product with ID: $id")
        return drinkDao.getDrinkProductById(id, threshold)
    }

    suspend fun insertDrinkProduct(product: DrinkProduct) {
        drinkDao.insertDrinkProduct(product)
    }

    private fun pastOffset(monthAmount: Int) =
        System.currentTimeMillis() - monthAmount * 30L * 24 * 60 * 60 * 1000
}