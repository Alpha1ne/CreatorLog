package com.voiceofmelody.songdailytracker.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.voiceofmelody.songdailytracker.data.local.AppDatabase
import com.voiceofmelody.songdailytracker.data.model.Reminder
import com.voiceofmelody.songdailytracker.data.model.RepeatType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra("reminder_id", 0)
        val title = intent.getStringExtra("reminder_title") ?: "Reminder"
        val note = intent.getStringExtra("reminder_note") ?: ""
        
        val notificationHelper = NotificationHelper(context)
        notificationHelper.showNotification(id, title, note)

        // Reschedule if recurring
        val db = AppDatabase.getDatabase(context)
        val reminderDao = db.reminderDao()
        
        CoroutineScope(Dispatchers.IO).launch {
            val reminder: Reminder? = reminderDao.getReminderById(id)
            if (reminder != null && reminder.repeatType != RepeatType.NONE && reminder.notificationsEnabled) {
                notificationHelper.scheduleReminder(reminder)
            }
        }
    }
}
