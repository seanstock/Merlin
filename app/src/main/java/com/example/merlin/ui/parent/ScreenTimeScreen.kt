package com.example.merlin.ui.parent

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.merlin.config.ServiceLocator
import com.example.merlin.ui.accessibility.AccessibilityConstants
import com.example.merlin.ui.theme.*
import java.time.DayOfWeek
import kotlin.math.*

// Data classes for screen time
data class ScreenTimeData(
    val day: String,
    val minutes: Int
)

data class SubjectTimeData(
    val day: String,
    val subjects: Map<String, Float> // percentage for each subject
)

data class DaySchedule(
    val day: String,
    val startTime: String?,
    val endTime: String?,
    val isEnabled: Boolean
)

/**
 * Screen time management screen with Apple-inspired design
 */
@Composable
fun ScreenTimeScreen(
    modifier: Modifier = Modifier,
    childId: String = "demo_child"
) {
    val screenTimeData = remember { generateFakeScreenTimeData() }
    val subjectData = remember { generateFakeSubjectData() }
    val scheduleData = remember { generateFakeScheduleData() }
    var adaptiveDifficulty by remember { mutableStateOf(0.7f) }
    var maxScreenTime by remember { mutableStateOf(120f) } // minutes

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(AppleSystemBackground)
            .padding(horizontal = AppleSpacing.medium),
        verticalArrangement = Arrangement.spacedBy(AppleSpacing.medium),
        contentPadding = PaddingValues(vertical = AppleSpacing.medium)
    ) {
        item {
            Text(
                text = "Screen Time",
                style = AppleLargeTitle,
                color = ApplePrimaryLabel,
                modifier = Modifier.padding(horizontal = AppleSpacing.small)
            )
        }
        
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppleSpacing.small)
            ) {
                ControlsCard(
                    adaptiveDifficulty = adaptiveDifficulty,
                    onDifficultyChanged = { adaptiveDifficulty = it },
                    maxScreenTime = maxScreenTime,
                    onMaxScreenTimeChanged = { maxScreenTime = it },
                    modifier = Modifier.weight(1f)
                )
                
                CompactWeeklySchedule(
                    scheduleData = scheduleData,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        item {
            ScreenTimeChart(screenTimeData = screenTimeData)
        }
        
        item {
            SubjectDistributionChart(subjectData = subjectData)
        }
    }
}

