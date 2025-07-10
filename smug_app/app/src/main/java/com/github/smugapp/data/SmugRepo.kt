package com.github.smugapp.data

import android.util.Log
import com.github.smugapp.model.DrinkProduct
import com.github.smugapp.model.Measurement
import kotlinx.coroutines.flow.Flow

const val TAG = "DrinkRepo"

class SmugRepo(private val drinkDao: DrinkDao, private val measurementDao: MeasurementDao) {
    fun getAllDrinkProducts(): Flow<List<DrinkProduct>> {
        val threshold = pastOffset(3)
        return drinkDao.getAllDrinkProducts(threshold)
    }

    fun getTodayDrinkProducts(): Flow<List<DrinkProduct>> {
        return drinkDao.getTodayDrinkProducts()
    }
    fun getWeeklyDrinkProducts(): Flow<List<DrinkProduct>> {
        val threshold = pastOffset(0) // 0 Monate → heute minus 7 Tage
        return drinkDao.getWeeklyDrinkProducts()
    }


    suspend fun getDrinkProductById(id: String): DrinkProduct? {
        val threshold = pastOffset(3)
        Log.d(TAG, "Getting drink product with ID: $id")
        return drinkDao.getDrinkProductById(id, threshold)
    }

    suspend fun insertDrinkProduct(product: DrinkProduct) {
        drinkDao.insertDrinkProduct(product)
    }

    fun getAllMeasurements(): Flow<List<Measurement>> {
        return measurementDao.getAllMeasurements()
    }

    suspend fun insertMeasurement(measurement: Measurement) {
        measurementDao.insertMeasurement(measurement)
    }

    suspend fun deleteMeasurementById(id: Long) {
        measurementDao.deleteMeasurementById(id)
    }

    suspend fun deleteDrinkProduct(drinkProduct: DrinkProduct) {
        drinkDao.deleteDrinkProduct(drinkProduct)
    }

    suspend fun deleteAllDrinkProducts() {
        drinkDao.deleteAllDrinkProducts()
    }

    suspend fun updateDrinkProduct(drinkProduct: DrinkProduct) {
        drinkDao.updateDrinkProduct(drinkProduct)
    }

    private fun pastOffset(monthAmount: Int) =
        System.currentTimeMillis() - monthAmount * 30L * 24 * 60 * 60 * 1000
}