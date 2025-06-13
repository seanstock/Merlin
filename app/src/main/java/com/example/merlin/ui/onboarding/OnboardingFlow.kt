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
import com.example.merlin.ui.theme.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

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
            
            OnboardingStep.THEME_SELECTION -> {
                ThemeSelectionScreen(
                    selectedThemeId = uiState.childProfile.selectedTheme,
                    onThemeSelected = { themeId ->
                        viewModel.updateSelectedTheme(themeId)
                    },
                    onContinue = {
                        if (viewModel.validateCurrentStep()) {
                            viewModel.nextStep()
                        }
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
        
        // Loading overlay with Apple design
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                AppleCard(
                    backgroundColor = AppleSystemBackground.copy(alpha = 0.95f),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = AppleBlue
                        )
                        Spacer(modifier = Modifier.height(AppleSpacing.medium))
                        Text(
                            text = "Setting up your profile...",
                            style = AppleBody,
                            color = ApplePrimaryLabel
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
    var name by remember { mutableStateOf(childProfile.name.ifBlank { "Ayla" }) }
    var age by remember { mutableStateOf(childProfile.age ?: 3) } // Default age 3
    var gender by remember { mutableStateOf(childProfile.gender.ifBlank { "Girl" }) }
    var interests by remember { mutableStateOf(childProfile.interests) }
    var preferredLanguage by remember { mutableStateOf(childProfile.preferredLanguage) }

    // Predefined options
    val genderOptions = listOf("", "Boy", "Girl", "Prefer not to say")
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
            .verticalScroll(rememberScrollState())
            .padding(AppleSpacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(AppleSpacing.large))
        
        // Header with Apple design
        Text(
            text = "👶", 
            fontSize = 64.sp,
            modifier = Modifier.padding(bottom = AppleSpacing.medium)
        )
        Text(
            text = "Tell us about your child",
            style = AppleNavigationTitle,
            color = ApplePrimaryLabel,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(AppleSpacing.extraLarge))

        // Form fields with Apple styling
        AppleCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(AppleSpacing.large),
                verticalArrangement = Arrangement.spacedBy(AppleSpacing.large)
            ) {
                // Name
                AppleTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Name",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Age (Slider, 3-16)
                Column {
                    Text(
                        text = "Age: $age",
                        style = AppleSubheadline.copy(fontWeight = FontWeight.SemiBold),
                        color = ApplePrimaryLabel
                    )
                    Spacer(modifier = Modifier.height(AppleSpacing.small))
                    Slider(
                        value = age.toFloat(),
                        onValueChange = { age = it.toInt() },
                        valueRange = 3f..16f,
                        steps = 13,
                        colors = SliderDefaults.colors(
                            thumbColor = AppleBlue,
                            activeTrackColor = AppleBlue
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Gender (Dropdown)
                var genderExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = genderExpanded,
                    onExpandedChange = { genderExpanded = !genderExpanded }
                ) {
                    AppleTextField(
                        value = gender,
                        onValueChange = {},
                        readOnly = true,
                        label = "Gender (optional)",
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

                // Interests (Multi-select chips)
                Column {
                    Text(
                        text = "Interests (optional)",
                        style = AppleSubheadline.copy(fontWeight = FontWeight.SemiBold),
                        color = ApplePrimaryLabel
                    )
                    Spacer(modifier = Modifier.height(AppleSpacing.small))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(AppleSpacing.small),
                        verticalArrangement = Arrangement.spacedBy(AppleSpacing.small),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        interestOptions.forEach { interest ->
                            val selected = interests.contains(interest)
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    interests = if (selected) interests - interest else interests + interest
                                },
                                label = { Text(interest) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AppleBlue,
                                    selectedLabelColor = AppleSystemBackground
                                )
                            )
                        }
                    }
                }

                // Preferred Language (Dropdown)
                var langExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = langExpanded,
                    onExpandedChange = { langExpanded = !langExpanded }
                ) {
                    AppleTextField(
                        value = languageOptions.find { it.first == preferredLanguage }?.second ?: "Select...",
                        onValueChange = {},
                        readOnly = true,
                        label = "Preferred Language",
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
            }
        }
        
        Spacer(modifier = Modifier.height(AppleSpacing.extraLarge))

        // Navigation buttons with Apple styling
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppleSpacing.medium)
        ) {
            AppleButton(
                text = "Back",
                onClick = onBack,
                style = AppleButtonStyle.Secondary,
                modifier = Modifier.weight(1f)
            )
            AppleButton(
                text = "Continue",
                onClick = onContinue,
                style = AppleButtonStyle.Primary,
                enabled = isValid,
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(AppleSpacing.large))
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
            .verticalScroll(rememberScrollState())
            .padding(AppleSpacing.large)
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(AppleSpacing.large))
        
        // Header with Apple design
        Text(
            text = "🔐",
            fontSize = 64.sp,
            modifier = Modifier.padding(bottom = AppleSpacing.medium)
        )
        Text(
            text = "Set a Parent PIN",
            style = AppleNavigationTitle,
            color = ApplePrimaryLabel,
            textAlign = TextAlign.Center
        )
        Text(
            text = "This PIN will be used to access parent settings. Choose something secure and memorable.",
            style = AppleBody,
            color = AppleSecondaryLabel,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = AppleSpacing.medium)
        )
        
        Spacer(modifier = Modifier.height(AppleSpacing.extraLarge))

        // PIN inputs with Apple styling
        AppleCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(AppleSpacing.large),
                verticalArrangement = Arrangement.spacedBy(AppleSpacing.large)
            ) {
                // PIN Input
                AppleTextField(
                    value = pin,
                    onValueChange = { if (it.length <= pinLength && it.all { char -> char.isDigit() }) onPinChange(it) },
                    label = "New PIN ($pinLength digits)",
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

                // PIN Confirmation Input
                AppleTextField(
                    value = pinConfirmation,
                    onValueChange = { if (it.length <= pinLength && it.all { char -> char.isDigit() }) onPinConfirmationChange(it) },
                    label = "Confirm PIN",
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
                
                // Error messages
                if (pinConfirmation.isNotEmpty() && pin != pinConfirmation && pin.length == pinLength) {
                    Text(
                        text = "PINs do not match",
                        style = AppleFootnote,
                        color = AppleRed
                    )
                }
                if (pin.isNotEmpty() && pin.length < pinLength) {
                    Text(
                        text = "PIN must be $pinLength digits",
                        style = AppleFootnote,
                        color = AppleSecondaryLabel
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Navigation buttons with Apple styling
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppleSpacing.medium)
        ) {
            AppleButton(
                text = "Back",
                onClick = onBack,
                style = AppleButtonStyle.Secondary,
                modifier = Modifier.weight(1f)
            )
            AppleButton(
                text = "Continue",
                onClick = onContinue,
                style = AppleButtonStyle.Primary,
                enabled = isPinValid,
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(AppleSpacing.large))
    }
}

@Composable
private fun TutorialScreen(
    onComplete: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(AppleSpacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Header with Apple design
        Text(
            text = "🎓",
            fontSize = 64.sp,
            modifier = Modifier.padding(bottom = AppleSpacing.medium)
        )
        Text(
            text = "Quick Tutorial",
            style = AppleNavigationTitle,
            color = ApplePrimaryLabel,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(AppleSpacing.extraLarge))
        
        // Tutorial content with Apple card
        AppleCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(AppleSpacing.large),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "You're almost ready!",
                    style = AppleHeadline,
                    color = ApplePrimaryLabel,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(AppleSpacing.medium))
                
                Text(
                    text = "Merlin is now set up and ready to provide a safe, educational experience for your child. You can access parent controls anytime using your PIN.",
                    style = AppleBody,
                    color = AppleSecondaryLabel,
                    textAlign = TextAlign.Center
                )
            }
        }
        
        Spacer(modifier = Modifier.height(AppleSpacing.extraLarge))
        
        // Navigation buttons with Apple styling
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppleSpacing.medium)
        ) {
            AppleButton(
                text = "Back",
                onClick = onBack,
                style = AppleButtonStyle.Secondary,
                modifier = Modifier.weight(1f)
            )
            AppleButton(
                text = "Got It!",
                onClick = onComplete,
                style = AppleButtonStyle.Primary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

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
            .padding(AppleSpacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Header with Apple design
        Text(
            text = "🧙‍♂️",
            fontSize = 80.sp,
            modifier = Modifier.padding(bottom = AppleSpacing.medium)
        )
        Text(
            text = "Hello, ${childName.ifBlank { "Friend" }}!",
            style = AppleNavigationTitle,
            color = ApplePrimaryLabel,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(AppleSpacing.extraLarge))
        
        // Introduction content with Apple card
        AppleCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(AppleSpacing.large),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "I'm Merlin, your learning companion!",
                    style = AppleHeadline,
                    color = ApplePrimaryLabel,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(AppleSpacing.medium))
                
                Text(
                    text = "I'm here to help you learn, explore, and discover amazing things together. Ask me questions, play learning games, or just chat about anything that interests you!",
                    style = AppleBody,
                    color = AppleSecondaryLabel,
                    textAlign = TextAlign.Center
                )
            }
        }
        
        Spacer(modifier = Modifier.height(AppleSpacing.extraLarge))
        
        // Navigation buttons with Apple styling
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppleSpacing.medium)
        ) {
            AppleButton(
                text = "Go Back",
                onClick = onBack,
                style = AppleButtonStyle.Secondary,
                modifier = Modifier.weight(1f)
            )
            AppleButton(
                text = "Let's Begin!",
                onClick = onComplete,
                style = AppleButtonStyle.Primary,
                modifier = Modifier.weight(1f)
            )
        }
    }
} 