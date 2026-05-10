package com.monkeys.projectmanager

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monkeys.projectmanager.models.Project
import com.monkeys.projectmanager.utils.ApiAdapter
import com.monkeys.projectmanager.utils.timeZone
import kotlin.math.max
import kotlin.time.Clock

private const val DAY_MS = 24L * 60L * 60L * 1000L

private data class ChartPoint(
    val label: String,
    val value: Float
)

@Composable
fun MorningReviewScreen(
    refreshKey: Int,
    modifier: Modifier = Modifier
) {
    var projects by remember { mutableStateOf<List<Project>>(emptyList()) }

    LaunchedEffect(refreshKey) {
        projects = ApiAdapter.getProjects()
    }

    val days = remember {
        val today = todayDayIndex()
        (13 downTo 0).map { today - it }
    }
    val openProjectPoints = remember(projects, days) {
        val today = todayDayIndex()
        days.map { day ->
            ChartPoint(
                label = relativeDayLabel(today - day),
                value = projects.count { it.createdDate <= endOfDay(day) }.toFloat()
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Утренний разбор",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.Black,
            fontWeight = FontWeight.Bold
        )

        ReviewChartCard(
            title = "Открытые проекты по дням",
            points = openProjectPoints,
            valueFormatter = { it.toInt().toString() }
        )
    }
}

@Composable
private fun ReviewChartCard(
    title: String,
    points: List<ChartPoint>,
    valueFormatter: (Float) -> String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = Color.Black)
            LineChart(points, valueFormatter)
        }
    }
}

@Composable
private fun LineChart(
    points: List<ChartPoint>,
    valueFormatter: (Float) -> String
) {
    val maxValue = max(1f, points.maxOfOrNull { it.value } ?: 1f)
    Box(Modifier.fillMaxWidth().height(240.dp)) {
        Canvas(Modifier.fillMaxSize().padding(bottom = 32.dp, top = 8.dp, end = 8.dp)) {
            val left = 36f
            val bottom = size.height - 24f
            val top = 16f
            val width = size.width - left - 8f
            val height = bottom - top
            val stepX = if (points.size <= 1) width else width / (points.size - 1)

            drawLine(Color(0xFFE0E0E0), Offset(left, top), Offset(left, bottom), strokeWidth = 2f)
            drawLine(Color(0xFFE0E0E0), Offset(left, bottom), Offset(size.width, bottom), strokeWidth = 2f)

            val offsets = points.mapIndexed { index, point ->
                val x = left + stepX * index
                val y = bottom - (point.value / maxValue) * height
                Offset(x, y)
            }

            offsets.zipWithNext().forEach { (from, to) ->
                drawLine(
                    color = Color(0xFF3B2D60),
                    start = from,
                    end = to,
                    strokeWidth = 5f,
                    cap = StrokeCap.Round
                )
            }
            offsets.forEach {
                drawCircle(Color.White, radius = 8f, center = it)
                drawCircle(Color(0xFF3B2D60), radius = 8f, center = it, style = Stroke(width = 4f))
            }
        }

        Row(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(start = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            points.forEachIndexed { index, point ->
                if (index % 2 == 0) {
                    Text(point.label, fontSize = 10.sp, color = Color.Gray, textAlign = TextAlign.Center)
                } else {
                    Spacer(Modifier.width(1.dp))
                }
            }
        }

        Text(
            text = valueFormatter(maxValue),
            modifier = Modifier.align(Alignment.TopStart),
            fontSize = 11.sp,
            color = Color.Gray
        )
    }
}

private fun todayDayIndex(): Long = dayIndex(Clock.System.now().toEpochMilliseconds())

private fun dayIndex(timestamp: Long): Long = (timestamp + timeZone) / DAY_MS

private fun endOfDay(dayIndex: Long): Long = dayIndex * DAY_MS - timeZone + DAY_MS - 1

private fun relativeDayLabel(daysAgo: Long): String {
    return if (daysAgo == 0L) "0д" else "-${daysAgo}д"
}
