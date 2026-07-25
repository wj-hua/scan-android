package com.scanapp.scanner

import com.scanapp.scanner.data.ScanHistoryEntity
import com.scanapp.scanner.data.ScanHistoryRepository
import com.scanapp.scanner.data.ScanSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScanHistoryViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `camera and gallery scans are both recorded including duplicates`() = runTest(dispatcher) {
        val repository = FakeScanHistoryRepository()
        val viewModel = ScanHistoryViewModel(repository) { 1234L }

        viewModel.recordScan("same-content", ScanSource.CAMERA)
        viewModel.recordScan("same-content", ScanSource.GALLERY)
        advanceUntilIdle()

        assertEquals(
            listOf(
                RecordedScan("same-content", 1234L, ScanSource.CAMERA),
                RecordedScan("same-content", 1234L, ScanSource.GALLERY),
            ),
            repository.recorded,
        )
    }

    @Test
    fun `blank scan is ignored`() = runTest(dispatcher) {
        val repository = FakeScanHistoryRepository()
        val viewModel = ScanHistoryViewModel(repository)

        viewModel.recordScan("   ", ScanSource.CAMERA)
        advanceUntilIdle()

        assertEquals(emptyList<RecordedScan>(), repository.recorded)
    }

    @Test
    fun `delete and clear are forwarded to repository`() = runTest(dispatcher) {
        val repository = FakeScanHistoryRepository()
        val viewModel = ScanHistoryViewModel(repository)

        viewModel.deleteScan(42L)
        viewModel.clearHistory()
        advanceUntilIdle()

        assertEquals(listOf(42L), repository.deletedIds)
        assertEquals(1, repository.clearCount)
    }
}

private data class RecordedScan(
    val content: String,
    val scannedAt: Long,
    val source: ScanSource,
)

private class FakeScanHistoryRepository : ScanHistoryRepository {
    private val history = MutableStateFlow<List<ScanHistoryEntity>>(emptyList())
    val recorded = mutableListOf<RecordedScan>()
    val deletedIds = mutableListOf<Long>()
    var clearCount = 0

    override fun observeHistory(): Flow<List<ScanHistoryEntity>> = history

    override suspend fun add(content: String, scannedAt: Long, source: ScanSource) {
        recorded += RecordedScan(content, scannedAt, source)
    }

    override suspend fun delete(id: Long) {
        deletedIds += id
    }

    override suspend fun clear() {
        clearCount += 1
    }
}
