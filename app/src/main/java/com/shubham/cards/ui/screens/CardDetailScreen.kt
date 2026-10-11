package com.shubham.cards.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shubham.cards.ui.CardsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailScreen(viewModel: CardsViewModel, cardId: Long, onBack: () -> Unit) {
    val cards by viewModel.savedCards.collectAsStateWithLifecycle()
    val card = cards.firstOrNull { it.id == cardId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(card?.title ?: "Card") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (card == null) {
            Text("Card not found.", Modifier.padding(padding).padding(16.dp))
            return@Scaffold
        }
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Text(card.techSummary, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(16.dp))
            card.properties.forEach { p ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
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
        }
    }
}
