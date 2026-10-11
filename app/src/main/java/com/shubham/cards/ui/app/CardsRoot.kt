package com.shubham.cards.ui.app

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.shubham.cards.ui.CardsViewModel
import com.shubham.cards.ui.NfcStatus
import com.shubham.cards.ui.screens.CardDetailScreen
import com.shubham.cards.ui.screens.LogsScreen
import com.shubham.cards.ui.screens.SavedScreen
import com.shubham.cards.ui.screens.ScanScreen

private sealed class Dest(val route: String, val label: String, val icon: ImageVector) {
    data object Scan : Dest("scan", "Scan", Icons.Filled.Contactless)
    data object Saved : Dest("saved", "Saved", Icons.AutoMirrored.Filled.List)
    data object Logs : Dest("logs", "Logs", Icons.Filled.Description)
}

@Composable
fun CardsRoot(viewModel: CardsViewModel, nfcStatus: NfcStatus) {
    val navController = rememberNavController()
    val tabs = listOf(Dest.Scan, Dest.Saved, Dest.Logs)

    Scaffold(
        bottomBar = {
            val backStack by navController.currentBackStackEntryAsState()
            val current = backStack?.destination
            NavigationBar {
                tabs.forEach { dest ->
                    val selected = current?.hierarchy?.any { it.route == dest.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(dest.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(dest.icon, contentDescription = dest.label) },
                        label = { Text(dest.label) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Dest.Scan.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(Dest.Scan.route) { ScanScreen(viewModel, nfcStatus) }
            composable(Dest.Saved.route) { backStackEntry ->
                SavedScreen(
                    viewModel = viewModel,
                    onOpen = { id -> navController.navigate("detail/$id") },
                )
            }
            composable(Dest.Logs.route) { LogsScreen(viewModel) }
            composable("detail/{id}") { entry ->
                val id = entry.arguments?.getString("id")?.toLongOrNull() ?: -1L
                CardDetailScreen(
                    viewModel = viewModel,
                    cardId = id,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
