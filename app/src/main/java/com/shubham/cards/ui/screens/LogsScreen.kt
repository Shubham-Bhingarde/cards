package com.shubham.cards.ui.screens

import android.content.Intent
import android.widget.Toast
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shubham.cards.log.AppLogger
import com.shubham.cards.log.LogExporter
import com.shubham.cards.ui.CardsViewModel

@Composable
fun LogsScreen(viewModel: CardsViewModel) {
    val context = LocalContext.current
    val verbose by viewModel.verbose.collectAsStateWithLifecycle()
    // Snapshot the log text; refresh on demand to keep this screen cheap.
    var logText by remember { mutableStateOf(AppLogger.readAll()) }

    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Diagnostics", style = MaterialTheme.typography.titleLarge)

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Verbose logging", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Off: identifiers (UID, payloads) are redacted in logs. " +
                                "On: logs include them — only enable if you understand the privacy trade-off.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Switch(checked = verbose, onCheckedChange = { viewModel.setVerbose(it) })
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = {
                val intent = LogExporter.shareIntent(context)
                if (intent != null) {
                    context.startActivity(Intent.createChooser(intent, "Share diagnostics log"))
                } else {
                    Toast.makeText(context, "No log to share yet", Toast.LENGTH_SHORT).show()
                }
            }) { Text("Share log") }

            OutlinedButton(onClick = { logText = AppLogger.readAll() }) { Text("Refresh") }

            OutlinedButton(onClick = {
                viewModel.clearLogs()
                logText = ""
            }) { Text("Clear") }
        }

        Card(Modifier.fillMaxWidth()) {
            Text(
                text = logText.ifBlank { "Log is empty. Scan a card, then tap Refresh." },
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                modifier = Modifier.padding(12.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}
