package com.github.smugapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.github.smugapp.model.Measurement

@Database(
    entities = [Measurement::class],
    version = 1,
    exportSchema = false
)
abstract class MeasurementDb: RoomDatabase() {
    abstract fun measurementDao(): MeasurementDao

    companion object {
        @Volatile
        private var INSTANCE: MeasurementDb? = null

        fun getDatabase(context: Context): MeasurementDb {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MeasurementDb::class.java,
                    "measurement_database"
                )
                    .fallbackToDestructiveMigration(false)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}