package com.example.merlin.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.merlin.ui.onboarding.OnboardingViewModel.OnboardingStep

/**
 * Main onboarding flow that manages navigation between different setup screens.
 * Handles the complete first-time setup process for new users.
 */
@Composable
fun OnboardingFlow(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Handle completion
    LaunchedEffect(uiState.currentStep) {
        if (uiState.currentStep == OnboardingStep.COMPLETED) {
            onComplete()
        }
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        when (uiState.currentStep) {
            OnboardingStep.WELCOME -> {
                WelcomeScreen(
                    onContinue = {
                        viewModel.nextStep()
                    }
                )
            }
            
            OnboardingStep.PERMISSIONS -> {
                PermissionsScreen(
                    onPermissionUpdate = { permission, granted ->
                        viewModel.updatePermissionStatus(permission, granted)
                    },
                    onContinue = {
                        if (viewModel.validateCurrentStep()) {
                            viewModel.nextStep()
                        }
                    },
                    onBack = {
                        viewModel.previousStep()
                    },
                    permissionsGranted = uiState.permissionsGranted
                )
            }
            
            OnboardingStep.CHILD_INFO -> {
                ChildInfoScreen(
                    childProfile = uiState.childProfile,
                    onUpdateProfile = { name, age, gender, interests, language ->
                        viewModel.updateChildProfile(
                            name = name,
                            age = age,
                            gender = gender,
                            interests = interests,
                            preferredLanguage = language
                        )
                    },
                    onContinue = {
                        if (viewModel.validateCurrentStep()) {
                            viewModel.nextStep()
                        }
                    },
                    onBack = {
                        viewModel.previousStep()
                    }
                )
            }
            
            OnboardingStep.PARENT_PIN -> {
                ParentPinScreen(
                    pin = uiState.parentPin,
                    pinConfirmation = uiState.parentPinConfirmation,
                    onPinChange = { pin ->
                        viewModel.updateParentPin(pin)
                    },
                    onPinConfirmationChange = { confirmation ->
                        viewModel.updateParentPinConfirmation(confirmation)
                    },
                    onContinue = {
                        if (viewModel.validateCurrentStep()) {
                            viewModel.nextStep()
                        }
                    },
                    onBack = {
                        viewModel.previousStep()
                    }
                )
            }
            
            OnboardingStep.TUTORIAL -> {
                TutorialScreen(
                    onComplete = {
                        viewModel.markTutorialCompleted()
                        viewModel.nextStep()
                    },
                    onBack = {
                        viewModel.previousStep()
                    }
                )
            }
            
            OnboardingStep.AI_INTRODUCTION -> {
                AIIntroductionScreen(
                    childName = uiState.childProfile.name,
                    onComplete = {
                        viewModel.nextStep()
                    },
                    onBack = {
                        viewModel.previousStep()
                    }
                )
            }
            
            OnboardingStep.COMPLETED -> {
                // This will trigger the LaunchedEffect above
                // which calls onComplete()
            }
        }
        
        // Loading overlay
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                Card(
                    modifier = Modifier.fillMaxSize(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Setting up your profile...",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
        
        // Error handling
        uiState.error?.let { error ->
            LaunchedEffect(error) {
                // You could show a snackbar or dialog here
                // For now, we'll just log it
                println("Onboarding error: $error")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ChildInfoScreen(
    childProfile: OnboardingViewModel.ChildProfileData,
    onUpdateProfile: (String?, Int?, String?, List<String>?, String?) -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Local state for form fields (mirrors ViewModel, but allows instant UI feedback)
    var name by remember { mutableStateOf(childProfile.name) }
    var age by remember { mutableStateOf(childProfile.age ?: 6) } // Default age 6
    var gender by remember { mutableStateOf(childProfile.gender) }
    var interests by remember { mutableStateOf(childProfile.interests) }
    var preferredLanguage by remember { mutableStateOf(childProfile.preferredLanguage) }

    // Predefined options
    val genderOptions = listOf("", "Boy", "Girl", "Non-binary", "Prefer not to say")
    val interestOptions = listOf("Math", "Reading", "Science", "Art", "Music", "Games", "Sports", "Animals")
    val languageOptions = listOf("en" to "English", "es" to "Spanish", "fr" to "French", "de" to "German", "zh" to "Chinese")

    // Validation
    val isValid = name.isNotBlank() && age > 0

    // Sync with ViewModel on change
    LaunchedEffect(name, age, gender, interests, preferredLanguage) {
        onUpdateProfile(name, age, gender, interests, preferredLanguage)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(text = "👶", fontSize = 64.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Tell us about your child", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        // Name
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Age (Slider, 3-16)
        Text(text = "Age: $age", style = MaterialTheme.typography.bodyLarge)
        Slider(
            value = age.toFloat(),
            onValueChange = { age = it.toInt() },
            valueRange = 3f..16f,
            steps = 13,
            modifier = Modifier.fillMaxWidth(0.9f)
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Gender (Dropdown)
        var genderExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = genderExpanded,
            onExpandedChange = { genderExpanded = !genderExpanded }
        ) {
            OutlinedTextField(
                value = gender,
                onValueChange = {},
                readOnly = true,
                label = { Text("Gender (optional)") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = genderExpanded,
                onDismissRequest = { genderExpanded = false }
            ) {
                genderOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.ifBlank { "Select..." }) },
                        onClick = {
                            gender = option
                            genderExpanded = false
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Interests (Multi-select chips)
        Text(text = "Interests (optional)", style = MaterialTheme.typography.bodyLarge)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            interestOptions.forEach { interest ->
                val selected = interests.contains(interest)
                FilterChip(
                    selected = selected,
                    onClick = {
                        interests = if (selected) interests - interest else interests + interest
                    },
                    label = { Text(interest) }
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Preferred Language (Dropdown)
        var langExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = langExpanded,
            onExpandedChange = { langExpanded = !langExpanded }
        ) {
            OutlinedTextField(
                value = languageOptions.find { it.first == preferredLanguage }?.second ?: "Select...",
                onValueChange = {},
                readOnly = true,
                label = { Text("Preferred Language") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = langExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = langExpanded,
                onDismissRequest = { langExpanded = false }
            ) {
                languageOptions.forEach { (code, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            preferredLanguage = code
                            langExpanded = false
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))

        // Navigation buttons
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedButton(onClick = onBack) { Text("Back") }
            Button(onClick = onContinue, enabled = isValid) { Text("Continue") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ParentPinScreen(
    pin: String,
    pinConfirmation: String,
    onPinChange: (String) -> Unit,
    onPinConfirmationChange: (String) -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pinLength = 4
    val isPinValid = pin.length >= pinLength && pin == pinConfirmation
    var showPin by remember { mutableStateOf(false) }
    var showPinConfirmation by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .imePadding(), // Handles keyboard overlaps
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top // Changed for better layout with keyboard
    ) {
        Text(text = "🔐", fontSize = 64.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Set a Parent PIN", style = MaterialTheme.typography.headlineMedium)
        Text(
            text = "This PIN will be used to access parent settings. Choose something secure and memorable.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))

        // PIN Input
        OutlinedTextField(
            value = pin,
            onValueChange = { if (it.length <= pinLength && it.all { char -> char.isDigit() }) onPinChange(it) },
            label = { Text("New PIN ($pinLength digits)") },
            singleLine = true,
            visualTransformation = if (showPin) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            trailingIcon = {
                val image = if (showPin) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                val description = if (showPin) "Hide PIN" else "Show PIN"
                IconButton(onClick = { showPin = !showPin }) {
                    Icon(imageVector = image, contentDescription = description)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        // PIN Confirmation Input
        OutlinedTextField(
            value = pinConfirmation,
            onValueChange = { if (it.length <= pinLength && it.all { char -> char.isDigit() }) onPinConfirmationChange(it) },
            label = { Text("Confirm PIN") },
            singleLine = true,
            visualTransformation = if (showPinConfirmation) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            isError = pinConfirmation.isNotEmpty() && pin != pinConfirmation && pin.length == pinLength,
            trailingIcon = {
                val image = if (showPinConfirmation) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                val description = if (showPinConfirmation) "Hide PIN" else "Show PIN"
                IconButton(onClick = { showPinConfirmation = !showPinConfirmation }) {
                    Icon(imageVector = image, contentDescription = description)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        if (pinConfirmation.isNotEmpty() && pin != pinConfirmation && pin.length == pinLength) {
            Text(
                text = "PINs do not match",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.Start)
            )
        }
        if (pin.isNotEmpty() && pin.length < pinLength) {
            Text(
                text = "PIN must be $pinLength digits",
                color = MaterialTheme.colorScheme.error, // Or a less severe color for guidance
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.Start)
            )
        }

        Spacer(modifier = Modifier.weight(1f)) // Pushes buttons to bottom

        // Navigation buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(onClick = onBack) { Text("Back") }
            Button(onClick = onContinue, enabled = isPinValid) { Text("Continue") }
        }
        Spacer(modifier = Modifier.height(16.dp)) // Padding at the very bottom
    }
}

/**
 * Placeholder for TutorialScreen - to be implemented in the next subtask.
 */
@Composable
private fun TutorialScreen(
    onComplete: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🎓",
            fontSize = 64.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Interactive Tutorial",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "Full tutorial coming soon! You'll learn all about Merlin here.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(16.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween // Adjusted for better button layout
        ) {
            OutlinedButton(onClick = onBack) {
                Text("Back")
            }
            Button(onClick = onComplete) {
                Text("Got It! (Next)") // Changed text for clarity
            }
        }
    }
}

/**
 * Placeholder for AIIntroductionScreen - to be implemented in the next subtask.
 */
@Composable
private fun AIIntroductionScreen(
    childName: String,
    onComplete: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🧙‍♂️", // Merlin emoji
            fontSize = 80.sp // Made a bit larger
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Hello, ${childName.ifBlank { "Friend" }}!", // Personalized greeting
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "I'm Merlin, your magical learning companion! I'm so excited to go on adventures and learn new things with you.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween // Adjusted for better button layout
        ) {
            OutlinedButton(onClick = onBack) {
                Text("Go Back")
            }
            Button(onClick = onComplete) {
                Text("Let's Begin! ✨")
            }
        }
    }
} 