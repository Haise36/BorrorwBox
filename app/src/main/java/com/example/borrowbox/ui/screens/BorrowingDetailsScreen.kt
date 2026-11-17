package com.example.borrowbox.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.borrowbox.data.model.*
import com.example.borrowbox.ui.theme.LocalStatusColors
import com.example.borrowbox.viewmodel.MainViewModel
import com.example.borrowbox.utils.*
import com.example.borrowbox.ui.components.BorrowingPhotoView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BorrowingDetailsScreen(
    vm: MainViewModel,
    borrowingId: Long,
    onEdit: (Long) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val allItems by vm.allBorrowings.collectAsState()
    val borrowing = remember(borrowingId, allItems) { allItems.find { it.id == borrowingId } }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showExtendPicker by remember { mutableStateOf(false) }
    var showReturnConfirm by remember { mutableStateOf(false) }
    var selectedReturnCondition by remember { mutableStateOf("Good") }

    if (borrowing == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    val status = borrowing.calculateStatus()
    val statusColors = LocalStatusColors.current
    val statusColor = when (status) {
        BorrowingStatus.ACTIVE -> statusColors.info
        BorrowingStatus.DUE_SOON -> statusColors.warning
        BorrowingStatus.DUE_TODAY -> statusColors.warning
        BorrowingStatus.OVERDUE -> statusColors.error
        BorrowingStatus.RETURNED -> statusColors.success
        BorrowingStatus.ARCHIVED -> MaterialTheme.colorScheme.outline
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Borrowing Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val calendarIntent = Intent(Intent.ACTION_INSERT).apply {
                            data = CalendarContract.Events.CONTENT_URI
                            putExtra(CalendarContract.Events.TITLE, "Return ${borrowing.itemName}")
                            putExtra(CalendarContract.Events.DESCRIPTION, "BorrowBox Reminder: Return ${borrowing.itemName} to ${borrowing.borrowerName}.")
                            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, borrowing.expectedReturnDate)
                            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, borrowing.expectedReturnDate + 3600000L)
                        }
                        context.startActivity(calendarIntent)
                    }) {
                        Icon(Icons.Default.EventAvailable, contentDescription = "Add to Calendar")
                    }
                    IconButton(onClick = {
                        val shareText = """
                            BorrowBox
                            
                            Item: ${borrowing.itemName}
                            Borrowed by: ${borrowing.borrowerName}
                            Date Borrowed: ${formatTimestamp(borrowing.dateBorrowed)}
                            Expected Return: ${formatTimestamp(borrowing.expectedReturnDate)}
                            Status: ${borrowing.getStatusLabel()}
                        """.trimIndent()
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "BorrowBox Item Details")
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share via"))
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                    IconButton(onClick = { vm.togglePinned(borrowing) }) {
                        Icon(
                            imageVector = if (borrowing.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                            contentDescription = "Pin",
                            tint = if (borrowing.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        },
        bottomBar = {
            if (!borrowing.isReturned && !borrowing.isArchived) {
                Surface(tonalElevation = 8.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = { showReturnConfirm = true },
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Mark Returned")
                        }
                    }
                }
            }
        }
    ) { p ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(p)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Photo View
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                if (borrowing.photoUri != null) {
                    AsyncImage(
                        model = borrowing.photoUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = getIconForCategory(borrowing.category),
                            contentDescription = null,
                            modifier = Modifier.size(100.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                // Headline Section
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(borrowing.itemName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                        Text(borrowing.category, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                    }
                    Surface(color = statusColor.copy(alpha = 0.1f), shape = MaterialTheme.shapes.medium) {
                        Text(
                            text = borrowing.getStatusLabel(), 
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = statusColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.fillMaxWidth(), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Borrower Information & Contact Buttons
                DetailSection(title = "Borrower Information") {
                    InfoRow(Icons.Default.Person, "Name", borrowing.borrowerName)
                    borrowing.contactNumber?.let { contact ->
                        InfoRow(Icons.Default.Phone, "Contact", contact)
                        
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$contact"))
                                    context.startActivity(dialIntent)
                                },
                                modifier = Modifier.weight(1f),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Call")
                            }

                            OutlinedButton(
                                onClick = {
                                    val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$contact"))
                                    context.startActivity(smsIntent)
                                },
                                modifier = Modifier.weight(1f),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Comment, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("SMS")
                            }
                        }

                        Button(
                            onClick = {
                                val message = "Hi ${borrowing.borrowerName}, just a friendly reminder to return my ${borrowing.itemName} by ${formatTimestamp(borrowing.expectedReturnDate)}. Thanks!"
                                val reminderIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$contact")).apply {
                                    putExtra("sms_body", message)
                                }
                                context.startActivity(reminderIntent)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Send SMS Reminder")
                        }
                    }
                }

                DetailSection(title = "Item Conditions") {
                    InfoRow(Icons.Default.Tune, "Condition when Lent", borrowing.conditionLent ?: "Good")
                    borrowing.conditionReturned?.let {
                        InfoRow(Icons.Default.CheckCircleOutline, "Condition when Returned", it)
                    }
                }

                DetailSection(title = "Timeline") {
                    InfoRow(Icons.Default.CalendarToday, "Borrowed", formatTimestamp(borrowing.dateBorrowed))
                    InfoRow(
                        Icons.Default.Event, 
                        if (borrowing.isReturned) "Returned" else "Expected Return", 
                        formatTimestamp(borrowing.returnedDate ?: borrowing.expectedReturnDate),
                        action = if (!borrowing.isReturned) { { IconButton(onClick = { showExtendPicker = true }) { Icon(Icons.Default.EditCalendar, null, tint = MaterialTheme.colorScheme.primary) } } } else null
                    )
                }

                if (borrowing.notes.isNotBlank()) {
                    DetailSection(title = "Notes") {
                        Text(borrowing.notes, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Secondary Action Buttons
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { onEdit(borrowing.id) },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(Icons.Default.Edit, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Edit")
                    }
                    OutlinedButton(
                        onClick = { vm.toggleArchived(borrowing) },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(if (borrowing.isArchived) Icons.Default.Unarchive else Icons.Default.Archive, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (borrowing.isArchived) "Restore" else "Archive")
                    }
                }
                
                Spacer(Modifier.height(40.dp))
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Record") },
            text = { Text("Permanently delete this borrowing? This action cannot be undone.") },
            confirmButton = {
                Button(onClick = {
                    vm.delete(borrowing)
                    onBack()
                }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showReturnConfirm) {
        AlertDialog(
            onDismissRequest = { showReturnConfirm = false },
            title = { Text("Confirm Return") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Mark '${borrowing.itemName}' as returned?")
                    Text("Select Return Condition:", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        BorrowingConstants.conditions.take(3).forEach { cond ->
                            FilterChip(
                                selected = selectedReturnCondition == cond,
                                onClick = { selectedReturnCondition = cond },
                                label = { Text(cond) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    vm.save(borrowing.copy(
                        isReturned = true,
                        returnedDate = System.currentTimeMillis(),
                        conditionReturned = selectedReturnCondition
                    ))
                    showReturnConfirm = false
                }) { Text("Confirm & Return") }
            },
            dismissButton = {
                TextButton(onClick = { showReturnConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showExtendPicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = borrowing.expectedReturnDate)
        DatePickerDialog(
            onDismissRequest = { showExtendPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { vm.extendReturnDate(borrowing, it) }
                    showExtendPicker = false
                }) { Text("Update") }
            },
            dismissButton = {
                TextButton(onClick = { showExtendPicker = false }) { Text("Cancel") }
            }
        ) { DatePicker(state = datePickerState) }
    }
}

@Composable
private fun DetailSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        content()
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String, action: (@Composable () -> Unit)? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small, modifier = Modifier.size(36.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        }
        action?.invoke()
    }
}
