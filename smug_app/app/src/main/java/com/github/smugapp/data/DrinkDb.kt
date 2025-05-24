package com.github.smugapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.github.smugapp.model.DrinkProduct

@Database(
    entities = [DrinkProduct::class],
    version = 3,
    exportSchema = false
)
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