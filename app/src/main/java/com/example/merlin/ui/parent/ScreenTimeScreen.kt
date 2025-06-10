package com.example.merlin.ui.parent

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.merlin.economy.service.LocalScreenTimeService
import com.example.merlin.ui.accessibility.AccessibilityConstants
import com.example.merlin.ui.theme.*
import java.time.DayOfWeek
import kotlin.math.roundToInt
import androidx.compose.foundation.shape.CircleShape

/**
 * Screen time management screen for parent dashboard
 * TODO: Implement screen time tracking and management functionality
 */
@Composable
fun ScreenTimeScreen(
    modifier: Modifier = Modifier,
    childId: String = "demo_child" // Default for now
) {
    val factory = ScreenTimeViewModelFactory(LocalScreenTimeService())
    val viewModel: ScreenTimeViewModel = viewModel(factory = factory)
    val state by viewModel.state.collectAsState()

    LaunchedEffect(key1 = childId) {
        viewModel.loadScreenTimeData(childId)
    }

    when {
        state.isLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        state.error != null -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Error: ${state.error}", color = Color.Red)
            }
        }
        else -> {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    Text(
                        "Screen Time Management",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = WisdomBlue
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                item { DailyUsageChart(dailyUsage = state.dailyUsage) }
                item { TimeRestrictionsSettings(restrictions = state.timeRestrictions, onLimitChanged = { viewModel.updateDailyLimit(childId, it) }) }
                item { SubjectDistributionChart(distribution = state.subjectDistribution) }
                item { WeeklyScheduler(schedule = state.weeklySchedule, onScheduleChanged = { day, slot -> viewModel.updateWeeklySchedule(childId, day, slot) }) }
            }
        }
    }
}

@Composable
fun DailyUsageChart(dailyUsage: List<DailyUsage>) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(20.dp)) {
            Text("Daily Usage (Last 7 Days)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            Canvas(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                drawBarChart(dailyUsage)
            }
        }
    }
}

fun DrawScope.drawBarChart(dailyUsage: List<DailyUsage>) {
    val maxUsage = dailyUsage.maxOfOrNull { it.usageMinutes } ?: 1
    val barWidth = size.width / (dailyUsage.size * 2)
    dailyUsage.forEachIndexed { index, usage ->
        val barHeight = (usage.usageMinutes.toFloat() / maxUsage) * size.height
        drawRect(
            color = AmberGlow,
            topLeft = Offset(x = (index * 2 + 0.5f) * barWidth, y = size.height - barHeight),
            size = Size(width = barWidth, height = barHeight)
        )
    }
}

@Composable
fun TimeRestrictionsSettings(restrictions: TimeRestrictions, onLimitChanged: (Int) -> Unit) {
    var sliderPosition by remember { mutableStateOf(restrictions.dailyLimitMinutes.toFloat()) }
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(20.dp)) {
            Text("Time Restrictions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            Text("Daily Limit: ${sliderPosition.roundToInt()} minutes", style = MaterialTheme.typography.bodyLarge)
            Slider(
                value = sliderPosition,
                onValueChange = { sliderPosition = it },
                valueRange = 30f..240f,
                onValueChangeFinished = { onLimitChanged(sliderPosition.roundToInt()) }
            )
        }
    }
}

@Composable
fun SubjectDistributionChart(distribution: Map<String, Float>) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(20.dp)) {
            Text("Subject Time Distribution", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth().height(20.dp).clip(RoundedCornerShape(10.dp))) {
                distribution.forEach { (subject, percentage) ->
                    Box(modifier = Modifier.fillMaxHeight().weight(percentage).background(getColorForSubject(subject)))
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                distribution.forEach { (subject, _) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).background(getColorForSubject(subject), CircleShape))
                        Spacer(Modifier.width(4.dp))
                        Text(subject, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

fun getColorForSubject(subject: String): Color {
    return when (subject) {
        "Math" -> WisdomBlue
        "Reading" -> RoyalPurple
        "Science" -> ForestGreen
        "Creative" -> AmberGlow
        else -> Color.Gray
    }
}

@Composable
fun WeeklyScheduler(schedule: Map<DayOfWeek, TimeSlot>, onScheduleChanged: (DayOfWeek, TimeSlot) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(20.dp)) {
            Text("Weekly Schedule", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            DayOfWeek.values().forEach { day ->
                val slot = schedule[day]
                Text(
                    text = "${day.name.take(3).capitalize()}: ${slot?.toString() ?: "No session scheduled"}",
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
} 