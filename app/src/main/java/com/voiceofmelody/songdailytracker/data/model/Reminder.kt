package com.voiceofmelody.songdailytracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Calendar

enum class RepeatType {
    NONE, DAILY, WEEKLY, MONTHLY, YEARLY
}

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val note: String,
    val reminderDate: Long, // Day start timestamp
    val reminderTime: Long? = null, // Milliseconds since start of day
    val repeatType: RepeatType = RepeatType.NONE,
    val notificationsEnabled: Boolean = false,
    val colorLabel: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun isOccurringOn(timestamp: Long): Boolean {
        if (repeatType == RepeatType.NONE) {
            return reminderDate == timestamp
        }

        val targetCal = Calendar.getInstance().apply { 
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val reminderCal = Calendar.getInstance().apply { 
            timeInMillis = reminderDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Reminder must not occur before its start date
        if (targetCal.before(reminderCal)) return false

        return when (repeatType) {
            RepeatType.DAILY -> true
            RepeatType.WEEKLY -> targetCal.get(Calendar.DAY_OF_WEEK) == reminderCal.get(Calendar.DAY_OF_WEEK)
            RepeatType.MONTHLY -> {
                val dayOfMonth = reminderCal.get(Calendar.DAY_OF_MONTH)
                val maxInTarget = targetCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                val effectiveDay = if (dayOfMonth > maxInTarget) maxInTarget else dayOfMonth
                targetCal.get(Calendar.DAY_OF_MONTH) == effectiveDay
            }
            RepeatType.YEARLY -> targetCal.get(Calendar.DAY_OF_MONTH) == reminderCal.get(Calendar.DAY_OF_MONTH) &&
                                targetCal.get(Calendar.MONTH) == reminderCal.get(Calendar.MONTH)
            else -> false
        }
    }
}