@Composable
fun ScreenTimeChart(
    screenTimeData: List<ScreenTimeData>,
    modifier: Modifier = Modifier
) {
    AppleCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(AppleSpacing.medium)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.ShowChart,
                    contentDescription = null,
                    tint = AppleBlue,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(AppleSpacing.small))
                Text(
                    text = "Daily Screen Time",
                    style = AppleHeadline,
                    color = ApplePrimaryLabel
                )
            }
            
            Spacer(modifier = Modifier.height(AppleSpacing.small))
            
            Text(
                text = "Past 7 days usage trend",
                style = AppleSubheadline,
                color = AppleSecondaryLabel
            )
            
            Spacer(modifier = Modifier.height(AppleSpacing.medium))
            
            // Chart
            val chartHeight = 150.dp
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight)
            ) {
                drawScreenTimeLineChart(screenTimeData, size)
            }
            
            Spacer(modifier = Modifier.height(AppleSpacing.medium))
            
            // Average display
            val avgMinutes = screenTimeData.map { it.minutes }.average().toInt()
            Text(
                text = "Average: ${avgMinutes} minutes per day",
                style = AppleBody,
                color = AppleSecondaryLabel,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ControlsCard(
    adaptiveDifficulty: Float,
    onDifficultyChanged: (Float) -> Unit,
    maxScreenTime: Float,
    onMaxScreenTimeChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    AppleCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(AppleSpacing.medium)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = AppleIndigo,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(AppleSpacing.small))
                Text(
                    text = "Learning Controls",
                    style = AppleHeadline,
                    color = ApplePrimaryLabel
                )
            }
            
            Spacer(modifier = Modifier.height(AppleSpacing.medium))
            
            // Adaptive Difficulty Section
            Text(
                text = "Adaptive Difficulty",
                style = AppleBody.copy(fontWeight = FontWeight.Medium),
                color = ApplePrimaryLabel
            )
            
            Text(
                text = "Adjusts lesson difficulty and coin acquisition rate based on performance",
                style = AppleCaption,
                color = AppleSecondaryLabel,
                modifier = Modifier.padding(top = 2.dp)
            )
            
            Spacer(modifier = Modifier.height(AppleSpacing.small))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Easy",
                    style = AppleCaption,
                    color = AppleSecondaryLabel
                )
                
                Spacer(modifier = Modifier.width(AppleSpacing.small))
                
                Slider(
                    value = adaptiveDifficulty,
                    onValueChange = onDifficultyChanged,
                    valueRange = 0f..1f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = AppleGreen,
                        activeTrackColor = AppleGreen,
                        inactiveTrackColor = AppleGray4
                    )
                )
                
                Spacer(modifier = Modifier.width(AppleSpacing.small))
                
                Text(
                    text = "Hard",
                    style = AppleCaption,
                    color = AppleSecondaryLabel
                )
            }
            
            Text(
                text = "${getDifficultyLabel(adaptiveDifficulty)} (${(adaptiveDifficulty * 100).toInt()}% coin rate)",
                style = AppleCaption,
                color = ApplePrimaryLabel,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(AppleSpacing.medium))
            
            // Max Screen Time Section
            Text(
                text = "Daily Screen Time Limit",
                style = AppleBody.copy(fontWeight = FontWeight.Medium),
                color = ApplePrimaryLabel
            )
            
            Text(
                text = "Maximum allowed screen time per day",
                style = AppleCaption,
                color = AppleSecondaryLabel,
                modifier = Modifier.padding(top = 2.dp)
            )
            
            Spacer(modifier = Modifier.height(AppleSpacing.small))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "30m",
                    style = AppleCaption,
                    color = AppleSecondaryLabel
                )
                
                Spacer(modifier = Modifier.width(AppleSpacing.small))
                
                Slider(
                    value = maxScreenTime,
                    onValueChange = onMaxScreenTimeChanged,
                    valueRange = 30f..240f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = AppleOrange,
                        activeTrackColor = AppleOrange,
                        inactiveTrackColor = AppleGray4
                    )
                )
                
                Spacer(modifier = Modifier.width(AppleSpacing.small))
                
                Text(
                    text = "4h",
                    style = AppleCaption,
                    color = AppleSecondaryLabel
                )
            }
            
            Text(
                text = "Limit: ${maxScreenTime.toInt()} minutes",
                style = AppleCaption,
                color = ApplePrimaryLabel,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SubjectDistributionChart(
    subjectData: List<SubjectTimeData>,
    modifier: Modifier = Modifier
) {
    AppleCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(AppleSpacing.medium)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.AreaChart,
                    contentDescription = null,
                    tint = ApplePurple,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(AppleSpacing.small))
                Text(
                    text = "Subject Distribution",
                    style = AppleHeadline,
                    color = ApplePrimaryLabel
                )
            }
            
            Spacer(modifier = Modifier.height(AppleSpacing.small))
            
            Text(
                text = "Subject time distribution over past 7 days",
                style = AppleSubheadline,
                color = AppleSecondaryLabel
            )
            
            Spacer(modifier = Modifier.height(AppleSpacing.medium))
            
            // Chart
            val chartHeight = 150.dp
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight)
            ) {
                drawSubjectAreaChart(subjectData, size)
            }
            
            Spacer(modifier = Modifier.height(AppleSpacing.small))
            
            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                getSubjectColors().forEach { (subject, color) ->
                    LegendItem(
                        color = color,
                        label = subject
                    )
                }
            }
        }
    }
}

