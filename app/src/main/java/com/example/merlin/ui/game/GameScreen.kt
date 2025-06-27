package com.example.merlin.ui.game

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.util.Log
import com.example.merlin.economy.service.LocalEconomyService
import com.example.merlin.data.repository.EconomyStateRepository
import com.example.merlin.data.repository.ChildProfileRepository
import com.example.merlin.data.database.DatabaseProvider
import com.example.merlin.utils.UserSessionRepository

/**
 * Main game screen that provides game selection and gameplay interface.
 * Integrates WebView games with native Android UI components and coin earning system.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    gameId: String? = null // Optional game ID to launch directly
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Economy service setup for coin earning
    val database = remember { DatabaseProvider.getInstance(context) }
    val economyStateRepository = remember { EconomyStateRepository(database.economyStateDao()) }
    val childProfileRepository = remember { ChildProfileRepository(database.childProfileDao()) }
    val economyService = remember { LocalEconomyService(economyStateRepository, childProfileRepository) }
    val userSessionRepository = remember { UserSessionRepository.getInstance(context) }
    
    // Game manager (pre-initialized at application startup) and result handler
    val gameManager = remember { GameManager.getInstance(context, coroutineScope) }
    val gameResultHandler = remember { GameResultHandler(coroutineScope) }
    
    // Screen state
    var selectedGame by remember { mutableStateOf<GameMetadata?>(null) }
    var currentLevel by remember { mutableIntStateOf(1) }
    var gameResult by remember { mutableStateOf<GameResult?>(null) }
    var showGameSelection by remember { mutableStateOf(gameId == null) }
    
    // Coin earning state
    var coinEarningMessage by remember { mutableStateOf<String?>(null) }
    
    // Available games (loaded instantly from static registry)
    val availableGames by gameManager.availableGamesFlow.collectAsState()
    
    // If a gameId is passed, find and set it as the selected game
    LaunchedEffect(gameId, availableGames) {
        if (gameId != null && availableGames.isNotEmpty()) {
            val gameToLaunch = availableGames.find { it.id == gameId }
            if (gameToLaunch != null) {
                selectedGame = gameToLaunch
                showGameSelection = false
                gameManager.preloadGame(gameToLaunch.id, 1)
            }
        }
    }

    // Show coin earning messages temporarily
    LaunchedEffect(coinEarningMessage) {
        if (coinEarningMessage != null) {
            delay(3000) // Show for 3 seconds
            coinEarningMessage = null
        }
    }

    // Coin earning callback
    val onCoinEarned: (Int, String, String) -> Unit = { amount, gameId, source ->
        coroutineScope.launch {
            try {
                val childId = userSessionRepository.getActiveChildId()
                if (childId != null) {
                    Log.d("GameScreen", "Attempting to award $amount coins for $gameId - $source")
                    
                    val result = economyService.awardGameCoins(
                        childId = childId,
                        amount = amount,
                        gameId = gameId,
                        source = source
                    )
                    
                    result.onSuccess { earningDto ->
                        val message = if (earningDto.wasLimited) {
                            "Daily limit reached! You can earn ${earningDto.remainingToday} more coins today."
                        } else {
                            "Earned ${earningDto.coinsAwarded} Merlin Coins! 🎉"
                        }
                        coinEarningMessage = message
                        Log.d("GameScreen", "Coin earning success: $message")
                    }.onFailure { error ->
                        coinEarningMessage = "Error earning coins: ${error.message}"
                        Log.e("GameScreen", "Coin earning failed", error)
                    }
                } else {
                    Log.w("GameScreen", "No active child ID found for coin earning")
                    coinEarningMessage = "Error: No active child profile"
                }
            } catch (e: Exception) {
                Log.e("GameScreen", "Unexpected error in coin earning", e)
                coinEarningMessage = "Unexpected error earning coins"
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Top App Bar
        TopAppBar(
            title = { 
                Text(
                    text = if (showGameSelection) "Select Game" else selectedGame?.name ?: "Game",
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = {
                    // Always navigate back to main UI instead of game selection
                    onNavigateBack()
                }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                if (!showGameSelection) {
                    IconButton(onClick = {
                        // Restart current game
                        selectedGame?.let { game ->
                            gameResult = null
                        }
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Restart")
                    }
                }
            }
        )

        // Coin earning message overlay
        coinEarningMessage?.let { message ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = message,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Content
        if (showGameSelection) {
            GameSelectionContent(
                games = availableGames,
                onGameSelected = { game ->
                    selectedGame = game
                    currentLevel = 1
                    showGameSelection = false
                    gameManager.preloadGame(game.id, currentLevel)
                }
            )
        } else {
            selectedGame?.let { game ->
                GamePlayContent(
                    game = game,
                    level = currentLevel,
                    gameResult = gameResult,
                    onGameComplete = { success, timeMs, score ->
                        gameResultHandler.handleGameResult(
                            gameId = game.id,
                            level = currentLevel,
                            success = success,
                            timeMs = timeMs,
                            rawScore = score
                        ) { result ->
                            gameResult = result
                        }
                    },
                    onCoinEarned = onCoinEarned,
                    onNextLevel = {
                        currentLevel++
                        gameResult = null
                        gameManager.preloadGame(game.id, currentLevel)
                    },
                    onRestartGame = {
                        gameResult = null
                    }
                )
            }
        }
    }
}

/**
 * Game selection screen content.
 */
