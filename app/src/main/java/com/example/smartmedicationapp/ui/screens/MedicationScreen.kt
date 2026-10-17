package com.example.smartmedicationapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smartmedicationapp.data.entity.Medication
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationScreen(viewModel: MedicationViewModel = viewModel()) {
    val medications by viewModel.allMedications.collectAsState(initial = emptyList())

    var name by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("Daily") }
    var showTimePicker by remember { mutableStateOf(false) }

    val calendar = remember { Calendar.getInstance() }
    var selectedTimeInMillis by remember { mutableLongStateOf(0L) }
    val timeFormatter = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

    var editingMedication by remember { mutableStateOf<Medication?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Smart Medication App") }) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Medication Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = dosage,
                onValueChange = { dosage = it },
                label = { Text("Dosage (e.g. 500mg)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Frequency: ")
                RadioButton(selected = frequency == "Daily", onClick = { frequency = "Daily" })
                Text("Daily")
                Spacer(modifier = Modifier.width(8.dp))
                RadioButton(selected = frequency == "Weekly", onClick = { frequency = "Weekly" })
                Text("Weekly")
            }

            Button(
                onClick = { showTimePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (selectedTimeInMillis == 0L) "Select Reminder Time" else "Time: ${timeFormatter.format(Date(selectedTimeInMillis))}")
            }

            Button(
                onClick = {
                    if (name.isNotBlank() && dosage.isNotBlank() && selectedTimeInMillis > System.currentTimeMillis()) {
                        if (editingMedication != null) {
                            viewModel.updateMedication(editingMedication!!, name, dosage, frequency, selectedTimeInMillis)
                            editingMedication = null
                        } else {
                            viewModel.addMedication(name, dosage, frequency, selectedTimeInMillis)
                        }

                        name = ""
                        dosage = ""
                        selectedTimeInMillis = 0L
                        frequency = "Daily"
                    }
                },
                enabled = name.isNotBlank() && dosage.isNotBlank() && selectedTimeInMillis > System.currentTimeMillis(),
                modifier = Modifier.padding(top = 8.dp).fillMaxWidth()
            ) {
                Text(if (editingMedication != null) "Update Prescription" else "Add Prescription")
            }

            if (editingMedication != null) {
                TextButton(
                    onClick = {
                        editingMedication = null
                        name = ""
                        dosage = ""
                        selectedTimeInMillis = 0L
                        frequency = "Daily"
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel Edit")
                }
            }

            LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 16.dp)) {
                items(medications) { med ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = med.name, style = MaterialTheme.typography.titleLarge)
                                Text(text = "${med.dosage} • ${med.frequency}")
                                Text(
                                    text = "Scheduled: ${timeFormatter.format(Date(med.targetTime))}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Row {
                                IconButton(onClick = {
                                    editingMedication = med
                                    name = med.name
                                    dosage = med.dosage
                                    frequency = med.frequency
                                    selectedTimeInMillis = med.targetTime
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { viewModel.deleteMedication(med) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTimePicker) {
        val initialHour = if (selectedTimeInMillis > 0) {
            val cal = Calendar.getInstance().apply { timeInMillis = selectedTimeInMillis }
            cal.get(Calendar.HOUR_OF_DAY)
        } else calendar.get(Calendar.HOUR_OF_DAY)

        val initialMinute = if (selectedTimeInMillis > 0) {
            val cal = Calendar.getInstance().apply { timeInMillis = selectedTimeInMillis }
            cal.get(Calendar.MINUTE)
        } else calendar.get(Calendar.MINUTE)

        val timePickerState = rememberTimePickerState(
            initialHour = initialHour,
            initialMinute = initialMinute
        )

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val newCal = Calendar.getInstance()
                    newCal.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                    newCal.set(Calendar.MINUTE, timePickerState.minute)
                    newCal.set(Calendar.SECOND, 0)

                    if (newCal.timeInMillis <= System.currentTimeMillis()) {
                        newCal.add(Calendar.DAY_OF_YEAR, 1)
                    }

                    selectedTimeInMillis = newCal.timeInMillis
                    showTimePicker = false
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            },
            text = { TimePicker(state = timePickerState) }
        )
    }
}