@Composable
fun CompactWeeklySchedule(
    scheduleData: List<DaySchedule>,
    modifier: Modifier = Modifier
) {
    AppleCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(AppleSpacing.medium)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = AppleIndigo,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Schedule",
                    style = AppleHeadline,
                    color = ApplePrimaryLabel
                )
            }
            
            Spacer(modifier = Modifier.height(AppleSpacing.small))
            
            // Very compact grid layout
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                scheduleData.chunked(2).forEach { rowData ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        rowData.forEach { day ->
                            CompactDayCard(
                                day = day,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Fill remaining space if odd number
                        if (rowData.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CompactDayCard(
    day: DaySchedule,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (day.isEnabled) AppleBlue.copy(alpha = 0.1f) 
                else AppleGray6
            )
            .clickable { /* TODO: Open time picker */ }
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = day.day.take(3),
                style = AppleCaption.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp
                ),
                color = if (day.isEnabled) AppleBlue else AppleSecondaryLabel
            )
            
            Text(
                text = when {
                    day.startTime != null && day.endTime != null -> {
                        if (day.startTime == "8:00 AM" && day.endTime == "8:00 PM") "8-8"
                        else "${day.startTime!!.split(" ")[0].replace(":00", "")}-${day.endTime!!.split(" ")[0].replace(":00", "")}"
                    }
                    !day.isEnabled -> "Off"
                    else -> "Set"
                },
                style = AppleCaption.copy(fontSize = 10.sp),
                color = if (day.isEnabled) ApplePrimaryLabel else AppleSecondaryLabel,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun LegendItem(
    color: Color,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(AppleSpacing.small))
        Text(
            text = label,
            style = AppleCaption,
            color = AppleSecondaryLabel
        )
    }
}

fun DrawScope.drawScreenTimeLineChart(screenTimeData: List<ScreenTimeData>, canvasSize: Size) {
    val padding = 40f
    val chartWidth = canvasSize.width - (padding * 2)
    val chartHeight = canvasSize.height - (padding * 2)
    
    val maxMinutes = screenTimeData.maxOfOrNull { it.minutes }?.toFloat() ?: 1f
    val minMinutes = screenTimeData.minOfOrNull { it.minutes }?.toFloat() ?: 0f
    val range = maxMinutes - minMinutes
    
    val points = screenTimeData.mapIndexed { index, data ->
        val x = padding + (index.toFloat() / (screenTimeData.size - 1)) * chartWidth
        val y = padding + chartHeight - ((data.minutes - minMinutes) / range) * chartHeight
        Offset(x, y)
    }
    
    // Draw line
    if (points.size > 1) {
        for (i in 0 until points.size - 1) {
            drawLine(
                color = AppleBlue,
                start = points[i],
                end = points[i + 1],
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
    
    // Draw points
    points.forEach { point ->
        drawCircle(
            color = AppleBlue,
            radius = 4.dp.toPx(),
            center = point
        )
        drawCircle(
            color = Color.White,
            radius = 2.dp.toPx(),
            center = point
        )
    }
}

fun DrawScope.drawSubjectAreaChart(subjectData: List<SubjectTimeData>, canvasSize: Size) {
    val padding = 40f
    val chartWidth = canvasSize.width - (padding * 2)
    val chartHeight = canvasSize.height - (padding * 2)
    
    val subjects = getSubjectColors().keys.toList()
    val colors = getSubjectColors().values.toList()
    
    subjectData.forEachIndexed { dayIndex, dayData ->
        val x = padding + (dayIndex.toFloat() / (subjectData.size - 1)) * chartWidth
        val dayWidth = chartWidth / subjectData.size
        
        var currentY = padding + chartHeight
        
        subjects.forEachIndexed { subjectIndex, subject ->
            val percentage = dayData.subjects[subject] ?: 0f
            val segmentHeight = (percentage / 100f) * chartHeight
            
            drawRect(
                color = colors[subjectIndex].copy(alpha = 0.8f),
                topLeft = Offset(x - dayWidth/2, currentY - segmentHeight),
                size = Size(dayWidth, segmentHeight)
            )
            
            currentY -= segmentHeight
        }
    }
}

fun generateFakeScreenTimeData(): List<ScreenTimeData> {
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    return days.map { day ->
        ScreenTimeData(
            day = day,
            minutes = (60..120).random()
        )
    }
}

fun generateFakeSubjectData(): List<SubjectTimeData> {
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    return days.map { day ->
        // Generate random percentages that sum to 100
        val math = (20..40).random().toFloat()
        val reading = (15..35).random().toFloat()
        val science = (10..25).random().toFloat()
        val art = (5..15).random().toFloat()
        val music = (5..15).random().toFloat()
        
        val total = math + reading + science + art + music
        
        SubjectTimeData(
            day = day,
            subjects = mapOf(
                "Math" to (math / total) * 100,
                "Reading" to (reading / total) * 100,
                "Science" to (science / total) * 100,
                "Art" to (art / total) * 100,
                "Music" to (music / total) * 100
            )
        )
    }
}

fun generateFakeScheduleData(): List<DaySchedule> {
    return listOf(
        DaySchedule("Monday", "9:00 AM", "11:00 AM", true),
        DaySchedule("Tuesday", "2:00 PM", "4:00 PM", true),
        DaySchedule("Wednesday", "10:00 AM", "12:00 PM", true),
        DaySchedule("Thursday", "3:00 PM", "5:00 PM", true),
        DaySchedule("Friday", "9:00 AM", "11:00 AM", true),
        DaySchedule("Saturday", "8:00 AM", "8:00 PM", true),
        DaySchedule("Sunday", null, null, false)
    )
}

fun getSubjectColors(): Map<String, Color> {
    return mapOf(
        "Math" to AppleBlue,
        "Reading" to AppleGreen,
        "Science" to ApplePurple,
        "Art" to AppleOrange,
        "Music" to AppleRed
    )
}

fun getDifficultyLabel(difficulty: Float): String {
    return when {
        difficulty < 0.3f -> "Beginner"
        difficulty < 0.7f -> "Intermediate"
        else -> "Advanced"
    }
} 