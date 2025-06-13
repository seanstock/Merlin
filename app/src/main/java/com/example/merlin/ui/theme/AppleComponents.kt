package com.example.merlin.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 🍎 APPLE DESIGN SYSTEM COMPONENTS 🍎
// Reusable components that match Apple's design language

// Additional Typography Styles
val AppleBody = TextStyle(
    fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
    fontWeight = FontWeight.Normal,
    fontSize = 17.sp,
    lineHeight = 22.sp,
    letterSpacing = (-0.41).sp
)

val AppleSubheadline = TextStyle(
    fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
    fontWeight = FontWeight.Normal,
    fontSize = 15.sp,
    lineHeight = 20.sp,
    letterSpacing = (-0.24).sp
)

val AppleHeadline = TextStyle(
    fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
    fontWeight = FontWeight.Bold,
    fontSize = 20.sp,
    lineHeight = 25.sp,
    letterSpacing = 0.38.sp
)

val AppleFootnote = TextStyle(
    fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
    lineHeight = 18.sp,
    letterSpacing = (-0.08).sp
)

val AppleCaption = TextStyle(
    fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
    fontWeight = FontWeight.Medium,
    fontSize = 12.sp,
    lineHeight = 16.sp,
    letterSpacing = 0.sp
)

val AppleCallout = TextStyle(
    fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
    fontWeight = FontWeight.Normal,
    fontSize = 16.sp,
    lineHeight = 21.sp,
    letterSpacing = (-0.32).sp
)

// Additional Colors
val AppleSystemGray6 = Color(0xFFF2F2F7)

/**
 * Apple-style card with subtle shadow and rounded corners
 */
@Composable
fun AppleCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    elevation: Int = 1, // 1 = light, 2 = medium, 3 = strong
    cornerRadius: Int = 12, // Apple's standard corner radius
    content: @Composable ColumnScope.() -> Unit
) {
    val shadowColor = when (elevation) {
        1 -> AppleShadowLight
        2 -> AppleShadowMedium
        3 -> AppleShadowStrong
        else -> AppleShadowLight
    }
    
    Card(
        modifier = modifier
            .shadow(
                elevation = (elevation * 2).dp,
                shape = RoundedCornerShape(cornerRadius.dp),
                ambientColor = shadowColor,
                spotColor = shadowColor
            ),
        shape = RoundedCornerShape(cornerRadius.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp // We handle elevation with shadow
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

/**
 * Apple-style button with system blue color and proper typography
 */
@Composable
fun AppleButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: AppleButtonStyle = AppleButtonStyle.Primary
) {
    val colors = when (style) {
        AppleButtonStyle.Primary -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
        AppleButtonStyle.Secondary -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
        AppleButtonStyle.Destructive -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    
    Button(
        onClick = onClick,
        modifier = modifier
            .height(44.dp) // Apple's standard button height
            .fillMaxWidth(),
        enabled = enabled,
        colors = colors,
        shape = RoundedCornerShape(10.dp), // Apple's button corner radius
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp
        )
    ) {
        Text(
            text = text,
            style = AppleButtonTextLarge,
            textAlign = TextAlign.Center
        )
    }
}

enum class AppleButtonStyle {
    Primary,
    Secondary, 
    Destructive
}

/**
 * Apple-style section header with proper typography and spacing
 */
@Composable
fun AppleSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = AppleLargeTitle,
            color = ApplePrimaryLabel
        )
        
        subtitle?.let {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = it,
                style = AppleCardSubtitle,
                color = AppleSecondaryLabel
            )
        }
    }
}

/**
 * Apple-style list item with proper spacing and typography
 */
@Composable
fun AppleListItem(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val clickableModifier = if (onClick != null) {
        modifier.clickable { onClick() }
    } else {
        modifier
    }
    
    Row(
        modifier = clickableModifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        leading?.invoke()
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Normal
                ),
                color = ApplePrimaryLabel
            )
            
            subtitle?.let {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppleSecondaryLabel
                )
            }
        }
        
        trailing?.invoke()
    }
}

/**
 * Apple-style segmented control (tabs)
 */
@Composable
fun AppleSegmentedControl(
    selectedIndex: Int,
    items: List<String>,
    onSelectionChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(
                color = AppleGray5,
                shape = RoundedCornerShape(10.dp)
            )
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        items.forEachIndexed { index, item ->
            val isSelected = index == selectedIndex
            val backgroundColor = if (isSelected) {
                AppleSystemBackground
            } else {
                Color.Transparent
            }
            val textColor = if (isSelected) {
                ApplePrimaryLabel
            } else {
                AppleSecondaryLabel
            }
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        color = backgroundColor,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { onSelectionChanged(index) }
                    .padding(vertical = 8.dp, horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    ),
                    color = textColor,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Apple-style text field with clean design
 */
@Composable
fun AppleTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "",
    placeholder: String = "",
    enabled: Boolean = true,
    singleLine: Boolean = true,
    readOnly: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = if (label.isNotEmpty()) {{ Text(label) }} else null,
        placeholder = if (placeholder.isNotEmpty()) {{ 
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }} else null,
        enabled = enabled,
        readOnly = readOnly,
        singleLine = singleLine,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        trailingIcon = trailingIcon,
        isError = isError,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onSurface
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(10.dp)
    )
}

/**
 * Apple-style separator line
 */
@Composable
fun AppleSeparator(
    modifier: Modifier = Modifier,
    color: Color = AppleSeparator,
    thickness: Int = 1
) {
    HorizontalDivider(
        modifier = modifier,
        color = color,
        thickness = thickness.dp
    )
}

/**
 * Apple-style spacing values for consistent layouts
 */
object AppleSpacing {
    val xs = 4.dp          // Tiny spacing
    val small = 8.dp       // Small spacing
    val medium = 16.dp     // Standard spacing (Apple's base unit)
    val large = 24.dp      // Large spacing
    val extraLarge = 32.dp // Extra large spacing
    val xl = 32.dp         // Extra large spacing (alias)
    val xxl = 48.dp        // Section spacing
}

/**
 * Apple-style corner radius values
 */
object AppleCornerRadius {
    val small = 6.dp    // Small elements
    val medium = 10.dp  // Buttons, text fields
    val large = 12.dp   // Cards, containers
    val xl = 16.dp      // Large cards
    val xxl = 20.dp     // Modal sheets
} 