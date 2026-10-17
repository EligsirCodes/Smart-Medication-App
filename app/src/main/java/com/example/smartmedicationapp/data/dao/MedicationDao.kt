package com.example.smartmedicationapp.data.dao

import androidx.room.*
import com.example.smartmedicationapp.data.entity.Medication
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {
    @Query("SELECT * FROM medications ORDER BY targetTime ASC")
    fun getAllMedications(): Flow<List<Medication>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(medication: Medication): Long

    @Update
    suspend fun update(medication: Medication)

    @Delete
    suspend fun delete(medication: Medication)

    @Query("SELECT * FROM medications WHERE isTaken = 0")
    suspend fun getPendingMedications(): List<Medication>

    @Query("UPDATE medications SET isTaken = 1 WHERE id = :id")
    suspend fun markAsTaken(id: Int)
}