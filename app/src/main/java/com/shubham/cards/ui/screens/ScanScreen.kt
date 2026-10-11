package com.shubham.cards.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shubham.cards.nfc.CardProperty
import com.shubham.cards.ui.CardsViewModel
import com.shubham.cards.ui.NfcStatus
import com.shubham.cards.ui.ScanState

@Composable
fun ScanScreen(viewModel: CardsViewModel, nfcStatus: NfcStatus) {
    val state by viewModel.scanState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (nfcStatus) {
            NfcStatus.UNSUPPORTED -> InfoCard(
                title = "This phone has no NFC",
                body = "Card reading needs NFC hardware, which this device doesn't have. " +
                    "You can still browse the app, but it can't read cards.",
            )
            NfcStatus.DISABLED -> InfoCard(
                title = "NFC is turned off",
                body = "Turn on NFC in Settings → Connected devices (or Quick Settings), " +
                    "then come back to this screen.",
            )
            NfcStatus.READY -> when (val s = state) {
                is ScanState.Read -> ReadResult(viewModel, s)
                is ScanState.Error -> InfoCard("Couldn't read that card", s.message)
                else -> WaitingForTap()
            }
        }

        Spacer(Modifier.height(24.dp))
        InfoCard(
            title = "Can't read a card at all?",
            body = "If tapping shows nothing, that card is almost certainly a 125 kHz " +
                "low-frequency card (common for parking/building access). Android phones " +
                "have no 125 kHz radio, so they physically cannot read those cards.",
            tonal = true,
        )
    }
}

@Composable
private fun WaitingForTap() {
    Spacer(Modifier.height(40.dp))
    Icon(
        Icons.Filled.Contactless,
        contentDescription = null,
        modifier = Modifier.height(96.dp),
        tint = MaterialTheme.colorScheme.primary,
    )
    Spacer(Modifier.height(16.dp))
    Text("Hold a card to the back of your phone", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(8.dp))
    Text(
        "Keep it still for a second. This reads the card only — it never writes to it.",
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun ReadResult(viewModel: CardsViewModel, s: ScanState.Read) {
    val i = s.inspection
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(i.displayTitle, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                "Technologies: ${i.technologies.joinToString(", ")}",
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(12.dp))
            i.properties.forEach { PropertyRow(it) }
            if (i.notes.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                i.notes.forEach {
                    Text("• $it", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
    Spacer(Modifier.height(16.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(onClick = { viewModel.saveCurrent() }, enabled = !s.saved) {
            Text(if (s.saved) "Saved ✓" else "Save card")
        }
        OutlinedButton(onClick = { viewModel.resetScan() }) { Text("Scan again") }
    }
}

@Composable
private fun PropertyRow(p: CardProperty) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(
            p.label + if (p.sensitive) " 🔒" else "",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.weight(0.42f),
        )
        Text(
            p.value,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            modifier = Modifier.weight(0.58f),
        )
    }
}

@Composable
private fun InfoCard(title: String, body: String, tonal: Boolean = false) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (tonal) CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ) else CardDefaults.cardColors(),
    ) {
        Row(Modifier.padding(16.dp)) {
            Icon(Icons.Filled.Info, contentDescription = null)
            Spacer(Modifier.height(0.dp))
            Column(Modifier.padding(start = 12.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(body, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
