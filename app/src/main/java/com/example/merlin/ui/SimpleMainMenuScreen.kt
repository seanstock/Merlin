package com.example.merlin.ui

import android.app.Application
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
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
import com.example.merlin.timer.ScreenTimeManager
import com.example.merlin.ui.wallet.SpendCoinsDialog
import com.example.merlin.ui.game.GameManager
import com.example.merlin.ui.theme.*
import com.example.merlin.ui.wallet.WalletDisplay
import com.example.merlin.ui.wallet.WalletViewModel
import com.example.merlin.ui.wallet.WalletViewModelFactory
import com.example.merlin.data.database.DatabaseProvider

import com.example.merlin.utils.UserSessionRepository
import com.example.merlin.viewmodels.SimpleMenuViewModel
import com.example.merlin.viewmodels.SimpleMenuViewModelFactory
import com.example.merlin.config.ServiceLocator
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.*
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.graphics.SolidColor

/**
 * Simple icon-based main menu for ages 3-4, using the app's design system.
 * Enhanced with Apple LiquidGlass-inspired visual design for better engagement.
 */
@Composable
fun SimpleMainMenuScreen(
    onNavigateToGames: (String) -> Unit,
    onNavigateToChat: () -> Unit,
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

    // Filter out chat button from menu items - we'll use the greeting as chat button instead
    val filteredMenuItems = remember(menuItems) {
        menuItems.filter { it.id != "chat" }
    }

    // Wallet ViewModel for coin balance
    val userSessionRepository = remember { UserSessionRepository.getInstance(context) }
    val activeChildId = remember { userSessionRepository.getActiveChildId() }
    
    val walletViewModel: WalletViewModel? = activeChildId?.let { childId ->
        androidx.lifecycle.viewmodel.compose.viewModel(
            factory = WalletViewModelFactory(application, childId)
        )
    }
    
    // Get child name
    var childName by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(activeChildId) {
        activeChildId?.let { childId ->
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val database = DatabaseProvider.getInstance(context)
                val childProfile = database.childProfileDao().getById(childId)
                childName = childProfile?.name
            }
        }
    }
    val coinBalance by walletViewModel?.balance?.collectAsState() ?: remember { mutableStateOf(0) }
    val walletLoading by walletViewModel?.isLoading?.collectAsState() ?: remember { mutableStateOf(false) }

    // Refresh balance when screen becomes visible
    LaunchedEffect(Unit) {
        walletViewModel?.refreshBalance()
    }

    // App Launch Service for spend coins dialog
    val appLaunchService = remember { ServiceLocator.getAppLaunchService(context) }

    // Theme service and current theme
    val themeService = remember { ServiceLocator.getThemeService(context) }
    val currentTheme by remember(activeChildId) {
        val childId = activeChildId
        if (childId != null) {
            themeService.getThemeFlowForChild(childId)
        } else {
            flowOf(AppThemes.getDefaultTheme())
        }
    }.collectAsState(initial = null)

    var showSpendDialog by remember { mutableStateOf(false) }

    if (currentTheme == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(color = AppleSystemBackground),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = AppleBlue)
        }
        return
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Background with subtle overlay for better readability
        Image(
            painter = painterResource(id = currentTheme!!.backgroundImage),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        // Subtle gradient overlay for better contrast
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.1f),
                            Color.Black.copy(alpha = 0.3f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Enhanced top status bar
            EnhancedTopStatusBar(
                title = currentTheme!!.tutorName,
                balance = coinBalance,
                isLoading = walletLoading,
                onSpendCoins = { showSpendDialog = true },
                onSettingsClick = onNavigateToSettings,
                modifier = Modifier.statusBarsPadding()
            )

            // Child name circle button in top left
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppleSpacing.large)
            ) {
                if (childName != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .size(80.dp)
                            .offset(x = 5.dp, y = 5.dp)
                            .background(
                                Color.Black.copy(alpha = 0.15f),
                                CircleShape
                            )
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .size(80.dp)
                            .clickable(onClick = onNavigateToChat)
                            .background(
                                brush = createSmoothLiquidGlassBrush(
                                    baseColor = AppleBlue.copy(alpha = 0.8f),
                                    shimmerAngle = 0f
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Hi\n${childName.take(8)}! 👋",
                            style = AppleCallout.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                lineHeight = 14.sp
                            ),
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            maxLines = 2
                        )
                    }
                }
            }

            // Enhanced grid with larger icons for landscape support
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = AppleSpacing.large),
                contentAlignment = Alignment.Center
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    contentPadding = PaddingValues(AppleSpacing.medium),
                    verticalArrangement = Arrangement.spacedBy(AppleSpacing.large),
                    horizontalArrangement = Arrangement.spacedBy(AppleSpacing.large)
                ) {
                    itemsIndexed(filteredMenuItems) { index, item ->
                        LiquidGlassMenuItem(
                            item = item,
                            onClick = {
                                when {
                                    item.id == "spend_coins" -> showSpendDialog = true
                                    item.isApp && item.packageName != null -> {
                                        // Launch the app directly
                                        coroutineScope.launch {
                                            appLaunchService.launchApp(item.packageName!!)
                                        }
                                    }
                                    else -> onNavigateToGames(item.id)
                                }
                            },
                            gridIndex = index,
                            totalItems = filteredMenuItems.size
                        )
                    }
                }
            }
        }
    }

    if (showSpendDialog && walletViewModel != null && activeChildId != null) {
        SpendCoinsDialog(
            currentBalance = coinBalance,
            appLaunchService = appLaunchService,
            childId = activeChildId,
            onDismiss = { showSpendDialog = false },
            onSpendCoins = { timeInSeconds, category ->
                walletViewModel.spendCoins(timeInSeconds, category)
                showSpendDialog = false
            },
            walletViewModel = walletViewModel
        )
    }
}

