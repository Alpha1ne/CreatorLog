package com.voiceofmelody.songdailytracker.util

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.voiceofmelody.songdailytracker.MainActivity
import com.voiceofmelody.songdailytracker.data.model.Reminder
import com.voiceofmelody.songdailytracker.data.model.RepeatType

class NotificationHelper(private val context: Context) {
    private val channelId = "reminder_channel"
    private val channelName = "Reminders"

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = "CreatorLog Reminders"
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun scheduleReminder(reminder: Reminder) {
        if (!reminder.notificationsEnabled) return

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("reminder_id", reminder.id)
            putExtra("reminder_title", reminder.title)
            putExtra("reminder_note", reminder.note)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        var triggerAt = reminder.reminderDate + (reminder.reminderTime ?: 0L)

        // If triggerAt is in the past and it's a recurring reminder, calculate next occurrence
        if (triggerAt <= System.currentTimeMillis() && reminder.repeatType != RepeatType.NONE) {
            triggerAt = calculateNextOccurrence(triggerAt, reminder.repeatType)
        }

        if (triggerAt > System.currentTimeMillis()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        }
    }

    private fun calculateNextOccurrence(currentTriggerAt: Long, repeatType: RepeatType): Long {
        val calendar = java.util.Calendar.getInstance().apply { timeInMillis = currentTriggerAt }
        val now = System.currentTimeMillis()
        
        while (calendar.timeInMillis <= now) {
            when (repeatType) {
                RepeatType.DAILY -> calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
                RepeatType.WEEKLY -> calendar.add(java.util.Calendar.WEEK_OF_YEAR, 1)
                RepeatType.MONTHLY -> calendar.add(java.util.Calendar.MONTH, 1)
                RepeatType.YEARLY -> calendar.add(java.util.Calendar.YEAR, 1)
                else -> return calendar.timeInMillis // Should not happen for NONE
            }
        }
        return calendar.timeInMillis
    }

    fun cancelReminder(reminderId: Int) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun showNotification(id: Int, title: String, note: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId).apply {
            setContentTitle(title)
            setContentText(note)
            setSmallIcon(com.voiceofmelody.songdailytracker.R.mipmap.ic_launcher)
            setAutoCancel(true)
            setContentIntent(pendingIntent)
            setPriority(NotificationCompat.PRIORITY_HIGH)
        }.build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(id, notification)
    }
}
