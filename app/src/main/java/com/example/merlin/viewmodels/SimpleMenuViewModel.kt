package com.example.merlin.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.merlin.ui.game.GameManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class SimpleMenuViewModel(private val gameManager: GameManager) : ViewModel() {

    data class MenuItem(val id: String, val title: String, val emoji: String)

    private val _menuItems = MutableStateFlow<List<MenuItem>>(emptyList())
    val menuItems: StateFlow<List<MenuItem>> = _menuItems

    init {
        loadMenuItems()
    }

    private fun loadMenuItems() {
        // Observe the list of available games from GameManager
        gameManager.availableGamesFlow.onEach { games ->
            // Create menu items for each available game
            val dynamicGames = games.map { game ->
                MenuItem(id = game.id, title = game.name, emoji = "🎮")
            }
            
            // Construct the final list with static and dynamic items
            _menuItems.value = listOf(
                MenuItem("chat", "Chat", "💬")
            ) + dynamicGames + listOf(
                MenuItem("spend_coins", "Spend Coins", "🪙")
            )
        }.launchIn(viewModelScope)
    }
} 