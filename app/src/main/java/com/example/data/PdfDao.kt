package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PdfDao {
    @Query("SELECT * FROM pdf_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<PdfHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(item: PdfHistoryEntity)

    @Delete
    suspend fun deleteHistory(item: PdfHistoryEntity)

    @Query("DELETE FROM pdf_history")
    suspend fun clearAll()
}
