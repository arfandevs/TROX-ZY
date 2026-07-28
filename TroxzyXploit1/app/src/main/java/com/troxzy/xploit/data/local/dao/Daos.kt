package com.troxzy.xploit.data.local.dao

import androidx.room.*
import com.troxzy.xploit.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatSessionDao {
    @Query("SELECT * FROM chat_sessions ORDER BY updatedAt DESC")
    fun getAllSessions(): Flow<List<ChatSessionEntity>>

    @Query("SELECT * FROM chat_sessions WHERE id = :id")
    suspend fun getSession(id: Long): ChatSessionEntity?

    @Insert
    suspend fun insertSession(session: ChatSessionEntity): Long

    @Update
    suspend fun updateSession(session: ChatSessionEntity)

    @Delete
    suspend fun deleteSession(session: ChatSessionEntity)

    @Query("DELETE FROM chat_sessions WHERE id = :id")
    suspend fun deleteSessionById(id: Long)
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSession(sessionId: Long): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getMessagesListForSession(sessionId: Long): List<ChatMessageEntity>

    @Insert
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Update
    suspend fun updateMessage(message: ChatMessageEntity)

    @Delete
    suspend fun deleteMessage(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun deleteMessagesForSession(sessionId: Long)
}

@Dao
interface ScanHistoryDao {
    @Query("SELECT * FROM scan_history ORDER BY createdAt DESC")
    fun getAllHistory(): Flow<List<ScanHistoryEntity>>

    @Query("SELECT * FROM scan_history WHERE scanType = :type ORDER BY createdAt DESC")
    fun getHistoryByType(type: String): Flow<List<ScanHistoryEntity>>

    @Insert
    suspend fun insertHistory(history: ScanHistoryEntity): Long

    @Delete
    suspend fun deleteHistory(history: ScanHistoryEntity)

    @Query("DELETE FROM scan_history")
    suspend fun clearAll()
}

@Dao
interface PasswordHistoryDao {
    @Query("SELECT * FROM password_history ORDER BY createdAt DESC LIMIT 10")
    fun getRecentPasswords(): Flow<List<PasswordHistoryEntity>>

    @Insert
    suspend fun insertPassword(password: PasswordHistoryEntity): Long

    @Query("DELETE FROM password_history")
    suspend fun clearAll()
}
