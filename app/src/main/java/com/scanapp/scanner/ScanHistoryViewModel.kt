package com.scanapp.scanner

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.scanapp.scanner.data.RoomScanHistoryRepository
import com.scanapp.scanner.data.ScanHistoryDatabase
import com.scanapp.scanner.data.ScanHistoryEntity
import com.scanapp.scanner.data.ScanHistoryRepository
import com.scanapp.scanner.data.ScanSource
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ScanHistoryViewModel(
    private val repository: ScanHistoryRepository,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    val history: StateFlow<List<ScanHistoryEntity>> = repository.observeHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun recordScan(content: String, source: ScanSource) {
        if (content.isBlank()) return
        viewModelScope.launch {
            repository.add(content, currentTimeMillis(), source)
        }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory {
            val repository = RoomScanHistoryRepository(
                ScanHistoryDatabase.getInstance(context).scanHistoryDao(),
            )
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(ScanHistoryViewModel::class.java))
                    return ScanHistoryViewModel(repository) as T
                }
            }
        }
    }
}
