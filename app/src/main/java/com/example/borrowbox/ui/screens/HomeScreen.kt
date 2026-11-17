package com.example.borrowbox.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.borrowbox.data.model.Borrowing
import com.example.borrowbox.data.model.BorrowingStatus
import com.example.borrowbox.data.model.BorrowingConstants
import com.example.borrowbox.ui.components.Dashboard
import com.example.borrowbox.ui.components.EmptyStateView
import com.example.borrowbox.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: MainViewModel,
    onAddBorrowing: () -> Unit,
    onBorrowingClick: (Borrowing) -> Unit,
    onBorrowersClick: () -> Unit,
    onStatClick: (String) -> Unit
) {
    val greeting = vm.generateGreeting()
    val summary by vm.dashboardSummary.collectAsState()
    val urgentItems by vm.urgentBorrowings.collectAsState()
    val searchQuery by vm.searchQuery.collectAsState()
    val activeItems by vm.activeBorrowings.collectAsState()
    
    val statusFilter by vm.statusFilter.collectAsState()
    val categoryFilter by vm.categoryFilter.collectAsState()
    
    var itemToReturn by remember { mutableStateOf<Borrowing?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddBorrowing,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = MaterialTheme.shapes.large
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Borrowing")
            }
        }
    ) { p ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(p)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(Modifier.height(16.dp))
                Dashboard(
                    greeting = greeting, 
                    stats = summary,
                    onStatClick = onStatClick
                )
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onBorrowersClick,
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                    ) {
                        Icon(Icons.Default.People, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Borrowers")
                    }
                    Button(
                        onClick = { onStatClick("all") },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer, contentColor = MaterialTheme.colorScheme.onTertiaryContainer)
                    ) {
                        Icon(Icons.Default.List, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("View All")
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { vm.updateSearch(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Quick search...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                )
            }

            // In-line filtering for Home
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = statusFilter == null,
                            onClick = { vm.updateStatusFilter(null) },
                            label = { Text("All Status") }
                        )
                    }
                    val statusFilters = listOf(BorrowingStatus.ACTIVE, BorrowingStatus.DUE_TODAY, BorrowingStatus.DUE_SOON, BorrowingStatus.OVERDUE)
                    items(statusFilters) { status ->
                        FilterChip(
                            selected = statusFilter == status,
                            onClick = { vm.updateStatusFilter(if (statusFilter == status) null else status) },
                            label = { Text(status.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
            }

            if (searchQuery.isNotBlank() || statusFilter != null || categoryFilter != null) {
                if (activeItems.isNotEmpty()) {
                    item {
                        Text(
                            text = "Search Results",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    items(activeItems) { item ->
                        SwipeableBorrowingCard(
                            borrowing = item,
                            onClick = { onBorrowingClick(item) },
                            onMarkAsReturned = { itemToReturn = item },
                            onArchive = { vm.toggleArchived(item) }
                        )
                    }
                } else {
                    val emptyTitle = when {
                        statusFilter == BorrowingStatus.DUE_TODAY -> "No active due today"
                        statusFilter == BorrowingStatus.OVERDUE -> "No overdue items"
                        statusFilter == BorrowingStatus.DUE_SOON -> "No items due soon"
                        statusFilter == BorrowingStatus.ACTIVE -> "No active borrowings"
                        categoryFilter != null -> "No records in $categoryFilter"
                        else -> "No matches found"
                    }
                    item {
                        EmptyStateView(
                            icon = Icons.Default.SearchOff,
                            title = emptyTitle,
                            subtitle = "Try different search terms or reset filters."
                        )
                    }
                }
            } else {
                if (urgentItems.isNotEmpty()) {
                    item {
                        Text(
                            text = "Needs Attention",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    items(urgentItems) { item ->
                        SwipeableBorrowingCard(
                            borrowing = item,
                            onClick = { onBorrowingClick(item) },
                            onMarkAsReturned = { itemToReturn = item },
                            onArchive = { vm.toggleArchived(item) }
                        )
                    }
                }
                
                if (activeItems.isNotEmpty()) {
                    item {
                        Text(
                            text = "Recent Borrowings",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    items(activeItems) { item ->
                        if (item !in urgentItems) {
                            SwipeableBorrowingCard(
                                borrowing = item,
                                onClick = { onBorrowingClick(item) },
                                onMarkAsReturned = { itemToReturn = item },
                                onArchive = { vm.toggleArchived(item) }
                            )
                        }
                    }
                } else if (urgentItems.isEmpty()) {
                    item {
                        EmptyStateView(
                            icon = Icons.Default.Inventory2,
                            title = "Clear for now!",
                            subtitle = "You have no active borrowings recorded.",
                            action = {
                                Button(
                                    onClick = onAddBorrowing,
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Add First Borrowing")
                                }
                            }
                        )
                    }
                }
            }
            
            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (itemToReturn != null) {
        AlertDialog(
            onDismissRequest = { itemToReturn = null },
            title = { Text("Confirm Return") },
            text = { Text("Mark '${itemToReturn?.itemName}' as returned?") },
            confirmButton = {
                Button(onClick = {
                    itemToReturn?.let { vm.markReturned(it) }
                    itemToReturn = null
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { itemToReturn = null }) { Text("Cancel") }
            }
        )
    }
}
