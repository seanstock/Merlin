package com.example.merlin.ui

import android.app.Application
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.statusBarsPadding
import com.example.merlin.R
import com.example.merlin.ui.game.GameManager
import com.example.merlin.ui.theme.AppleCard
import com.example.merlin.ui.theme.AppleSpacing
import com.example.merlin.ui.theme.AppTheme
import com.example.merlin.ui.theme.AppThemes
import com.example.merlin.ui.wallet.WalletDisplay
import com.example.merlin.ui.wallet.WalletViewModel
import com.example.merlin.ui.wallet.WalletViewModelFactory
import com.example.merlin.utils.UserSessionRepository
import com.example.merlin.viewmodels.SimpleMenuViewModel
import com.example.merlin.viewmodels.SimpleMenuViewModelFactory
import com.example.merlin.config.ServiceLocator
import kotlinx.coroutines.flow.flowOf

/**
 * Simple icon-based main menu for ages 3-4, using the app's design system.
 */
@Composable
fun SimpleMainMenuScreen(
    onNavigateToGames: (String) -> Unit,
    onNavigateToChat: () -> Unit,
    onSpendCoins: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val coroutineScope = rememberCoroutineScope()
    
    // Game and Menu ViewModels
    val gameManager = remember { GameManager.getInstance(context, coroutineScope) }
    val menuViewModel: SimpleMenuViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = remember { SimpleMenuViewModelFactory(gameManager) }
    )
    val menuItems by menuViewModel.menuItems.collectAsState()

    // Wallet ViewModel for coin balance
    val userSessionRepository = remember { UserSessionRepository.getInstance(context) }
    val activeChildId by produceState<String?>(initialValue = null, producer = {
        value = userSessionRepository.getActiveChildId()
    })
    
    val walletViewModel: WalletViewModel? = activeChildId?.let { childId ->
        androidx.lifecycle.viewmodel.compose.viewModel(
            factory = WalletViewModelFactory(application, childId)
        )
    }
    val coinBalance by walletViewModel?.balance?.collectAsState() ?: remember { mutableStateOf(0) }
    val walletLoading by walletViewModel?.isLoading?.collectAsState() ?: remember { mutableStateOf(false) }

    // Theme service and current theme
    val themeService = remember { ServiceLocator.getThemeService(context) }
    // CHANGE HERE: Initialize with null instead of a default theme.
    val currentTheme by remember(activeChildId) {
        val childId = activeChildId
        if (childId != null) {
            themeService.getThemeFlowForChild(childId)
        } else {
            flowOf(AppThemes.getDefaultTheme())
        }
    }.collectAsState(initial = null) // Set initial value to null

    // Show a loading state or a neutral background until the theme is loaded.
    if (currentTheme == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(color = Color.White), // A neutral background
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return // Stop execution here until theme is loaded
    }

    // Once the theme is not null, the rest of the composable will render with the correct theme.
    Box(modifier = modifier.fillMaxSize()) {
        // Background Image from the now-loaded currentTheme
        Image(
            painter = painterResource(id = currentTheme!!.backgroundImage), // Use !! because we already checked for null
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillHeight
        )

        // Main content with proper spacing for top status bar
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Status Bar with system bar padding
            TopStatusBar(
                title = currentTheme!!.tutorName,
                balance = coinBalance,
                isLoading = walletLoading,
                onSpendCoins = onSpendCoins,
                onSettingsClick = onNavigateToSettings,
                modifier = Modifier.statusBarsPadding()
            )

            // Centered 2x2 Grid Menu - takes remaining space
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = AppleSpacing.large),
                contentAlignment = Alignment.Center
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 120.dp), // Reduced from 160dp
                    contentPadding = PaddingValues(AppleSpacing.small), // Reduced padding
                    verticalArrangement = Arrangement.spacedBy(AppleSpacing.medium), // Reduced spacing
                    horizontalArrangement = Arrangement.spacedBy(AppleSpacing.medium) // Reduced spacing
                ) {
                    items(menuItems) { item ->
                        SimpleMenuItem(
                            item = item,
                            onClick = {
                                when (item.id) {
                                    "chat" -> onNavigateToChat()
                                    "spend_coins" -> onSpendCoins()
                                    else -> onNavigateToGames(item.id)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SimpleMenuItem(
    item: SimpleMenuViewModel.MenuItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .background(Color.Black.copy(alpha = 0.4f))
            .padding(AppleSpacing.small)
            .width(110.dp)
            .height(110.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = item.emoji,
            fontSize = 40.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = item.title,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun TopStatusBar(
    title: String,
    balance: Int,
    isLoading: Boolean,
    onSpendCoins: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.3f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Title
        Text(
            text = title,
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        // Wallet and Settings
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Use the proper WalletDisplay component
            WalletDisplay(
                balance = balance,
                isLoading = isLoading,
                onClick = onSpendCoins
            )
            
            // Settings Icon
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
} 