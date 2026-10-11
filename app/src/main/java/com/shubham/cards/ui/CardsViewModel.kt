package com.shubham.cards.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shubham.cards.data.CardRepository
import com.shubham.cards.data.SavedCard
import com.shubham.cards.log.AppLogger
import com.shubham.cards.nfc.CardInspection
import com.shubham.cards.nfc.CardInspector
import android.nfc.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** What the scan screen is currently showing. */
sealed interface ScanState {
    data object Idle : ScanState
    data object Waiting : ScanState
    data class Read(val inspection: CardInspection, val saved: Boolean) : ScanState
    data class Error(val message: String) : ScanState
}

class CardsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = CardRepository(app)
    private val tag = "CardsViewModel"

    val savedCards: StateFlow<List<SavedCard>> =
        repo.cards.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState

    private val _verbose = MutableStateFlow(false)
    val verbose: StateFlow<Boolean> = _verbose

    fun setWaiting() {
        _scanState.value = ScanState.Waiting
    }

    /** Called from the reader-mode callback when a card is tapped. */
    fun onTagDiscovered(nfcTag: Tag) {
        viewModelScope.launch {
            try {
                // Tech I/O (e.g. NDEF connect) is blocking — keep it off the main thread.
                val inspection = withContext(Dispatchers.IO) { CardInspector.inspect(nfcTag) }
                logInspection(inspection)
                _scanState.value = ScanState.Read(inspection, saved = false)
            } catch (e: Exception) {
                AppLogger.e(tag, "Failed to inspect tag", e)
                _scanState.value = ScanState.Error(e.message ?: "Unknown read error")
            }
        }
    }

    fun saveCurrent() {
        val current = _scanState.value
        if (current is ScanState.Read && !current.saved) {
            viewModelScope.launch {
                repo.save(current.inspection)
                _scanState.value = current.copy(saved = true)
            }
        }
    }

    fun resetScan() {
        _scanState.value = ScanState.Idle
    }

    fun delete(id: Long) = viewModelScope.launch { repo.delete(id) }

    fun deleteAll() = viewModelScope.launch { repo.deleteAll() }

    fun setVerbose(enabled: Boolean) {
        _verbose.value = enabled
        AppLogger.setVerbose(enabled)
    }

    fun clearLogs() = AppLogger.clear()

    private fun logInspection(i: CardInspection) {
        AppLogger.i(tag, "Card read: kind='${i.cardKind}' techs=[${i.technologies.joinToString(", ")}] hasNdef=${i.hasNdef}")
        i.properties.forEach { AppLogger.value(tag, it.label, it.value, it.sensitive) }
        i.notes.forEach { AppLogger.i(tag, "note: $it") }
    }
}