@Composable
fun ChatWelcomeSection(
    tutorName: String,
    childName: String?,
    onChatClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Animation for subtle scaling effect
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "chat_button_scale"
    )

    AppleCard(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                onClick = onChatClick,
                onClickLabel = "Start chatting with $tutorName"
            )
            .semantics {
                contentDescription = "Chat with $tutorName button"
                role = Role.Button
            },
        backgroundColor = AppleSystemBackground.copy(alpha = 0.95f),
        elevation = 3,
        cornerRadius = 20
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppleSpacing.large)
        ) {
            // Animated chat icon with LiquidGlass effect
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        brush = createLiquidGlassBrush(AppleBlue),
                        shape = RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "💬",
                    fontSize = 32.sp
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (childName != null) "Hi $childName! 👋" else "Hi there! 👋",
                    style = AppleHeadline.copy(fontSize = 22.sp),
                    color = ApplePrimaryLabel
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Would you like to talk with $tutorName?",
                    style = AppleBody,
                    color = AppleSecondaryLabel
                )
            }
        }
    }
}

@Composable
fun LiquidGlassMenuItem(
    item: SimpleMenuViewModel.MenuItem,
    onClick: () -> Unit,
    gridIndex: Int = 0,
    totalItems: Int = 6 // Add total items parameter
) {
    // Animation states for LiquidGlass effects
    var isPressed by remember { mutableStateOf(false) }
    var isHovered by remember { mutableStateOf(false) }
    
    val infiniteTransition = rememberInfiniteTransition(label = "liquid_glass_animation")
    
    // Calculate items per row based on screen width and item size
    // Approximate calculation: assuming ~160dp per item + spacing
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val itemWidth = 160.dp + 16.dp // minSize + approximate spacing
    val itemsPerRow = maxOf(1, (screenWidth / itemWidth).toInt())
    
    // Calculate position in current row
    val positionInRow = gridIndex % itemsPerRow
    
    // Wave timing: 0.8 seconds divided by items per row
    val waveOffsetPerItem = 800f / itemsPerRow // milliseconds
    val timeOffset = positionInRow * waveOffsetPerItem
    
    // Wave floating animation with calculated offset
    val glassOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 16f, // Big movement
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(
                offsetMillis = timeOffset.toInt()
            )
        ),
        label = "glass_float_wave"
    )
    
    // Smooth continuous shimmer effect
    val shimmerAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_angle"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "menu_item_scale"
    )

    // Check if this is the memory button for comparison
    val isMemoryButton = item.id == "sample-game"

            // Strong 3D effect with pronounced shadow
        Box(
            modifier = Modifier
                .scale(scale)
                .offset(y = glassOffset.dp)
                .size(180.dp),
            contentAlignment = Alignment.Center
        ) {
            // Simple solid shadow
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(
                        x = if (isPressed) 2.dp else 5.dp,
                        y = if (isPressed) 3.dp else 5.dp
                    )
                    .background(
                        Color.Black.copy(alpha = 0.15f),
                        RoundedCornerShape(32.dp)
                    )
            )

        // Main button with child-friendly bright colors
        Card(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    onClick = onClick,
                    onClickLabel = item.title
                )
                .semantics {
                    contentDescription = "${item.title} button"
                    role = Role.Button
                },
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 0.dp
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // Bright, child-friendly base color layer
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = if (isMemoryButton) {
                                // Spinning gradient for Merlin's Memory
                                createSmoothLiquidGlassBrush(
                                    baseColor = getItemAccentColor(item.id).copy(alpha = 0.7f), // More translucent
                                    shimmerAngle = shimmerAngle
                                )
                            } else {
                                // Shimmer effect for other buttons
                                createSmoothLiquidGlassBrush(
                                    baseColor = getItemAccentColor(item.id).copy(alpha = 0.7f), // More translucent
                                    shimmerAngle = shimmerAngle
                                )
                            },
                            shape = RoundedCornerShape(32.dp)
                        )
                )
                
                // Rounded corner bevel using gradients
                // TOP-LEFT to BOTTOM-RIGHT light bevel
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.6f),
                                    Color.Transparent,
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.4f)
                                ),
                                start = Offset(0f, 0f),
                                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                            ),
                            shape = RoundedCornerShape(32.dp)
                        )
                )
                
                // Circular highlight for rounded 3D effect
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.4f),
                                    Color.White.copy(alpha = 0.2f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.2f)
                                ),
                                center = Offset(0.3f, 0.3f),
                                radius = 1.2f
                            ),
                            shape = RoundedCornerShape(32.dp)
                        )
                )
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize() // Keep this but make content transparent
                ) {
                    // App icon or emoji with NO background so gradients show through
                    Box(
                        modifier = Modifier.size(80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (item.isApp && item.appIcon != null) {
                            // Display real app icon
                            val bitmap = remember(item.appIcon) {
                                item.appIcon!!.toBitmap(192, 192).asImageBitmap() // 80dp * 2.4 density ≈ 192px
                            }
                            Image(
                                bitmap = bitmap,
                                contentDescription = item.title,
                                modifier = Modifier.size(64.dp),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            // Fallback to emoji for games and apps without icons
        Text(
                                text = if (item.isApp) item.emoji else getDistinctEmojiForGame(item.id),
                                fontSize = 64.sp, // Increased from 48sp
                                textAlign = TextAlign.Center,
                                style = TextStyle(
                                    shadow = if (isMemoryButton) null else Shadow(
                                        color = Color.Black.copy(alpha = 0.3f),
                                        offset = Offset(2f, 2f),
                                        blurRadius = 3f
                                    )
                                )
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(AppleSpacing.medium))
                    
                    // Clean text with more padding and lighter background
                    Box(
                        modifier = Modifier
                            .padding(horizontal = AppleSpacing.large) // More padding from icon edges
                            .background(
                                Color.Black.copy(alpha = 0.25f), // Much lighter background (was 0.5f)
                                RoundedCornerShape(12.dp)
                            )
                            .padding(
                                horizontal = AppleSpacing.medium,
                                vertical = AppleSpacing.small
                            )
                    ) {
        Text(
            text = item.title,
                            style = AppleCallout.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
            color = Color.White,
                            textAlign = TextAlign.Center,
                            maxLines = 2
        )
                    }
                }
            }
        }
    }
}

