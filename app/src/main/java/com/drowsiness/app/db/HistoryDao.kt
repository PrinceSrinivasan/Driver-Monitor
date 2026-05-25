package com.drowsiness.app.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface HistoryDao {

    @Insert
    suspend fun insert(history: HistoryEntity)

    @Query("SELECT * FROM history ORDER BY id DESC LIMIT :limit")
    suspend fun getLastN(limit: Int): List<HistoryEntity>

    @Query("SELECT COUNT(*) FROM history WHERE status = 'DROWSY'")
    suspend fun getDrowsyCount(): Int

    @Query("SELECT COUNT(*) FROM history WHERE status = 'AWAKE'")
    suspend fun getAwakeCount(): Int

    @Query("DELETE FROM history")
    suspend fun clearAll()
}
