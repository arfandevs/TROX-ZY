package com.troxzy.xploit.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.troxzy.xploit.data.local.entity.*
import com.troxzy.xploit.data.local.dao.*

@Database(
    entities = [
        ChatSessionEntity::class,
        ChatMessageEntity::class,
        ScanHistoryEntity::class,
        PasswordHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatSessionDao(): ChatSessionDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun scanHistoryDao(): ScanHistoryDao
    abstract fun passwordHistoryDao(): PasswordHistoryDao
}
