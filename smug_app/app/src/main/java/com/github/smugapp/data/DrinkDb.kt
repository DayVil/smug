package com.github.smugapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.github.smugapp.model.DrinkProduct
import com.github.smugapp.model.DrinkProductConverter

@Database(
    entities = [DrinkProduct::class],
    version = 5,
    exportSchema = false
)

@TypeConverters(DrinkProductConverter::class)
abstract class DrinkDb : RoomDatabase() {
    abstract fun drinkDao(): DrinkDao

    companion object {
        @Volatile
        private var INSTANCE: DrinkDb? = null

        fun getDatabase(context: Context): DrinkDb {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DrinkDb::class.java,
                    "drink_database"
                )
                    .fallbackToDestructiveMigration(false)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}