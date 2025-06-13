package com.example.merlin.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.merlin.ui.theme.*

/**
 * Welcome screen that introduces the app with Apple's clean design language.
 * Simple, focused, and accessible design suitable for all users.
 */
@Composable
fun WelcomeScreen(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(AppleSpacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(AppleSpacing.extraLarge))
        
        // App Icon/Logo placeholder - clean and simple
        Text(
            text = "🧙‍♂️",
            fontSize = 80.sp,
            modifier = Modifier.padding(bottom = AppleSpacing.large)
        )
        
        // Main title with Apple typography
        Text(
            text = "Welcome to Merlin",
            style = AppleNavigationTitle,
            color = ApplePrimaryLabel,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = AppleSpacing.small)
        )
        
        // Subtitle
        Text(
            text = "Your intelligent learning companion",
            style = AppleBody,
            color = AppleSecondaryLabel,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = AppleSpacing.extraLarge)
        )
        
        // Clean feature list with Apple design
        AppleCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(AppleSpacing.large),
                verticalArrangement = Arrangement.spacedBy(AppleSpacing.large)
            ) {
                Text(
                    text = "What Merlin Offers",
                    style = AppleHeadline,
                    color = ApplePrimaryLabel,
                    modifier = Modifier.padding(bottom = AppleSpacing.medium)
                )
                
                FeatureItem(
                    title = "Smart Conversations",
                    description = "Ask questions and get helpful, age-appropriate answers"
                )
                
                FeatureItem(
                    title = "Safe Learning Environment",
                    description = "Designed with privacy and child safety as top priorities"
                )
                
                FeatureItem(
                    title = "Progress Tracking",
                    description = "Parents can monitor learning progress and engagement"
                )
                
                FeatureItem(
                    title = "Personalized Experience",
                    description = "Adapts to your child's interests and learning style"
                )
            }
        }
        
        Spacer(modifier = Modifier.height(AppleSpacing.extraLarge))
        
        // Privacy note with Apple styling
        AppleCard(
            backgroundColor = AppleSystemGray6
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppleSpacing.medium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🔒",
                    fontSize = 20.sp,
                    modifier = Modifier.padding(end = AppleSpacing.small)
                )
                Column {
                    Text(
                        text = "Privacy First",
                        style = AppleCallout.copy(fontWeight = FontWeight.SemiBold),
                        color = ApplePrimaryLabel
                    )
                    Text(
                        text = "Your data stays secure and private",
                        style = AppleFootnote,
                        color = AppleSecondaryLabel
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(AppleSpacing.extraLarge))
        
        // Continue button with Apple styling
        AppleButton(
            text = "Get Started",
            onClick = onContinue,
            style = AppleButtonStyle.Primary,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(AppleSpacing.large))
    }
}

@Composable
private fun FeatureItem(
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .offset(y = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .background(
                        color = AppleBlue,
                        shape = CircleShape
                    )
            )
        }
        
        Spacer(modifier = Modifier.width(AppleSpacing.medium))
        
        Column {
            Text(
                text = title,
                style = AppleSubheadline.copy(fontWeight = FontWeight.SemiBold),
                color = ApplePrimaryLabel
            )
            Text(
                text = description,
                style = AppleBody,
                color = AppleSecondaryLabel,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WelcomeScreenPreview() {
    MerlinTheme {
        WelcomeScreen(
            onContinue = {}
        )
    }
} 