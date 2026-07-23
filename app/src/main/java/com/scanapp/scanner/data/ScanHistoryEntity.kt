package com.scanapp.scanner.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_history")
data class ScanHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val content: String,
    val scannedAt: Long,
    val source: ScanSource,
)

enum class ScanSource {
    CAMERA,
    GALLERY,
}
