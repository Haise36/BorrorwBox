package com.example.borrowbox.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.borrowbox.ui.theme.LocalStatusColors
import com.example.borrowbox.viewmodel.DashboardSummary

@Composable
fun Dashboard(
    greeting: String,
    stats: DashboardSummary,
    onStatClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColors = LocalStatusColors.current
    
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column {
            Text(
                text = greeting,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Welcome to your BorrowBox",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Dynamic Summary Cards
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryCard(
                    title = "Total Loans",
                    count = stats.totalCount.toString(),
                    icon = Icons.Default.AllInbox,
                    color = MaterialTheme.colorScheme.primary,
                    onClick = { onStatClick("all") },
                    modifier = Modifier.weight(1.3f)
                )
                SummaryCard(
                    title = "Due Today",
                    count = stats.dueTodayCount.toString(),
                    icon = Icons.Default.Event,
                    color = Color(0xFFFF5722), // Deep Orange
                    onClick = { onStatClick("due_today") },
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryCard(
                    title = "Active",
                    count = stats.activeCount.toString(),
                    icon = Icons.Default.Inventory2,
                    color = statusColors.info,
                    onClick = { onStatClick("borrowed") },
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    title = "Returned",
                    count = stats.returnedCount.toString(),
                    icon = Icons.Default.CheckCircle,
                    color = statusColors.success,
                    onClick = { onStatClick("returned") },
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryCard(
                    title = "Overdue",
                    count = stats.overdueCount.toString(),
                    icon = Icons.Default.PriorityHigh,
                    color = statusColors.error,
                    onClick = { onStatClick("overdue") },
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    title = "Due Soon",
                    count = stats.dueSoonCount.toString(),
                    icon = Icons.Default.Schedule,
                    color = statusColors.warning,
                    onClick = { onStatClick("due_soon") },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    count: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = MaterialTheme.shapes.large,
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = color.copy(alpha = 0.1f),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = count,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
