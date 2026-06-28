package com.expstudio.localai.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.expstudio.localai.LocalAiApp
import com.expstudio.localai.ui.screens.ChatScreen
import com.expstudio.localai.ui.screens.HomeScreen
import com.expstudio.localai.ui.screens.ModelManagerScreen
import com.expstudio.localai.ui.screens.OnboardingScreen
import com.expstudio.localai.ui.screens.SettingsScreen

private sealed class Dest(val route: String, val label: String, val icon: ImageVector) {
    data object Home : Dest("home", "Chats", Icons.AutoMirrored.Filled.Chat)
    data object Models : Dest("models", "Models", Icons.Filled.Download)
    data object Settings : Dest("settings", "Settings", Icons.Filled.Settings)
}

private val bottomItems = listOf(Dest.Home, Dest.Models, Dest.Settings)

@Composable
fun LocalAiNavHost() {
    val context = LocalContext.current
    val store = remember { (context.applicationContext as LocalAiApp).container.settingsStore }
    val settings by store.settings.collectAsState()

    if (!settings.onboarded) {
        OnboardingScreen(onDone = { store.setOnboarded(true) })
        return
    }

    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination

    // Hide the bottom bar inside a specific chat for an immersive conversation.
    val showBottomBar = currentRoute?.route?.startsWith("chat/") != true

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomItems.forEach { dest ->
                        val selected = currentRoute?.hierarchy?.any { it.route == dest.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(dest.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(dest.icon, contentDescription = dest.label) },
                            label = { Text(dest.label) },
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Dest.Home.route,
            modifier = androidx.compose.ui.Modifier.padding(padding),
        ) {
            composable(Dest.Home.route) {
                HomeScreen(
                    onOpenChat = { id -> navController.navigate("chat/$id") },
                    onBrowseModels = { navController.navigate(Dest.Models.route) },
                )
            }
            composable("chat/{sessionId}") { entry ->
                val id = entry.arguments?.getString("sessionId")?.toLongOrNull()
                ChatScreen(
                    sessionId = id,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Dest.Models.route) { ModelManagerScreen() }
            composable(Dest.Settings.route) { SettingsScreen() }
        }
    }
}