@Composable
private fun GameSelectionContent(
    games: List<GameMetadata>,
    onGameSelected: (GameMetadata) -> Unit,
    modifier: Modifier = Modifier
) {
    if (games.isEmpty()) {
        // No games available
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "No Games Available",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Games will appear here when they are added to the app.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        // Games list
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Choose a game to play:",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            items(games) { game ->
                GameCard(
                    game = game,
                    onClick = { onGameSelected(game) }
                )
            }
        }
    }
}

/**
 * Individual game card in the selection list.
 */
@Composable
private fun GameCard(
    game: GameMetadata,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = game.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = game.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Max Level: ${game.maxLevel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                
                if (game.supportedFeatures.isNotEmpty()) {
                    Text(
                        text = game.supportedFeatures.take(2).joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Game play content with WebView and result display.
 */
@Composable
private fun GamePlayContent(
    game: GameMetadata,
    level: Int,
    gameResult: GameResult?,
    onGameComplete: (Boolean, Long, Int) -> Unit,
    onCoinEarned: (Int, String, String) -> Unit,
    onNextLevel: () -> Unit,
    onRestartGame: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Game info header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = game.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Level $level",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                gameResult?.let { result ->
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Score: ${result.finalScore}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = result.performance.name.lowercase().replace("_", " "),
                            style = MaterialTheme.typography.bodySmall,
                            color = when (result.performance) {
                                GamePerformance.EXCELLENT -> MaterialTheme.colorScheme.primary
                                GamePerformance.GOOD -> MaterialTheme.colorScheme.secondary
                                GamePerformance.AVERAGE -> MaterialTheme.colorScheme.tertiary
                                GamePerformance.NEEDS_IMPROVEMENT -> MaterialTheme.colorScheme.error
                                GamePerformance.FAILED -> MaterialTheme.colorScheme.error
                            }
                        )
                    }
                }
            }
        }

        // Game WebView
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            GameWebView(
                gameId = game.id,
                level = level,
                onGameComplete = onGameComplete,
                onCoinEarned = onCoinEarned,
                onGameError = { error ->
                    // Handle game errors
                    // Could show error dialog or fallback UI
                }
            )
        }

        // Game result and controls
        gameResult?.let { result ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (result.success) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.errorContainer
                    }
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (result.success) "Level Complete!" else "Game Over",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Time: ${formatTime(result.timeMs)}")
                        Text("Score: ${result.finalScore}")
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onRestartGame,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Play Again")
                        }
                        
                        if (result.success && level < game.maxLevel) {
                            Button(
                                onClick = onNextLevel,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Next Level")
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Format time in milliseconds to a readable string.
 */
private fun formatTime(timeMs: Long): String {
    val seconds = timeMs / 1000
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    
    return if (minutes > 0) {
        "${minutes}m ${remainingSeconds}s"
    } else {
        "${remainingSeconds}s"
    }
} 