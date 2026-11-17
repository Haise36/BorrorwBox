package com.example.borrowbox.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.borrowbox.ui.theme.LocalStatusColors
import com.example.borrowbox.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(vm: MainViewModel) {
    val stats by vm.analytics.collectAsState()
    val allBorrowings by vm.allBorrowings.collectAsState()
    val statusColors = LocalStatusColors.current
    val primaryColor = MaterialTheme.colorScheme.primary

    val monthlyData = remember(allBorrowings) {
        val calendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("MMM", Locale.getDefault())
        val result = mutableListOf<Pair<String, Int>>()
        for (i in 5 downTo 0) {
            val cal = (calendar.clone() as Calendar).apply { add(Calendar.MONTH, -i) }
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH)
            val monthLabel = sdf.format(cal.time)
            val count = allBorrowings.count { borrowing ->
                val bCal = Calendar.getInstance().apply { timeInMillis = borrowing.dateBorrowed }
                bCal.get(Calendar.YEAR) == year && bCal.get(Calendar.MONTH) == month
            }
            result.add(Pair(monthLabel, count))
        }
        result
    }

    val lateReturnCount = remember(allBorrowings) {
        allBorrowings.count { item ->
            item.isReturned && item.returnedDate != null && item.returnedDate > item.expectedReturnDate
        }
    }
    val lateReturnRate = remember(stats.returnedLoans, lateReturnCount) {
        if (stats.returnedLoans == 0) 0 else ((lateReturnCount * 100) / stats.returnedLoans)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Insights & Analytics") }
            )
        }
    ) { p ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(p)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            
            // Major Analytics Card
            AnalyticHighlightCard(
                label = "Total Loan Records",
                value = stats.totalLoans.toString(),
                icon = Icons.Default.Inventory2,
                color = MaterialTheme.colorScheme.primary
            )

            // Monthly Borrowing Trend (Compose Canvas Bar Chart)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = MaterialTheme.shapes.large
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Monthly Activity Trend", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = primaryColor)
                    Spacer(Modifier.height(16.dp))
                    MonthlyBarChartCanvas(monthlyData = monthlyData, barColor = primaryColor)
                }
            }

            // Statistical Grid
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                AnalyticMiniCard(
                    label = "Active",
                    value = stats.activeLoans.toString(),
                    icon = Icons.Default.Schedule,
                    color = statusColors.info,
                    modifier = Modifier.weight(1f)
                )
                AnalyticMiniCard(
                    label = "Returned",
                    value = stats.returnedLoans.toString(),
                    icon = Icons.Default.CheckCircle,
                    color = statusColors.success,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                AnalyticMiniCard(
                    label = "Overdue",
                    value = stats.overdueLoans.toString(),
                    icon = Icons.Default.PriorityHigh,
                    color = statusColors.error,
                    modifier = Modifier.weight(1f)
                )
                AnalyticMiniCard(
                    label = "Upcoming",
                    value = stats.dueSoonLoans.toString(),
                    icon = Icons.Default.Event,
                    color = statusColors.warning,
                    modifier = Modifier.weight(1f)
                )
            }

            // Detailed Metrics
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                MetricRow("On-Time Return Rate", "${stats.returnRatePercentage}%", Icons.Default.Percent)
                MetricRow("Late Return Rate", "$lateReturnRate%", Icons.Default.RunningWithErrors)
                MetricRow("Top Category", stats.topCategory, Icons.Default.Category)
                MetricRow("Most Borrowed", stats.topItem, Icons.AutoMirrored.Filled.Assignment)
                MetricRow("Avg. Loan Duration", "${String.format(Locale.getDefault(), "%.1f", stats.averageDurationDays)} days", Icons.Default.Timelapse)
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun MonthlyBarChartCanvas(monthlyData: List<Pair<String, Int>>, barColor: Color) {
    val maxVal = (monthlyData.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)
    
    Column {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            val width = size.width
            val height = size.height
            val count = monthlyData.size
            val barWidth = (width / count) * 0.5f
            val spacing = width / count

            for (i in 0 until count) {
                val value = monthlyData[i].second
                val barHeight = (value.toFloat() / maxVal.toFloat()) * (height - 30.dp.toPx())
                val x = i * spacing + (spacing - barWidth) / 2f
                val y = height - barHeight - 20.dp.toPx()

                // Draw background line grid
                drawLine(
                    color = Color.Gray.copy(alpha = 0.15f),
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1.dp.toPx()
                )

                // Draw bar
                drawRect(
                    color = barColor,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight.coerceAtLeast(4.dp.toPx()))
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            monthlyData.forEach { (label, count) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Text("$count", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun AnalyticHighlightCard(label: String, value: String, icon: ImageVector, color: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.8f))
                Text(value, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black, color = Color.White)
            }
            Icon(icon, null, modifier = Modifier.size(64.dp), tint = Color.White.copy(alpha = 0.2f))
        }
    }
}

@Composable
private fun AnalyticMiniCard(label: String, value: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(12.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium, modifier = Modifier.size(40.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}
