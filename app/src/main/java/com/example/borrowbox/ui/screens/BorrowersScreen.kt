package com.example.borrowbox.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.borrowbox.data.local.BorrowerEntity
import com.example.borrowbox.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BorrowersScreen(
    vm: MainViewModel,
    onBorrowerClick: (String) -> Unit,
    onBack: () -> Unit
) {
    val borrowers by vm.allBorrowers.collectAsState()
    var borrowerToDelete by remember { mutableStateOf<BorrowerEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Borrowers") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { p ->
        if (borrowers.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(p), contentAlignment = Alignment.Center) {
                Text("No borrower records found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(p),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(borrowers) { borrower ->
                    val borrowingsFlow = remember(borrower.name) { vm.getBorrowingsForUser(borrower.name) }
                    val items by borrowingsFlow.collectAsState()
                    
                    BorrowerAnalyticsCard(
                        borrower = borrower,
                        totalBorrowed = items.size,
                        activeCount = items.count { !it.isReturned && !it.isArchived },
                        onClick = { onBorrowerClick(borrower.name) },
                        onDelete = { borrowerToDelete = borrower }
                    )
                }
            }
        }
    }

    if (borrowerToDelete != null) {
        AlertDialog(
            onDismissRequest = { borrowerToDelete = null },
            title = { Text("Delete Borrower") },
            text = { Text("Are you sure you want to delete ${borrowerToDelete?.name}? This will not delete their borrowing history, but they will be removed from this list.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        borrowerToDelete?.let { vm.deleteBorrower(it) }
                        borrowerToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { borrowerToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun BorrowerAnalyticsCard(
    borrower: BorrowerEntity,
    totalBorrowed: Int,
    activeCount: Int,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        borrower.name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(borrower.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(borrower.contactNumber ?: "No contact provided", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "$activeCount Active", 
                        style = MaterialTheme.typography.labelMedium, 
                        color = if (activeCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text("$totalBorrowed Total", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Borrower",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
