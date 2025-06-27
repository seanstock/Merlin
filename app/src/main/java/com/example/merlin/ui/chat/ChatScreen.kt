package com.example.merlin.ui.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.example.merlin.ui.theme.*
import com.example.merlin.ui.accessibility.AccessibilityConstants
import com.example.merlin.utils.UserSessionRepository
import androidx.compose.ui.text.TextStyle
import androidx.compose.material3.LocalTextStyle
// Wallet imports
import com.example.merlin.ui.wallet.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.Manifest

/**
 * Main chat screen for interacting with Merlin the AI tutor.
 * Optimized for performance with gradient caching, animation management, and memory efficiency.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit = { },
    onLaunchGame: (gameId: String, level: Int) -> Unit = { _, _ -> },
    showNavigateBack: Boolean = true,
    modifier: Modifier = Modifier,
    chatViewModelFactory: ChatViewModelFactory? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // ⚡ PERFORMANCE OPTIMIZATION: Initialize performance monitoring
    LaunchedEffect(Unit) {
        ChatScreenPerformance.PerformanceMonitor.recordFrameTime()
        // Set overlay mode if running from lock screen
        ChatScreenPerformance.LockScreenOptimization.setOverlayMode(showNavigateBack == false)
    }
    
    // Performance-aware cleanup
    PerformanceAwareDisposableEffect()
    
    // Get the active child ID and create the ViewModel
    val userSessionRepository = remember { UserSessionRepository.getInstance(context) }
    val activeChildId = userSessionRepository.getActiveChildId() ?: "unknown_child"
    
    val factory = chatViewModelFactory ?: ChatViewModelFactory(
        application = context.applicationContext as android.app.Application,
        childId = activeChildId
    )
    
    val viewModel: ChatViewModel = viewModel(factory = factory)
    
    // 💰 WALLET INTEGRATION: Create wallet ViewModel
    val walletViewModelFactory = remember { 
        WalletViewModelFactory(
            application = context.applicationContext as android.app.Application,
            childId = activeChildId
        )
    }
    val walletViewModel: WalletViewModel = viewModel(factory = walletViewModelFactory)
    
    // App Launch Service for spend coins dialog
    val appLaunchService = remember { 
        com.example.merlin.config.ServiceLocator.getAppLaunchService(context) 
    }
    
    // State
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentInput by viewModel.currentInput.collectAsState()
    val isTtsEnabled by viewModel.isTtsEnabled.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val gameLaunchEvent by viewModel.gameLaunchEvent.collectAsState()
    
    // 💰 WALLET STATE
    val walletBalance by walletViewModel.balance.collectAsState()
    val walletLoading by walletViewModel.isLoading.collectAsState()
    val walletError by walletViewModel.errorMessage.collectAsState()
    val notificationEvent by walletViewModel.notificationEvent.collectAsState()
    
    // 💰 WALLET UI STATE
    var showSpendDialog by remember { mutableStateOf(false) }
    
    // 🎙️ VOICE CHAT STATE
    var isVoiceChatActive by remember { mutableStateOf(false) }
    var voiceTranscript by remember { mutableStateOf("") }
    var pendingVoiceSend by remember { mutableStateOf(false) }
    
    // Speech-to-Text manager with performance optimization
    val speechToTextManager = remember { SpeechToTextManager(context) }
    val isListening by speechToTextManager.isListeningFlow.collectAsState()
    val speechError by speechToTextManager.errorFlow.collectAsState()
    val isSTTAvailable by speechToTextManager.isAvailableFlow.collectAsState()
    
    // List state for auto-scrolling with performance optimization
    val listState = rememberLazyListState()
    
    // ⚡ PERFORMANCE OPTIMIZATION: Check if we should virtualize messages
    val shouldVirtualize = ChatScreenPerformance.MessageOptimization.shouldVirtualizeMessages(messages.size)
    
    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }
    
    // Handle game launch events
    LaunchedEffect(gameLaunchEvent) {
        gameLaunchEvent?.let { event ->
            onLaunchGame(event.gameId, event.level)
            viewModel.clearGameLaunchEvent()
        }
    }
    
    // 💰 WALLET NOTIFICATIONS: Handle wallet notification events
    LaunchedEffect(notificationEvent) {
        notificationEvent?.let { event ->
            when (event) {
                is WalletNotificationEvent.EarningSuccess -> {
                    // Show earning notification
                    // TODO: Add snackbar or toast
                }
                is WalletNotificationEvent.SpendingSuccess -> {
                    // Show spending success notification  
                    // TODO: Add snackbar or toast
                }
                is WalletNotificationEvent.BalanceWarning -> {
                    // Show balance warning
                    // TODO: Add snackbar or toast
                }
                is WalletNotificationEvent.ScreenTimeExpired -> {
                    // Handle screen time expiration
                    // TODO: Re-enable lock screen
                }
            }
            walletViewModel.clearNotificationEvent()
        }
    }
    
    // Cleanup speech recognition on dispose
    DisposableEffect(Unit) {
        onDispose {
            speechToTextManager.cleanup()
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            // Permission granted, start voice chat immediately
            isVoiceChatActive = true
            voiceTranscript = ""
            pendingVoiceSend = true
            speechToTextManager.startListening { recognizedText ->
                voiceTranscript = recognizedText
            }
        }
    }

    fun ensureMicPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            // Already granted
            isVoiceChatActive = true
            voiceTranscript = ""
            pendingVoiceSend = true
            speechToTextManager.startListening { recognizedText ->
                voiceTranscript = recognizedText
            }
        } else {
            // Request permission
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // 🌿 SOFT SCANDINAVIAN BACKGROUND 🌿
        val scandinavianBackgroundGradient = rememberOptimizedGradient(
            key = "scandinavian_background",
            colors = listOf(
                Color(0xFFF8F9FA), // Off-white
                Color(0xFFF3F4F6), // Light gray
                Color(0xFFE5E7EB)  // Slightly darker gray
            ),
            isVertical = true
        )
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(scandinavianBackgroundGradient)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top App Bar - Minimalist Scandinavian Style
                TopAppBar(
                    title = { 
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.semantics {
                                contentDescription = "Chat with Merlin AI"
                                heading()
                            }
                        ) {
                            Text(
                                text = "🧙‍♂️ Merlin",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF475569) // Soft slate
                            )
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .semantics {
                                            contentDescription = AccessibilityConstants.ContentDescriptions.LOADING_INDICATOR
                                        },
                                    strokeWidth = 3.dp,
                                    color = Color(0xFF059669) // Soft emerald
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        if (showNavigateBack) {
                            IconButton(
                                onClick = onNavigateBack,
                                modifier = Modifier
                                    .size(AccessibilityConstants.RECOMMENDED_TOUCH_TARGET)
                                    .semantics {
                                        contentDescription = AccessibilityConstants.ContentDescriptions.BACK_BUTTON
                                        role = Role.Button
                                    }
                            ) {
                                Icon(
                                    Icons.Default.ArrowBack, 
                                    contentDescription = null,
                                    tint = Color(0xFF64748B), // Soft slate
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    },
                    actions = {
                        // 💰 WALLET DISPLAY
                        WalletDisplay(
                            balance = walletBalance,
                            isLoading = walletLoading,
                            onClick = { showSpendDialog = true },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        
                        // Small TTS button in top bar
                        IconButton(
                            onClick = { viewModel.toggleTts() },
                            modifier = Modifier
                                .size(40.dp) // Smaller size
                                .clip(CircleShape)
                                .background(
                                    if (isTtsEnabled) 
                                        Color(0xFF10B981).copy(alpha = 0.1f) // Soft emerald background
                                    else 
                                        Color(0xFF6B7280).copy(alpha = 0.1f) // Soft gray background
                                )
                                .semantics {
                                    contentDescription = if (isTtsEnabled) {
                                        "Disable Merlin voice replies"
                                    } else {
                                        "Enable Merlin voice replies"
                                    }
                                    role = Role.Button
                                }
                        ) {
                            Icon(
                                imageVector = if (isTtsEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                contentDescription = null,
                                tint = if (isTtsEnabled) Color(0xFF10B981) else Color(0xFF6B7280), // Soft colors
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        
                        // Settings button
                        IconButton(
                            onClick = onNavigateToSettings,
                            modifier = Modifier
                                .size(AccessibilityConstants.RECOMMENDED_TOUCH_TARGET)
                                .semantics {
                                    contentDescription = AccessibilityConstants.ContentDescriptions.SETTINGS
                                    role = Role.Button
                                }
                        ) {
                            Icon(
                                Icons.Default.Settings, 
                                contentDescription = null,
                                tint = Color(0xFF64748B), // Soft slate
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        
                        // Clear chat
                        IconButton(
                            onClick = { viewModel.clearChat() },
                            modifier = Modifier
                                .size(AccessibilityConstants.RECOMMENDED_TOUCH_TARGET)
                                .semantics {
                                    contentDescription = AccessibilityConstants.ContentDescriptions.CLEAR_CHAT
                                    role = Role.Button
                                }
                        ) {
                            Icon(
                                Icons.Default.Refresh, 
                                contentDescription = null,
                                tint = Color(0xFF6B7280), // Soft gray
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFFFFFFFF) // Pure white
                    )
                )

                // 💰 WALLET ERROR MESSAGE
                walletError?.let { error ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { walletViewModel.clearError() }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                // Error message
                errorMessage?.let { error ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { viewModel.clearError() }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                // Speech error
                speechError?.let { error ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { speechToTextManager.clearError() }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                // Messages list
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = AccessibilityConstants.SemanticRoles.CHAT_AREA
                        },
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(messages, key = { it.id }) { message ->
                        MessageBubble(
                            message = message,
                            onGameLaunch = onLaunchGame
                        )
                    }
                }

                // Large Talk Button for Toddlers
                ToddlerTalkButton(
                    isVoiceChatActive = isVoiceChatActive,
                    isLoading = isLoading,
                    onStartVoiceChat = {
                        println("🎙️ Voice chat button pressed - STARTING voice chat")
                        ensureMicPermissionAndStart()
                    },
                    onStopVoiceChat = {
                        println("🎙️ Voice chat button pressed - STOPPING voice chat")
                        isVoiceChatActive = false
                        speechToTextManager.stopListening()
                    }
                )
            }
        }
    }
    
    // 💰 SPEND COINS DIALOG
    if (showSpendDialog) {
        SpendCoinsDialog(
            currentBalance = walletBalance,
            appLaunchService = appLaunchService,
            childId = activeChildId,
            onSpendCoins = { timeInSeconds, category ->
                walletViewModel.spendCoins(timeInSeconds, category)
            },
            onDismiss = { showSpendDialog = false }
        )
    }

    // Auto-reset voice chat button colour when STT stops
    LaunchedEffect(isListening) {
        if (!isListening) {
            isVoiceChatActive = false
            if (pendingVoiceSend && voiceTranscript.isNotBlank()) {
                viewModel.sendMessage(voiceTranscript)
            }
            pendingVoiceSend = false
            voiceTranscript = ""
        }
    }
}

/**
 * Individual message bubble component with performance optimization.
 */
