package com.shubham.cards

import android.nfc.NfcAdapter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.shubham.cards.log.AppLogger
import com.shubham.cards.ui.CardsViewModel
import com.shubham.cards.ui.NfcStatus
import com.shubham.cards.ui.app.CardsRoot
import com.shubham.cards.ui.theme.CardsTheme

/**
 * Single activity. NFC is driven by Reader Mode (the modern, foreground-only API)
 * rather than intent filters, so the app reads cards only while open and in the
 * foreground — it never silently intercepts tags in the background.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: CardsViewModel by viewModels()
    private var nfcAdapter: NfcAdapter? = null

    private val readerFlags =
        NfcAdapter.FLAG_READER_NFC_A or
            NfcAdapter.FLAG_READER_NFC_B or
            NfcAdapter.FLAG_READER_NFC_F or
            NfcAdapter.FLAG_READER_NFC_V or
            NfcAdapter.FLAG_READER_NFC_BARCODE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        setContent {
            CardsTheme {
                val status = remember { mutableStateOf(currentNfcStatus()) }
                // Re-evaluate when composition restarts (e.g. after returning from settings).
                status.value = currentNfcStatus()
                CardsRoot(viewModel = viewModel, nfcStatus = status.value)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        enableReaderMode()
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableReaderMode(this)
    }

    private fun enableReaderMode() {
        val adapter = nfcAdapter ?: return
        if (!adapter.isEnabled) return
        adapter.enableReaderMode(
            this,
            { tag ->
                AppLogger.d("MainActivity", "Tag discovered via reader mode")
                viewModel.onTagDiscovered(tag)
            },
            readerFlags,
            null,
        )
        viewModel.setWaiting()
    }

    private fun currentNfcStatus(): NfcStatus = when {
        nfcAdapter == null -> NfcStatus.UNSUPPORTED
        nfcAdapter?.isEnabled == false -> NfcStatus.DISABLED
        else -> NfcStatus.READY
    }
}
