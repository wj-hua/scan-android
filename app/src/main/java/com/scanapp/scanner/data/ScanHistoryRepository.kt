package com.scanapp.scanner.data

import kotlinx.coroutines.flow.Flow

interface ScanHistoryRepository {
    fun observeHistory(): Flow<List<ScanHistoryEntity>>

    suspend fun add(content: String, scannedAt: Long, source: ScanSource)
}

class RoomScanHistoryRepository(
    private val dao: ScanHistoryDao,
) : ScanHistoryRepository {
    override fun observeHistory(): Flow<List<ScanHistoryEntity>> = dao.observeAll()

    override suspend fun add(content: String, scannedAt: Long, source: ScanSource) {
        dao.insert(
            ScanHistoryEntity(
                content = content,
                scannedAt = scannedAt,
                source = source,
            ),
        )
    }
}
