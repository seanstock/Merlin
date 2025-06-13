package com.example.merlin.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.merlin.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildProfileScreen(
    onNavigateBack: () -> Unit,
    viewModel: ChildProfileSettingsViewModel = viewModel()
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
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val profile = uiState.childProfile
            if (profile != null) {
                Column(
                    modifier = Modifier
                        .padding(paddingValues)
                        .padding(16.dp)
                        .fillMaxSize()
                ) {
                    OutlinedTextField(
                        value = profile.name ?: "",
                        onValueChange = { viewModel.onNameChanged(it) },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = profile.age?.toString() ?: "",
                        onValueChange = { viewModel.onAgeChanged(it) },
                        label = { Text("Age") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Theme", style = MaterialTheme.typography.titleMedium)
                    ThemeSelector(
                        themes = uiState.availableThemes,
                        selectedThemeId = profile.selectedTheme,
                        onThemeSelected = { viewModel.onThemeChanged(it.id) }
                    )
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Could not load child profile.")
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
        modifier = Modifier.padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
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
    Card(
        modifier = Modifier.clickable(onClick = onClick),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // In a real app, you'd show an image or icon here
            Text(theme.name, style = MaterialTheme.typography.bodyLarge)
            Text(theme.tutorName, style = MaterialTheme.typography.bodySmall)
        }
    }
} 