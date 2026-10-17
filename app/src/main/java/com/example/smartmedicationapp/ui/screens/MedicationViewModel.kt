package com.example.smartmedicationapp.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.example.smartmedicationapp.data.AppDatabase
import com.example.smartmedicationapp.data.entity.Medication
import com.example.smartmedicationapp.workmanager.MedicationWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MedicationViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).medicationDao()
    val allMedications: Flow<List<Medication>> = dao.getAllMedications()

    fun addMedication(name: String, dosage: String, frequency: String, targetTime: Long) {
        if (targetTime <= System.currentTimeMillis()) return

        viewModelScope.launch {
            val medId = dao.insert(
                Medication(name = name, dosage = dosage, frequency = frequency, targetTime = targetTime)
            ).toInt()

            scheduleWork(medId, name, dosage, frequency, targetTime)
        }
    }

    fun updateMedication(medication: Medication, newName: String, newDosage: String, newFrequency: String, newTargetTime: Long) {
        if (newTargetTime <= System.currentTimeMillis()) return

        viewModelScope.launch {
            val updatedMed = medication.copy(
                name = newName,
                dosage = newDosage,
                frequency = newFrequency,
                targetTime = newTargetTime
            )
            dao.update(updatedMed)

            WorkManager.getInstance(getApplication()).cancelUniqueWork("med_${medication.id}")

            scheduleWork(medication.id, newName, newDosage, newFrequency, newTargetTime)
        }
    }

    fun deleteMedication(medication: Medication) {
        viewModelScope.launch {
            dao.delete(medication)
            WorkManager.getInstance(getApplication()).cancelUniqueWork("med_${medication.id}")
        }
    }

    private fun scheduleWork(id: Int, name: String, dosage: String, frequency: String, time: Long) {
        val workManager = WorkManager.getInstance(getApplication())
        val delay = time - System.currentTimeMillis()

        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val data = workDataOf("MED_ID" to id, "MED_NAME" to name, "MED_DOSAGE" to dosage)

        if (frequency == "Daily") {
            val dailyWorkRequest = PeriodicWorkRequestBuilder<MedicationWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setConstraints(constraints)
                .setInputData(data)
                .build()

            workManager.enqueueUniquePeriodicWork("med_$id", ExistingPeriodicWorkPolicy.UPDATE, dailyWorkRequest)
        } else if (frequency == "Weekly") {
            val weeklyWorkRequest = PeriodicWorkRequestBuilder<MedicationWorker>(7, TimeUnit.DAYS)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setConstraints(constraints)
                .setInputData(data)
                .build()

            workManager.enqueueUniquePeriodicWork("med_$id", ExistingPeriodicWorkPolicy.UPDATE, weeklyWorkRequest)
        } else {
            val workRequest = OneTimeWorkRequestBuilder<MedicationWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setConstraints(constraints)
                .setInputData(data)
                .build()

            workManager.enqueueUniqueWork("med_$id", ExistingWorkPolicy.REPLACE, workRequest)
        }
    }
}