package com.astralimit.dogfit.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [DogProfileEntity::class, VaccinationEntity::class, DewormingEntity::class, DailyActivityEntity::class],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class DogFitDatabase : RoomDatabase() {
    abstract fun dogProfileDao(): DogProfileDao
    abstract fun vaccinationDao(): VaccinationDao
    abstract fun dewormingDao(): DewormingDao
    abstract fun dailyActivityDao(): DailyActivityDao

    companion object {
        @Volatile
        private var INSTANCE: DogFitDatabase? = null

        fun getDatabase(context: Context): DogFitDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DogFitDatabase::class.java,
                    "dogfit_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
