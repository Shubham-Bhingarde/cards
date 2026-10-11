package com.shubham.cards.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shubham.cards.data.SavedCard
import com.shubham.cards.ui.CardsViewModel
import java.text.DateFormat
import java.util.Date

@Composable
fun SavedScreen(viewModel: CardsViewModel, onOpen: (Long) -> Unit) {
    val cards by viewModel.savedCards.collectAsStateWithLifecycle()

    if (cards.isEmpty()) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("No saved cards yet", style = MaterialTheme.typography.titleMedium)
            Text(
                "Scan a card and tap Save to keep its details here (encrypted on device).",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        return
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(cards, key = { it.id }) { card ->
            SavedRow(card, onOpen = { onOpen(card.id) }, onDelete = { viewModel.delete(card.id) })
        }
    }
}

@Composable
private fun SavedRow(card: SavedCard, onOpen: () -> Unit, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable { onOpen() }) {
        Column(Modifier.padding(16.dp)) {
            Column(Modifier.fillMaxWidth()) {
                Text(card.title, style = MaterialTheme.typography.titleMedium)
                Text(card.techSummary, style = MaterialTheme.typography.bodySmall)
                Text(
                    DateFormat.getDateTimeInstance().format(Date(card.createdAt)),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.align(Alignment.End)) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete card")
            }
        }
    }
}
