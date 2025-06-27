package com.example.merlin.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.merlin.ui.theme.*
import com.example.merlin.config.ServiceLocator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildProfileScreen(
    onNavigateBack: () -> Unit,
    viewModel: ChildProfileSettingsViewModel = viewModel(),
    showTopBar: Boolean = true
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            snackbarHostState.showSnackbar("Profile saved!")
            viewModel.onSaveHandled()
        }
    }
    
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = { Text("Child Profile") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        Button(
                            onClick = { viewModel.saveChanges() },
                            enabled = uiState.isSavable
                        ) {
                            Text("Save")
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val profile = uiState.childProfile
            if (profile != null) {
                val context = LocalContext.current
                val themeService = remember { ServiceLocator.getThemeService(context) }
                val currentTheme = remember(profile.selectedTheme) { 
                    AppThemes.getThemeById(profile.selectedTheme ?: "under_the_sea") 
                }
                
                Box(modifier = Modifier.fillMaxSize()) {
                    // Background with current theme
                    if (currentTheme != null) {
                        Image(
                            painter = painterResource(id = currentTheme.backgroundImage),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        
                        // Overlay for better text readability
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.3f))
                        )
                    }
                    
                    Column(
                        modifier = Modifier
                            .padding(paddingValues)
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = AppleSpacing.large),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(AppleSpacing.xl))
                        
                        // Profile Header
                        AppleCard(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = 8
                        ) {
                            Column(
                                modifier = Modifier.padding(AppleSpacing.large),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "My Profile",
                                    style = AppleLargeTitle,
                                    color = ApplePrimaryLabel,
                                    textAlign = TextAlign.Center
                                )
                                
                                Spacer(modifier = Modifier.height(AppleSpacing.medium))
                                
                                Text(
                                    text = "Customize your learning adventure",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = AppleSecondaryLabel,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(AppleSpacing.xl))
                        
                        // Name Section
                        AppleCard(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = 4
                        ) {
                            Column(modifier = Modifier.padding(AppleSpacing.large)) {
                                Text(
                                    text = "What's your name?",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = ApplePrimaryLabel,
                                    fontWeight = FontWeight.SemiBold
                                )
                                
                                Spacer(modifier = Modifier.height(AppleSpacing.medium))
                                
                                OutlinedTextField(
                                    value = profile.name ?: "",
                                    onValueChange = { viewModel.onNameChanged(it) },
                                    label = { Text("Name") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(AppleCornerRadius.medium),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AppleBlue,
                                        focusedLabelColor = AppleBlue
                                    )
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(AppleSpacing.large))
                        
                        // Age Section
                        AppleCard(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = 4
                        ) {
                            Column(modifier = Modifier.padding(AppleSpacing.large)) {
                                Text(
                                    text = "How old are you?",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = ApplePrimaryLabel,
                                    fontWeight = FontWeight.SemiBold
                                )
                                
                                Spacer(modifier = Modifier.height(AppleSpacing.medium))
                                
                                OutlinedTextField(
                                    value = profile.age?.toString() ?: "",
                                    onValueChange = { viewModel.onAgeChanged(it) },
                                    label = { Text("Age") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(AppleCornerRadius.medium),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AppleBlue,
                                        focusedLabelColor = AppleBlue
                                    )
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(AppleSpacing.xl))
                        
                        // Theme Section
                        AppleCard(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = 4
                        ) {
                            Column(modifier = Modifier.padding(AppleSpacing.large)) {
                                Text(
                                    text = "Choose Your Adventure Theme",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = ApplePrimaryLabel,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                
                                Spacer(modifier = Modifier.height(AppleSpacing.medium))
                                
                                Text(
                                    text = "Pick a magical world to explore with your tutor",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AppleSecondaryLabel,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                
                                Spacer(modifier = Modifier.height(AppleSpacing.large))
                                
                                ThemeSelector(
                                    themes = uiState.availableThemes,
                                    selectedThemeId = profile.selectedTheme,
                                    onThemeSelected = { viewModel.onThemeChanged(it.id) }
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(AppleSpacing.xl))
                        
                        // Save Button
                        Button(
                            onClick = { viewModel.saveChanges() },
                            enabled = uiState.isSavable,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppleBlue,
                                disabledContainerColor = AppleGray4
                            ),
                            shape = RoundedCornerShape(AppleCornerRadius.large)
                        ) {
                            Text(
                                text = "Save Changes",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(AppleSpacing.xl))
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    AppleCard(
                        modifier = Modifier.padding(AppleSpacing.large),
                        elevation = 4
                    ) {
                        Text(
                            text = "Could not load child profile.",
                            modifier = Modifier.padding(AppleSpacing.large),
                            style = MaterialTheme.typography.bodyLarge,
                            color = AppleSecondaryLabel,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ThemeSelector(
    themes: List<AppTheme>,
    selectedThemeId: String?,
    onThemeSelected: (AppTheme) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(AppleSpacing.medium),
        contentPadding = PaddingValues(horizontal = AppleSpacing.small)
    ) {
        items(themes) { theme ->
            ThemeItem(
                theme = theme,
                isSelected = theme.id == selectedThemeId,
                onClick = { onThemeSelected(theme) }
            )
        }
    }
}

@Composable
fun ThemeItem(
    theme: AppTheme,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    AppleCard(
        modifier = Modifier
            .width(140.dp)
            .height(180.dp)
            .clickable { onClick() }
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 3.dp,
                        color = AppleBlue,
                        shape = RoundedCornerShape(AppleCornerRadius.large)
                    )
                } else {
                    Modifier
                }
            ),
        elevation = if (isSelected) 8 else 4
    ) {
        Box {
            // Background preview
            Image(
                painter = painterResource(id = theme.backgroundImage),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(AppleCornerRadius.large)),
                contentScale = ContentScale.Crop
            )
            
            // Overlay for better text readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Color.Black.copy(alpha = 0.4f),
                        RoundedCornerShape(AppleCornerRadius.large)
                    )
            )
            
            // Theme content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(AppleSpacing.medium),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Theme name
                Text(
                    text = theme.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(AppleSpacing.xs))
                
                // Tutor info
                Text(
                    text = "Meet ${theme.tutorName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(AppleSpacing.xs))
                
                Text(
                    text = theme.tutorDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
            }
            
            // Selection indicator
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(AppleSpacing.small)
                        .size(24.dp)
                        .background(
                            AppleBlue,
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✓",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
} 