@Composable
fun EnhancedTopStatusBar(
    title: String,
    balance: Int,
    isLoading: Boolean,
    onSpendCoins: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val remainingSeconds by ScreenTimeManager.remainingSeconds.collectAsState()

    AppleCard(
        modifier = modifier.fillMaxWidth(),
        backgroundColor = AppleSystemBackground.copy(alpha = 0.95f),
        elevation = 1,
        cornerRadius = 0 // Square corners for status bar
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
            // Enhanced tutor name with dramatic styling
        Text(
            text = title,
                style = TextStyle(
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                    fontWeight = FontWeight.ExtraBold, // Much bolder font
                    fontSize = 28.sp, // Much larger size
                    letterSpacing = 0.5.sp,
                    shadow = Shadow(
                        color = AppleBlue.copy(alpha = 0.3f),
                        offset = Offset(1f, 1f),
                        blurRadius = 2f
                    )
                ),
                color = ApplePrimaryLabel
            )

            // Right side controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppleSpacing.medium)
            ) {
                // Timer display with LiquidGlass styling
            if (remainingSeconds > 0) {
                val minutes = remainingSeconds / 60
                val seconds = remainingSeconds % 60
                    Box(
                        modifier = Modifier
                            .background(
                                brush = createLiquidGlassBrush(AppleBlue),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(
                                horizontal = AppleSpacing.medium,
                                vertical = AppleSpacing.small
                            )
                    ) {
                Text(
                    text = String.format("%02d:%02d", minutes, seconds),
                            style = AppleCallout.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }
                
                // Enhanced wallet display
            WalletDisplay(
                balance = balance,
                isLoading = isLoading,
                onClick = onSpendCoins
            )
            
                // Enhanced settings button with LiquidGlass
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            brush = createLiquidGlassBrush(AppleGray2),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = onSettingsClick,
                        modifier = Modifier.size(44.dp)
                    ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                            tint = AppleBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Create Apple LiquidGlass-inspired brush with dynamic light refraction
 */
@Composable
private fun createLiquidGlassBrush(baseColor: Color): Brush {
    // Non-rotating gradient removed – use solid color
    return SolidColor(baseColor)
}

/**
 * Advanced LiquidGlass brush with smooth seamless shimmer effects
 */
@Composable
private fun createSmoothLiquidGlassBrush(
    baseColor: Color,
    shimmerAngle: Float
): Brush {
    // Convert angle to radians for smooth circular motion
    val angleRad = Math.toRadians(shimmerAngle.toDouble())
    
    // Create smooth flowing shimmer using sweeping gradient
    val shimmerColors = listOf(
        baseColor.copy(alpha = 0.7f),
        baseColor.copy(alpha = 0.9f),
        Color.White.copy(alpha = 0.6f), // Bright shimmer highlight
        baseColor.copy(alpha = 0.8f),
        baseColor.copy(alpha = 0.6f),
        Color.White.copy(alpha = 0.3f), // Secondary highlight
        baseColor.copy(alpha = 0.8f),
        baseColor.copy(alpha = 0.7f)
    )
    
    // Calculate smooth sweeping motion across the surface
    val centerX = 90f // Center of 180dp item
    val centerY = 90f
    val radius = 150f
    
    val startX = centerX + (Math.cos(angleRad) * radius).toFloat()
    val startY = centerY + (Math.sin(angleRad) * radius).toFloat()
    val endX = centerX - (Math.cos(angleRad) * radius).toFloat()
    val endY = centerY - (Math.sin(angleRad) * radius).toFloat()
    
    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(startX, startY),
        end = Offset(endX, endY)
    )
}

/**
 * Get accent color for different menu items with enhanced variety
 */
@Composable
private fun getItemAccentColor(itemId: String): Color {
    return when (itemId) {
        "spend_coins" -> AppleYellow
        else -> when (itemId.hashCode() % 8) {
            0 -> ApplePurple
            1 -> AppleGreen
            2 -> AppleOrange
            3 -> AppleTeal
            4 -> AppleIndigo
            5 -> AppleCyan
            6 -> AppleMint
            else -> AppleBlue
        }
    }
}

/**
 * Create a much stronger, more visible dome gradient
 */
@Composable
private fun createStrongDomeGradient(): Brush {
    // Removed – return transparent brush so no overlay
    return SolidColor(Color.Transparent)
}

/**
 * Create a bright, visible highlight effect
 */
@Composable
private fun createBrightHighlight(): Brush {
    // Removed top highlight
    return SolidColor(Color.Transparent)
}

/**
 * Strong 3D effect for emoji container
 */
@Composable
private fun createEmojiContainerBrush(baseColor: Color): Brush {
    return Brush.radialGradient(
        colors = listOf(
            baseColor.copy(alpha = 0.8f),
            baseColor.copy(alpha = 0.6f),
            baseColor.copy(alpha = 0.4f),
            Color.White.copy(alpha = 0.3f), // Bright center highlight
            baseColor.copy(alpha = 0.5f)
        ),
        center = Offset(0.5f, 0.3f),
        radius = 0.8f
    )
}

/**
 * Get a distinct emoji for different games that 3-year-olds can easily recognize
 */
@Composable
private fun getDistinctEmojiForGame(gameId: String): String {
    return when (gameId) {
        "sample-game" -> "🧠"        // Brain for Merlin's Memory
        "color-match" -> "🌈"        // Rainbow for Color Match
        "shape-match" -> "🔷"        // Blue diamond for Shape Match
        "number-match" -> "🔢"       // Numbers symbol for Number Match
        "shape-drop" -> "🎯"         // Target for Shape Drop Adventure
        "spend-coins" -> "🪙"        // Coin for Spend Coins
        else -> "🎮"                 // Game controller for unknown games
    }
}

// Helper extensions for simple lighten/darken adjustments (factor 0f-1f)
private fun Color.lighten(factor: Float): Color {
    val newFactor = factor.coerceIn(0f, 1f)
    return Color(
        red = red + (1f - red) * newFactor,
        green = green + (1f - green) * newFactor,
        blue = blue + (1f - blue) * newFactor,
        alpha = alpha
    )
}

private fun Color.darken(factor: Float): Color {
    val newFactor = factor.coerceIn(0f, 1f)
    return Color(
        red = red * (1f - newFactor),
        green = green * (1f - newFactor),
        blue = blue * (1f - newFactor),
        alpha = alpha
    )
}