package com.scanapp.scanner.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanHistoryDao {
    @Query("SELECT * FROM scan_history ORDER BY scannedAt DESC, id DESC")
    fun observeAll(): Flow<List<ScanHistoryEntity>>

    @Insert
    suspend fun insert(item: ScanHistoryEntity)
}
