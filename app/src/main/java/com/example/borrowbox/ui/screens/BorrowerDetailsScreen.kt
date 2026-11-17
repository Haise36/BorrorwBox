package com.example.borrowbox.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.borrowbox.data.model.Borrowing
import com.example.borrowbox.data.model.calculateCalendarDaysBetween
import com.example.borrowbox.ui.theme.LocalStatusColors
import com.example.borrowbox.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BorrowerDetailsScreen(
    vm: MainViewModel,
    borrowerName: String,
    onBorrowingClick: (Borrowing) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val statusColors = LocalStatusColors.current

    val borrowingsFlow = remember(borrowerName) { vm.getBorrowingsForUser(borrowerName) }
    val borrowings by borrowingsFlow.collectAsState()
    val allBorrowers by vm.allBorrowers.collectAsState()
    
    val borrowerEntity = remember(borrowerName, allBorrowers) {
        allBorrowers.find { it.name.equals(borrowerName, ignoreCase = true) }
    }

    val contactNumber = borrowerEntity?.contactNumber ?: borrowings.firstOrNull { !it.contactNumber.isNull_or_blank() }?.contactNumber
    val classSection = borrowerEntity?.classSection ?: borrowings.firstOrNull { !it.classSection.isNull_or_blank() }?.classSection

    val totalBorrowed = borrowings.size
    val currentlyOut = borrowings.count { !it.isReturned && !it.isArchived }
    val returnedLate = borrowings.count { 
        it.isReturned && it.returnedDate != null && calculateCalendarDaysBetween(it.expectedReturnDate, it.returnedDate) > 0 
    }
    val totalReturned = borrowings.count { it.isReturned }
    val onTimeReturned = borrowings.count { 
        it.isReturned && (it.returnedDate == null || calculateCalendarDaysBetween(it.expectedReturnDate, it.returnedDate) <= 0) 
    }
    val reliabilityPercentage = if (totalReturned == 0) 100 else ((onTimeReturned * 100) / totalReturned)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(borrowerName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { p ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(p),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Borrower Profile Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = androidx.compose.foundation.shape.CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(borrowerName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                if (!classSection.isNull_or_blank()) {
                                    Text(classSection ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        if (!contactNumber.isNull_or_blank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$contactNumber"))
                                        context.startActivity(dialIntent)
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Call")
                                }

                                OutlinedButton(
                                    onClick = {
                                        val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$contactNumber"))
                                        context.startActivity(smsIntent)
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Comment, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("SMS")
                                }
                            }
                        }

                        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // Stats Grid
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            BorrowerMetricCard("Total", totalBorrowed.toString(), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                            BorrowerMetricCard("Active", currentlyOut.toString(), statusColors.warning, Modifier.weight(1f))
                            BorrowerMetricCard("Late", returnedLate.toString(), statusColors.error, Modifier.weight(1f))
                            BorrowerMetricCard("Reliability", "$reliabilityPercentage%", statusColors.success, Modifier.weight(1f))
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Borrowing History (${borrowings.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(borrowings) { item ->
                SwipeableBorrowingCard(
                    borrowing = item,
                    onClick = { onBorrowingClick(item) },
                    onMarkAsReturned = { vm.markReturned(item) },
                    onArchive = { vm.toggleArchived(item) }
                )
            }
        }
    }
}

@Composable
private fun BorrowerMetricCard(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun String?.isNull_or_blank(): Boolean = this == null || this.isBlank()
