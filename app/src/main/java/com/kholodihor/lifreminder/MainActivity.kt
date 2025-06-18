package com.kholodihor.lifreminder

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import java.util.*
import androidx.room.Room
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults

class MainActivity : ComponentActivity() {

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.i("LifeReminder", "Notification permission granted")
        } else {
            Log.w("LifeReminder", "Notification permission denied")
        }
    }

    private lateinit var db: ReminderDatabase
    private lateinit var dao: ReminderDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        db = Room.databaseBuilder(
            applicationContext,
            ReminderDatabase::class.java,
            "reminders_db"
        ).build()

        dao = db.reminderDao()

        // Request POST_NOTIFICATIONS for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            MaterialTheme {
                ReminderScreen(dao)
            }
        }
    }



    @Composable
    fun ReminderScreen(dao: ReminderDao) {
        var time by remember { mutableStateOf("") }
        var message by remember { mutableStateOf("") }
        var confirmed by remember { mutableStateOf(false) }
        val context = LocalContext.current
        val activity = context as? Activity
        val coroutineScope = rememberCoroutineScope()

        val remindersFlow = remember { dao.getAllReminders() }
        val reminders by remindersFlow.collectAsState(initial = emptyList())

        val primaryGreen = Color(0xFFA3EBB1) // #a3ebb1
        val darkGreen = Color(0xFF116530)   // #116530
        val textGreen = Color(0xFF18A558)   // #18a558

        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        )  {
            // Заголовок
            Text(
                text = "Life Reminder",
                style = MaterialTheme.typography.headlineMedium,
                color = darkGreen,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                label = { Text("Exercise Name") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryGreen,
                    unfocusedBorderColor = primaryGreen.copy(alpha = 0.5f),
                    cursorColor = darkGreen,
                    focusedLabelColor = darkGreen,
                    unfocusedLabelColor = darkGreen.copy(alpha = 0.7f),
                    focusedTextColor = textGreen,
                    unfocusedTextColor = textGreen
                )
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        activity?.let {
                            showTimePickerDialog(it) { selectedTime ->
                                time = selectedTime
                                confirmed = false
                            }
                        }
                    }
            ) {
                OutlinedTextField(
                    value = time,
                    onValueChange = {},
                    label = { Text("Reminder Time") },
                    readOnly = true,
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledBorderColor = primaryGreen,
                        disabledTextColor = textGreen,
                        disabledLabelColor = darkGreen,
                        disabledPlaceholderColor = darkGreen
                    )
                )
            }

            Button(
                onClick = {
                    val parts = time.split(":")
                    val hour = parts.getOrNull(0)?.toIntOrNull()
                    val minute = parts.getOrNull(1)?.toIntOrNull()

                    if (hour != null && minute != null) {
                        val reminderMessage = if (message.isBlank()) "Time to exercise!" else message

                        coroutineScope.launch {
                            val reminder = ReminderEntity(
                                id = 0,
                                message = reminderMessage,
                                time = time
                            )

                            val insertedId = dao.insertReminder(reminder).toInt()
                            scheduleReminder(context, reminderMessage, hour, minute, insertedId)
                            confirmed = true
                        }
                    }
                },
                enabled = time.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryGreen,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Set Reminder")
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "Your Reminders:",
                style = MaterialTheme.typography.titleMedium,
                color = darkGreen
            )

            reminders.forEach { reminder ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, shape = RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${reminder.time} - ${reminder.message}", color = textGreen)

                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                dao.deleteReminder(reminder)
                                cancelScheduledReminder(context, reminder.id)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Reminder",
                            tint = Color(0xFFD32F2F) // Red for delete
                        )
                    }
                }
            }
        }
    }




    private fun cancelScheduledReminder(context: Context, reminderId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }


    private fun showTimePickerDialog(activity: Activity, onTimeSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        TimePickerDialog(
            activity,
            { _, selectedHour, selectedMinute ->
                val formatted = String.format("%02d:%02d", selectedHour, selectedMinute)
                onTimeSelected(formatted)
            },
            hour,
            minute,
            true
        ).show()
    }

    private fun scheduleReminder(context: Context, message: String, hour: Int, minute: Int, reminderId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("REMINDER_MESSAGE", message)
            putExtra("REMINDER_ID", reminderId) // 👈 додаємо ID
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId, // 👈 використовуємо ID як requestCode
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        val canScheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else true

        if (!canScheduleExact && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intentSettings = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            intentSettings.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intentSettings)
            return
        }

        if (canScheduleExact) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }
}
