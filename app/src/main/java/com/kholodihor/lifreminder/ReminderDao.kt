package com.kholodihor.lifreminder

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Insert
    suspend fun insertReminder(reminder: ReminderEntity): Long

    @Delete
    suspend fun deleteReminder(reminder: ReminderEntity)

    @Query("SELECT * FROM reminders ORDER BY time ASC")
    fun getAllReminders(): Flow<List<ReminderEntity>>

    @Query("DELETE FROM reminders WHERE id = :reminderId")
    suspend fun deleteById(reminderId: Int)
}