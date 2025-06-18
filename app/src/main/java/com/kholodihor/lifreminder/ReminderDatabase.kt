package com.kholodihor.lifreminder

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ReminderEntity::class], version = 1)
abstract class ReminderDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao
}
