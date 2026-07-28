package com.voiceofmelody.songdailytracker.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.voiceofmelody.songdailytracker.data.local.AppDatabase
import com.voiceofmelody.songdailytracker.data.model.Reminder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val db = AppDatabase.getDatabase(context)
            val reminderDao = db.reminderDao()
            val notificationHelper = NotificationHelper(context)

            CoroutineScope(Dispatchers.IO).launch {
                val allReminders = reminderDao.getAllReminders().first()
                allReminders.forEach { reminder ->
                    if (reminder.notificationsEnabled) {
                        notificationHelper.scheduleReminder(reminder)
                    }
                }
            }
        }
    }
}
