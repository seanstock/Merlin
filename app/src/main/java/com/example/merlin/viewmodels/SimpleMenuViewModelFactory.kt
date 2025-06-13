package com.example.merlin.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.merlin.ui.game.GameManager

class SimpleMenuViewModelFactory(private val gameManager: GameManager) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SimpleMenuViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SimpleMenuViewModel(gameManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 