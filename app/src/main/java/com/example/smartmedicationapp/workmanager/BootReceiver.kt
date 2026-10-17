package com.example.smartmedicationapp.workmanager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.*
import com.example.smartmedicationapp.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val db = AppDatabase.getDatabase(context)
            val workManager = WorkManager.getInstance(context)

            CoroutineScope(Dispatchers.IO).launch {
                val pendingMeds = db.medicationDao().getPendingMedications()

                pendingMeds.forEach { med ->
                    val delay = med.targetTime - System.currentTimeMillis()

                    if (delay > 0) {
                        val constraints = Constraints.Builder()
                            .setRequiresBatteryNotLow(true)
                            .build()

                        val data = workDataOf(
                            "MED_ID" to med.id,
                            "MED_NAME" to med.name,
                            "MED_DOSAGE" to med.dosage
                        )

                        val workRequest = OneTimeWorkRequestBuilder<MedicationWorker>()
                            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                            .setConstraints(constraints)
                            .setInputData(data)
                            .build()

                        workManager.enqueueUniqueWork(
                            "med_${med.id}",
                            ExistingWorkPolicy.REPLACE,
                            workRequest
                        )
                    }
                }
            }
        }
    }
}