@Composable
private fun MessageBubble(
    message: ChatMessage,
    onGameLaunch: (gameId: String, level: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    
    // ⚡ PERFORMANCE OPTIMIZATION: Use soft Scandinavian gradients for avatars
    val merlinAvatarGradient = rememberOptimizedGradient(
        key = "merlin_avatar",
        colors = listOf(Color(0xFF6366F1), Color(0xFF4F46E5)) // Soft indigo
    )
    
    val userAvatarGradient = rememberOptimizedGradient(
        key = "user_avatar", 
        colors = listOf(Color(0xFF10B981), Color(0xFF059669)) // Soft emerald
    )
    
    // ⚡ PERFORMANCE OPTIMIZATION: Record frame time for monitoring
    LaunchedEffect(Unit) {
        ChatScreenPerformance.PerformanceMonitor.recordFrameTime()
    }
    
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isFromUser) Arrangement.End else Arrangement.Start
    ) {
        if (!message.isFromUser) {
            // 🧙‍♂️ MERLIN'S SOPHISTICATED AVATAR 🧙‍♂️
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(merlinAvatarGradient)
                    .semantics {
                        contentDescription = AccessibilityConstants.ContentDescriptions.MERLIN_AVATAR
                        role = Role.Image
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🧙‍♂️",
                    fontSize = 24.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
        }
        
        Column(
            modifier = Modifier.widthIn(max = 280.dp),
            horizontalAlignment = if (message.isFromUser) Alignment.End else Alignment.Start
        ) {
            // 🎨 ELEGANT MESSAGE BUBBLE 🎨
            Card(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (message.isFromUser) 16.dp else 4.dp,
                    bottomEnd = if (message.isFromUser) 4.dp else 16.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (message.isFromUser) {
                        Color(0xFF10B981).copy(alpha = 0.1f) // Soft emerald background
                    } else if (message.hasError) {
                        Color(0xFFEF4444).copy(alpha = 0.1f) // Soft red background
                    } else {
                        Color(0xFF6366F1).copy(alpha = 0.08f) // Soft indigo background
                    }
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.semantics {
                    contentDescription = if (message.isFromUser) {
                        "${AccessibilityConstants.ContentDescriptions.CHAT_MESSAGE_FROM_USER}: ${message.content}"
                    } else if (message.hasError) {
                        "${AccessibilityConstants.ContentDescriptions.ERROR_MESSAGE}: ${message.content}"
                    } else {
                        "${AccessibilityConstants.ContentDescriptions.CHAT_MESSAGE_FROM_MERLIN}: ${message.content}"
                    }
                }
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = message.content,
                        color = if (message.isFromUser) {
                            Color(0xFF059669) // Darker emerald for text
                        } else if (message.hasError) {
                            Color(0xFFDC2626) // Darker red for error text
                        } else {
                            Color(0xFF1E293B) // Dark slate for text
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        fontSize = AccessibilityConstants.CHILD_BODY_TEXT * 1.5f, // 50% bigger font size for toddlers
                        lineHeight = 33.sp
                    )
                    
                    // Function call button (e.g., for launching games)
                    message.functionCall?.let { functionCall ->
                        if (functionCall.name == "launch_game") {
                            val gameId = functionCall.arguments["game_id"] as? String
                            val level = (functionCall.arguments["level"] as? Number)?.toInt() ?: 1
                            
                            if (gameId != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { onGameLaunch(gameId, level) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = AccessibilityConstants.RECOMMENDED_TOUCH_TARGET)
                                        .semantics {
                                            contentDescription = "Launch $gameId game at level $level"
                                            role = Role.Button
                                        },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondary
                                    )
                                ) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = null, // Already described by button
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "Play Game",
                                        fontSize = AccessibilityConstants.CHILD_BODY_TEXT
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Timestamp
            Text(
                text = timeFormat.format(Date(message.timestamp)),
                style = MaterialTheme.typography.bodySmall,
                fontSize = 14.sp, // Keep timestamp text normal size
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                    .semantics {
                        contentDescription = "Message sent at ${timeFormat.format(Date(message.timestamp))}"
                    }
            )
        }
        
        if (message.isFromUser) {
            Spacer(modifier = Modifier.width(12.dp))
            // 👤 USER'S ELEGANT AVATAR 👤
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(userAvatarGradient)
                    .semantics {
                        contentDescription = AccessibilityConstants.ContentDescriptions.USER_AVATAR
                        role = Role.Image
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "👤",
                    fontSize = 24.sp
                )
            }
        }
    }
}

/**
 * Chat input area with text field and voice button.
 * Optimized for performance with cached gradients and adaptive animations.
 */
@Composable
private fun ChatInputArea(
    currentInput: String,
    onInputChange: (String) -> Unit,
    onSendMessage: (String) -> Unit,
    isLoading: Boolean,
    isTtsEnabled: Boolean,
    onToggleTts: () -> Unit,
    onStartVoiceChat: () -> Unit,
    onStopVoiceChat: () -> Unit,
    isVoiceChatActive: Boolean,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    
    // ⚡ PERFORMANCE OPTIMIZATION: Use cached gradients
    val voiceButtonActiveGradient = rememberOptimizedGradient(
        key = "voice_active",
        colors = listOf(WarmTerracotta, AmberGlow)
    )
    
    val voiceButtonInactiveGradient = rememberOptimizedGradient(
        key = "voice_inactive", 
        colors = listOf(WisdomBlue, DeepOcean)
    )
    
    val sendButtonActiveGradient = rememberOptimizedGradient(
        key = "send_active",
        colors = listOf(SageGreen, ForestGreen)
    )
    
    val sendButtonInactiveGradient = rememberOptimizedGradient(
        key = "send_inactive",
        colors = listOf(CloudySky, MoonlightSilver)
    )
    
    // Voice chat button gradients
    val voiceChatActiveGradient = rememberOptimizedGradient(
        key = "voice_chat_active",
        colors = listOf(AmberGlow, WarmTerracotta)
    )
    
    val voiceChatInactiveGradient = rememberOptimizedGradient(
        key = "voice_chat_inactive",
        colors = listOf(SageGreen, ForestGreen)
    )
    
    // ⚡ PERFORMANCE OPTIMIZATION: Adaptive animation duration
    val animationDuration = ChatScreenPerformance.LockScreenOptimization.getAnimationDuration()
    val shouldUseSimplifiedAnimations = ChatScreenPerformance.LockScreenOptimization.shouldUseSimplifiedAnimations()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .semantics {
                contentDescription = AccessibilityConstants.SemanticRoles.INPUT_AREA
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = CloudWhite.copy(alpha = 0.98f)
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 📣 TTS TOGGLE BUTTON
            run {
                IconButton(
                    onClick = {
                        onToggleTts()
                    },
                    modifier = Modifier
                        .size(AccessibilityConstants.LARGE_TOUCH_TARGET) // Larger for primary action
                        .clip(CircleShape)
                        .background(
                            if (isTtsEnabled) voiceButtonActiveGradient else voiceButtonInactiveGradient
                        )
                        .semantics {
                            contentDescription = if (isTtsEnabled) {
                                "Disable Merlin voice replies"
                            } else {
                                "Enable Merlin voice replies"
                            }
                            role = Role.Button
                        }
                ) {
                    Icon(
                        imageVector = if (isTtsEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                        contentDescription = null,
                        tint = CloudWhite,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            // ✨ ELEGANT TEXT FIELD ✨
            OutlinedTextField(
                value = currentInput,
                onValueChange = onInputChange,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .semantics {
                        contentDescription = "Type your message to Merlin here"
                    },
                placeholder = { 
                    Text(
                        text = if (isVoiceChatActive) "🎤 Listening..." else "💭 Share your thoughts with Merlin...",
                        color = MoonlightSilver.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyLarge,
                        fontSize = AccessibilityConstants.CHILD_BODY_TEXT,
                        fontWeight = FontWeight.Medium
                    )
                },
                textStyle = LocalTextStyle.current.copy(
                    fontSize = AccessibilityConstants.CHILD_BODY_TEXT // Larger text for children
                ),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Send,
                    autoCorrect = false
                ),
                keyboardActions = KeyboardActions(
                    onSend = { 
                        if (currentInput.isNotBlank() && !isLoading) {
                            onSendMessage(currentInput)
                        }
                    }
                ),
                enabled = !isLoading && !isVoiceChatActive,
                maxLines = 3,
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = WisdomBlue,
                    unfocusedBorderColor = MoonlightSilver.copy(alpha = 0.5f),
                    focusedTextColor = MidnightNavy,
                    unfocusedTextColor = MidnightNavy
                )
            )
            
            // 🚀 ELEGANT SEND BUTTON WITH OPTIMIZATION 🚀
            val sendButtonScale by animateFloatAsState(
                targetValue = if (currentInput.isNotBlank() && !isLoading && !isVoiceChatActive) 1.08f else 1f,
                animationSpec = if (shouldUseSimplifiedAnimations) {
                    tween(durationMillis = animationDuration / 2)
                } else {
                    tween(durationMillis = animationDuration)
                },
                label = "send_button_scale"
            )
            
            IconButton(
                onClick = { 
                    if (currentInput.isNotBlank()) {
                        onSendMessage(currentInput)
                    }
                },
                enabled = currentInput.isNotBlank() && !isLoading && !isVoiceChatActive,
                modifier = Modifier
                    .size(AccessibilityConstants.LARGE_TOUCH_TARGET) // Larger for primary action
                    .scale(sendButtonScale)
                    .clip(CircleShape)
                    .background(
                        if (currentInput.isNotBlank() && !isLoading && !isVoiceChatActive) {
                            sendButtonActiveGradient
                        } else {
                            sendButtonInactiveGradient
                        }
                    )
                    .semantics {
                        contentDescription = AccessibilityConstants.ContentDescriptions.SEND_MESSAGE
                        role = Role.Button
                    }
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(24.dp)
                            .semantics {
                                contentDescription = "Sending message to Merlin"
                            },
                        strokeWidth = 3.dp,
                        color = CloudWhite
                    )
                } else {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = null, // Already described by button
                        tint = if (currentInput.isNotBlank()) CloudWhite else StormyGray,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            // 🎙️ VOICE CHAT BUTTON (Press-to-Talk) 🎙️
            val voiceChatButtonScale by animateFloatAsState(
                targetValue = if (isVoiceChatActive) 1.1f else 1f,
                animationSpec = if (shouldUseSimplifiedAnimations) {
                    tween(durationMillis = animationDuration / 2)
                } else {
                    tween(durationMillis = animationDuration)
                },
                label = "voice_chat_button_scale"
            )
            
            IconButton(
                onClick = { 
                    println("🎙️ Voice chat IconButton clicked! Current state: $isVoiceChatActive")
                    if (isVoiceChatActive) {
                        onStopVoiceChat()
                    } else {
                        onStartVoiceChat()
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .size(AccessibilityConstants.LARGE_TOUCH_TARGET)
                    .scale(voiceChatButtonScale)
                    .clip(CircleShape)
                    .background(
                        if (isVoiceChatActive) {
                            voiceChatActiveGradient
                        } else {
                            voiceChatInactiveGradient
                        }
                    )
                    .semantics {
                        contentDescription = if (isVoiceChatActive) {
                            "Stop voice chat with Merlin"
                        } else {
                            "Start voice chat with Merlin"
                        }
                        role = Role.Button
                    }
            ) {
                Icon(
                    imageVector = if (isVoiceChatActive) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = null, // Already described by button
                    tint = CloudWhite,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }

    // Removed automatic focus to prevent keyboard from opening on screen entry
    // The keyboard will now only open when user explicitly taps the text field
}

/**
 * Large talk button designed specifically for toddlers.
 * Features big, easy-to-tap design with soft Scandinavian colors.
 */
@Composable
private fun ToddlerTalkButton(
    isVoiceChatActive: Boolean,
    isLoading: Boolean,
    onStartVoiceChat: () -> Unit,
    onStopVoiceChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Soft Scandinavian gradients for the button
    val talkButtonActiveGradient = rememberOptimizedGradient(
        key = "talk_active",
        colors = listOf(Color(0xFF10B981), Color(0xFF059669)) // Soft emerald
    )
    
    val talkButtonInactiveGradient = rememberOptimizedGradient(
        key = "talk_inactive",
        colors = listOf(Color(0xFF6366F1), Color(0xFF4F46E5)) // Soft indigo
    )
    
    // Button scale animation
    val buttonScale by animateFloatAsState(
        targetValue = if (isVoiceChatActive) 1.05f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "talk_button_scale"
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Button(
            onClick = {
                if (isVoiceChatActive) {
                    onStopVoiceChat()
                } else {
                    onStartVoiceChat()
                }
            },
            enabled = !isLoading,
            modifier = Modifier
                .size(180.dp) // 50% bigger for toddlers (120 * 1.5 = 180)
                .scale(buttonScale)
                .semantics {
                    contentDescription = if (isVoiceChatActive) {
                        "Stop talking to Merlin"
                    } else {
                        "Start talking to Merlin"
                    }
                    role = Role.Button
                },
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(
                        if (isVoiceChatActive) {
                            talkButtonActiveGradient
                        } else {
                            talkButtonInactiveGradient
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 4.dp,
                            color = Color.White
                        )
                    } else {
                        Icon(
                            imageVector = if (isVoiceChatActive) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(60.dp) // Bigger icon for bigger button
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isVoiceChatActive) "Stop" else "Talk",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp // Double the font size
                        )
                    }
                }
            }
        }
    }
}