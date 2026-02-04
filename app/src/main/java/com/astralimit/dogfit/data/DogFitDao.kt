package com.astralimit.dogfit.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DogProfileDao {
    @Query("SELECT * FROM dog_profiles WHERE id = 1")
    fun getProfile(): Flow<DogProfileEntity?>

    @Query("SELECT * FROM dog_profiles WHERE id = 1")
    suspend fun getProfileSync(): DogProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: DogProfileEntity)

    @Update
    suspend fun updateProfile(profile: DogProfileEntity)
}

@Dao
interface VaccinationDao {
    @Query("SELECT * FROM vaccinations ORDER BY nextDueDate ASC")
    fun getAllVaccinations(): Flow<List<VaccinationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaccination(vaccination: VaccinationEntity)

    @Update
    suspend fun updateVaccination(vaccination: VaccinationEntity)

    @Delete
    suspend fun deleteVaccination(vaccination: VaccinationEntity)
}

@Dao
interface DewormingDao {
    @Query("SELECT * FROM dewormings ORDER BY nextDueDate ASC")
    fun getAllDewormings(): Flow<List<DewormingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeworming(deworming: DewormingEntity)

    @Update
    suspend fun updateDeworming(deworming: DewormingEntity)

    @Delete
    suspend fun deleteDeworming(deworming: DewormingEntity)
}

@Dao
interface DailyActivityDao {
    @Query("SELECT * FROM daily_activity WHERE date = :date")
    suspend fun getActivityForDate(date: String): DailyActivityEntity?

    @Query("SELECT * FROM daily_activity ORDER BY date DESC")
    fun getAllActivity(): Flow<List<DailyActivityEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: DailyActivityEntity)
}
