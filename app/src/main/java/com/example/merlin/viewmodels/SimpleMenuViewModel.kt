package com.example.merlin.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.merlin.ui.game.GameManager
import com.example.merlin.economy.service.AppSessionManager
import com.example.merlin.config.ServiceLocator
import android.graphics.drawable.Drawable
import kotlinx.coroutines.flow.*

class SimpleMenuViewModel(private val gameManager: GameManager) : ViewModel() {

    data class MenuItem(
        val id: String, 
        val title: String, 
        val emoji: String,
        val isApp: Boolean = false,
        val packageName: String? = null,
        val remainingMinutes: Long = 0,
        val appIcon: Drawable? = null
    )

    private val _menuItems = MutableStateFlow<List<MenuItem>>(emptyList())
    val menuItems: StateFlow<List<MenuItem>> = _menuItems
    
    private val appSessionManager = ServiceLocator.getAppSessionManager()

    init {
        loadMenuItems()
    }

    private fun loadMenuItems() {
        // Combine games and app sessions
        combine(
            gameManager.availableGamesFlow,
            appSessionManager.activeSessions
        ) { games, activeSessions ->
            // Create menu items for each available game
            val dynamicGames = games.map { game ->
                MenuItem(id = game.id, title = game.name, emoji = "🎮")
            }
            
            // Create menu items for active app sessions
            val activeApps = activeSessions.values.map { session ->
                MenuItem(
                    id = "app_${session.packageName}",
                    title = "${session.displayName} (${session.remainingTime}m)",
                    emoji = getAppEmoji(session.packageName), // Fallback emoji
                    isApp = true,
                    packageName = session.packageName,
                    remainingMinutes = session.remainingTime,
                    appIcon = session.appIcon // Real app icon from system
                )
            }
            
            // Construct the final list with static, dynamic, and app items
            listOf(
                MenuItem("chat", "Chat", "💬")
            ) + dynamicGames + activeApps + listOf(
                MenuItem("spend_coins", "Spend Coins", "💰")
            )
        }.onEach { items ->
            _menuItems.value = items
        }.launchIn(viewModelScope)
    }
    
    private fun getAppEmoji(packageName: String): String {
        return when (packageName) {
            "com.google.android.apps.youtube.kids" -> "📺"
            "com.android.camera" -> "📸"
            "com.google.android.calculator" -> "🧮"
            "com.android.settings" -> "⚙️"
            else -> "📱"
        }
    }
} 