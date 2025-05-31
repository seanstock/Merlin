package com.example.merlin.ui.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.merlin.R
import com.example.merlin.ui.theme.MerlinTheme
import com.example.merlin.ui.theme.*

/**
 * Welcome screen that introduces the app to children and parents.
 * Features child-friendly design with animations and clear explanations.
 */
@Composable
fun WelcomeScreen(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    // ✨ MAGICAL ANIMATIONS ✨
    val infiniteTransition = rememberInfiniteTransition(label = "magical_welcome")
    val wizardScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wizard_scale"
    )
    
    val sparkleRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sparkle_rotation"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MagicalBlue.copy(alpha = 0.2f),
                        StardustPink.copy(alpha = 0.2f),
                        SunshineYellow.copy(alpha = 0.1f),
                        UnicornMint.copy(alpha = 0.2f)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            // ✨ MAGICAL WIZARD WITH SPARKLES ✨
            Box(
                contentAlignment = Alignment.Center
            ) {
                // Sparkles around the wizard
                Text(
                    text = "✨",
                    fontSize = 32.sp,
                    modifier = Modifier
                        .offset(x = (-40).dp, y = (-30).dp)
                        .rotate(sparkleRotation)
                )
                Text(
                    text = "⭐",
                    fontSize = 28.sp,
                    modifier = Modifier
                        .offset(x = 45.dp, y = (-20).dp)
                        .rotate(-sparkleRotation * 1.5f)
                )
                Text(
                    text = "🌟",
                    fontSize = 24.sp,
                    modifier = Modifier
                        .offset(x = (-50).dp, y = 40.dp)
                        .rotate(sparkleRotation * 0.8f)
                )
                Text(
                    text = "💫",
                    fontSize = 30.sp,
                    modifier = Modifier
                        .offset(x = 40.dp, y = 35.dp)
                        .rotate(-sparkleRotation * 1.2f)
                )
                
                // Main wizard emoji
                Text(
                    text = "🧙‍♂️",
                    fontSize = 96.sp,
                    modifier = Modifier.scale(wizardScale)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // 🌈 MAGICAL TITLE CARD 🌈
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MagicalPurple.copy(alpha = 0.9f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "WELCOME TO",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = SunshineYellow,
                        letterSpacing = 3.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "MERLIN! 🪄",
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Black,
                        color = CloudWhite,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Text(
                        text = "Your Magical Learning Companion! 🎪✨",
                        style = MaterialTheme.typography.headlineSmall,
                        color = UnicornMint,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // 🎪 MAGICAL FEATURES CARD 🎪
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = DragonGreen.copy(alpha = 0.9f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text(
                        text = "🎨 What Makes Merlin AWESOME? 🎨",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = CloudWhite,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    MagicalFeatureItem(
                        emoji = "💬",
                        title = "Chat & Ask Questions!",
                        description = "Talk to Merlin about ANYTHING! Math, science, stories, or just for fun! 🤖✨"
                    )
                    
                    MagicalFeatureItem(
                        emoji = "🎮",
                        title = "Play Magical Games!",
                        description = "Educational games that are actually FUN! Learn while you play and earn cool rewards! 🏆🎪"
                    )
                    
                    MagicalFeatureItem(
                        emoji = "🧠",
                        title = "Learn Amazing Things!",
                        description = "Discover new subjects, solve puzzles, and become a learning CHAMPION! 🌟📚"
                    )
                    
                    MagicalFeatureItem(
                        emoji = "🎉",
                        title = "Have Epic Adventures!",
                        description = "Every day brings new magical learning adventures with your wizard friend! 🦄🌈"
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // 🚀 MAGICAL START BUTTON 🚀
            Card(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                shape = RoundedCornerShape(40.dp),
                colors = CardDefaults.cardColors(
                    containerColor = StardustPink
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "🚀",
                            fontSize = 36.sp,
                            modifier = Modifier.scale(wizardScale * 0.8f)
                        )
                        Text(
                            text = "START THE MAGIC!",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = CloudWhite,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "🌟",
                            fontSize = 36.sp,
                            modifier = Modifier.rotate(sparkleRotation * 0.5f)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun MagicalFeatureItem(
    emoji: String,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            SunshineYellow,
                            PhoenixOrange
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = emoji,
                fontSize = 28.sp
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = CloudWhite
            )
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                color = CloudWhite.copy(alpha = 0.9f),
                fontWeight = FontWeight.Medium,
                lineHeight = 22.sp
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WelcomeScreenPreview() {
    MerlinTheme {
        WelcomeScreen(
            onContinue = { }
        )
    }
} 