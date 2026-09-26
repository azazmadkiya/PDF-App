package com.example.ui

import android.net.Uri
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.PdfDatabase
import com.example.data.PdfHistoryEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PdfViewModel(application: Application) : AndroidViewModel(application) {
    private val pdfDao = PdfDatabase.getDatabase(application).pdfDao()

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _incomingPdfUri = MutableStateFlow<Uri?>(null)
    val incomingPdfUri: StateFlow<Uri?> = _incomingPdfUri.asStateFlow()

    fun setIncomingPdfUri(uri: Uri?) {
        _incomingPdfUri.value = uri
    }

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    val historyList: StateFlow<List<PdfHistoryEntity>> = pdfDao.getAllHistory()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addHistory(title: String, uriString: String, actionType: String) {
        viewModelScope.launch {
            pdfDao.insertHistory(
                PdfHistoryEntity(
                    title = title,
                    uriString = uriString,
                    actionType = actionType
                )
            )
        }
    }

    fun deleteHistory(item: PdfHistoryEntity) {
        viewModelScope.launch {
            pdfDao.deleteHistory(item)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            pdfDao.clearAll()
        }
    }
}
