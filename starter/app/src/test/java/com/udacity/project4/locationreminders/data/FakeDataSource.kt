package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource(var reminders: MutableList<ReminderDTO>? = mutableListOf()) :
            ReminderDataSource {

    private var returnsError = false

    fun setReturnsError(value: Boolean) {
        returnsError = value
    }

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        if (returnsError) return Result.Error("ERROR")
        else {
            reminders?.let { return Result.Success(ArrayList(it)) }
            return Result.Error("Reminders not found"
            )
        }
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminders?.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        if (returnsError) return Result.Error("ERROR")
        else {
            reminders?.let {
                for (reminder in it) {
                    if (reminder.id == id) return Result.Success(reminder)
                }
            }
            return Result.Error("Reminder not found")
        }
    }

    override suspend fun deleteAllReminders() {
        reminders?.clear()
    }
}